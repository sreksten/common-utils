package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.implementations.injection.spi.configurators.AnnotatedTypeConfiguratorImpl;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.inject.spi.*;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

/**
 * ProcessAnnotatedType event implementation.
 * 
 * <p>Fired for each discovered type during bean discovery. Extensions can use this event to:
 * <ul>
 *   <li>Veto the type via {@link #veto()}</li>
 *   <li>Replace the AnnotatedType via {@link #setAnnotatedType(AnnotatedType)}</li>
 *   <li>Configure the AnnotatedType via {@link #configureAnnotatedType()}</li>
 * </ul>
 *
 * @param <T> the type being processed
 * @see jakarta.enterprise.inject.spi.ProcessAnnotatedType
 */
public class ProcessAnnotatedTypeImpl<T> extends PhaseAware implements ProcessAnnotatedType<T>, ObserverInvocationLifecycle {

    private AnnotatedType<T> annotatedType;
    private boolean vetoed = false;
    private boolean setAnnotatedTypeCalled = false;
    private boolean configureAnnotatedTypeCalled = false;
    private boolean configuredAnnotatedTypeCompleted = false;
    private AnnotatedTypeConfigurator<T> configurator;
    private final ThreadLocal<Boolean> observerInvocationActive = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public ProcessAnnotatedTypeImpl(MessageHandler messageHandler, AnnotatedType<T> annotatedType) {
        super(messageHandler);
        this.annotatedType = annotatedType;
    }

    @Override
    public AnnotatedType<T> getAnnotatedType() {
        assertObserverInvocationActive();
        return annotatedType;
    }

    @Override
    public void setAnnotatedType(AnnotatedType<T> type) {
        assertObserverInvocationActive();
        if (type == null) {
            throw new IllegalArgumentException("AnnotatedType cannot be null");
        }
        if (configureAnnotatedTypeCalled) {
            throw new IllegalStateException("setAnnotatedType() and configureAnnotatedType() cannot both be used");
        }
        info(Phase.PROCESS_ANNOTATED_TYPE, "Changing AnnotatedType " + annotatedType.getJavaClass().getName() +
                " with " + type.getJavaClass().getName());
        this.annotatedType = type;
        this.setAnnotatedTypeCalled = true;
    }

    @Override
    public AnnotatedTypeConfigurator<T> configureAnnotatedType() {
        assertObserverInvocationActive();
        if (setAnnotatedTypeCalled) {
            throw new IllegalStateException("setAnnotatedType() and configureAnnotatedType() cannot both be used");
        }
        info(Phase.PROCESS_ANNOTATED_TYPE, "Creating AnnotatedTypeConfigurator for " +
                annotatedType.getJavaClass().getName());
        this.configureAnnotatedTypeCalled = true;
        if (configurator == null) {
            configurator = new AnnotatedTypeConfiguratorImpl<T>(annotatedType) {
                @Override
                public AnnotatedType<T> complete() {
                    AnnotatedType<T> configured = super.complete();
                    annotatedType = configured;
                    return configured;
                }
            };
        }
        return configurator;
    }

    @Override
    public void veto() {
        assertObserverInvocationActive();
        info(Phase.PROCESS_ANNOTATED_TYPE, "Veto on " + annotatedType.getJavaClass().getName());
        this.vetoed = true;
    }

    public boolean isVetoed() {
        return vetoed;
    }

    public AnnotatedType<T> getAnnotatedTypeInternal() {
        completeConfiguredAnnotatedTypeIfNeeded();
        return annotatedType;
    }

    @Override
    public void beginObserverInvocation() {
        observerInvocationActive.set(Boolean.TRUE);
    }

    @Override
    public void endObserverInvocation() {
        observerInvocationActive.set(Boolean.FALSE);
    }

    private void assertObserverInvocationActive() {
        if (!observerInvocationActive.get()) {
            throw new IllegalStateException("ProcessAnnotatedType methods may only be called during observer method invocation");
        }
    }

    @SuppressWarnings("unchecked")
    private void completeConfiguredAnnotatedTypeIfNeeded() {
        if (!configureAnnotatedTypeCalled || configurator == null || configuredAnnotatedTypeCompleted) {
            return;
        }
        if (configurator instanceof AnnotatedTypeConfiguratorImpl<?>) {
            annotatedType = ((AnnotatedTypeConfiguratorImpl<T>) configurator).complete();
        }
        configuredAnnotatedTypeCompleted = true;
    }
}
