package com.threeamigos.common.util.implementations.injection.spievents;

import jakarta.enterprise.inject.spi.*;
import jakarta.enterprise.inject.spi.configurator.ProducerConfigurator;

/**
 * ProcessProducer event implementation.
 *
 * <p>Fired for each producer method or field discovered in managed beans.
 * Extensions can observe this event to:
 * <ul>
 *   <li>Inspect producer metadata</li>
 *   <li>Replace the Producer instance via {@link #setProducer(Producer)}</li>
 *   <li>Wrap the Producer to customize production logic</li>
 * </ul>
 *
 * <p>This is the base implementation for both ProcessProducerMethod and ProcessProducerField.
 *
 * @param <T> the class declaring the producer method/field
 * @param <X> the return type of the producer method/field
 * @see jakarta.enterprise.inject.spi.ProcessProducer
 */
public class ProcessProducerImpl<T, X> implements ProcessProducer<T, X> {

    private final AnnotatedMember<T> annotatedMember;
    private Producer<X> producer;
    private final BeanManager beanManager;

    public ProcessProducerImpl(AnnotatedMember<T> annotatedMember, Producer<X> producer, BeanManager beanManager) {
        this.annotatedMember = annotatedMember;
        this.producer = producer;
        this.beanManager = beanManager;
    }

    @Override
    public AnnotatedMember<T> getAnnotatedMember() {
        return annotatedMember;
    }

    @Override
    public Producer<X> getProducer() {
        return producer;
    }

    @Override
    public void setProducer(Producer<X> producer) {
        if (producer == null) {
            throw new IllegalArgumentException("Producer cannot be null");
        }
        System.out.println("[ProcessProducer] Producer wrapped/replaced for: " +
                          annotatedMember.getJavaMember().getName());
        this.producer = producer;
    }

    @Override
    public ProducerConfigurator<X> configureProducer() {
        System.out.println("[ProcessProducer] configureProducer() called");
        throw new UnsupportedOperationException("ProducerConfigurator not yet implemented");
    }

    @Override
    public void addDefinitionError(Throwable t) {
        System.out.println("[ProcessProducer] addDefinitionError: " + t.getMessage());
        // TODO: Add error to knowledge base
    }

    /**
     * Returns the final Producer after extensions may have wrapped/replaced it.
     */
    public Producer<X> getFinalProducer() {
        return producer;
    }
}
