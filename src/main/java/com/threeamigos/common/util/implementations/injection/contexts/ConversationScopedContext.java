package com.threeamigos.common.util.implementations.injection.contexts;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of ConversationScoped context.
 * Maintains instances for the duration of a conversation, which spans multiple requests
 * and must be explicitly started and ended.
 *
 * @author Stefano Reksten
 */
public class ConversationScopedContext implements ScopeContext {

    private final Map<String, Map<Bean<?>, Object>> conversationInstances = new ConcurrentHashMap<>();
    private final Map<String, Map<Bean<?>, CreationalContext<?>>> conversationContexts = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentConversationId = new ThreadLocal<>();
    private volatile boolean active = true;

    /**
     * Begins a new conversation with the given ID.
     *
     * @param conversationId the unique identifier for this conversation
     */
    public void beginConversation(String conversationId) {
        currentConversationId.set(conversationId);
        conversationInstances.putIfAbsent(conversationId, new ConcurrentHashMap<>());
        conversationContexts.putIfAbsent(conversationId, new ConcurrentHashMap<>());
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Bean<T> bean, CreationalContext<T> creationalContext) {
        if (!active) {
            throw new IllegalStateException("ConversationScoped context is not active");
        }

        String conversationId = currentConversationId.get();
        if (conversationId == null) {
            throw new IllegalStateException("No active conversation. Call beginConversation() first.");
        }

        Map<Bean<?>, Object> instances = conversationInstances.get(conversationId);
        Map<Bean<?>, CreationalContext<?>> contexts = conversationContexts.get(conversationId);

        return (T) instances.computeIfAbsent(bean, b -> {
            contexts.put(bean, creationalContext);
            return bean.create(creationalContext);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getIfExists(Bean<T> bean) {
        String conversationId = currentConversationId.get();
        if (conversationId == null) {
            return null;
        }

        Map<Bean<?>, Object> instances = conversationInstances.get(conversationId);
        return instances != null ? (T) instances.get(bean) : null;
    }

    @Override
    public void destroy() {
        for (String conversationId : conversationInstances.keySet()) {
            destroyConversation(conversationId);
        }
        conversationInstances.clear();
        conversationContexts.clear();
        active = false;
    }

    @Override
    public boolean isActive() {
        return active && currentConversationId.get() != null;
    }

    @SuppressWarnings("unchecked")
    private void destroyConversation(String conversationId) {
        Map<Bean<?>, Object> instances = conversationInstances.remove(conversationId);
        Map<Bean<?>, CreationalContext<?>> contexts = conversationContexts.remove(conversationId);

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
