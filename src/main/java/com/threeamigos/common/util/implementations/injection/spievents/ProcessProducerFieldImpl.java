package com.threeamigos.common.util.implementations.injection.spievents;

import jakarta.enterprise.inject.spi.*;

/**
 * ProcessProducerField event implementation.
 *
 * <p>Fired for each producer field discovered in managed beans.
 * This is a specialized ProcessProducer event specifically for fields annotated with @Produces.
 *
 * <p>Extensions can observe this event to:
 * <ul>
 *   <li>Inspect producer field metadata</li>
 *   <li>Replace the Producer instance via {@link #setProducer(Producer)}</li>
 *   <li>Wrap the Producer to customize field access logic</li>
 *   <li>Add custom validation or logging</li>
 * </ul>
 *
 * @param <T> the class declaring the producer field
 * @param <X> the type of the producer field
 * @see jakarta.enterprise.inject.spi.ProcessProducerField
 */
public class ProcessProducerFieldImpl<T, X> extends ProcessProducerImpl<T, X>
        implements ProcessProducerField<T, X> {

    private final Bean<X> bean;
    private final AnnotatedField<T> annotatedField;
    private final AnnotatedParameter<T> disposerParameter;

    /**
     * Constructor for producer field without disposer.
     */
    public ProcessProducerFieldImpl(Bean<X> bean,
                                    AnnotatedField<T> annotatedField,
                                    Producer<X> producer,
                                    BeanManager beanManager) {
        super(annotatedField, producer, beanManager);
        this.bean = bean;
        this.annotatedField = annotatedField;
        this.disposerParameter = null;
    }

    /**
     * Constructor for producer field with disposer.
     */
    public ProcessProducerFieldImpl(Bean<X> bean,
                                    AnnotatedField<T> annotatedField,
                                    Producer<X> producer,
                                    AnnotatedParameter<T> disposerParameter,
                                    BeanManager beanManager) {
        super(annotatedField, producer, beanManager);
        this.bean = bean;
        this.annotatedField = annotatedField;
        this.disposerParameter = disposerParameter;
    }

    @Override
    public Bean<X> getBean() {
        return bean;
    }

    @Override
    public Annotated getAnnotated() {
        return annotatedField;
    }

    @Override
    public AnnotatedField<T> getAnnotatedProducerField() {
        return annotatedField;
    }

    @Override
    public AnnotatedParameter<T> getAnnotatedDisposedParameter() {
        return disposerParameter;
    }
}
