package com.threeamigos.common.util.implementations.injection.scopes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.spi.Bean;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all scoped contexts for the CDI container.
 * Maps scope annotations to their corresponding context implementations.
 *
 * @author Stefano Reksten
 */
public class ContextManager {

    private final Map<Class<? extends Annotation>, ScopeContext> contexts = new ConcurrentHashMap<>();
    private final ConversationScopedContext conversationContext;
    private final SessionScopedContext sessionContext;
    private final RequestScopedContext requestContext;
    private final ClientProxyGenerator proxyGenerator;

    public ContextManager() {
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

        // Initialize proxy generator
        proxyGenerator = new ClientProxyGenerator(this);
    }

    /**
     * Gets the context for a given scope annotation.
     *
     * @param scopeAnnotation the scope annotation class
     * @return the corresponding context
     * @throws IllegalArgumentException if the scope is not supported
     */
    public ScopeContext getContext(Class<? extends Annotation> scopeAnnotation) {
        ScopeContext context = contexts.get(scopeAnnotation);
        if (context == null) {
            throw new IllegalArgumentException("Unsupported scope: " + scopeAnnotation.getName());
        }
        return context;
    }

    /**
     * Destroys all contexts and their instances.
     */
    public void destroyAll() {
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
    public void beginConversation(String conversationId) {
        conversationContext.beginConversation(conversationId);
    }

    /**
     * Ends the current conversation.
     */
    public void endConversation() {
        conversationContext.endConversation();
    }

    /**
     * Ends a specific conversation by ID.
     *
     * @param conversationId the conversation to end
     */
    public void endConversation(String conversationId) {
        conversationContext.endConversation(conversationId);
    }

    /**
     * Gets the current conversation ID.
     *
     * @return the current conversation ID, or null if no conversation is active
     */
    public String getCurrentConversationId() {
        return conversationContext.getCurrentConversationId();
    }

    // === Session Scope Management ===

    /**
     * Activates a session for the current thread.
     *
     * @param sessionId the session identifier
     */
    public void activateSession(String sessionId) {
        sessionContext.activateSession(sessionId);
    }

    /**
     * Deactivates the session from the current thread.
     */
    public void deactivateSession() {
        sessionContext.deactivateSession();
    }

    /**
     * Invalidates and destroys a specific session.
     *
     * @param sessionId the session to invalidate
     */
    public void invalidateSession(String sessionId) {
        sessionContext.invalidateSession(sessionId);
    }

    /**
     * Gets the current session ID.
     *
     * @return the current session ID, or null if no session is active
     */
    public String getCurrentSessionId() {
        return sessionContext.getCurrentSessionId();
    }

    // === Request Scope Management ===

    /**
     * Activates the request scope for the current thread.
     */
    public void activateRequest() {
        requestContext.activateRequest();
    }

    /**
     * Deactivates the request scope for the current thread.
     */
    public void deactivateRequest() {
        requestContext.deactivateRequest();
    }

    // === Proxy Management ===

    /**
     * Checks if a scope annotation represents a normal scope (requires proxies).
     * <p>
     * Normal scopes in CDI:
     * - @ApplicationScoped
     * - @RequestScoped
     * - @SessionScoped
     * - @ConversationScoped
     * <p>
     * Pseudo-scopes (no proxies needed):
     * - @Dependent
     *
     * @param scopeAnnotation the scope annotation to check
     * @return true if this is a normal scope that requires proxies
     */
    public boolean isNormalScope(Class<? extends Annotation> scopeAnnotation) {
        // Dependent is a pseudo-scope, not a normal scope
        if (scopeAnnotation == Dependent.class) {
            return false;
        }

        // All other registered scopes are normal scopes
        return contexts.containsKey(scopeAnnotation);
    }

    /**
     * Creates a client proxy for a bean.
     * <p>
     * This is called during bean creation for normal-scoped beans.
     * The proxy will delegate all method calls to the contextual instance
     * from the appropriate scope.
     *
     * @param bean the bean to create a proxy for
     * @param <T> the bean type
     * @return a client proxy instance
     */
    public <T> T createClientProxy(Bean<T> bean) {
        return proxyGenerator.createProxy(bean);
    }

    /**
     * Registers a custom scope context programmatically.
     *
     * <p>This allows runtime registration of custom scopes, useful for:
     * <ul>
     *   <li>Testing with custom scopes</li>
     *   <li>Legacy ScopeHandler adaptation</li>
     *   <li>Dynamic scope registration</li>
     * </ul>
     *
     * @param scopeAnnotation the scope annotation class
     * @param context the scope context implementation
     * @throws IllegalArgumentException if any parameter is null
     */
    public void registerContext(Class<? extends Annotation> scopeAnnotation, ScopeContext context) {
        if (scopeAnnotation == null) {
            throw new IllegalArgumentException("Scope annotation cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        contexts.put(scopeAnnotation, context);
    }
}
