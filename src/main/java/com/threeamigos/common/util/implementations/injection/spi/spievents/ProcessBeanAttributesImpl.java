package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.implementations.injection.spi.configurators.BeanAttributesConfiguratorImpl;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.inject.spi.configurator.BeanAttributesConfigurator;

/**
 * ProcessBeanAttributes event implementation.
 */
public class ProcessBeanAttributesImpl<T> extends PhaseAware implements ProcessBeanAttributes<T> {

    private final Annotated annotated;
    private BeanAttributes<T> beanAttributes;
    private boolean vetoed = false;
    private boolean ignoreFinalMethods = false;
    private final KnowledgeBase knowledgeBase;

    public ProcessBeanAttributesImpl(MessageHandler messageHandler, Annotated annotated,
                                     BeanAttributes<T> beanAttributes,
                                     BeanManager beanManager,
                                     KnowledgeBase knowledgeBase) {
        super(messageHandler);
        checkNotNull(annotated, "Annotated");
        checkNotNull(beanAttributes, "BeanAttributes");
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
        checkNotNull(beanAttributes, "BeanAttributes");
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
        info(Phase.PROCESS_BEAN_ATTRIBUTES, "Veto on " + annotated.getBaseType().getClass().getName());
        this.vetoed = true;
    }

    @Override
    public void ignoreFinalMethods() {
        info(Phase.PROCESS_BEAN_ATTRIBUTES, "Ignoring final methods on " +
                annotated.getBaseType().getClass().getName());
        this.ignoreFinalMethods = true;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        knowledgeBase.addDefinitionError(Phase.PROCESS_BEAN_ATTRIBUTES, "Definition error from extension", t);
    }

    public boolean isVetoed() {
        return vetoed;
    }

    public boolean isIgnoreFinalMethods() {
        return ignoreFinalMethods;
    }
}
