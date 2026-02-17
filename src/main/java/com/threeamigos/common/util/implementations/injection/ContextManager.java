package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all scoped contexts for the CDI container.
 * Maps scope annotations to their corresponding context implementations.
 *
 * @author Stefano Reksten
 */
class ContextManager {

    private final Map<Class<? extends Annotation>, ScopeContext> contexts = new ConcurrentHashMap<>();

    ContextManager() {
        // Initialize built-in contexts
        contexts.put(ApplicationScoped.class, new ApplicationScopedContext());
        contexts.put(Dependent.class, new DependentContext());
    }

    /**
     * Gets the context for a given scope annotation.
     *
     * @param scopeAnnotation the scope annotation class
     * @return the corresponding context
     * @throws IllegalArgumentException if the scope is not supported
     */
    ScopeContext getContext(Class<? extends Annotation> scopeAnnotation) {
        ScopeContext context = contexts.get(scopeAnnotation);
        if (context == null) {
            throw new IllegalArgumentException("Unsupported scope: " + scopeAnnotation.getName());
        }
        return context;
    }

    /**
     * Destroys all contexts and their instances.
     */
    void destroyAll() {
        for (ScopeContext context : contexts.values()) {
            try {
                context.destroy();
            } catch (Exception e) {
                System.err.println("Error destroying context: " + e.getMessage());
            }
        }
    }
}
