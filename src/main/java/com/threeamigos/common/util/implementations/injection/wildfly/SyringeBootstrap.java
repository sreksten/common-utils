package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
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
import java.util.List;
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

    private final Syringe syringe;
    private final Set<Class<?>> discoveredClasses;
    private final ClassLoader classLoader;
    private final List<BeansXml> preDiscoveredBeansXmlConfigurations;
    private final String deploymentName;

    /**
     * Creates a new SyringeBootstrap with pre-discovered classes.
     *
     * @param discoveredClasses the set of classes discovered by the application server
     * @param classLoader the class loader for the deployment
     * @throws IllegalArgumentException if any parameter is null
     */
    public SyringeBootstrap(Set<Class<?>> discoveredClasses, ClassLoader classLoader) {
        this(discoveredClasses, classLoader, null, null);
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
        this(discoveredClasses, classLoader, preDiscoveredBeansXmlConfigurations, null);
    }

    public SyringeBootstrap(Set<Class<?>> discoveredClasses,
                            ClassLoader classLoader,
                            Collection<BeansXml> preDiscoveredBeansXmlConfigurations,
                            String deploymentName) {
        this.discoveredClasses = Objects.requireNonNull(discoveredClasses, "discoveredClasses cannot be null");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader cannot be null");
        this.syringe = new Syringe(); // Use no-args constructor for managed bootstrap
        this.preDiscoveredBeansXmlConfigurations = new ArrayList<BeansXml>();
        this.deploymentName = deploymentName;
        if (preDiscoveredBeansXmlConfigurations != null) {
            for (BeansXml beansXml : preDiscoveredBeansXmlConfigurations) {
                if (beansXml != null) {
                    this.preDiscoveredBeansXmlConfigurations.add(beansXml);
                }
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

            // 1. Initialize core infrastructure (extensions, BeanManager)
            syringe.initialize();

            // 2. Register beans.xml metadata from deployment classloader so scan exclusions
            // and alternatives/decorators/interceptors declarations are honored.
            registerBeansXmlConfigurations();

            // 3. Feed discovered classes to the container
            for (Class<?> clazz : discoveredClasses) {
                syringe.addDiscoveredClass(clazz);
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
                List<URL> filtered = new ArrayList<URL>();
                while (delegateResources.hasMoreElements()) {
                    URL url = delegateResources.nextElement();
                    if (url != null && url.toExternalForm().contains(deploymentMarker)) {
                        filtered.add(url);
                    }
                }
                return Collections.enumeration(filtered);
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
