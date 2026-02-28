package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.inject.spi.configurator.BeanAttributesConfigurator;

/**
 * ProcessBeanAttributes event implementation.
 */
public class ProcessBeanAttributesImpl<T> implements ProcessBeanAttributes<T> {

    private final Annotated annotated;
    private BeanAttributes<T> beanAttributes;
    private boolean vetoed = false;
    private boolean ignoreFinalMethods = false;
    private final KnowledgeBase knowledgeBase;

    public ProcessBeanAttributesImpl(Annotated annotated,
                                     BeanAttributes<T> beanAttributes,
                                     BeanManager beanManager,
                                     KnowledgeBase knowledgeBase) {
        if (annotated == null) {
            throw new IllegalArgumentException("annotated cannot be null");
        }
        if (beanAttributes == null) {
            throw new IllegalArgumentException("beanAttributes cannot be null");
        }
        this.annotated = annotated;
        this.beanAttributes = beanAttributes;
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public Annotated getAnnotated() {
        return annotated;
    }

    @Override
    public BeanAttributes<T> getBeanAttributes() {
        return beanAttributes;
    }

    @Override
    public void setBeanAttributes(BeanAttributes<T> beanAttributes) {
        if (beanAttributes == null) {
            throw new IllegalArgumentException("beanAttributes cannot be null");
        }
        this.beanAttributes = beanAttributes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public BeanAttributesConfigurator<T> configureBeanAttributes() {
        return new BeanAttributesConfiguratorImpl(beanAttributes) {
            @Override
            public BeanAttributes<T> complete() {
                BeanAttributes<T> configured = super.complete();
                setBeanAttributes(configured);
                return configured;
            }
        };
    }

    @Override
    public void veto() {
        this.vetoed = true;
    }

    @Override
    public void ignoreFinalMethods() {
        this.ignoreFinalMethods = true;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        String message = "ProcessBeanAttributes definition error: " + (t != null ? t.getMessage() : "null");
        System.err.println("[ProcessBeanAttributes] " + message);
        if (knowledgeBase != null) {
            knowledgeBase.addDefinitionError(message);
        }
        if (t != null) {
            t.printStackTrace();
        }
    }

    public boolean isVetoed() {
        return vetoed;
    }

    public boolean isIgnoreFinalMethods() {
        return ignoreFinalMethods;
    }
}
