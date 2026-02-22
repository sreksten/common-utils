package com.threeamigos.common.util.implementations.injection.scopehandlers;

import com.threeamigos.common.util.implementations.injection.LifecycleMethodHelper;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Deprecated
public class SessionScopeHandler implements ScopeHandler {
    private final Map<String, Map<Class<?>, Object>> sessionBeans = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentSessionId = new ThreadLocal<>();

    public void setCurrentSession(String sessionId) {
        currentSessionId.set(sessionId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz, Supplier<T> provider) {
        String sessionId = currentSessionId.get();
        if (sessionId == null) throw new IllegalStateException("No session context");

        Map<Class<?>, Object> beans = sessionBeans.computeIfAbsent(
                sessionId, k -> new ConcurrentHashMap<>()
        );
        return (T) beans.computeIfAbsent(clazz, k -> provider.get());
    }

    @Override
    public void close() {
        // Called when the session expires
        String sessionId = currentSessionId.get();
        if (sessionId != null) {
            Map<Class<?>, Object> beans = sessionBeans.remove(sessionId);
            if (beans != null) {
                beans.values().forEach(bean -> {
                    try {
                        LifecycleMethodHelper.invokeLifecycleMethod(bean, PreDestroy.class);
                    } catch (Exception e) {
                        // Log error but continue destroying others
                    }
                });
            }
        }
    }
}
