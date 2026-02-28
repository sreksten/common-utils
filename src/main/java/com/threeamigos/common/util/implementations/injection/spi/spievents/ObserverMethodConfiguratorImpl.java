package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.inject.spi.configurator.ObserverMethodConfigurator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of ObserverMethodConfigurator for building observer methods programmatically.
 *
 * <p>Provides a fluent API for configuring observer method behavior:
 * <ul>
 *   <li>Observed event type via {@link #observedType(Type)}</li>
 *   <li>Qualifiers via {@link #addQualifier(Annotation)}</li>
 *   <li>Reception strategy via {@link #reception(Reception)}</li>
 *   <li>Transaction phase via {@link #transactionPhase(TransactionPhase)}</li>
 *   <li>Priority via {@link #priority(int)}</li>
 *   <li>Async flag via {@link #async(boolean)}</li>
 *   <li>Notification callback via {@link #notifyWith(java.util.function.Consumer)}</li>
 * </ul>
 *
 * @param <T> the type of event observed
 * @see jakarta.enterprise.inject.spi.configurator.ObserverMethodConfigurator
 * @see jakarta.enterprise.inject.spi.ProcessObserverMethod
 */
public class ObserverMethodConfiguratorImpl<T> implements ObserverMethodConfigurator<T> {

    private final KnowledgeBase knowledgeBase;
    private Class<?> beanClass;
    private Type observedType;
    private final Set<Annotation> qualifiers = new HashSet<>();
    private Reception reception = Reception.ALWAYS;
    private TransactionPhase transactionPhase = TransactionPhase.IN_PROGRESS;
    private int priority = ObserverMethod.DEFAULT_PRIORITY;
    private boolean async = false;
    private ObserverMethodConfigurator.EventConsumer<T> notifyCallback;

    /**
     * Creates an ObserverMethodConfigurator.
     *
     * @param knowledgeBase the knowledge base to register the observer in
     */
    public ObserverMethodConfiguratorImpl(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public ObserverMethodConfigurator<T> read(java.lang.reflect.Method method) {
        if (method == null) {
            throw new IllegalArgumentException("Method cannot be null");
        }
        // Read metadata from the Method
        this.beanClass = method.getDeclaringClass();
        this.observedType = method.getGenericParameterTypes().length > 0
            ? method.getGenericParameterTypes()[0]
            : Object.class;
        // Note: qualifiers, reception, transactionPhase would need to be read from annotations
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> read(jakarta.enterprise.inject.spi.AnnotatedMethod<?> method) {
        if (method == null) {
            throw new IllegalArgumentException("AnnotatedMethod cannot be null");
        }
        // Read metadata from AnnotatedMethod
        this.beanClass = method.getDeclaringType().getJavaClass();
        if (!method.getParameters().isEmpty()) {
            this.observedType = method.getParameters().get(0).getBaseType();
        }
        // Note: qualifiers, reception, transactionPhase would need to be read from annotations
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> read(ObserverMethod<T> observerMethod) {
        if (observerMethod == null) {
            throw new IllegalArgumentException("ObserverMethod cannot be null");
        }
        // Copy all configuration from existing ObserverMethod
        this.beanClass = observerMethod.getBeanClass();
        this.observedType = observerMethod.getObservedType();
        this.qualifiers.clear();
        this.qualifiers.addAll(observerMethod.getObservedQualifiers());
        this.reception = observerMethod.getReception();
        this.transactionPhase = observerMethod.getTransactionPhase();
        this.priority = observerMethod.getPriority();
        this.async = observerMethod.isAsync();
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> beanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> observedType(Type type) {
        this.observedType = type;
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> addQualifier(Annotation qualifier) {
        if (qualifier != null) {
            this.qualifiers.add(qualifier);
        }
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> addQualifiers(Annotation... qualifiers) {
        if (qualifiers != null) {
            for (Annotation q : qualifiers) {
                if (q != null) {
                    this.qualifiers.add(q);
                }
            }
        }
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> addQualifiers(Set<Annotation> qualifiers) {
        if (qualifiers != null) {
            this.qualifiers.addAll(qualifiers);
        }
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> qualifiers(Annotation... qualifiers) {
        this.qualifiers.clear();
        return addQualifiers(qualifiers);
    }

    @Override
    public ObserverMethodConfigurator<T> qualifiers(Set<Annotation> qualifiers) {
        this.qualifiers.clear();
        return addQualifiers(qualifiers);
    }

    @Override
    public ObserverMethodConfigurator<T> reception(Reception reception) {
        if (reception != null) {
            this.reception = reception;
        }
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> transactionPhase(TransactionPhase transactionPhase) {
        if (transactionPhase != null) {
            this.transactionPhase = transactionPhase;
        }
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> notifyWith(ObserverMethodConfigurator.EventConsumer<T> callback) {
        this.notifyCallback = callback;
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> async(boolean async) {
        this.async = async;
        return this;
    }

    /**
     * Completes the configuration and returns a configured ObserverMethod.
     *
     * <p>This method creates a synthetic ObserverMethod implementation.
     *
     * @return the configured ObserverMethod
     */
    public ObserverMethod<T> complete() {
        if (observedType == null) {
            throw new IllegalStateException("Observed type must be set");
        }
        if (notifyCallback == null) {
            throw new IllegalStateException("Notification callback must be set via notifyWith()");
        }

        System.out.println("[ObserverMethodConfigurator] Created synthetic observer: " +
                          "observedType=" + observedType +
                          ", async=" + async +
                          ", priority=" + priority);

        return new SyntheticObserverMethod<>(
            beanClass,
            observedType,
            new HashSet<>(qualifiers),
            reception,
            transactionPhase,
            priority,
            async,
            notifyCallback
        );
    }

    /**
     * Synthetic ObserverMethod implementation.
     */
    private static class SyntheticObserverMethod<T> implements ObserverMethod<T> {
        private final Class<?> beanClass;
        private final Type observedType;
        private final Set<Annotation> qualifiers;
        private final Reception reception;
        private final TransactionPhase transactionPhase;
        private final int priority;
        private final boolean async;
        private final ObserverMethodConfigurator.EventConsumer<T> notifyCallback;

        SyntheticObserverMethod(
                Class<?> beanClass,
                Type observedType,
                Set<Annotation> qualifiers,
                Reception reception,
                TransactionPhase transactionPhase,
                int priority,
                boolean async,
                ObserverMethodConfigurator.EventConsumer<T> notifyCallback) {
            this.beanClass = beanClass;
            this.observedType = observedType;
            this.qualifiers = new HashSet<>(qualifiers);
            this.reception = reception;
            this.transactionPhase = transactionPhase;
            this.priority = priority;
            this.async = async;
            this.notifyCallback = notifyCallback;
        }

        @Override
        public Class<?> getBeanClass() {
            return beanClass;
        }

        @Override
        public Type getObservedType() {
            return observedType;
        }

        @Override
        public Set<Annotation> getObservedQualifiers() {
            return qualifiers;
        }

        @Override
        public Reception getReception() {
            return reception;
        }

        @Override
        public TransactionPhase getTransactionPhase() {
            return transactionPhase;
        }

        @Override
        public void notify(T event) {
            if (notifyCallback != null) {
                // Wrap the event in an EventContext
                EventContext<T> eventContext = new EventContext<T>() {
                    @Override
                    public T getEvent() {
                        return event;
                    }

                    @Override
                    public jakarta.enterprise.inject.spi.EventMetadata getMetadata() {
                        // TODO: Provide proper EventMetadata
                        return null;
                    }
                };
                try {
                    notifyCallback.accept(eventContext);
                } catch (Exception e) {
                    throw new RuntimeException("Error notifying observer", e);
                }
            }
        }

        @Override
        public boolean isAsync() {
            return async;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }
}
