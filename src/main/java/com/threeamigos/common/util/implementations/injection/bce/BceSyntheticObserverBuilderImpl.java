package com.threeamigos.common.util.implementations.injection.bce;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserver;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserverBuilder;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.types.Type;
import jakarta.enterprise.inject.spi.ObserverMethod;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class BceSyntheticObserverBuilderImpl<T> implements SyntheticObserverBuilder<T> {

    private final KnowledgeBase knowledgeBase;
    private final BceInvokerRegistry invokerRegistry;
    private final Class<T> observedClass;

    private Class<?> declaringClass = Object.class;
    private final Set<Annotation> qualifiers = new LinkedHashSet<Annotation>();
    private int priority = ObserverMethod.DEFAULT_PRIORITY;
    private boolean async = false;
    private TransactionPhase transactionPhase = TransactionPhase.IN_PROGRESS;
    private final Map<String, Object> params = new LinkedHashMap<String, Object>();
    private Class<? extends SyntheticObserver<T>> observerClass;

    BceSyntheticObserverBuilderImpl(KnowledgeBase knowledgeBase,
                                    BceInvokerRegistry invokerRegistry,
                                    Class<T> observedClass) {
        this.knowledgeBase = knowledgeBase;
        this.invokerRegistry = invokerRegistry;
        this.observedClass = observedClass;
    }

    @Override
    public SyntheticObserverBuilder<T> declaringClass(Class<?> declaringClass) {
        if (declaringClass != null) {
            this.declaringClass = declaringClass;
        }
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> declaringClass(ClassInfo declaringClass) {
        if (declaringClass != null) {
            this.declaringClass = BceMetadata.unwrapClassInfo(declaringClass);
        }
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> qualifier(Class<? extends Annotation> qualifier) {
        if (qualifier == null) {
            return this;
        }
        try {
            Annotation annotation = qualifier.getDeclaredConstructor().newInstance();
            this.qualifiers.add(annotation);
            return this;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate qualifier " + qualifier.getName() +
                ". Use qualifier(Annotation) or qualifier(AnnotationInfo).", e);
        }
    }

    @Override
    public SyntheticObserverBuilder<T> qualifier(AnnotationInfo qualifier) {
        if (qualifier != null) {
            this.qualifiers.add(BceMetadata.unwrapAnnotationInfo(qualifier));
        }
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> qualifier(Annotation qualifier) {
        if (qualifier != null) {
            this.qualifiers.add(qualifier);
        }
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> async(boolean async) {
        this.async = async;
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> transactionPhase(TransactionPhase transactionPhase) {
        if (transactionPhase != null) {
            this.transactionPhase = transactionPhase;
        }
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, boolean value) {
        return withParamInternal(name, Boolean.valueOf(value));
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, boolean[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, int value) {
        return withParamInternal(name, Integer.valueOf(value));
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, int[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, long value) {
        return withParamInternal(name, Long.valueOf(value));
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, long[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, double value) {
        return withParamInternal(name, Double.valueOf(value));
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, double[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, String value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, String[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, Enum<?> value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, Enum<?>[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, Class<?> value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, ClassInfo value) {
        return withParamInternal(name, value != null ? BceMetadata.unwrapClassInfo(value) : null);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, Class<?>[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, ClassInfo[] value) {
        if (value == null) {
            return withParamInternal(name, null);
        }
        Class<?>[] converted = new Class<?>[value.length];
        for (int i = 0; i < value.length; i++) {
            converted[i] = value[i] != null ? BceMetadata.unwrapClassInfo(value[i]) : null;
        }
        return withParamInternal(name, converted);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, AnnotationInfo value) {
        return withParamInternal(name, value != null ? BceMetadata.unwrapAnnotationInfo(value) : null);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, Annotation value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, AnnotationInfo[] value) {
        if (value == null) {
            return withParamInternal(name, null);
        }
        Annotation[] converted = new Annotation[value.length];
        for (int i = 0; i < value.length; i++) {
            converted[i] = value[i] != null ? BceMetadata.unwrapAnnotationInfo(value[i]) : null;
        }
        return withParamInternal(name, converted);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, Annotation[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, InvokerInfo value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> withParam(String name, InvokerInfo[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticObserverBuilder<T> observeWith(Class<? extends SyntheticObserver<T>> observerClass) {
        this.observerClass = observerClass;
        return this;
    }

    private SyntheticObserverBuilder<T> withParamInternal(String name, Object value) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter name must not be blank");
        }
        this.params.put(name, value);
        return this;
    }

    void complete() {
        if (observerClass == null) {
            throw new IllegalStateException("Synthetic observer implementation is required via observeWith()");
        }
        Set<Annotation> effectiveQualifiers = new LinkedHashSet<Annotation>(qualifiers);
        if (effectiveQualifiers.isEmpty()) {
            effectiveQualifiers.add(Default.Literal.INSTANCE);
        }
        BceSyntheticObserverMethod<T> observerMethod = new BceSyntheticObserverMethod<T>(
            declaringClass,
            observedClass,
            effectiveQualifiers,
            priority,
            async,
            transactionPhase,
            observerClass,
            Collections.unmodifiableMap(new LinkedHashMap<String, Object>(params)),
            invokerRegistry
        );
        knowledgeBase.addSyntheticObserverMethod(observerMethod);
    }
}
