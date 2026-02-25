package com.threeamigos.common.util.implementations.injection.contexts;

import com.threeamigos.common.util.implementations.injection.BeanImpl;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Implementation of ConversationScoped context.
 * Maintains instances for the duration of a conversation, which spans multiple requests
 * and must be explicitly started and ended.
 *
 * <p><b>PHASE 2 - Interceptor Support:</b> This context automatically wraps beans that
 * have interceptors with interceptor-aware proxies. This ensures that interceptor chains
 * are executed before business methods are called.
 *
 * <p><b>CDI 4.1 Conversation Timeout:</b> This context implements automatic timeout handling
 * per CDI 4.1 Section 6.7.4. Conversations that are inactive for longer than the configured
 * timeout period are automatically destroyed. The default timeout is 30 minutes, but can be
 * configured via {@link #setDefaultTimeout(long, TimeUnit)}. Each conversation access updates
 * the last access time, preventing premature timeout.
 *
 * @author Stefano Reksten
 */
public class ConversationScopedContext implements ScopeContext {

    private final Map<String, Map<Bean<?>, Object>> conversationInstances = new ConcurrentHashMap<>();
    private final Map<String, Map<Bean<?>, CreationalContext<?>>> conversationContexts = new ConcurrentHashMap<>();
    private final Map<String, ConversationMetadata> conversationMetadata = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentConversationId = new ThreadLocal<>();
    private volatile boolean active = true;

    // Timeout configuration (default 30 minutes per CDI spec)
    private volatile long defaultTimeoutMillis = 30 * 60 * 1000; // 30 minutes

    // Scheduled cleanup for timed-out conversations
    private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "ConversationTimeout-Cleaner");
        t.setDaemon(true); // Don't prevent JVM shutdown
        return t;
    });

    /**
     * Metadata for tracking conversation timeout.
     * Thread-safe via volatile fields and atomic operations.
     */
    private static class ConversationMetadata {
        private volatile long lastAccessTime;
        private volatile long timeoutMillis;

        ConversationMetadata(long timeoutMillis) {
            this.lastAccessTime = System.currentTimeMillis();
            this.timeoutMillis = timeoutMillis;
        }

        void touch() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        boolean isTimedOut() {
            return System.currentTimeMillis() - lastAccessTime > timeoutMillis;
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }

        long getTimeoutMillis() {
            return timeoutMillis;
        }

        void setTimeout(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }
    }

    /**
     * Constructor that starts the timeout cleanup scheduler.
     * The scheduler runs every 5 minutes to clean up expired conversations.
     */
    public ConversationScopedContext() {
        // Schedule cleanup every 5 minutes
        timeoutScheduler.scheduleAtFixedRate(
            this::cleanupTimedOutConversations,
            5, // initial delay
            5, // period
            TimeUnit.MINUTES
        );
    }

    /**
     * Sets the default timeout for new conversations.
     * Existing conversations are not affected.
     *
     * @param timeout the timeout value
     * @param unit the time unit
     */
    public void setDefaultTimeout(long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        this.defaultTimeoutMillis = unit.toMillis(timeout);
    }

    /**
     * Gets the default timeout in milliseconds.
     *
     * @return the default timeout
     */
    public long getDefaultTimeoutMillis() {
        return defaultTimeoutMillis;
    }

    /**
     * Sets the timeout for a specific conversation.
     *
     * @param conversationId the conversation ID
     * @param timeout the timeout value
     * @param unit the time unit
     */
    public void setTimeout(String conversationId, long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        ConversationMetadata metadata = conversationMetadata.get(conversationId);
        if (metadata != null) {
            metadata.setTimeout(unit.toMillis(timeout));
        }
    }

    /**
     * Gets the timeout for a specific conversation in milliseconds.
     *
     * @param conversationId the conversation ID
     * @return the timeout in milliseconds, or -1 if conversation doesn't exist
     */
    public long getTimeout(String conversationId) {
        ConversationMetadata metadata = conversationMetadata.get(conversationId);
        return metadata != null ? metadata.getTimeoutMillis() : -1;
    }

    /**
     * Gets the last access time for a specific conversation.
     *
     * @param conversationId the conversation ID
     * @return the last access time in milliseconds since epoch, or -1 if conversation doesn't exist
     */
    public long getLastAccessTime(String conversationId) {
        ConversationMetadata metadata = conversationMetadata.get(conversationId);
        return metadata != null ? metadata.getLastAccessTime() : -1;
    }

    /**
     * Begins a new conversation with the given ID and default timeout.
     *
     * @param conversationId the unique identifier for this conversation
     */
    public void beginConversation(String conversationId) {
        currentConversationId.set(conversationId);
        conversationInstances.putIfAbsent(conversationId, new ConcurrentHashMap<>());
        conversationContexts.putIfAbsent(conversationId, new ConcurrentHashMap<>());
        conversationMetadata.putIfAbsent(conversationId, new ConversationMetadata(defaultTimeoutMillis));
    }

    /**
     * Begins a new conversation with the given ID and custom timeout.
     *
     * @param conversationId the unique identifier for this conversation
     * @param timeout the timeout value
     * @param unit the time unit
     */
    public void beginConversation(String conversationId, long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        currentConversationId.set(conversationId);
        conversationInstances.putIfAbsent(conversationId, new ConcurrentHashMap<>());
        conversationContexts.putIfAbsent(conversationId, new ConcurrentHashMap<>());
        conversationMetadata.putIfAbsent(conversationId, new ConversationMetadata(unit.toMillis(timeout)));
    }

    /**
     * Ends the current conversation, destroying all associated instances.
     */
    public void endConversation() {
        String conversationId = currentConversationId.get();
        if (conversationId != null) {
            destroyConversation(conversationId);
            currentConversationId.remove();
        }
    }

    /**
     * Ends a specific conversation by ID.
     *
     * @param conversationId the conversation to end
     */
    public void endConversation(String conversationId) {
        destroyConversation(conversationId);
        if (conversationId.equals(currentConversationId.get())) {
            currentConversationId.remove();
        }
    }

    /**
     * Gets the current conversation ID.
     *
     * @return the current conversation ID, or null if no conversation is active
     */
    public String getCurrentConversationId() {
        return currentConversationId.get();
    }

    /**
     * Touches the current conversation, updating its last access time.
     * This prevents the conversation from timing out.
     */
    private void touchCurrentConversation() {
        String conversationId = currentConversationId.get();
        if (conversationId != null) {
            ConversationMetadata metadata = conversationMetadata.get(conversationId);
            if (metadata != null) {
                metadata.touch();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Bean<T> bean, CreationalContext<T> creationalContext) {
        if (!active) {
            throw new ContextNotActiveException("ConversationScoped context is not active");
        }

        String conversationId = currentConversationId.get();
        if (conversationId == null) {
            throw new ContextNotActiveException("No active conversation. Call beginConversation() first.");
        }

        // Touch conversation to update last access time
        touchCurrentConversation();

        Map<Bean<?>, Object> instances = conversationInstances.get(conversationId);
        Map<Bean<?>, CreationalContext<?>> contexts = conversationContexts.get(conversationId);

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
        String conversationId = currentConversationId.get();
        if (conversationId == null) {
            return null;
        }

        // Touch conversation to update last access time
        touchCurrentConversation();

        Map<Bean<?>, Object> instances = conversationInstances.get(conversationId);
        return instances != null ? (T) instances.get(bean) : null;
    }

    @Override
    public void destroy() {
        // Shutdown timeout scheduler
        timeoutScheduler.shutdown();
        try {
            if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Destroy all conversations
        for (String conversationId : conversationInstances.keySet()) {
            destroyConversation(conversationId);
        }
        conversationInstances.clear();
        conversationContexts.clear();
        conversationMetadata.clear();
        active = false;
    }

    @Override
    public boolean isActive() {
        return active && currentConversationId.get() != null;
    }

    @Override
    public boolean isPassivationCapable() {
        // ConversationScoped beans CAN be passivated (serialized to disk/database)
        // when long-running conversations are passivated by the servlet container
        // Therefore, beans in this scope MUST be Serializable
        return true;
    }

    /**
     * Cleans up timed-out conversations.
     * Called periodically by the timeout scheduler.
     */
    private void cleanupTimedOutConversations() {
        if (!active) {
            return;
        }

        for (Map.Entry<String, ConversationMetadata> entry : conversationMetadata.entrySet()) {
            String conversationId = entry.getKey();
            ConversationMetadata metadata = entry.getValue();

            if (metadata.isTimedOut()) {
                System.out.println("Conversation " + conversationId + " timed out after " +
                    TimeUnit.MILLISECONDS.toMinutes(metadata.getTimeoutMillis()) + " minutes of inactivity. Destroying...");
                destroyConversation(conversationId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void destroyConversation(String conversationId) {
        Map<Bean<?>, Object> instances = conversationInstances.remove(conversationId);
        Map<Bean<?>, CreationalContext<?>> contexts = conversationContexts.remove(conversationId);
        conversationMetadata.remove(conversationId);

        if (instances != null && contexts != null) {
            for (Map.Entry<Bean<?>, Object> entry : instances.entrySet()) {
                Bean<Object> bean = (Bean<Object>) entry.getKey();
                Object instance = entry.getValue();
                CreationalContext<Object> ctx = (CreationalContext<Object>) contexts.get(bean);

                try {
                    bean.destroy(instance, ctx);
                } catch (Exception e) {
                    System.err.println("Error destroying bean " + bean.getBeanClass().getName() +
                                     " in conversation " + conversationId + ": " + e.getMessage());
                }
            }
        }
    }
}
