package com.threeamigos.common.util.implementations.injection.spievents;

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

    public ProcessBeanImpl(Bean<X> bean, Annotated annotated, BeanManager beanManager) {
        this.bean = bean;
        this.annotated = annotated;
        this.beanManager = beanManager;
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
        System.out.println("ProcessBean: addDefinitionError(" + t.getMessage() + ")");
        // TODO: Add error to knowledge base
    }
}
