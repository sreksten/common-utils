package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

/**
 * Interface for managing scoped bean instances.
 * Each scope type (ApplicationScoped, RequestScoped, etc.) has its own context implementation.
 *
 * @author Stefano Reksten
 */
interface ScopeContext {

    /**
     * Gets an existing instance from this scope, or creates a new one if needed.
     *
     * @param bean the bean to get/create
     * @param creationalContext the creation context for dependency injection
     * @return the scoped instance
     */
    <T> T get(Bean<T> bean, CreationalContext<T> creationalContext);

    /**
     * Gets an existing instance from this scope without creating a new one.
     *
     * @param bean the bean to get
     * @return the existing instance, or null if not present
     */
    <T> T getIfExists(Bean<T> bean);

    /**
     * Destroys all instances in this scope.
     */
    void destroy();

    /**
     * Checks if this scope is active.
     *
     * @return true if the scope is currently active
     */
    boolean isActive();
}
