package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.broken.invalid;

import jakarta.enterprise.inject.spi.BeanAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

abstract class ForwardingBeanAttributes<T> implements BeanAttributes<T> {

    protected abstract BeanAttributes<T> attributes();

    @Override
    public Set<Type> getTypes() {
        return attributes().getTypes();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return attributes().getQualifiers();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return attributes().getScope();
    }

    @Override
    public String getName() {
        return attributes().getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return attributes().getStereotypes();
    }

    @Override
    public boolean isAlternative() {
        return attributes().isAlternative();
    }
}
