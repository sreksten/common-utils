package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.annotated;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public class TestAnnotatedType<X> implements AnnotatedType<X> {

    private final AnnotatedType<X> delegate;
    private static boolean getConstructorsUsed = false;
    private static boolean getFieldsUsed = false;
    private static boolean getMethodsUsed = false;

    public TestAnnotatedType(AnnotatedType<X> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<AnnotatedConstructor<X>> getConstructors() {
        getConstructorsUsed = true;
        return delegate.getConstructors();
    }

    @Override
    public Set<AnnotatedField<? super X>> getFields() {
        getFieldsUsed = true;
        return delegate.getFields();
    }

    @Override
    public Class<X> getJavaClass() {
        return delegate.getJavaClass();
    }

    @Override
    public Set<AnnotatedMethod<? super X>> getMethods() {
        getMethodsUsed = true;
        return delegate.getMethods();
    }

    @Override
    public Type getBaseType() {
        return delegate.getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure() {
        return delegate.getTypeClosure();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return delegate.getAnnotation(annotationType);
    }

    @Override
    public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
        return delegate.getAnnotations(annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return delegate.getAnnotations();
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return delegate.isAnnotationPresent(annotationType);
    }

    public static boolean isGetConstructorsUsed() {
        return getConstructorsUsed;
    }

    public static boolean isGetFieldsUsed() {
        return getFieldsUsed;
    }

    public static boolean isGetMethodsUsed() {
        return getMethodsUsed;
    }
}
