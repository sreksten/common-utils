package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.contexts.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all scoped contexts for the CDI container.
 * Maps scope annotations to their corresponding context implementations.
 *
 * @author Stefano Reksten
 */
class ContextManager {

    private final Map<Class<? extends Annotation>, ScopeContext> contexts = new ConcurrentHashMap<>();
    private final ConversationScopedContext conversationContext;
    private final SessionScopedContext sessionContext;
    private final RequestScopedContext requestContext;

    ContextManager() {
        // Initialize built-in contexts
        contexts.put(ApplicationScoped.class, new ApplicationScopedContext());
        contexts.put(Dependent.class, new DependentContext());

        // Initialize and register conversation, session, and request contexts
        conversationContext = new ConversationScopedContext();
        sessionContext = new SessionScopedContext();
        requestContext = new RequestScopedContext();

        contexts.put(ConversationScoped.class, conversationContext);
        contexts.put(SessionScoped.class, sessionContext);
        contexts.put(RequestScoped.class, requestContext);
    }

    /**
     * Gets the context for a given scope annotation.
     *
     * @param scopeAnnotation the scope annotation class
     * @return the corresponding context
     * @throws IllegalArgumentException if the scope is not supported
     */
    ScopeContext getContext(Class<? extends Annotation> scopeAnnotation) {
        ScopeContext context = contexts.get(scopeAnnotation);
        if (context == null) {
            throw new IllegalArgumentException("Unsupported scope: " + scopeAnnotation.getName());
        }
        return context;
    }

    /**
     * Destroys all contexts and their instances.
     */
    void destroyAll() {
        for (ScopeContext context : contexts.values()) {
            try {
                context.destroy();
            } catch (Exception e) {
                System.err.println("Error destroying context: " + e.getMessage());
            }
        }
    }

    // === Conversation Scope Management ===

    /**
     * Begins a new conversation with the given ID.
     *
     * @param conversationId the unique identifier for this conversation
     */
    void beginConversation(String conversationId) {
        conversationContext.beginConversation(conversationId);
    }

    /**
     * Ends the current conversation.
     */
    void endConversation() {
        conversationContext.endConversation();
    }

    /**
     * Ends a specific conversation by ID.
     *
     * @param conversationId the conversation to end
     */
    void endConversation(String conversationId) {
        conversationContext.endConversation(conversationId);
    }

    /**
     * Gets the current conversation ID.
     *
     * @return the current conversation ID, or null if no conversation is active
     */
    String getCurrentConversationId() {
        return conversationContext.getCurrentConversationId();
    }

    // === Session Scope Management ===

    /**
     * Activates a session for the current thread.
     *
     * @param sessionId the session identifier
     */
    void activateSession(String sessionId) {
        sessionContext.activateSession(sessionId);
    }

    /**
     * Deactivates the session from the current thread.
     */
    void deactivateSession() {
        sessionContext.deactivateSession();
    }

    /**
     * Invalidates and destroys a specific session.
     *
     * @param sessionId the session to invalidate
     */
    void invalidateSession(String sessionId) {
        sessionContext.invalidateSession(sessionId);
    }

    /**
     * Gets the current session ID.
     *
     * @return the current session ID, or null if no session is active
     */
    String getCurrentSessionId() {
        return sessionContext.getCurrentSessionId();
    }

    // === Request Scope Management ===

    /**
     * Activates the request scope for the current thread.
     */
    void activateRequest() {
        requestContext.activateRequest();
    }

    /**
     * Deactivates the request scope for the current thread.
     */
    void deactivateRequest() {
        requestContext.deactivateRequest();
    }
}
