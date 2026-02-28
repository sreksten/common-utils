package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import jakarta.enterprise.inject.spi.*;

/**
 * ProcessBean event implementation.
 * 
 * <p>Fired for each registered bean (managed beans, producer methods, producer fields).
 * Extensions can observe this event to:
 * <ul>
 *   <li>Inspect bean metadata</li>
 *   <li>Validate bean configuration</li>
 * </ul>
 *
 * @param <X> the bean class type
 * @see jakarta.enterprise.inject.spi.ProcessBean
 */
public class ProcessBeanImpl<X> implements ProcessBean<X> {

    private final Bean<X> bean;
    private final Annotated annotated;
    private final BeanManager beanManager;
    private final com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase knowledgeBase;

    public ProcessBeanImpl(Bean<X> bean, Annotated annotated, BeanManager beanManager) {
        this.bean = bean;
        this.annotated = annotated;
        this.beanManager = beanManager;
        // Extract KnowledgeBase from BeanManager
        if (beanManager instanceof BeanManagerImpl) {
            this.knowledgeBase = ((BeanManagerImpl) beanManager).getKnowledgeBase();
        } else {
            this.knowledgeBase = null;
        }
    }

    @Override
    public Annotated getAnnotated() {
        return annotated;
    }

    @Override
    public Bean<X> getBean() {
        return bean;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        String errorMsg = "ProcessBean definition error for " +
                         bean.getBeanClass().getName() + ": " +
                         (t != null ? t.getMessage() : "null");
        System.err.println("[ProcessBean] " + errorMsg);

        if (knowledgeBase != null) {
            knowledgeBase.addDefinitionError(errorMsg);
        } else {
            System.err.println("[ProcessBean] WARNING: Cannot propagate error - KnowledgeBase not available");
        }

        // Also print stack trace for debugging
        if (t != null) {
            t.printStackTrace();
        }
    }
}
