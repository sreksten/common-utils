package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.spi.wrappers.AnnotatedConstructorWrapper;
import com.threeamigos.common.util.implementations.injection.spi.wrappers.AnnotatedFieldWrapper;
import com.threeamigos.common.util.implementations.injection.spi.wrappers.AnnotatedMethodWrapper;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.context.NormalScope;
import jakarta.inject.Scope;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        if (annotationType == null) {
            return null;
        }
        for (Annotation annotation : getAnnotations()) {
            if (annotationType.equals(annotation.annotationType())) {
                return annotationType.cast(annotation);
            }
        }
        return null;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        Set<Annotation> annotations = new HashSet<Annotation>();
        // Always include annotations declared directly on the type.
        java.util.Collections.addAll(annotations, javaClass.getDeclaredAnnotations());

        // CDI 4.1: only special scope inheritance rules are applied on types.
        if (!hasDeclaredScope(javaClass)) {
            Class<?> current = javaClass.getSuperclass();
            while (current != null && current != Object.class) {
                if (hasDeclaredScope(current)) {
                    for (Annotation annotation : current.getDeclaredAnnotations()) {
                        if (isScopeAnnotation(annotation.annotationType())) {
                            annotations.add(annotation);
                        }
                    }
                    break;
                }
                current = current.getSuperclass();
            }
        }

        return annotations;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }

    private Set<AnnotatedField<? super T>> buildFields() {
        Set<AnnotatedField<? super T>> result = new HashSet<AnnotatedField<? super T>>();
        Class<?> current = javaClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                result.add(new AnnotatedFieldWrapper<T>(field, this));
            }
            current = current.getSuperclass();
        }
        return result;
    }

    private Set<AnnotatedMethod<? super T>> buildMethods() {
        Set<AnnotatedMethod<? super T>> result = new HashSet<AnnotatedMethod<? super T>>();
        Class<?> current = javaClass;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                result.add(new AnnotatedMethodWrapper<T>(method, this));
            }
            current = current.getSuperclass();
        }
        return result;
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

    private boolean hasDeclaredScope(Class<?> type) {
        for (Annotation annotation : type.getDeclaredAnnotations()) {
            if (isScopeAnnotation(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    private boolean isScopeAnnotation(Class<? extends Annotation> annotationType) {
        return annotationType.isAnnotationPresent(Scope.class) ||
                annotationType.isAnnotationPresent(NormalScope.class);
    }
}
