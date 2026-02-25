package com.threeamigos.common.util.implementations.injection.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.configurator.InjectionPointConfigurator;

/**
 * ProcessInjectionPoint event implementation.
 */
public class ProcessInjectionPointImpl<T, X> implements ProcessInjectionPoint<T, X> {

    private InjectionPoint injectionPoint;
    private final BeanManager beanManager;
    private final KnowledgeBase knowledgeBase;

    public ProcessInjectionPointImpl(InjectionPoint injectionPoint,
                                     BeanManager beanManager,
                                     KnowledgeBase knowledgeBase) {
        if (injectionPoint == null) {
            throw new IllegalArgumentException("injectionPoint cannot be null");
        }
        this.injectionPoint = injectionPoint;
        this.beanManager = beanManager;
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public InjectionPoint getInjectionPoint() {
        return injectionPoint;
    }

    @Override
    public void setInjectionPoint(InjectionPoint injectionPoint) {
        if (injectionPoint == null) {
            throw new IllegalArgumentException("injectionPoint cannot be null");
        }
        this.injectionPoint = injectionPoint;
    }

    @Override
    public InjectionPointConfigurator configureInjectionPoint() {
        InjectionPointConfiguratorImpl configurator = new InjectionPointConfiguratorImpl(injectionPoint) {
            @Override
            public InjectionPoint complete() {
                InjectionPoint configured = super.complete();
                setInjectionPoint(configured);
                return configured;
            }
        };
        return configurator;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        String message = "ProcessInjectionPoint definition error: " + (t != null ? t.getMessage() : "null");
        System.err.println("[ProcessInjectionPoint] " + message);
        if (knowledgeBase != null) {
            knowledgeBase.addDefinitionError(message);
        }
        if (t != null) {
            t.printStackTrace();
        }
    }
}
