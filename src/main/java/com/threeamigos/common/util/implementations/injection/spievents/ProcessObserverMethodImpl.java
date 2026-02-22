package com.threeamigos.common.util.implementations.injection.spievents;

import jakarta.enterprise.inject.spi.*;
import jakarta.enterprise.inject.spi.configurator.ObserverMethodConfigurator;

/**
 * ProcessObserverMethod event implementation.
 * 
 * <p>Fired for each observer method discovered in managed beans.
 * Extensions can observe this event to:
 * <ul>
 *   <li>Inspect observer method metadata</li>
 *   <li>Replace the observer method via {@link #configureObserverMethod()}</li>
 *   <li>Veto the observer method via {@link #veto()}</li>
 * </ul>
 *
 * @param <T> the event type being observed
 * @param <X> the bean class containing the observer method
 * @see jakarta.enterprise.inject.spi.ProcessObserverMethod
 */
public class ProcessObserverMethodImpl<T, X> implements ProcessObserverMethod<T, X> {

    private final ObserverMethod<T> observerMethod;
    private final AnnotatedMethod<X> annotatedMethod;
    private final BeanManager beanManager;
    private boolean vetoed = false;

    public ProcessObserverMethodImpl(ObserverMethod<T> observerMethod, AnnotatedMethod<X> annotatedMethod, BeanManager beanManager) {
        this.observerMethod = observerMethod;
        this.annotatedMethod = annotatedMethod;
        this.beanManager = beanManager;
    }

    @Override
    public AnnotatedMethod<X> getAnnotatedMethod() {
        return annotatedMethod;
    }

    @Override
    public ObserverMethod<T> getObserverMethod() {
        return observerMethod;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        System.out.println("ProcessObserverMethod: addDefinitionError(" + t.getMessage() + ")");
        // TODO: Add error to knowledge base
    }

    @Override
    public ObserverMethodConfigurator<T> configureObserverMethod() {
        System.out.println("ProcessObserverMethod: configureObserverMethod()");
        throw new UnsupportedOperationException("ObserverMethodConfigurator not yet implemented");
    }

    @Override
    public void veto() {
        this.vetoed = true;
        System.out.println("ProcessObserverMethod: veto()");
    }

    @Override
    public void setObserverMethod(ObserverMethod<T> observerMethod) {
        // TODO: Replace the observer method
        System.out.println("ProcessObserverMethod: setObserverMethod()");
    }

    public boolean isVetoed() {
        return vetoed;
    }
}
