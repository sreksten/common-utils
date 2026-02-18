package com.threeamigos.common.util.implementations.injection.contexts;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of SessionScoped context.
 * Maintains instances for the duration of a user session.
 *
 * @author Stefano Reksten
 */
public class SessionScopedContext implements ScopeContext {

    private final Map<String, Map<Bean<?>, Object>> sessionInstances = new ConcurrentHashMap<>();
    private final Map<String, Map<Bean<?>, CreationalContext<?>>> sessionContexts = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentSessionId = new ThreadLocal<>();
    private volatile boolean active = true;

    /**
     * Associates a session ID with the current thread.
     *
     * @param sessionId the session identifier
     */
    public void activateSession(String sessionId) {
        currentSessionId.set(sessionId);
        sessionInstances.putIfAbsent(sessionId, new ConcurrentHashMap<>());
        sessionContexts.putIfAbsent(sessionId, new ConcurrentHashMap<>());
    }

    /**
     * Disassociates the session from the current thread.
     */
    public void deactivateSession() {
        currentSessionId.remove();
    }

    /**
     * Invalidates and destroys a specific session.
     *
     * @param sessionId the session to invalidate
     */
    public void invalidateSession(String sessionId) {
        destroySession(sessionId);
        if (sessionId.equals(currentSessionId.get())) {
            currentSessionId.remove();
        }
    }

    /**
     * Gets the current session ID.
     *
     * @return the current session ID, or null if no session is active
     */
    public String getCurrentSessionId() {
        return currentSessionId.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Bean<T> bean, CreationalContext<T> creationalContext) {
        if (!active) {
            throw new IllegalStateException("SessionScoped context is not active");
        }

        String sessionId = currentSessionId.get();
        if (sessionId == null) {
            throw new IllegalStateException("No active session. Call activateSession() first.");
        }

        Map<Bean<?>, Object> instances = sessionInstances.get(sessionId);
        Map<Bean<?>, CreationalContext<?>> contexts = sessionContexts.get(sessionId);

        return (T) instances.computeIfAbsent(bean, b -> {
            contexts.put(bean, creationalContext);
            return bean.create(creationalContext);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getIfExists(Bean<T> bean) {
        String sessionId = currentSessionId.get();
        if (sessionId == null) {
            return null;
        }

        Map<Bean<?>, Object> instances = sessionInstances.get(sessionId);
        return instances != null ? (T) instances.get(bean) : null;
    }

    @Override
    public void destroy() {
        for (String sessionId : sessionInstances.keySet()) {
            destroySession(sessionId);
        }
        sessionInstances.clear();
        sessionContexts.clear();
        active = false;
    }

    @Override
    public boolean isActive() {
        return active && currentSessionId.get() != null;
    }

    @Override
    public boolean isPassivationCapable() {
        // SessionScoped beans CAN be passivated (serialized to disk/database)
        // when the HTTP session is passivated by the servlet container
        // Therefore, beans in this scope MUST be Serializable
        return true;
    }

    @SuppressWarnings("unchecked")
    private void destroySession(String sessionId) {
        Map<Bean<?>, Object> instances = sessionInstances.remove(sessionId);
        Map<Bean<?>, CreationalContext<?>> contexts = sessionContexts.remove(sessionId);

        if (instances != null && contexts != null) {
            for (Map.Entry<Bean<?>, Object> entry : instances.entrySet()) {
                Bean<Object> bean = (Bean<Object>) entry.getKey();
                Object instance = entry.getValue();
                CreationalContext<Object> ctx = (CreationalContext<Object>) contexts.get(bean);

                try {
                    bean.destroy(instance, ctx);
                } catch (Exception e) {
                    System.err.println("Error destroying bean " + bean.getBeanClass().getName() +
                                     " in session " + sessionId + ": " + e.getMessage());
                }
            }
        }
    }
}
