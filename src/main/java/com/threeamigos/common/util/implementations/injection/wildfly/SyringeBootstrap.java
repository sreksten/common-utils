package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.spi.SyringeCDIProvider;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.DeploymentException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
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

    private final Syringe syringe;
    private final Set<Class<?>> discoveredClasses;
    private final ClassLoader classLoader;

    /**
     * Creates a new SyringeBootstrap with pre-discovered classes.
     *
     * @param discoveredClasses the set of classes discovered by the application server
     * @param classLoader the class loader for the deployment
     * @throws IllegalArgumentException if any parameter is null
     */
    public SyringeBootstrap(Set<Class<?>> discoveredClasses, ClassLoader classLoader) {
        this.discoveredClasses = Objects.requireNonNull(discoveredClasses, "discoveredClasses cannot be null");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader cannot be null");
        this.syringe = new Syringe(); // Use no-args constructor for managed bootstrap
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
        currentThread.setContextClassLoader(classLoader);
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

    /**
     * Shuts down the Syringe container.
     */
    public void shutdown() {
        mirrorProviderCleanupToDeploymentClassLoader();
        syringe.shutdown();
    }

    private void registerBeansXmlConfigurations() throws IOException {
        BeansXmlParser parser = new BeansXmlParser();
        Set<String> seenUrls = new HashSet<String>();
        loadBeansXmlFromPath(parser, seenUrls, "META-INF/beans.xml");
        loadBeansXmlFromPath(parser, seenUrls, "WEB-INF/beans.xml");
    }

    private void loadBeansXmlFromPath(BeansXmlParser parser,
                                      Set<String> seenUrls,
                                      String path) throws IOException {
        Enumeration<URL> resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String external = url.toExternalForm();
            if (!seenUrls.add(external)) {
                continue;
            }
            InputStream stream = null;
            try {
                stream = url.openStream();
                BeansXml beansXml = parser.parse(stream);
                syringe.addBeansXmlConfiguration(beansXml);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
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
