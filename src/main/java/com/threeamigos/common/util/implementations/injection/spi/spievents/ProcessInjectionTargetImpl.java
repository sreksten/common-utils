package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.ProcessInjectionTarget;

/**
 * ProcessInjectionTarget event implementation.
 */
public class ProcessInjectionTargetImpl<X> implements ProcessInjectionTarget<X> {

    private final AnnotatedType<X> annotatedType;
    private InjectionTarget<X> injectionTarget;
    private final KnowledgeBase knowledgeBase;

    public ProcessInjectionTargetImpl(AnnotatedType<X> annotatedType,
                                      InjectionTarget<X> injectionTarget,
                                      BeanManager beanManager,
                                      KnowledgeBase knowledgeBase) {
        if (annotatedType == null) {
            throw new IllegalArgumentException("annotatedType cannot be null");
        }
        if (injectionTarget == null) {
            throw new IllegalArgumentException("injectionTarget cannot be null");
        }
        this.annotatedType = annotatedType;
        this.injectionTarget = injectionTarget;
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public AnnotatedType<X> getAnnotatedType() {
        return annotatedType;
    }

    @Override
    public InjectionTarget<X> getInjectionTarget() {
        return injectionTarget;
    }

    @Override
    public void setInjectionTarget(InjectionTarget<X> injectionTarget) {
        if (injectionTarget == null) {
            throw new IllegalArgumentException("injectionTarget cannot be null");
        }
        this.injectionTarget = injectionTarget;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        String message = "ProcessInjectionTarget definition error: " + (t != null ? t.getMessage() : "null");
        System.err.println("[ProcessInjectionTarget] " + message);
        if (knowledgeBase != null) {
            knowledgeBase.addDefinitionError(message);
        }
        if (t != null) {
            t.printStackTrace();
        }
    }
}
