package com.threeamigos.common.util.implementations.injection.spievents;

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

    @Override
    public void addContext(Context context) {
        // TODO: Add custom context to context manager
        System.out.println("AfterBeanDiscovery: addContext(" + context.getScope().getName() + ")");
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
