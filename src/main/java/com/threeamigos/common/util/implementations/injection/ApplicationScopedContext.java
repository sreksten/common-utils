package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of ApplicationScoped context.
 * Maintains a single instance per bean for the entire application lifecycle.
 *
 * @author Stefano Reksten
 */
class ApplicationScopedContext implements ScopeContext {

    private final Map<Bean<?>, Object> instances = new ConcurrentHashMap<>();
    private final Map<Bean<?>, CreationalContext<?>> creationalContexts = new ConcurrentHashMap<>();
    private volatile boolean active = true;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Bean<T> bean, CreationalContext<T> creationalContext) {
        if (!active) {
            throw new IllegalStateException("ApplicationScoped context is not active");
        }

        return (T) instances.computeIfAbsent(bean, b -> {
            creationalContexts.put(bean, creationalContext);
            return bean.create(creationalContext);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getIfExists(Bean<T> bean) {
        return (T) instances.get(bean);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void destroy() {
        for (Map.Entry<Bean<?>, Object> entry : instances.entrySet()) {
            Bean<Object> bean = (Bean<Object>) entry.getKey();
            Object instance = entry.getValue();
            CreationalContext<Object> ctx = (CreationalContext<Object>) creationalContexts.get(bean);

            try {
                bean.destroy(instance, ctx);
            } catch (Exception e) {
                // Log error but continue destroying other beans
                System.err.println("Error destroying bean " + bean.getBeanClass().getName() + ": " + e.getMessage());
            }
        }

        instances.clear();
        creationalContexts.clear();
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
