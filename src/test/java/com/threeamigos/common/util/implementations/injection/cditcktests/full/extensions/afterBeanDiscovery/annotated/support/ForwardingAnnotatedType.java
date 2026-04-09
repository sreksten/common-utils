package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.support;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import java.util.Set;

public abstract class ForwardingAnnotatedType<X> extends ForwardingAnnotated implements AnnotatedType<X> {

    @Override
    public abstract AnnotatedType<X> delegate();

    @Override
    public Set<AnnotatedConstructor<X>> getConstructors() {
        return delegate().getConstructors();
    }

    @Override
    public Set<AnnotatedField<? super X>> getFields() {
        return delegate().getFields();
    }

    @Override
    public Class<X> getJavaClass() {
        return delegate().getJavaClass();
    }

    @Override
    public Set<AnnotatedMethod<? super X>> getMethods() {
        return delegate().getMethods();
    }
}
