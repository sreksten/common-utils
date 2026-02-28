package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
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
public class ProcessAnnotatedTypeImpl<T> implements ProcessAnnotatedType<T> {

    private final BeanManager beanManager;
    private final com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase knowledgeBase;
    private AnnotatedType<T> annotatedType;
    private boolean vetoed = false;

    public ProcessAnnotatedTypeImpl(AnnotatedType<T> annotatedType, BeanManager beanManager) {
        this.annotatedType = annotatedType;
        this.beanManager = beanManager;
        // Extract KnowledgeBase from BeanManager
        if (beanManager instanceof BeanManagerImpl) {
            this.knowledgeBase = ((BeanManagerImpl) beanManager).getKnowledgeBase();
        } else {
            this.knowledgeBase = null;
        }
    }

    @Override
    public AnnotatedType<T> getAnnotatedType() {
        return annotatedType;
    }

    @Override
    public void setAnnotatedType(AnnotatedType<T> type) {
        this.annotatedType = type;
        System.out.println("ProcessAnnotatedType: setAnnotatedType(" + type.getJavaClass().getName() + ")");
    }

    @Override
    public AnnotatedTypeConfigurator<T> configureAnnotatedType() {
        System.out.println("[ProcessAnnotatedType] Creating AnnotatedTypeConfigurator for: " + annotatedType.getJavaClass().getName());

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
        this.vetoed = true;
        System.out.println("ProcessAnnotatedType: veto(" + annotatedType.getJavaClass().getName() + ")");
    }

    public boolean isVetoed() {
        return vetoed;
    }
}
