package com.threeamigos.common.util.implementations.injection.spievents;

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
    private AnnotatedType<T> annotatedType;
    private boolean vetoed = false;

    public ProcessAnnotatedTypeImpl(AnnotatedType<T> annotatedType, BeanManager beanManager) {
        this.annotatedType = annotatedType;
        this.beanManager = beanManager;
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
        // TODO: Return configurator for modifying the AnnotatedType
        System.out.println("ProcessAnnotatedType: configureAnnotatedType()");
        throw new UnsupportedOperationException("AnnotatedTypeConfigurator not yet implemented");
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
