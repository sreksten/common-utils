package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.custom;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class ClusterContext implements Context {

    private final Map<Contextual<?>, Object> instances = new HashMap<Contextual<?>, Object>();

    private boolean isGetCalled;

    @Override
    public Class<? extends Annotation> getScope() {
        return ClusterScoped.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> ctx) {
        isGetCalled = true;
        assertSerializable(contextual, "Contextual must be serializable for passivating custom scope");
        if (ctx != null) {
            assertSerializable(ctx, "CreationalContext must be serializable for passivating custom scope");
        }
        synchronized (instances) {
            T instance = (T) instances.get(contextual);
            if (instance == null && ctx != null) {
                instances.put(contextual, contextual.create(ctx));
                instance = (T) instances.get(contextual);
            }
            return instance;
        }
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return get(contextual, null);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    public boolean isGetCalled() {
        return isGetCalled;
    }

    public void reset() {
        isGetCalled = false;
    }

    private void assertSerializable(Object value, String message) {
        if (!(value instanceof Serializable)) {
            throw new AssertionError(message + ": " + value);
        }
    }
}
