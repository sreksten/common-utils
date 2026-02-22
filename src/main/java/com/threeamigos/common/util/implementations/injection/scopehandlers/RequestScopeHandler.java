package com.threeamigos.common.util.implementations.injection.scopehandlers;

import com.threeamigos.common.util.implementations.injection.LifecycleMethodHelper;

import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Deprecated
public class RequestScopeHandler implements ScopeHandler {
    private final ThreadLocal<Map<Class<?>, Object>> requestBeans = new ThreadLocal<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz, Supplier<T> provider) {
        Map<Class<?>, Object> beans = requestBeans.get();
        if (beans == null) {
            beans = new HashMap<>();
            requestBeans.set(beans);
        }
        return (T) beans.computeIfAbsent(clazz, k -> provider.get());
    }

    @Override
    public void close() {
        Map<Class<?>, Object> beans = requestBeans.get();
        if (beans != null) {
            for (Object bean : beans.values()) {
                try {
                    // Call @PreDestroy on each bean
                    LifecycleMethodHelper.invokeLifecycleMethod(bean, PreDestroy.class);
                } catch (Exception e) {
                    // Log error
                }
            }
            requestBeans.remove();
        }
    }
}
