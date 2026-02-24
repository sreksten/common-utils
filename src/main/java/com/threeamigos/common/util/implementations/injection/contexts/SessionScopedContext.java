package com.threeamigos.common.util.implementations.injection.contexts;

import com.threeamigos.common.util.implementations.injection.BeanImpl;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of SessionScoped context.
 * Maintains instances for the duration of a user session.
 *
 * <p><b>PHASE 2 - Interceptor Support:</b> This context automatically wraps beans that
 * have interceptors with interceptor-aware proxies. This ensures that interceptor chains
 * are executed before business methods are called.
 *
 * <p><b>CDI 4.1 Passivation Support:</b> This context supports session passivation and activation
 * per CDI 4.1 Section 6.6.4. The {@link #passivateSession(String)} method serializes the session
 * state to a byte array, invoking {@code @PrePassivate} callbacks before serialization. The
 * {@link #activateSession(String, byte[])} method deserializes the session state and invokes
 * {@code @PostActivate} callbacks after deserialization. This allows session-scoped beans to
 * be serialized to disk or replicated across a cluster in distributed environments.
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

            // Step 1: Create the actual bean instance
            T instance = bean.create(creationalContext);

            // Step 2: PHASE 2 - Wrap with interceptor-aware proxy if bean has interceptors
            if (bean instanceof BeanImpl) {
                BeanImpl<T> beanImpl = (BeanImpl<T>) bean;
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
        String sessionId = currentSessionId.get();
        if (sessionId == null) {
            return null;
        }

        Map<Bean<?>, Object> instances = sessionInstances.get(sessionId);
        return instances != null ? (T) instances.get(bean) : null;
    }

    /**
     * Passivates (serializes) a session to a byte array.
     * <p>
     * <b>CDI 4.1 Section 6.6.4 - Passivation and passivating scopes:</b>
     * Before serialization, the container must invoke all @PrePassivate methods on beans
     * in the session scope. This allows beans to prepare for serialization by closing
     * non-serializable resources (database connections, file handles, etc.).
     * <p>
     * <b>Usage:</b> This method is typically called by the servlet container when the
     * HTTP session needs to be passivated to disk or replicated across a cluster.
     * <p>
     * <b>Thread Safety:</b> This method should be called while the session is not being
     * actively used by other threads.
     *
     * @param sessionId the session identifier to passivate
     * @return serialized byte array containing the session state, or null if session doesn't exist
     * @throws RuntimeException if serialization fails (wraps IOException)
     */
    @SuppressWarnings("unchecked")
    public byte[] passivateSession(String sessionId) {
        Map<Bean<?>, Object> instances = sessionInstances.get(sessionId);
        Map<Bean<?>, CreationalContext<?>> contexts = sessionContexts.get(sessionId);

        if (instances == null || contexts == null) {
            return null;
        }

        // Step 1: Invoke @PrePassivate on all beans in the session
        for (Map.Entry<Bean<?>, Object> entry : instances.entrySet()) {
            Bean<?> bean = entry.getKey();
            Object instance = entry.getValue();

            if (bean instanceof BeanImpl) {
                @SuppressWarnings("unchecked")
                BeanImpl<Object> beanImpl = (BeanImpl<Object>) bean;
                try {
                    beanImpl.invokePrePassivate(instance);
                } catch (Exception e) {
                    System.err.println("Error invoking @PrePassivate on bean " +
                        bean.getBeanClass().getName() + " in session " + sessionId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // Step 2: Serialize the session storage to byte array
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            // Create a serializable storage structure
            // Note: We serialize instances only, not CreationalContexts (they're not serializable)
            oos.writeObject(instances);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Session passivation failed for session " + sessionId, e);
        }
    }

    /**
     * Activates (deserializes) a session from a byte array and restores it.
     * <p>
     * <b>CDI 4.1 Section 6.6.4 - Passivation and passivating scopes:</b>
     * After deserialization, the container must invoke all @PostActivate methods on beans
     * in the session scope. This allows beans to restore state by re-opening non-serializable
     * resources (database connections, file handles, etc.).
     * <p>
     * <b>Usage:</b> This method is typically called by the servlet container when the
     * HTTP session is being restored from disk or received from cluster replication.
     * <p>
     * <b>Important:</b> After activation, the session is associated with the current thread.
     * Call this method before accessing any session-scoped beans.
     *
     * @param sessionId the session identifier
     * @param serializedData the serialized session data (from passivateSession)
     * @throws RuntimeException if deserialization fails or @PostActivate methods fail
     */
    @SuppressWarnings("unchecked")
    public void activateSession(String sessionId, byte[] serializedData) {
        if (serializedData == null) {
            throw new IllegalArgumentException("Serialized data cannot be null");
        }

        // Step 1: Deserialize the session storage
        Map<Bean<?>, Object> instances;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
             ObjectInputStream ois = new ObjectInputStream(bais)) {

            instances = (Map<Bean<?>, Object>) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Session activation failed for session " + sessionId, e);
        }

        // Step 2: Restore the session storage
        sessionInstances.put(sessionId, new ConcurrentHashMap<>(instances));
        // Note: CreationalContexts are not serializable, so we create an empty map
        // Beans that were already created won't need new CreationalContexts
        sessionContexts.put(sessionId, new ConcurrentHashMap<>());

        // Step 3: Associate session with current thread
        currentSessionId.set(sessionId);

        // Step 4: Invoke @PostActivate on all beans in the session
        for (Map.Entry<Bean<?>, Object> entry : instances.entrySet()) {
            Bean<?> bean = entry.getKey();
            Object instance = entry.getValue();

            if (bean instanceof BeanImpl) {
                @SuppressWarnings("unchecked")
                BeanImpl<Object> beanImpl = (BeanImpl<Object>) bean;
                try {
                    beanImpl.invokePostActivate(instance);
                } catch (Exception e) {
                    System.err.println("Error invoking @PostActivate on bean " +
                        bean.getBeanClass().getName() + " in session " + sessionId + ": " + e.getMessage());
                    e.printStackTrace();
                    // Continue with other beans even if one fails
                }
            }
        }
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
