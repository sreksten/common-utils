package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.contract.lifecycleCallback.wrapped;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import java.util.Set;

abstract class ForwardingAnnotatedType<T> extends ForwardingAnnotated implements AnnotatedType<T> {

    @Override
    public abstract AnnotatedType<T> delegate();

    @Override
    public Class<T> getJavaClass() {
        return delegate().getJavaClass();
    }

    @Override
    public Set<AnnotatedConstructor<T>> getConstructors() {
        return delegate().getConstructors();
    }

    @Override
    public Set<AnnotatedMethod<? super T>> getMethods() {
        return delegate().getMethods();
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields() {
        return delegate().getFields();
    }
}
