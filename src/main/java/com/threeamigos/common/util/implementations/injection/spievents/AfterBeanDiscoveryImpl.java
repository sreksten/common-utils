package com.threeamigos.common.util.implementations.injection.spievents;

import com.threeamigos.common.util.implementations.injection.BeanManagerImpl;
import com.threeamigos.common.util.implementations.injection.contexts.ContextManager;
import com.threeamigos.common.util.implementations.injection.contexts.CustomContextAdapter;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.inject.spi.*;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import jakarta.enterprise.inject.spi.configurator.ObserverMethodConfigurator;

/**
 * AfterBeanDiscovery event implementation.
 * 
 * <p>Fired after bean discovery completes, before validation. Extensions can use this event to:
 * <ul>
 *   <li>Add custom beans via {@link #addBean()}</li>
 *   <li>Add observer methods via {@link #addObserverMethod()}</li>
 *   <li>Add custom contexts via {@link #addContext(Context)}</li>
 *   <li>Register deployment problems via {@link #addDefinitionError(Throwable)}</li>
 * </ul>
 *
 * @see jakarta.enterprise.inject.spi.AfterBeanDiscovery
 */
public class AfterBeanDiscoveryImpl implements AfterBeanDiscovery {

    private final KnowledgeBase knowledgeBase;
    private final BeanManager beanManager;

    public AfterBeanDiscoveryImpl(KnowledgeBase knowledgeBase, BeanManager beanManager) {
        this.knowledgeBase = knowledgeBase;
        this.beanManager = beanManager;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        knowledgeBase.addError("Definition error from extension: " + t.getMessage());
        System.out.println("AfterBeanDiscovery: addDefinitionError(" + t.getMessage() + ")");
    }

    @Override
    public void addBean(Bean<?> bean) {
        // TODO: Add custom bean to knowledge base
        System.out.println("AfterBeanDiscovery: addBean(" + bean.getBeanClass().getName() + ")");
    }

    @Override
    public <T> BeanConfigurator<T> addBean() {
        // TODO: Return configurator for programmatic bean building
        System.out.println("AfterBeanDiscovery: addBean()");
        throw new UnsupportedOperationException("BeanConfigurator not yet implemented");
    }

    @Override
    public <T> ObserverMethodConfigurator<T> addObserverMethod() {
        // TODO: Return configurator for programmatic observer method building
        System.out.println("AfterBeanDiscovery: addObserverMethod()");
        throw new UnsupportedOperationException("ObserverMethodConfigurator not yet implemented");
    }

    @Override
    public void addObserverMethod(ObserverMethod<?> observerMethod) {
        // TODO: Add observer method to knowledge base
        System.out.println("AfterBeanDiscovery: addObserverMethod(ObserverMethod)");
    }

    /**
     * Registers a custom context with the container.
     * <p>
     * This method allows portable extensions to register custom scopes by providing
     * a Context implementation. The context will be used for all beans with the
     * corresponding scope annotation.
     * <p>
     * <h3>Example:</h3>
     * <pre>{@code
     * public class MyExtension implements Extension {
     *     public void registerCustomScope(@Observes AfterBeanDiscovery event) {
     *         event.addContext(new MyCustomScopeContext());
     *     }
     * }
     *
     * // Custom context implementation
     * public class MyCustomScopeContext implements Context {
     *     public Class<? extends Annotation> getScope() {
     *         return MyCustomScope.class;
     *     }
     *
     *     public <T> T get(Contextual<T> contextual, CreationalContext<T> ctx) {
     *         // Custom scope logic
     *     }
     *
     *     public <T> T get(Contextual<T> contextual) {
     *         // Return existing instance or null
     *     }
     *
     *     public boolean isActive() {
     *         // Return whether this scope is currently active
     *     }
     * }
     * }</pre>
     *
     * @param context the custom context to register
     * @throws IllegalStateException if the BeanManager is not properly initialized
     * @throws IllegalArgumentException if context is null or if a context for the same scope already exists
     */
    @Override
    public void addContext(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        System.out.println("[AfterBeanDiscovery] Registering custom context for scope: " +
                          context.getScope().getName());

        // Get the ContextManager from BeanManager
        if (!(beanManager instanceof BeanManagerImpl)) {
            throw new IllegalStateException(
                "Cannot register context: BeanManager is not BeanManagerImpl. " +
                "This should never happen and indicates a container bug."
            );
        }

        BeanManagerImpl beanManagerImpl = (BeanManagerImpl) beanManager;
        ContextManager contextManager = beanManagerImpl.getContextManager();

        // Wrap the Jakarta CDI Context in our internal ScopeContext adapter
        CustomContextAdapter adaptedContext = new CustomContextAdapter(context);

        try {
            // Register the custom context with the scope annotation
            contextManager.registerContext(context.getScope(), adaptedContext);

            System.out.println("[AfterBeanDiscovery] Successfully registered custom context: " +
                              context.getClass().getName() +
                              " for scope @" + context.getScope().getSimpleName());
        } catch (Exception e) {
            String errorMsg = "Failed to register custom context for scope @" +
                             context.getScope().getSimpleName() + ": " + e.getMessage();
            System.err.println("[AfterBeanDiscovery] " + errorMsg);
            knowledgeBase.addError(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    @Override
    public <T> java.util.List<AnnotatedType<T>> getAnnotatedTypes(Class<T> type) {
        // TODO: Return annotated types for the given class
        System.out.println("AfterBeanDiscovery: getAnnotatedTypes(" + type.getName() + ")");
        return new java.util.ArrayList<>();
    }

    @Override
    public <T> AnnotatedType<T> getAnnotatedType(Class<T> type, String id) {
        // TODO: Return specific annotated type by ID
        System.out.println("AfterBeanDiscovery: getAnnotatedType(" + type.getName() + ", id=" + id + ")");
        return null;
    }
}
