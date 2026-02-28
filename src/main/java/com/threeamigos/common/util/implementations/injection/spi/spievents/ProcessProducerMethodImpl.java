package com.threeamigos.common.util.implementations.injection.spi.spievents;

import jakarta.enterprise.inject.spi.*;

/**
 * ProcessProducerMethod event implementation.
 *
 * <p>Fired for each producer method discovered in managed beans.
 * This is a specialized ProcessProducer event specifically for methods annotated with @Produces.
 *
 * <p>Extensions can observe this event to:
 * <ul>
 *   <li>Inspect producer method metadata</li>
 *   <li>Replace the Producer instance via {@link #setProducer(Producer)}</li>
 *   <li>Wrap the Producer to customize method invocation logic</li>
 *   <li>Add custom validation or logging</li>
 * </ul>
 *
 * @param <T> the class declaring the producer method
 * @param <X> the return type of the producer method
 * @see jakarta.enterprise.inject.spi.ProcessProducerMethod
 */
public class ProcessProducerMethodImpl<T, X> extends ProcessProducerImpl<T, X>
        implements ProcessProducerMethod<T, X> {

    private final Bean<X> bean;
    private final AnnotatedMethod<T> annotatedMethod;
    private final AnnotatedParameter<T> disposerParameter;

    /**
     * Constructor for producer method without disposer.
     */
    public ProcessProducerMethodImpl(Bean<X> bean,
                                     AnnotatedMethod<T> annotatedMethod,
                                     Producer<X> producer,
                                     BeanManager beanManager) {
        super(annotatedMethod, producer, beanManager);
        this.bean = bean;
        this.annotatedMethod = annotatedMethod;
        this.disposerParameter = null;
    }

    /**
     * Constructor for producer method with disposer.
     */
    public ProcessProducerMethodImpl(Bean<X> bean,
                                     AnnotatedMethod<T> annotatedMethod,
                                     Producer<X> producer,
                                     AnnotatedParameter<T> disposerParameter,
                                     BeanManager beanManager) {
        super(annotatedMethod, producer, beanManager);
        this.bean = bean;
        this.annotatedMethod = annotatedMethod;
        this.disposerParameter = disposerParameter;
    }

    @Override
    public Bean<X> getBean() {
        return bean;
    }

    @Override
    public Annotated getAnnotated() {
        return annotatedMethod;
    }

    @Override
    public AnnotatedMethod<T> getAnnotatedProducerMethod() {
        return annotatedMethod;
    }

    @Override
    public AnnotatedParameter<T> getAnnotatedDisposedParameter() {
        return disposerParameter;
    }
}
