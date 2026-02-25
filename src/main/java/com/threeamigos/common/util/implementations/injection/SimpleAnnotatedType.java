package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Minimal AnnotatedType implementation used for SPI events where full
 * annotation metadata is not required. Provides type closure and class
 * reference; returns empty annotation/members collections.
 */
public class SimpleAnnotatedType<T> implements AnnotatedType<T> {

    private final Class<T> javaClass;
    private final Set<Type> typeClosure;
    private final Set<AnnotatedField<? super T>> fields;
    private final Set<AnnotatedMethod<? super T>> methods;
    private final Set<AnnotatedConstructor<T>> constructors;

    public SimpleAnnotatedType(Class<T> javaClass) {
        if (javaClass == null) {
            throw new IllegalArgumentException("javaClass cannot be null");
        }
        this.javaClass = javaClass;
        this.typeClosure = buildTypeClosure(javaClass);
        this.fields = buildFields();
        this.methods = buildMethods();
        this.constructors = buildConstructors();
    }

    @Override
    public Class<T> getJavaClass() {
        return javaClass;
    }

    @Override
    public Set<AnnotatedConstructor<T>> getConstructors() {
        return Collections.unmodifiableSet(constructors);
    }

    @Override
    public Set<AnnotatedMethod<? super T>> getMethods() {
        return Collections.unmodifiableSet(methods);
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields() {
        return Collections.unmodifiableSet(fields);
    }

    @Override
    public Type getBaseType() {
        return javaClass;
    }

    @Override
    public Set<Type> getTypeClosure() {
        return Collections.unmodifiableSet(typeClosure);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return null;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return new HashSet<>(java.util.Arrays.asList(javaClass.getAnnotations()));
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return javaClass.isAnnotationPresent(annotationType);
    }

    private Set<AnnotatedField<? super T>> buildFields() {
        return java.util.Arrays.stream(javaClass.getDeclaredFields())
                .map(f -> new AnnotatedFieldWrapper<T>(f, this))
                .collect(Collectors.toSet());
    }

    private Set<AnnotatedMethod<? super T>> buildMethods() {
        return java.util.Arrays.stream(javaClass.getDeclaredMethods())
                .map(m -> new AnnotatedMethodWrapper<T>(m, this))
                .collect(Collectors.toSet());
    }

    private Set<AnnotatedConstructor<T>> buildConstructors() {
        return java.util.Arrays.stream(javaClass.getDeclaredConstructors())
                .map(c -> new AnnotatedConstructorWrapper<T>((java.lang.reflect.Constructor<T>) c, this))
                .collect(Collectors.toSet());
    }

    private Set<Type> buildTypeClosure(Class<?> clazz) {
        Set<Type> closure = new HashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            closure.add(current);
            for (Class<?> iface : current.getInterfaces()) {
                closure.add(iface);
            }
            current = current.getSuperclass();
        }
        closure.add(Object.class);
        return closure;
    }
}
