package com.threeamigos.common.util.implementations.injection.scopehandlers;

import com.threeamigos.common.util.implementations.injection.util.LifecycleMethodHelper;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Deprecated
public class SingletonScopeHandler implements ScopeHandler {

    private final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz, Supplier<T> provider) {
        // We don't use computeIfAbsent to handle concurrency issues, when A depends on B and
        // both are Singletons
        Object instance = instances.get(clazz);
        if (instance == null) {
            synchronized (instances) {
                instance = instances.get(clazz);
                if (instance == null) {
                    instance = provider.get();
                    instances.put(clazz, instance);
                }
            }
        }
        return (T) instance;
    }

    @Override
    public void close() {
        // Call @PreDestroy on all singletons
        for (Object instance : instances.values()) {
            try {
                LifecycleMethodHelper.invokeLifecycleMethod(instance, PreDestroy.class);
            } catch (Exception e) {
                // Log but continue destroying others
            }
        }
        instances.clear();
    }
}
