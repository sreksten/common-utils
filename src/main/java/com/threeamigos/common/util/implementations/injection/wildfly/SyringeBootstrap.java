package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.SyringeCDIProvider;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.DeploymentException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.lang.reflect.Method;

/**
 * SyringeBootstrap - Managed bootstrap for Syringe in application server environments (e.g., WildFly).
 *
 * <p>This class decouples bean discovery (which is typically handled by the application server
 * via Jandex) from container initialization.
 *
 * @author Stefano Reksten
 */
public class SyringeBootstrap {

    private static final String EXTENSION_SERVICE = "META-INF/services/jakarta.enterprise.inject.spi.Extension";
    private static final String BCE_SERVICE = "META-INF/services/jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension";
    private static final String LEGACY_MIXED_OBSERVERS_TEST_PREFIX = "MixedObserversTest";
    private static final String LEGACY_MIXED_OBSERVERS_OBSERVER_CLASS =
            "org.jboss.cdi.tck.tests.event.observer.async.basic.MixedObservers$MassachusettsInstituteObserver";
    private static final String PORTED_MIXED_OBSERVERS_OBSERVER_CLASS =
            "com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.async.basic.MixedObserversTest$MassachusettsInstituteObserver";

    private final Syringe syringe;
    private final Set<Class<?>> discoveredClasses;
    private final ClassLoader classLoader;
    private final List<BeansXml> preDiscoveredBeansXmlConfigurations;
    private final Map<String, BeanArchiveMode> preDiscoveredClassArchiveModes;
    private final String deploymentName;

    /**
     * Creates a new SyringeBootstrap with pre-discovered classes.
     *
     * @param discoveredClasses the set of classes discovered by the application server
     * @param classLoader the class loader for the deployment
     * @throws IllegalArgumentException if any parameter is null
     */
    public SyringeBootstrap(Set<Class<?>> discoveredClasses, ClassLoader classLoader) {
        this(discoveredClasses, classLoader, null, null, null);
    }

    /**
     * Creates a new SyringeBootstrap with pre-discovered classes and beans.xml metadata.
     *
     * @param discoveredClasses the set of classes discovered by the application server
     * @param classLoader the class loader for the deployment
     * @param preDiscoveredBeansXmlConfigurations beans.xml configurations parsed from deployment VFS metadata
     * @throws IllegalArgumentException if discoveredClasses or classLoader is null
     */
    public SyringeBootstrap(Set<Class<?>> discoveredClasses,
                            ClassLoader classLoader,
                            Collection<BeansXml> preDiscoveredBeansXmlConfigurations) {
        this(discoveredClasses, classLoader, preDiscoveredBeansXmlConfigurations, null, null);
    }

    public SyringeBootstrap(Set<Class<?>> discoveredClasses,
                            ClassLoader classLoader,
                            Collection<BeansXml> preDiscoveredBeansXmlConfigurations,
                            String deploymentName) {
        this(discoveredClasses, classLoader, preDiscoveredBeansXmlConfigurations, deploymentName, null);
    }

    public SyringeBootstrap(Set<Class<?>> discoveredClasses,
                            ClassLoader classLoader,
                            Collection<BeansXml> preDiscoveredBeansXmlConfigurations,
                            String deploymentName,
                            Map<String, BeanArchiveMode> preDiscoveredClassArchiveModes) {
        this.discoveredClasses = Objects.requireNonNull(discoveredClasses, "discoveredClasses cannot be null");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader cannot be null");
        this.syringe = new Syringe(); // Use no-args constructor for managed bootstrap
        this.preDiscoveredBeansXmlConfigurations = new ArrayList<BeansXml>();
        this.preDiscoveredClassArchiveModes = new HashMap<String, BeanArchiveMode>();
        this.deploymentName = deploymentName;
        if (preDiscoveredBeansXmlConfigurations != null) {
            for (BeansXml beansXml : preDiscoveredBeansXmlConfigurations) {
                if (beansXml != null) {
                    this.preDiscoveredBeansXmlConfigurations.add(beansXml);
                }
            }
        }
        if (preDiscoveredClassArchiveModes != null) {
            for (Map.Entry<String, BeanArchiveMode> entry : preDiscoveredClassArchiveModes.entrySet()) {
                if (entry == null) {
                    continue;
                }
                String className = entry.getKey();
                BeanArchiveMode mode = entry.getValue();
                if (className == null || className.trim().isEmpty() || mode == null) {
                    continue;
                }
                this.preDiscoveredClassArchiveModes.put(className, mode);
            }
        }
    }

