package com.threeamigos.common.util.implementations.injection.contexts;

import com.threeamigos.common.util.implementations.injection.BeanImpl;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of ApplicationScoped context.
 * Maintains a single instance per bean for the entire application lifecycle.
 *
 * <p><b>PHASE 2 - Interceptor Support:</b> This context automatically wraps beans that
 * have interceptors with interceptor-aware proxies. This ensures that interceptor chains
 * are executed before business methods are called.
 *
 * @author Stefano Reksten
 */
public class ApplicationScopedContext implements ScopeContext {

    private final Map<Bean<?>, Object> instances = new ConcurrentHashMap<>();
    private final Map<Bean<?>, CreationalContext<?>> creationalContexts = new ConcurrentHashMap<>();
    private volatile boolean active = true;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Bean<T> bean, CreationalContext<T> creationalContext) {
        if (!active) {
            throw new ContextNotActiveException("ApplicationScoped context is not active");
        }

        return (T) instances.computeIfAbsent(bean, b -> {
            creationalContexts.put(bean, creationalContext);

            // Step 1: Create the actual bean instance (with full dependency injection and lifecycle callbacks)
            T instance = bean.create(creationalContext);

            // Step 2: PHASE 2 - Wrap with interceptor-aware proxy if bean has interceptors
            // Check if this is a BeanImpl (our managed beans) and if it has interceptors configured
            if (bean instanceof BeanImpl) {
                BeanImpl<T> beanImpl = (BeanImpl<T>) bean;

                // If the bean has interceptors, wrap the instance with an interceptor-aware proxy
                // The proxy will intercept method calls and execute the interceptor chain before
                // delegating to the actual bean instance
                if (beanImpl.hasInterceptors()) {
                    instance = beanImpl.createInterceptorAwareProxy(instance);
                }
            }

            return instance;
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

    @Override
    public boolean isPassivationCapable() {
        // ApplicationScoped beans live for the entire application lifetime
        // They are never passivated, so no serialization requirement
        return false;
    }
}
