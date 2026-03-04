package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.implementations.injection.spi.configurators.ProducerConfiguratorImpl;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
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
public class ProcessProducerImpl<T, X> extends PhaseAware implements ProcessProducer<T, X> {

    private final KnowledgeBase knowledgeBase;
    private final Phase phase;
    private final AnnotatedMember<T> annotatedMember;
    private Producer<X> producer;

    public ProcessProducerImpl(MessageHandler messageHandler, KnowledgeBase knowledgeBase, Phase phase,
                               AnnotatedMember<T> annotatedMember, Producer<X> producer) {
        super(messageHandler);
        checkNotNull(annotatedMember, "AnnotatedMember");
        checkNotNull(producer, "Producer");
        this.knowledgeBase = knowledgeBase;
        this.phase = phase;
        this.annotatedMember = annotatedMember;
        this.producer = producer;
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
        checkNotNull(producer, "Producer");
        info(phase, "Changing Producer for " + annotatedMember.getJavaMember().getName());
        this.producer = producer;
    }

    @Override
    public ProducerConfigurator<X> configureProducer() {
        info(phase, "Configuring Producer for " + annotatedMember.getJavaMember().getName());
        return new ProducerConfiguratorImpl<X>(producer) {
            @Override
            public Producer<X> complete() {
                // When configuration completes, update the producer and return it
                Producer<X> configuredProducer = super.complete();
                setProducer(configuredProducer);
                return configuredProducer;
            }
        };
    }

    @Override
    public void addDefinitionError(Throwable t) {
        knowledgeBase.addDefinitionError(phase, "Definition error for " + annotatedMember.getJavaMember().getName(), t);
    }

    /**
     * Returns the final Producer after extensions may have wrapped/replaced it.
     */
    public Producer<X> getFinalProducer() {
        return producer;
    }
}
