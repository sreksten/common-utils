package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.interceptors.custom;

import com.threeamigos.common.util.implementations.injection.util.TypeClosureHelper;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

/**
 * Utility class for extension-provided interceptor tests.
 */
public abstract class AbstractInterceptor<T> implements Interceptor<T> {

    public Set<Type> getTypes() {
        return TypeClosureHelper.extractTypesFromClass(getBeanClass());
    }

    public Set<Annotation> getQualifiers() {
        return Collections.emptySet();
    }

    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    public String getName() {
        return null;
    }

    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    public boolean isAlternative() {
        return false;
    }

    public boolean isNullable() {
        return false;
    }

    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    public T create(CreationalContext<T> creationalContext) {
        try {
            return (T) getBeanClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error creating an instance of " + getBeanClass());
        }
    }

    public void destroy(T instance, CreationalContext<T> creationalContext) {
        creationalContext.release();
    }
}
