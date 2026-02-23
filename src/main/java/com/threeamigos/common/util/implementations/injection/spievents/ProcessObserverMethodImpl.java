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

    private ObserverMethod<T> observerMethod;
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
        System.out.println("[ProcessObserverMethod] Creating ObserverMethodConfigurator");

        // Create a configurator reading from the current observer method
        return new ObserverMethodConfiguratorImpl<T>(null) {
            {
                // Read existing observer method configuration
                read(observerMethod);
            }

            @Override
            public ObserverMethod<T> complete() {
                // When configuration completes, replace the observer method
                ObserverMethod<T> configured = super.complete();
                setObserverMethod(configured);
                return configured;
            }
        };
    }

    @Override
    public void veto() {
        this.vetoed = true;
        System.out.println("[ProcessObserverMethod] Observer method vetoed");
    }

    @Override
    public void setObserverMethod(ObserverMethod<T> observerMethod) {
        if (observerMethod == null) {
            throw new IllegalArgumentException("Observer method cannot be null");
        }
        System.out.println("[ProcessObserverMethod] Observer method replaced");
        this.observerMethod = observerMethod;
    }

    public boolean isVetoed() {
        return vetoed;
    }
}
