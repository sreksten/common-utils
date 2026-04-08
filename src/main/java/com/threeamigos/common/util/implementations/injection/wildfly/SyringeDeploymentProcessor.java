package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleLoadException;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * DeploymentUnitProcessor that initializes Syringe for each deployment.
 */
public class SyringeDeploymentProcessor implements DeploymentUnitProcessor {

    private static final Pattern HASH_SUFFIX = Pattern.compile("^[0-9a-fA-F]{24,}$");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Index localIndex = deploymentUnit.getAttachment(Attachments.ANNOTATION_INDEX);
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        if (module == null) {
            return;
        }

        List<String> localIndexedClassNames = new ArrayList<String>();
        if (localIndex != null) {
            for (ClassInfo classInfo : localIndex.getKnownClasses()) {
                localIndexedClassNames.add(classInfo.name().toString());
            }
        }

        List<String> compositeIndexedClassNames = new ArrayList<String>();
        if (compositeIndex != null) {
            for (ClassInfo classInfo : compositeIndex.getKnownClasses()) {
                compositeIndexedClassNames.add(classInfo.name().toString());
            }
        }
        List<String> indexedClassNames = selectIndexedClassNames(localIndexedClassNames, compositeIndexedClassNames);

        if (indexedClassNames.isEmpty()) {
            return;
        }
        List<String> deploymentClassNames = collectDeploymentClassNames(deploymentUnit);
        boolean applyHashedDeploymentIsolation = shouldApplyHashedDeploymentIsolation(deploymentClassNames);
        if (!deploymentClassNames.isEmpty()) {
            indexedClassNames = deploymentClassNames;
        }

        String scopedPackagePrefix = applyHashedDeploymentIsolation
                ? resolveScopedPackagePrefix(indexedClassNames, deploymentUnit.getName())
                : null;

        // 1. Discover classes via Jandex
        Set<Class<?>> discoveredClasses = new HashSet<>();
        for (String className : indexedClassNames) {
            if (shouldSkipInfrastructureClass(className)) {
                continue;
            }
            if (scopedPackagePrefix != null
                    && !scopedPackagePrefix.equals(packageName(className))) {
                continue;
            }
            try {
                Class<?> clazz = module.getClassLoader().loadClass(className);
                discoveredClasses.add(clazz);
            } catch (ClassNotFoundException e) {
                // Ignore classes that cannot be resolved.
            } catch (LinkageError e) {
                // Ignore classes that fail to link because optional/transitive types are not visible.
            } catch (RuntimeException e) {
                // Keep discovery resilient for non-application classes in third-party libraries.
            }
        }

        // Narrow discovered classes to deployment-local test package for TCK-generated archives.
        discoveredClasses = applyHashedDeploymentIsolation(
                discoveredClasses,
                deploymentUnit.getName(),
                deploymentClassNames);

        if (discoveredClasses.isEmpty()) {
            return;
        }

        List<BeansXml> deploymentBeansXmlConfigurations = collectDeploymentBeansXmlConfigurations(deploymentUnit);

