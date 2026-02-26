package com.threeamigos.common.util.implementations.injection.contexts;

import com.threeamigos.common.util.implementations.injection.BeanImpl;
import com.threeamigos.common.util.implementations.injection.LifecycleMethodHelper;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.PassivationCapable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    private final Map<String, Map<String, Object>> sessionInstances = new ConcurrentHashMap<>();
    private final Map<String, Map<String, CreationalContext<?>>> sessionContexts = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Bean<?>>> sessionBeans = new ConcurrentHashMap<>();
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
        sessionBeans.putIfAbsent(sessionId, new ConcurrentHashMap<>());
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
            throw new ContextNotActiveException("SessionScoped context is not active");
        }

        String sessionId = currentSessionId.get();
        if (sessionId == null) {
            throw new ContextNotActiveException("No active session. Call activateSession() first.");
        }

        Map<String, Object> instances = sessionInstances.get(sessionId);
        Map<String, CreationalContext<?>> contexts = sessionContexts.get(sessionId);
        Map<String, Bean<?>> beans = sessionBeans.get(sessionId);

        String beanId = getBeanId(bean);

        return (T) instances.computeIfAbsent(beanId, b -> {
            contexts.put(beanId, creationalContext);
            beans.put(beanId, bean);

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

        Map<String, Object> instances = sessionInstances.get(sessionId);
        String beanId = getBeanId(bean);
        return instances != null ? (T) instances.get(beanId) : null;
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
        Map<String, Object> instances = sessionInstances.get(sessionId);
        Map<String, Bean<?>> beans = sessionBeans.get(sessionId);

        if (instances == null || instances.isEmpty()) {
            return null;
        }

        // Step 1: Invoke @PrePassivate on all beans in the session
        if (beans != null) {
            for (Map.Entry<String, Object> entry : instances.entrySet()) {
                Bean<?> bean = beans.get(entry.getKey());
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
                } else if (bean == null) {
                    // Fallback: invoke annotation directly if metadata is missing
                    invokeAnnotationIfPresent(instance, "jakarta.ejb.PrePassivate");
                }
            }
        }

        // Step 2: Serialize the session storage to byte array (instances + bean class metadata)
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            Map<String, Object> serializableInstances = new HashMap<>(instances);
            Map<String, String> beanClasses = new HashMap<>();
            if (beans != null) {
                beans.forEach((id, b) -> beanClasses.put(id, b.getBeanClass().getName()));
            }

            SessionPassivationData data = new SessionPassivationData(serializableInstances, beanClasses);
            oos.writeObject(data);
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

        SessionPassivationData data;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            data = (SessionPassivationData) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Session activation failed for session " + sessionId, e);
        }

        // Step 2: Restore the session storage
        Map<String, Object> restoredInstances = new ConcurrentHashMap<>(data.getInstances());
        sessionInstances.put(sessionId, restoredInstances);
        sessionContexts.put(sessionId, new ConcurrentHashMap<>());
        Map<String, Bean<?>> beans = sessionBeans.computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>());

        // Try to re-associate Bean metadata (for lifecycle callbacks) using BeanManager
        BeanManager beanManager = CDI.current().getBeanManager();
        for (Map.Entry<String, String> entry : data.getBeanClasses().entrySet()) {
            String beanId = entry.getKey();
            if (beans.containsKey(beanId)) {
                continue; // already present (same JVM passivation)
            }

            Bean<?> bean = resolveBean(beanManager, beanId, entry.getValue());
            if (bean != null) {
                beans.put(beanId, bean);
            }
        }

        // Step 3: Associate session with current thread
        currentSessionId.set(sessionId);

        // Step 4: Invoke @PostActivate on all beans in the session
        for (Map.Entry<String, Object> entry : restoredInstances.entrySet()) {
            Bean<?> bean = beans.get(entry.getKey());
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
                }
            } else {
                // Fallback when bean metadata isn't available
                invokeAnnotationIfPresent(instance, "jakarta.ejb.PostActivate");
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
        sessionBeans.clear();
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
        Map<String, Object> instances = sessionInstances.remove(sessionId);
        Map<String, CreationalContext<?>> contexts = sessionContexts.remove(sessionId);
        Map<String, Bean<?>> beans = sessionBeans.remove(sessionId);

        if (instances != null && beans != null) {
            for (Map.Entry<String, Object> entry : instances.entrySet()) {
                @SuppressWarnings("unchecked")
                Bean<Object> bean = (Bean<Object>) beans.get(entry.getKey());
                Object instance = entry.getValue();
                CreationalContext<Object> ctx = contexts != null ? (CreationalContext<Object>) contexts.get(entry.getKey()) : null;

                if (bean == null) {
                    continue;
                }

                try {
                    bean.destroy(instance, ctx);
                } catch (Exception e) {
                    System.err.println("Error destroying bean " + bean.getBeanClass().getName() +
                                     " in session " + sessionId + ": " + e.getMessage());
                }
            }
        }
    }

    private String getBeanId(Bean<?> bean) {
        if (bean instanceof PassivationCapable) {
            String id = ((PassivationCapable) bean).getId();
            if (id != null) {
                return id;
            }
        }

        String qualifierSignature = bean.getQualifiers().stream()
            .map(Annotation::annotationType)
            .map(Class::getName)
            .sorted()
            .collect(Collectors.joining(","));

        return bean.getBeanClass().getName() + "|" + qualifierSignature;
    }

    private Bean<?> resolveBean(BeanManager beanManager, String beanId, String className) {
        try {
            Bean<?> bean = beanManager.getPassivationCapableBean(beanId);
            if (bean != null) {
                return bean;
            }
        } catch (Exception ignored) {
            // getPassivationCapableBean may throw if ID not found; ignore and fallback
        }

        try {
            Class<?> clazz = Class.forName(className);
            Set<Bean<?>> candidates = beanManager.getBeans(clazz);
            if (!candidates.isEmpty()) {
                return beanManager.resolve(candidates);
            }
        } catch (ClassNotFoundException ignored) {
            // Class disappeared between passivation/activation
        }
        return null;
    }

    private void invokeAnnotationIfPresent(Object instance, String annotationClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> annClass =
                (Class<? extends Annotation>) Class.forName(annotationClassName);

            for (Class<?> clazz : LifecycleMethodHelper.buildHierarchy(instance)) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(annClass)) {
                        method.setAccessible(true);
                        method.invoke(instance);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // jakarta.ejb not on classpath; nothing to do
        } catch (Exception e) {
            System.err.println("Error invoking @" + annotationClassName + " on " +
                instance.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Serializable wrapper for passivated session state. Bean metadata is reduced to class names
     * so that instances can still be restored even if Bean references are not serializable.
     */
    private static class SessionPassivationData implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Map<String, Object> instances;
        private final Map<String, String> beanClasses;

        SessionPassivationData(Map<String, Object> instances, Map<String, String> beanClasses) {
            this.instances = instances;
            this.beanClasses = beanClasses;
        }

        Map<String, Object> getInstances() {
            return instances;
        }

        Map<String, String> getBeanClasses() {
            return beanClasses;
        }
    }
}
