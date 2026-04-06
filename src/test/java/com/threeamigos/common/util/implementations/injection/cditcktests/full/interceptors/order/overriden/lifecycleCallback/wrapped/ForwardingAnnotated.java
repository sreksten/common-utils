package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.order.overriden.lifecycleCallback.wrapped;

import jakarta.enterprise.inject.spi.Annotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

abstract class ForwardingAnnotated implements Annotated {

    protected abstract Annotated delegate();

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return delegate().getAnnotation(annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return delegate().getAnnotations();
    }

    @Override
    public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
        return delegate().getAnnotations(annotationType);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return delegate().isAnnotationPresent(annotationType);
    }

    @Override
    public Type getBaseType() {
        return delegate().getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure() {
        return delegate().getTypeClosure();
    }

    @Override
    public boolean equals(Object other) {
        return delegate().equals(other);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }
}