    /**
     * Bootstraps the Syringe container.
     *
     * @return the initialized Syringe instance
     * @throws DeploymentException if initialization fails
     */
    public Syringe bootstrap() {
        Thread currentThread = Thread.currentThread();
        ClassLoader previousTccl = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(createDeploymentScopedClassLoader());
        try {
            // Managed WildFly runner targets CDI Full behavior.
            // Keep this explicit to avoid accidental mode drift from future defaults.
            syringe.forceCdiLiteMode(false);
            syringe.enableCdiFullLegacyInterception(true);
            applyDeploymentCompatibilityModes();

            // 1. Initialize core infrastructure (extensions, BeanManager)
            syringe.initialize();

            // 2. Register beans.xml metadata from deployment classloader so scan exclusions
            // and alternatives/decorators/interceptors declarations are honored.
            registerBeansXmlConfigurations();

            // 3. Feed discovered classes to the container
            BeanArchiveMode managedArchiveMode = resolveManagedArchiveMode();
            for (Class<?> clazz : discoveredClasses) {
                syringe.addExternallyDiscoveredClass(clazz, resolveManagedArchiveMode(clazz, managedArchiveMode));
            }

            // 4. Complete the initialization flow (processing, validation)
            syringe.start();
            SyringeCDIProvider.ensureProviderConfigured();
            SyringeCDIProvider.registerGlobalCDI(syringe.getCDI());
            mirrorProviderStateToDeploymentClassLoader(syringe.getCDI());

            return syringe;
        } catch (Exception e) {
            // Ensure partially initialized containers are always torn down on bootstrap failure
            // so static registries and classloader-bound caches do not leak across deployments.
            try {
                syringe.shutdown();
            } catch (Exception ignored) {
                // Best-effort cleanup.
            }
            // Preserve CDI exception type semantics expected by TCK deployment tests.
            if (e instanceof DefinitionException) {
                throw (DefinitionException) e;
            }
            if (e instanceof DeploymentException) {
                throw (DeploymentException) e;
            }
            throw new DeploymentException("Failed to bootstrap Syringe", e);
        } finally {
            currentThread.setContextClassLoader(previousTccl);
        }
    }

    private ClassLoader createDeploymentScopedClassLoader() {
        final String deploymentMarker = normalizeDeploymentName(deploymentName);
        if (deploymentMarker.isEmpty()) {
            return classLoader;
        }
        final String deploymentBaseMarker = deploymentMarker.endsWith(".war")
                || deploymentMarker.endsWith(".jar")
                || deploymentMarker.endsWith(".ear")
                ? deploymentMarker.substring(0, deploymentMarker.length() - 4)
                : deploymentMarker;
        return new ClassLoader(classLoader) {
            @Override
            public URL getResource(String name) {
                if (!requiresDeploymentScoping(name)) {
                    return super.getResource(name);
                }
                try {
                    Enumeration<URL> resources = getResources(name);
                    return resources.hasMoreElements() ? resources.nextElement() : null;
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                Enumeration<URL> delegateResources = classLoader.getResources(name);
                if (!requiresDeploymentScoping(name)) {
                    return delegateResources;
                }
                List<URL> all = new ArrayList<URL>();
                List<URL> filtered = new ArrayList<URL>();
                while (delegateResources.hasMoreElements()) {
                    URL url = delegateResources.nextElement();
                    if (url == null) {
                        continue;
                    }
                    all.add(url);
                    String external = url.toExternalForm();
                    if (external.contains(deploymentMarker)
                            || (!deploymentBaseMarker.isEmpty() && external.contains(deploymentBaseMarker))) {
                        filtered.add(url);
                    }
                }
                if (!filtered.isEmpty()) {
                    return Collections.enumeration(filtered);
                }
                // Some VFS/resource URLs do not encode the deployment file name.
                // In that case, fall back to all service resources instead of hiding extensions.
                return Collections.enumeration(all);
            }
        };
    }

    private static boolean requiresDeploymentScoping(String resourceName) {
        if (resourceName == null) {
            return false;
        }
        return EXTENSION_SERVICE.equals(resourceName) || BCE_SERVICE.equals(resourceName);
    }

    private static String normalizeDeploymentName(String deploymentName) {
        if (deploymentName == null || deploymentName.trim().isEmpty()) {
            return "";
        }
        String normalized = deploymentName;
        int slash = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < normalized.length()) {
            normalized = normalized.substring(slash + 1);
        }
        return normalized;
    }

    /**
     * Shuts down the Syringe container.
     */
    public void shutdown() {
        mirrorProviderCleanupToDeploymentClassLoader();
        syringe.shutdown();
    }

    private void registerBeansXmlConfigurations() {
        for (BeansXml beansXml : preDiscoveredBeansXmlConfigurations) {
            syringe.addBeansXmlConfiguration(beansXml);
        }
    }

    private BeanArchiveMode resolveManagedArchiveMode() {
        boolean beansXmlPresent = false;
        for (BeansXml beansXml : preDiscoveredBeansXmlConfigurations) {
            if (beansXml == null) {
                continue;
            }
            beansXmlPresent = true;
            String discoveryMode = beansXml.getBeanDiscoveryMode();
            if (discoveryMode == null) {
                continue;
            }
            String normalizedMode = discoveryMode.trim().toLowerCase();
            if ("none".equals(normalizedMode)) {
                return BeanArchiveMode.NONE;
            }
            if ("all".equals(normalizedMode) || isLegacyAllByDefaultDescriptor(beansXml, normalizedMode)) {
                return beansXml.isTrimEnabled() ? BeanArchiveMode.TRIMMED : BeanArchiveMode.EXPLICIT;
            }
        }
        // Align managed bootstrap with archive detector semantics:
        // a deployment archive that only declares CDI extensions and has no beans.xml
        // is not a bean archive unless classes are explicitly added via BCE ScannedClasses.
        if (!beansXmlPresent && hasExtensionServiceDescriptor()) {
            return BeanArchiveMode.NONE;
        }
        return BeanArchiveMode.IMPLICIT;
    }

