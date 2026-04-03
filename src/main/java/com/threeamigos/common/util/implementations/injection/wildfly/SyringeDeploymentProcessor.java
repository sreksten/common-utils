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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
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
        if (!deploymentClassNames.isEmpty()) {
            indexedClassNames = deploymentClassNames;
        }

        String scopedPackagePrefix = resolveScopedPackagePrefix(indexedClassNames, deploymentUnit.getName());

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
        discoveredClasses = narrowToDeploymentScope(discoveredClasses, deploymentUnit.getName());

        if (discoveredClasses.isEmpty()) {
            return;
        }

        List<BeansXml> deploymentBeansXmlConfigurations = collectDeploymentBeansXmlConfigurations(deploymentUnit);
        System.out.println("[Syringe][WildFly] Deployment beans.xml configurations for "
                + deploymentUnit.getName() + ": " + deploymentBeansXmlConfigurations.size());
        for (BeansXml beansXml : deploymentBeansXmlConfigurations) {
            System.out.println("[Syringe][WildFly]  - " + beansXml);
        }

        // 2. Initialize Syringe via Bootstrap
        SyringeBootstrap bootstrap = new SyringeBootstrap(
                discoveredClasses,
                module.getClassLoader(),
                deploymentBeansXmlConfigurations);
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
        List<String> classNames = new ArrayList<String>();
        if (deploymentUnit == null) {
            return classNames;
        }

        ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (deploymentRoot == null || deploymentRoot.getRoot() == null) {
            return classNames;
        }

        try {
            Object root = deploymentRoot.getRoot();
            Method getChild = root.getClass().getMethod("getChild", String.class);
            Object classesRoot = getChild.invoke(root, "WEB-INF/classes");

            Method exists = classesRoot.getClass().getMethod("exists");
            boolean classesRootExists = Boolean.TRUE.equals(exists.invoke(classesRoot));
            if (!classesRootExists) {
                classesRoot = root;
            }

            Method getChildrenRecursively = classesRoot.getClass().getMethod("getChildrenRecursively");
            @SuppressWarnings("unchecked")
            List<Object> files = (List<Object>) getChildrenRecursively.invoke(classesRoot);
            for (Object file : files) {
                Method relativePathMethod = file.getClass().getMethod("getPathNameRelativeTo", classesRoot.getClass());
                String relativePath = (String) relativePathMethod.invoke(file, classesRoot);
                if (relativePath == null || !relativePath.endsWith(".class")) {
                    continue;
                }
                if (relativePath.contains("/WEB-INF/lib/") || relativePath.contains("\\WEB-INF\\lib\\")) {
                    continue;
                }
                if (relativePath.endsWith("module-info.class") || relativePath.endsWith("package-info.class")) {
                    continue;
                }
                String className = relativePath
                        .substring(0, relativePath.length() - ".class".length())
                        .replace('/', '.')
                        .replace('\\', '.');
                classNames.add(className);
            }
        } catch (Exception e) {
            return new ArrayList<String>();
        }

        return classNames;
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
            collectBeansXmlFromVirtualRoot(deploymentRoot.getRoot(), parser, seenPaths, configurations);
        }

        List<ResourceRoot> resourceRoots = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        if (resourceRoots != null) {
            for (ResourceRoot resourceRoot : resourceRoots) {
                if (resourceRoot == null || resourceRoot.getRoot() == null) {
                    continue;
                }
                collectBeansXmlFromVirtualRoot(resourceRoot.getRoot(), parser, seenPaths, configurations);
            }
        }

        return configurations;
    }

    private static void collectBeansXmlFromVirtualRoot(Object root,
                                                       BeansXmlParser parser,
                                                       Set<String> seenPaths,
                                                       List<BeansXml> sink) {
        try {
            Method getChildrenRecursively = root.getClass().getMethod("getChildrenRecursively");
            @SuppressWarnings("unchecked")
            List<Object> files = (List<Object>) getChildrenRecursively.invoke(root);

            for (Object file : files) {
                if (file == null) {
                    continue;
                }

                String path = null;
                try {
                    Method pathName = file.getClass().getMethod("getPathName");
                    Object value = pathName.invoke(file);
                    if (value instanceof String) {
                        path = (String) value;
                    }
                } catch (Exception ignored) {
                    // Best effort only.
                }

                if (path == null || path.isEmpty()) {
                    continue;
                }

                String normalizedPath = path.replace('\\', '/');
                if (!normalizedPath.endsWith("/META-INF/beans.xml")
                        && !normalizedPath.endsWith("/WEB-INF/beans.xml")
                        && !"META-INF/beans.xml".equals(normalizedPath)
                        && !"WEB-INF/beans.xml".equals(normalizedPath)) {
                    continue;
                }

                if (!seenPaths.add(normalizedPath)) {
                    continue;
                }

                Method openStream = file.getClass().getMethod("openStream");
                InputStream inputStream = null;
                try {
                    inputStream = (InputStream) openStream.invoke(file);
                    if (inputStream != null) {
                        byte[] rawBytes = readBytes(inputStream);
                        String rawXml = new String(rawBytes, StandardCharsets.UTF_8);
                        String preview = rawXml.replace('\n', ' ').replace('\r', ' ');
                        if (preview.length() > 400) {
                            preview = preview.substring(0, 400) + "...";
                        }
                        System.out.println("[Syringe][WildFly] Raw beans.xml preview from " + normalizedPath + ": " + preview);
                        BeansXml beansXml = parser.parse(new ByteArrayInputStream(rawBytes));
                        sink.add(beansXml);
                        System.out.println("[Syringe][WildFly] Parsed beans.xml from VFS path: "
                                + normalizedPath + " => " + beansXml);
                    }
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            }
        } catch (Exception ignored) {
            // Best effort; beans.xml may be unavailable for some roots.
        }
    }

    private static byte[] readBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }
}
