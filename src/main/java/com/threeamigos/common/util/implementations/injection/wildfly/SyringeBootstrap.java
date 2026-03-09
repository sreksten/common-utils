package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import jakarta.enterprise.inject.spi.DeploymentException;
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

            // 2. Feed discovered classes to the container
            for (Class<?> clazz : discoveredClasses) {
                syringe.addDiscoveredClass(clazz);
            }

            // 3. Complete the initialization flow (processing, validation)
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
}
