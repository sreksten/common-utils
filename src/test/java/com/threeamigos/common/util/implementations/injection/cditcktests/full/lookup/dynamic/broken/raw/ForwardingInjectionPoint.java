package com.threeamigos.common.util.implementations.injection.cditcktests.full.lookup.dynamic.broken.raw;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;

abstract class ForwardingInjectionPoint implements InjectionPoint {

    protected abstract InjectionPoint delegate();

    @Override
    public Annotated getAnnotated() {
        return delegate().getAnnotated();
    }

    @Override
    public Type getType() {
        return delegate().getType();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return delegate().getQualifiers();
    }

    @Override
    public Bean<?> getBean() {
        return delegate().getBean();
    }

    @Override
    public Member getMember() {
        return delegate().getMember();
    }

    @Override
    public boolean isDelegate() {
        return delegate().isDelegate();
    }

    @Override
    public boolean isTransient() {
        return delegate().isTransient();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ForwardingInjectionPoint) {
            return delegate().equals(((ForwardingInjectionPoint) other).delegate());
        }
        return delegate().equals(other);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }
}
