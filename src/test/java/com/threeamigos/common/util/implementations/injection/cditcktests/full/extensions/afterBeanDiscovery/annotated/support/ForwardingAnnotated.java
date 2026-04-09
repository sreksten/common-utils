package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.support;

import jakarta.enterprise.inject.spi.Annotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Base forwarding implementation used to mirror original CDI TCK test fixtures.
 */
public abstract class ForwardingAnnotated implements Annotated {

    protected abstract Annotated delegate();

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return delegate().getAnnotation(annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return delegate().getAnnotations();
    }

    @Override
    public <A extends Annotation> Set<A> getAnnotations(Class<A> annotationType) {
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
    public boolean equals(Object obj) {
        return delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }
}