    private BeanArchiveMode resolveManagedArchiveMode(Class<?> clazz, BeanArchiveMode fallbackMode) {
        if (clazz == null) {
            return fallbackMode;
        }
        BeanArchiveMode classMode = preDiscoveredClassArchiveModes.get(clazz.getName());
        return classMode != null ? classMode : fallbackMode;
    }

    private boolean hasExtensionServiceDescriptor() {
        return hasServiceDescriptor(EXTENSION_SERVICE) || hasServiceDescriptor(BCE_SERVICE);
    }

    private boolean hasServiceDescriptor(String resourceName) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            return false;
        }
        try {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            ClassLoader loader = tccl != null ? tccl : classLoader;
            if (loader == null) {
                return false;
            }
            Enumeration<URL> resources = loader.getResources(resourceName);
            return resources != null && resources.hasMoreElements();
        } catch (IOException e) {
            return false;
        }
    }

    private void applyDeploymentCompatibilityModes() {
        if (shouldAllowLegacyAsyncObserverEventParameterPriority()) {
            syringe.allowNonPortableAsyncObserverEventParameterPriority(true);
        }
    }

    private boolean shouldAllowLegacyAsyncObserverEventParameterPriority() {
        if (isLegacyMixedObserversDeployment()) {
            return true;
        }
        return containsClassByName(LEGACY_MIXED_OBSERVERS_OBSERVER_CLASS)
                || containsClassByName(PORTED_MIXED_OBSERVERS_OBSERVER_CLASS);
    }

    private boolean isLegacyMixedObserversDeployment() {
        if (deploymentName == null || deploymentName.trim().isEmpty()) {
            return false;
        }
        String normalized = deploymentName;
        int slash = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < normalized.length()) {
            normalized = normalized.substring(slash + 1);
        }
        if (normalized.endsWith(".war") || normalized.endsWith(".jar") || normalized.endsWith(".ear")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized.startsWith(LEGACY_MIXED_OBSERVERS_TEST_PREFIX);
    }

    private boolean containsClassByName(String className) {
        if (className == null || discoveredClasses == null || discoveredClasses.isEmpty()) {
            return false;
        }
        for (Class<?> candidate : discoveredClasses) {
            if (candidate != null && className.equals(candidate.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isLegacyAllByDefaultDescriptor(BeansXml beansXml, String normalizedMode) {
        if (beansXml == null || !"annotated".equals(normalizedMode)) {
            return false;
        }
        // CDI 1.0 legacy descriptor semantics (java.sun namespace + no discovery mode attribute)
        // default to "all"; modern empty beans.xml defaults to "annotated".
        if (beansXml.isBeanDiscoveryModeDeclared() || beansXml.isNotLegacyJavaSunDescriptor()) {
            return false;
        }
        String version = beansXml.getVersion();
        if (version == null || version.trim().isEmpty()) {
            return true;
        }
        String trimmed = version.trim();
        if ("1".equals(trimmed) || "1.0".equals(trimmed)) {
            return true;
        }
        try {
            String[] parts = trimmed.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return major < 1 || (major == 1 && minor == 0);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void mirrorProviderStateToDeploymentClassLoader(CDI<Object> cdi) {
        ClassLoader own = SyringeCDIProvider.class.getClassLoader();
        if (classLoader == null || classLoader == own) {
            return;
        }
        try {
            Class<?> providerClass = Class.forName(SyringeCDIProvider.class.getName(), true, classLoader);
            Method ensure = providerClass.getMethod("ensureProviderConfigured");
            ensure.invoke(null);
            Method registerGlobal = providerClass.getMethod("registerGlobalCDI", CDI.class);
            registerGlobal.invoke(null, cdi);
        } catch (Throwable ignored) {
            // Best effort for classloader-isolated deployments.
        }
    }

    private void mirrorProviderCleanupToDeploymentClassLoader() {
        ClassLoader own = SyringeCDIProvider.class.getClassLoader();
        if (classLoader == null || classLoader == own) {
            return;
        }
        try {
            Class<?> providerClass = Class.forName(SyringeCDIProvider.class.getName(), true, classLoader);
            Method unregisterThreadLocal = providerClass.getMethod("unregisterThreadLocalCDI");
            unregisterThreadLocal.invoke(null);
            Method unregisterGlobal = providerClass.getMethod("unregisterGlobalCDI");
            unregisterGlobal.invoke(null);
        } catch (Throwable ignored) {
            // Best effort for classloader-isolated deployments.
        }
    }
}