        // 2. Initialize Syringe via Bootstrap
        SyringeBootstrap bootstrap = new SyringeBootstrap(
                discoveredClasses,
                module.getClassLoader(),
                deploymentBeansXmlConfigurations,
                deploymentUnit.getName());
        Syringe syringe = null;
        try {
            syringe = bootstrap.bootstrap();

            // 3. Attach Syringe to the deployment unit for later use (e.g., in Setup Actions)
            deploymentUnit.putAttachment(SyringeAttachments.SYRINGE_CONTAINER, syringe);

            // 4. Register SetupAction for CDI.current() support and context activation.
            SyringeSetupAction setupAction = new SyringeSetupAction(syringe);
            deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.SETUP_ACTIONS, setupAction);
            registerEeSetupActionAttachments(deploymentUnit, setupAction);
        } catch (RuntimeException e) {
            // Defensive cleanup on deployment failure before attachment is established.
            try {
                bootstrap.shutdown();
            } catch (Exception ignored) {
                // Best-effort cleanup.
            }
            throw e;
        }
    }

    private static void registerEeSetupActionAttachments(DeploymentUnit deploymentUnit, SyringeSetupAction setupAction) {
        ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        if (moduleLoader == null) {
            return;
        }
        try {
            Module eeModule = moduleLoader.loadModule("org.jboss.as.ee");
            if (eeModule == null) {
                return;
            }
            ClassLoader eeClassLoader = eeModule.getClassLoader();
            Class<?> eeAttachments = Class.forName("org.jboss.as.ee.component.Attachments", false, eeClassLoader);
            addSetupActionToAttachmentList(deploymentUnit, eeAttachments, "WEB_SETUP_ACTIONS", setupAction);
            addSetupActionToAttachmentList(deploymentUnit, eeAttachments, "OTHER_EE_SETUP_ACTIONS", setupAction);
        } catch (ModuleLoadException ignored) {
            // Best effort: registration in generic SETUP_ACTIONS remains available.
        } catch (ClassNotFoundException ignored) {
            // Best effort for WildFly variants without EE attachments API exposed.
        } catch (NoSuchFieldException ignored) {
            // Best effort for version-specific attachment naming differences.
        } catch (IllegalAccessException ignored) {
            // Best effort if reflective field access is restricted.
        } catch (RuntimeException ignored) {
            // Keep deployment resilient; generic setup action is already registered.
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addSetupActionToAttachmentList(DeploymentUnit deploymentUnit,
                                                       Class<?> attachmentsClass,
                                                       String fieldName,
                                                       SyringeSetupAction setupAction)
            throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field field = attachmentsClass.getField(fieldName);
        Object key = field.get(null);
        if (key instanceof AttachmentKey) {
            deploymentUnit.addToAttachmentList((AttachmentKey) key, setupAction);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Retrieve the Syringe instance and shut it down
        Syringe syringe = context.getAttachment(SyringeAttachments.SYRINGE_CONTAINER);
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    private static boolean shouldSkipInfrastructureClass(String className) {
        return className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jakarta.")
                || className.startsWith("sun.")
                || className.startsWith("com.sun.")
                || className.startsWith("org.jboss.as.")
                || className.startsWith("org.jboss.modules.")
                || className.startsWith("org.jboss.msc.")
                || className.startsWith("org.jboss.threads.")
                || className.startsWith("org.wildfly.")
                || className.startsWith("org.jboss.arquillian.")
                || className.startsWith("org.jboss.shrinkwrap.")
                || className.startsWith("org.testng.")
                || className.startsWith("org.junit.")
                || className.startsWith("org.hamcrest.")
                || className.startsWith("org.apache.")
                || className.startsWith("org.slf4j.")
                || className.startsWith("ch.qos.logback.")
                || className.startsWith("org.jboss.logging.")
                || className.startsWith("com.threeamigos.common.util.implementations.injection.arquillian.");
    }

    static String resolveScopedPackagePrefix(List<String> indexedClassNames, String deploymentName) {
        if (indexedClassNames == null || indexedClassNames.isEmpty() || deploymentName == null || deploymentName.trim().isEmpty()) {
            return null;
        }
        String baseName = normalizeDeploymentBaseName(deploymentName);
        if (baseName.isEmpty()) {
            return null;
        }

        String anchorClassName = null;
        int bestLen = -1;
        for (String className : indexedClassNames) {
            String simpleName = simpleName(className);
            if (simpleName.isEmpty() || !baseName.startsWith(simpleName)) {
                continue;
            }
            if (simpleName.length() > bestLen) {
                anchorClassName = className;
                bestLen = simpleName.length();
            }
        }
        if (anchorClassName == null) {
            return null;
        }

        String anchorSimpleName = simpleName(anchorClassName);
        String suffix = baseName.substring(anchorSimpleName.length());
        if (!HASH_SUFFIX.matcher(suffix).matches()) {
            return null;
        }

        int idx = anchorClassName.lastIndexOf('.');
        return idx > 0 ? anchorClassName.substring(0, idx) : null;
    }

    private static String normalizeDeploymentBaseName(String deploymentName) {
        String baseName = deploymentName;
        int slash = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < baseName.length()) {
            baseName = baseName.substring(slash + 1);
        }
        if (baseName.endsWith(".war") || baseName.endsWith(".jar") || baseName.endsWith(".ear")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        return baseName;
    }

    private static String simpleName(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        int idx = className.lastIndexOf('.');
        return idx >= 0 ? className.substring(idx + 1) : className;
    }

    static Set<Class<?>> narrowToDeploymentScope(Set<Class<?>> candidates, String deploymentName) {
        if (candidates == null || candidates.isEmpty() || deploymentName == null || deploymentName.trim().isEmpty()) {
            return candidates;
        }

        String baseName = normalizeDeploymentBaseName(deploymentName);

        Class<?> anchorClass = findAnchorClass(baseName, candidates);
        if (anchorClass == null) {
            return candidates;
        }

        String simpleName = anchorClass.getSimpleName();
        if (!baseName.startsWith(simpleName)) {
            return candidates;
        }
        String suffix = baseName.substring(simpleName.length());
        if (!HASH_SUFFIX.matcher(suffix).matches()) {
            // Not a TCK hashed archive name, keep original behavior.
            return candidates;
        }

        Package anchorPackage = anchorClass.getPackage();
        if (anchorPackage == null) {
            return candidates;
        }
        String packagePrefix = anchorPackage.getName();

        Set<Class<?>> filtered = new HashSet<Class<?>>();
        for (Class<?> candidate : candidates) {
            try {
                Package candidatePackage = candidate.getPackage();
                String candidatePackageName = candidatePackage != null ? candidatePackage.getName() : "";
                if (candidatePackageName.equals(packagePrefix)) {
                    filtered.add(candidate);
                }
            } catch (LinkageError e) {
                // Skip classes that cannot be safely introspected in the deployment module.
            }
        }

        return filtered.isEmpty() ? candidates : filtered;
    }

    static boolean shouldApplyHashedDeploymentIsolation(List<String> deploymentClassNames) {
        return deploymentClassNames == null || deploymentClassNames.isEmpty();
    }

    static Set<Class<?>> applyHashedDeploymentIsolation(Set<Class<?>> candidates,
                                                        String deploymentName,
                                                        List<String> deploymentClassNames) {
        if (!shouldApplyHashedDeploymentIsolation(deploymentClassNames)) {
            return candidates;
        }
        return narrowToDeploymentScope(candidates, deploymentName);
    }

    private static Class<?> findAnchorClass(String baseName, Set<Class<?>> candidates) {
        Class<?> best = null;
        int bestLen = -1;

        for (Class<?> candidate : candidates) {
            try {
                String simpleName = candidate.getSimpleName();
                if (simpleName == null || simpleName.isEmpty()) {
                    continue;
                }
                if (!baseName.startsWith(simpleName)) {
                    continue;
                }
                if (simpleName.length() > bestLen) {
                    best = candidate;
                    bestLen = simpleName.length();
                }
            } catch (LinkageError e) {
                // Skip classes that cannot be safely introspected in the deployment module.
            }
        }
        return best;
    }

    static List<String> selectIndexedClassNames(List<String> localIndexedClassNames, List<String> compositeIndexedClassNames) {
        if (localIndexedClassNames != null && !localIndexedClassNames.isEmpty()) {
            return localIndexedClassNames;
        }
        if (compositeIndexedClassNames != null) {
            return compositeIndexedClassNames;
        }
        return new ArrayList<String>();
    }

    private static String packageName(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        int idx = className.lastIndexOf('.');
        return idx > 0 ? className.substring(0, idx) : "";
    }

    static List<String> collectDeploymentClassNames(DeploymentUnit deploymentUnit) {
        Set<String> classNames = new HashSet<String>();
        if (deploymentUnit == null) {
            return new ArrayList<String>();
        }

        ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (deploymentRoot != null && deploymentRoot.getRoot() != null) {
            // Prefer deployment classes only; avoid traversing mounted container/module resources.
            collectClassNamesFromDeploymentRoot(deploymentRoot.getRoot(), classNames);
        }

        return new ArrayList<String>(classNames);
    }

    private static void collectClassNamesFromDeploymentRoot(Object deploymentRoot,
                                                            Set<String> classNames) {
        if (deploymentRoot == null || classNames == null) {
            return;
        }

        List<Object> files = listChildrenRecursively(deploymentRoot);
        if (files.isEmpty()) {
            return;
        }

        List<String> relativePaths = new ArrayList<String>();
        for (Object file : files) {
            String relativePath = extractRelativePath(file, deploymentRoot);
            if (relativePath != null) {
                relativePaths.add(relativePath.replace('\\', '/'));
            }
        }

        Set<String> beanArchiveLibraryPrefixes = collectBeanArchiveLibraryPrefixes(relativePaths);
        for (String relativePath : relativePaths) {
            String classEntry = toDeploymentClassEntry(relativePath, beanArchiveLibraryPrefixes);
            if (classEntry == null) {
                continue;
            }
            String className = classEntry
                    .substring(0, classEntry.length() - ".class".length())
                    .replace('/', '.');
            classNames.add(className);
        }
    }

    static String toDeploymentClassEntry(String relativePath, Set<String> beanArchiveLibraryPrefixes) {
        if (relativePath == null || !relativePath.endsWith(".class")) {
            return null;
        }

        String normalizedPath = trimLeadingSlashes(relativePath.replace('\\', '/'));
        int jarSeparator = normalizedPath.indexOf(".jar/");
        if (jarSeparator >= 0) {
            String jarPrefix = normalizedPath.substring(0, jarSeparator + ".jar/".length());
            if (!beanArchiveLibraryPrefixes.contains(jarPrefix)) {
                return null;
            }
            normalizedPath = normalizedPath.substring(jarSeparator + ".jar/".length());
        } else {
            normalizedPath = trimToWebInfClasses(normalizedPath);
            if (normalizedPath == null) {
                return null;
            }
        }

        normalizedPath = trimLeadingSlashes(normalizedPath);
        if (normalizedPath.startsWith("META-INF/")) {
            return null;
        }
        if (normalizedPath.endsWith("module-info.class") || normalizedPath.endsWith("package-info.class")) {
            return null;
        }
        return normalizedPath;
    }

    static Set<String> collectBeanArchiveLibraryPrefixes(List<String> relativePaths) {
        if (relativePaths == null || relativePaths.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> prefixes = new HashSet<String>();
        for (String relativePath : relativePaths) {
            if (relativePath == null) {
                continue;
            }
            String normalized = trimLeadingSlashes(relativePath.replace('\\', '/'));
            if (!isBeanArchiveLibraryBeansXmlPath(normalized)) {
                continue;
            }
            int jarSeparator = normalized.indexOf(".jar/");
            if (jarSeparator < 0) {
                continue;
            }
            prefixes.add(normalized.substring(0, jarSeparator + ".jar/".length()));
        }
        return prefixes;
    }

    private static String extractRelativePath(Object file, Object root) {
        if (file == null || root == null) {
            return null;
        }
        try {
            Method[] methods = file.getClass().getMethods();
            for (Method method : methods) {
                if (!"getPathNameRelativeTo".equals(method.getName())) {
                    continue;
                }
                if (method.getParameterTypes().length != 1) {
                    continue;
                }
                Object value = method.invoke(file, root);
                if (value instanceof String) {
                    return (String) value;
                }
            }
        } catch (Exception ignored) {
            // Fall back to path name.
        }
        String filePath = getPathName(file);
        String rootPath = getPathName(root);
        if (filePath != null && rootPath != null) {
            String normalizedFilePath = normalizeForComparison(filePath);
            String normalizedRootPath = normalizeForComparison(rootPath);
            if (normalizedFilePath.startsWith(normalizedRootPath)) {
                String relative = normalizedFilePath.substring(normalizedRootPath.length());
                while (relative.startsWith("/")) {
                    relative = relative.substring(1);
                }
                return relative;
            }
        }
        return null;
    }

    static List<BeansXml> collectDeploymentBeansXmlConfigurations(DeploymentUnit deploymentUnit) {
        List<BeansXml> configurations = new ArrayList<BeansXml>();
        if (deploymentUnit == null) {
            return configurations;
        }

        BeansXmlParser parser = new BeansXmlParser();
        Set<String> seenPaths = new HashSet<String>();

        ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (deploymentRoot != null && deploymentRoot.getRoot() != null) {
            collectBeansXmlFromDeploymentRoot(deploymentRoot.getRoot(), parser, seenPaths, configurations);
        }

        return configurations;
    }

    private static void collectBeansXmlFromDeploymentRoot(Object deploymentRoot,
                                                          BeansXmlParser parser,
                                                          Set<String> seenPaths,
                                                          List<BeansXml> sink) {
        List<Object> files = listChildrenRecursively(deploymentRoot);
        if (!files.isEmpty()) {
            for (Object file : files) {
                String relativePath = extractRelativePath(file, deploymentRoot);
                if (relativePath == null) {
                    continue;
                }
                String normalized = trimLeadingSlashes(relativePath.replace('\\', '/'));
                if (isDeploymentBeansXmlPath(normalized)) {
                    addBeansXml(file, parser, seenPaths, sink);
                }
            }
            return;
        }

        collectBeansXmlFromPath(deploymentRoot, "WEB-INF/beans.xml", parser, seenPaths, sink);
        collectBeansXmlFromPath(deploymentRoot, "META-INF/beans.xml", parser, seenPaths, sink);
        collectBeansXmlFromPath(deploymentRoot, "WEB-INF/classes/META-INF/beans.xml", parser, seenPaths, sink);
    }

    private static void collectBeansXmlFromPath(Object deploymentRoot,
                                                String relativePath,
                                                BeansXmlParser parser,
                                                Set<String> seenPaths,
                                                List<BeansXml> sink) {
        if (deploymentRoot == null || relativePath == null || relativePath.isEmpty()) {
            return;
        }
        try {
            Method getChild = deploymentRoot.getClass().getMethod("getChild", String.class);
            Object child = getChild.invoke(deploymentRoot, relativePath);
            if (child == null) {
                return;
            }
            Method exists = child.getClass().getMethod("exists");
            if (!Boolean.TRUE.equals(exists.invoke(child))) {
                return;
            }
            addBeansXml(child, parser, seenPaths, sink);
        } catch (Exception ignored) {
            // Best effort only.
        }
    }

    private static void addBeansXml(Object file,
                                    BeansXmlParser parser,
                                    Set<String> seenPaths,
                                    List<BeansXml> sink) {
        try {
            String path = getPathName(file);
            if (path == null || path.isEmpty()) {
                return;
            }
            String normalizedPath = path.replace('\\', '/');
            if (!seenPaths.add(normalizedPath)) {
                return;
            }

            Method openStream = file.getClass().getMethod("openStream");
            InputStream inputStream = null;
            try {
                inputStream = (InputStream) openStream.invoke(file);
                if (inputStream == null) {
                    return;
                }
                BeansXml beansXml = parser.parse(inputStream);
                sink.add(beansXml);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (Exception ignored) {
            // Best effort only.
        }
    }

    private static String getPathName(Object file) {
        if (file == null) {
            return null;
        }
        try {
            Method pathNameMethod = file.getClass().getMethod("getPathName");
            Object value = pathNameMethod.invoke(file);
            if (value instanceof String) {
                return (String) value;
            }
        } catch (Exception ignored) {
            // Best effort only.
        }
        return null;
    }

    private static String normalizeForComparison(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("vfs:")) {
            normalized = normalized.substring("vfs:".length());
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static List<Object> listChildrenRecursively(Object root) {
        if (root == null) {
            return Collections.emptyList();
        }
        try {
            Method getChildrenRecursively = root.getClass().getMethod("getChildrenRecursively");
            @SuppressWarnings("unchecked")
            List<Object> files = (List<Object>) getChildrenRecursively.invoke(root);
            return files == null ? Collections.<Object>emptyList() : files;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static String trimLeadingSlashes(String path) {
        if (path == null) {
            return null;
        }
        String normalized = path;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String trimToWebInfClasses(String normalizedPath) {
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            return null;
        }
        if (normalizedPath.startsWith("WEB-INF/classes/")) {
            return normalizedPath.substring("WEB-INF/classes/".length());
        }
        int classesSeparator = normalizedPath.indexOf("/WEB-INF/classes/");
        if (classesSeparator >= 0) {
            return normalizedPath.substring(classesSeparator + "/WEB-INF/classes/".length());
        }
        return null;
    }

    static boolean isDeploymentBeansXmlPath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }
        String normalized = trimLeadingSlashes(relativePath.replace('\\', '/'));
        if ("WEB-INF/beans.xml".equals(normalized)
                || "META-INF/beans.xml".equals(normalized)
                || "WEB-INF/classes/META-INF/beans.xml".equals(normalized)) {
            return true;
        }
        return isBeanArchiveLibraryBeansXmlPath(normalized);
    }

    private static boolean isBeanArchiveLibraryBeansXmlPath(String normalizedPath) {
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            return false;
        }
        if (!normalizedPath.endsWith("/META-INF/beans.xml")) {
            return false;
        }
        int jarSeparator = normalizedPath.indexOf(".jar/");
        if (jarSeparator < 0) {
            return false;
        }
        String jarPrefix = normalizedPath.substring(0, jarSeparator + ".jar/".length());
        return jarPrefix.startsWith("WEB-INF/lib/")
                || jarPrefix.contains("/WEB-INF/lib/");
    }

}
