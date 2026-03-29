package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import jakarta.enterprise.inject.spi.DeploymentException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
        try {
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

            return syringe;
        } catch (Exception e) {
            throw new DeploymentException("Failed to bootstrap Syringe", e);
        }
    }

    /**
     * Shuts down the Syringe container.
     */
    public void shutdown() {
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
}
