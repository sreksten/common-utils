package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
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
public class ProcessAnnotatedTypeImpl<T> extends PhaseAware implements ProcessAnnotatedType<T> {

    private AnnotatedType<T> annotatedType;
    private boolean vetoed = false;

    public ProcessAnnotatedTypeImpl(MessageHandler messageHandler, AnnotatedType<T> annotatedType) {
        super(messageHandler);
        this.annotatedType = annotatedType;
    }

    @Override
    public AnnotatedType<T> getAnnotatedType() {
        return annotatedType;
    }

    @Override
    public void setAnnotatedType(AnnotatedType<T> type) {
        info(Phase.PROCESS_ANNOTATED_TYPE, "Changing AnnotatedType " + annotatedType.getJavaClass().getName() +
                " with " + type.getJavaClass().getName());
        this.annotatedType = type;
    }

    @Override
    public AnnotatedTypeConfigurator<T> configureAnnotatedType() {
        info(Phase.PROCESS_ANNOTATED_TYPE, "Creating AnnotatedTypeConfigurator for " +
                annotatedType.getJavaClass().getName());
        // Return a configurator that modifies the current annotatedType
        return new AnnotatedTypeConfiguratorImpl<T>(annotatedType) {
            @Override
            public AnnotatedType<T> complete() {
                // When configuration completes, update the annotated type
                AnnotatedType<T> configured = super.complete();
                setAnnotatedType(configured);
                return configured;
            }
        };
    }

    @Override
    public void veto() {
        info(Phase.PROCESS_ANNOTATED_TYPE, "Veto on " + annotatedType.getJavaClass().getName());
        this.vetoed = true;
    }

    public boolean isVetoed() {
        return vetoed;
    }
}
