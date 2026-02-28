package com.threeamigos.common.util.implementations.injection.builtinbeans;

import com.threeamigos.common.util.implementations.injection.scopes.ConversationImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Conversation;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Built-in bean for injecting the Conversation object for conversation scope management.
 *
 * <p>CDI 4.1 Specification (Section 6.7.4) requires that the Conversation
 * is available for injection to allow programmatic conversation lifecycle control:
 * <pre>{@code
 * @Inject Conversation conversation;
 *
 * public String startWizard() {
 *     conversation.begin();
 *     return "wizard-step1";
 * }
 *
 * public String completeWizard() {
 *     conversation.end();
 *     return "wizard-complete";
 * }
 * }</pre>
 *
 * <p><b>Conversation Usage:</b>
 * <ul>
 *   <li>Inject Conversation to control the conversation lifecycle</li>
 *   <li>Call {@code begin()} to promote from transient to long-running</li>
 *   <li>Call {@code end()} to terminate and destroy conversation-scoped beans</li>
 *   <li>Set timeout via {@code setTimeout()} to control an inactivity period</li>
 * </ul>
 *
 * <p><b>Bean Characteristics:</b>
 * <ul>
 *   <li>Type: Conversation</li>
 *   <li>Scope: @ApplicationScoped (singleton, but uses ThreadLocal internally)</li>
 *   <li>Qualifiers: @Default, @Any</li>
 *   <li>Stereotypes: None</li>
 *   <li>Alternative: No</li>
 * </ul>
 *
 * <p><b>Implementation Note:</b> Although the bean is @ApplicationScoped (singleton),
 * the ConversationImpl uses ThreadLocal to maintain the per-request conversation state,
 * making it effectively request-scoped in behavior while being a singleton in the container.
 *
 * @author Stefano Reksten
 * @see Conversation
 * @see ConversationImpl
 */
public class ConversationBean implements Bean<Conversation> {

    private final Conversation conversationInstance;

    /**
     * Creates a built-in bean for Conversation.
     */
    public ConversationBean() {
        // Create a single ConversationImpl instance
        // It uses ThreadLocal internally for per-request state
        this.conversationInstance = new ConversationImpl();
    }

    @Override
    public Class<?> getBeanClass() {
        return Conversation.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet(); // Built-in beans have no injection points
    }

    @Override
    public Conversation create(CreationalContext<Conversation> context) {
        // Return the singleton instance (ThreadLocal handles per-request state)
        return conversationInstance;
    }

    @Override
    public void destroy(Conversation instance, CreationalContext<Conversation> context) {
        // Singleton instance is never destroyed
        // ThreadLocal cleanup should happen at the end of the request via ConversationImpl.clearCurrentConversation()
        if (context != null) {
            context.release();
        }
    }

    @Override
    public Set<Type> getTypes() {
        Set<Type> types = new HashSet<>();
        types.add(Conversation.class);
        types.add(Object.class);
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(Default.Literal.INSTANCE);
        qualifiers.add(Any.Literal.INSTANCE);
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        // @ApplicationScoped - singleton bean
        // (ConversationImpl uses ThreadLocal for per-request state)
        return ApplicationScoped.class;
    }

    @Override
    public String getName() {
        return null; // Not a named bean
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String toString() {
        return "ConversationBean[type=Conversation, scope=@ApplicationScoped, qualifiers=@Default]";
    }
}
