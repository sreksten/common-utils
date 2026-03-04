package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.implementations.injection.spi.configurators.ObserverMethodConfiguratorImpl;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
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
public class ProcessObserverMethodImpl<T, X> extends PhaseAware implements ProcessObserverMethod<T, X> {

    private final AnnotatedMethod<X> annotatedMethod;
    private final KnowledgeBase knowledgeBase;
    private ObserverMethod<T> observerMethod;
    private boolean vetoed = false;

    public ProcessObserverMethodImpl(MessageHandler messageHandler, KnowledgeBase knowledgeBase,
                                     ObserverMethod<T> observerMethod, AnnotatedMethod<X> annotatedMethod) {
        super(messageHandler);
        this.knowledgeBase = knowledgeBase;
        this.observerMethod = observerMethod;
        this.annotatedMethod = annotatedMethod;
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
        knowledgeBase.addDefinitionError(Phase.PROCESS_OBSERVER_METHOD, "Definition error for " +
                annotatedMethod.getJavaMember().getName(), t);
    }

    @Override
    public ObserverMethodConfigurator<T> configureObserverMethod() {
        info(Phase.PROCESS_OBSERVER_METHOD, "Creating ObserverMethodConfigurator");

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
        info(Phase.PROCESS_OBSERVER_METHOD, "Veto on " + annotatedMethod.getJavaMember().getName());
        this.vetoed = true;
    }

    @Override
    public void setObserverMethod(ObserverMethod<T> observerMethod) {
        checkNotNull(observerMethod, "ObserverMethod");
        info(Phase.PROCESS_OBSERVER_METHOD, "Changing observer method for " +
                annotatedMethod.getJavaMember().getName());
        this.observerMethod = observerMethod;
    }

    public boolean isVetoed() {
        return vetoed;
    }
}
