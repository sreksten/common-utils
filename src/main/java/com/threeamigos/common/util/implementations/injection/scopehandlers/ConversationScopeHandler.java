package com.threeamigos.common.util.implementations.injection.scopehandlers;

import com.threeamigos.common.util.implementations.injection.LifecycleMethodHelper;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Deprecated
public class ConversationScopeHandler implements ScopeHandler {
    private final Map<String, Map<Class<?>, Object>> conversations = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentConversation = new ThreadLocal<>();

    public void beginConversation(String conversationId) {
        currentConversation.set(conversationId);
        conversations.putIfAbsent(conversationId, new ConcurrentHashMap<>());
    }

    public void endConversation(String conversationId) {
        Map<Class<?>, Object> beans = conversations.remove(conversationId);
        if (beans != null) {
            beans.values().forEach(bean -> {
                try {
                    LifecycleMethodHelper.invokeLifecycleMethod(bean, PreDestroy.class);
                } catch (Exception e) {
                    // Log error but continue destroying others
                }
            });
        }
        currentConversation.remove();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz, Supplier<T> provider) {
        String conversationId = currentConversation.get();
        if (conversationId == null) throw new IllegalStateException("No active conversation");

        Map<Class<?>, Object> beans = conversations.get(conversationId);
        if (beans == null) throw new IllegalStateException("Conversation not found: " + conversationId);

        return (T) beans.computeIfAbsent(clazz, k -> provider.get());
    }

    @Override
    public void close() throws Exception {
        String conversationId = currentConversation.get();
        if (conversationId != null) {
            endConversation(conversationId);
        }
    }
}
