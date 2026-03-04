package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.injection.scopes.ContextManager;
import com.threeamigos.common.util.implementations.injection.scopes.CustomContextAdapter;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.implementations.injection.spi.configurators.BeanConfiguratorImpl;
import com.threeamigos.common.util.implementations.injection.spi.configurators.ObserverMethodConfiguratorImpl;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.inject.spi.*;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import jakarta.enterprise.inject.spi.configurator.ObserverMethodConfigurator;

import java.util.ArrayList;
import java.util.List;

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
public class AfterBeanDiscoveryImpl extends PhaseAware implements AfterBeanDiscovery {

    private final KnowledgeBase knowledgeBase;
    private final BeanManager beanManager;

    public AfterBeanDiscoveryImpl(MessageHandler messageHandler, KnowledgeBase knowledgeBase, BeanManager beanManager) {
        super(messageHandler);
        this.knowledgeBase = knowledgeBase;
        this.beanManager = beanManager;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        knowledgeBase.addDefinitionError(Phase.AFTER_BEAN_DISCOVERY, "Definition error from extension", t);
    }

    @Override
    public void addBean(Bean<?> bean) {
        checkNotNull(bean, "Bean");
        info(Phase.AFTER_BEAN_DISCOVERY, "Registering synthetic bean: " + bean.getBeanClass().getSimpleName() +
                " with types: " + bean.getTypes());
        knowledgeBase.addBean(bean);
    }

    /**
     * Return the configurator directly.<br/>
     * Note: The configurator's complete() method will be called when the extension method returns.<br/>
     * Extensions are responsible for calling createWith() to provide the creation callback.<br/>
     * @return the configurator
     * @param <T> the bean type
     */
    @Override
    public <T> BeanConfigurator<T> addBean() {
        info(Phase.AFTER_BEAN_DISCOVERY, "Creating BeanConfigurator for synthetic bean");
        return new BeanConfiguratorImpl<>(messageHandler, knowledgeBase);
    }

    /**
     * Return the configurator directly.<br/>
     * Note: The configurator's complete() method returns an ObserverMethod which should be added via
     * addObserverMethod(ObserverMethod)
     * @return the configurator
     * @param <T> the observed type
     */
    @Override
    public <T> ObserverMethodConfigurator<T> addObserverMethod() {
        info(Phase.AFTER_BEAN_DISCOVERY, "Creating ObserverMethodConfigurator for synthetic observer");
        return new ObserverMethodConfiguratorImpl<T>(knowledgeBase) {
            @Override
            public ObserverMethod<T> complete() {
                ObserverMethod<T> observer = super.complete();
                addObserverMethod(observer);
                return observer;
            }
        };
    }

    @Override
    public void addObserverMethod(ObserverMethod<?> observerMethod) {
        checkNotNull(observerMethod, "ObserverMethod");
        info(Phase.AFTER_BEAN_DISCOVERY, "Registering synthetic observer method: observedType=" +
                observerMethod.getObservedType() + ", async=" + observerMethod.isAsync());
        knowledgeBase.addSyntheticObserverMethod(observerMethod);
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
        checkNotNull(context, "Context");
        info(Phase.AFTER_BEAN_DISCOVERY, "Registering custom context: " + context.getClass().getName() +
                " for scope @" + context.getScope().getSimpleName());

        BeanManagerImpl beanManagerImpl = (BeanManagerImpl) beanManager;
        ContextManager contextManager = beanManagerImpl.getContextManager();

        // Wrap the Jakarta CDI Context in our internal ScopeContext adapter
        CustomContextAdapter adaptedContext = new CustomContextAdapter(context);

        try {
            contextManager.registerContext(context.getScope(), adaptedContext);
        } catch (Exception e) {
            knowledgeBase.addError(Phase.AFTER_BEAN_DISCOVERY, "Failed to register custom context for scope @" +
                    context.getScope().getSimpleName(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<AnnotatedType<T>> getAnnotatedTypes(Class<T> type) {
        checkNotNull(type, "Class");
        info(Phase.AFTER_BEAN_DISCOVERY, "Getting annotated types for: " + type.getName());
        List<AnnotatedType<T>> result = new ArrayList<>();
        for (AnnotatedType<?> annotatedType : knowledgeBase.getRegisteredAnnotatedTypes().values()) {
            if (annotatedType.getJavaClass().equals(type)) {
                result.add((AnnotatedType<T>) annotatedType);
            }
        }
        info(Phase.AFTER_BEAN_DISCOVERY, "Found " + result.size() + " annotated type(s) for: " + type.getName());
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> AnnotatedType<T> getAnnotatedType(Class<T> type, String id) {
        checkNotNull(type, "Class");
        checkNotNull(id, "ID");

        info(Phase.AFTER_BEAN_DISCOVERY, "Getting annotated type: " + type.getName() + " with ID: " + id);

        AnnotatedType<?> annotatedType = knowledgeBase.getRegisteredAnnotatedType(id);
        if (annotatedType != null) {
            // Verify the class matches
            if (!annotatedType.getJavaClass().equals(type)) {
                knowledgeBase.addWarning(Phase.AFTER_BEAN_DISCOVERY, "AnnotatedType with ID '" + id +
                        "' has class " + annotatedType.getJavaClass().getName() + " but requested type is " +
                        type.getName());
                return null;
            }
            return (AnnotatedType<T>) annotatedType;
        }

        info(Phase.AFTER_BEAN_DISCOVERY, "No annotated type found with ID: " + id);
        return null;
    }
}
