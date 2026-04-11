package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.AnnotationsEnum;
import com.threeamigos.common.util.implementations.injection.spi.wrappers.AnnotatedConstructorWrapper;
import com.threeamigos.common.util.implementations.injection.spi.wrappers.AnnotatedFieldWrapper;
import com.threeamigos.common.util.implementations.injection.spi.wrappers.AnnotatedMethodWrapper;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Minimal AnnotatedType implementation used for SPI events where full
 * annotation metadata is not required. Provides type closure and class
 * reference; returns empty annotation/member collections.
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
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
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
        Map<Class<? extends Annotation>, Annotation> annotationsByType =
                new LinkedHashMap<>();
        for (Annotation annotation : javaClass.getDeclaredAnnotations()) {
            annotationsByType.put(annotation.annotationType(), annotation);
        }

        // CDI metadata inheritance for type annotations: inherit only CDI-relevant
        // @Inherited annotations (qualifiers, stereotypes, interceptor bindings).
        Class<?> current = javaClass.getSuperclass();
        while (current != null && current != Object.class) {
            for (Annotation annotation : current.getDeclaredAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationsByType.containsKey(annotationType)) {
                    continue;
                }
                if (!AnnotationsEnum.hasInheritedAnnotation(annotationType)) {
                    continue;
                }
                if (!isCdiInheritableTypeAnnotation(annotationType)) {
                    continue;
                }
                annotationsByType.put(annotationType, annotation);
            }
            current = current.getSuperclass();
        }

        Set<Annotation> annotations = new HashSet<>(annotationsByType.values());

        boolean hasDeclaredScope = hasDeclaredScope(javaClass);
        if (hasDeclaredScope) {
            Set<Class<? extends Annotation>> declaredScopeTypes = new HashSet<>();
            for (Annotation annotation : javaClass.getDeclaredAnnotations()) {
                if (isScopeAnnotation(annotation.annotationType())) {
                    declaredScopeTypes.add(annotation.annotationType());
                }
            }
            Iterator<Annotation> iterator = annotations.iterator();
            while (iterator.hasNext()) {
                Annotation annotation = iterator.next();
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (isScopeAnnotation(annotationType) && !declaredScopeTypes.contains(annotationType)) {
                    iterator.remove();
                }
            }
            return annotations;
        }

        // CDI special rule: if no scope is declared directly on this type, inherit the nearest
        // scope from the superclass hierarchy even if that scope annotation is not @Inherited.
        annotations.removeIf(annotation -> isScopeAnnotation(annotation.annotationType()));

        Class<?> scopeSource = javaClass.getSuperclass();
        while (scopeSource != null && scopeSource != Object.class) {
            if (hasDeclaredScope(scopeSource)) {
                for (Annotation annotation : scopeSource.getDeclaredAnnotations()) {
                    if (isScopeAnnotation(annotation.annotationType())) {
                        annotations.add(annotation);
                    }
                }
                break;
            }
            scopeSource = scopeSource.getSuperclass();
        }

        return annotations;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }

    private Set<AnnotatedField<? super T>> buildFields() {
        Set<AnnotatedField<? super T>> result = new HashSet<>();
        Class<?> current = javaClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }
                result.add(new AnnotatedFieldWrapper<>(field, this));
            }
            current = current.getSuperclass();
        }
        return result;
    }

    private Set<AnnotatedMethod<? super T>> buildMethods() {
        Set<AnnotatedMethod<? super T>> result = new HashSet<>();
        Class<?> current = javaClass;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isSynthetic() || method.isBridge()) {
                    continue;
                }
                result.add(new AnnotatedMethodWrapper<>(method, this));
            }
            current = current.getSuperclass();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Set<AnnotatedConstructor<T>> buildConstructors() {
        return java.util.Arrays.stream(javaClass.getDeclaredConstructors())
                .map(c -> new AnnotatedConstructorWrapper<>((java.lang.reflect.Constructor<T>) c, this))
                .collect(Collectors.toSet());
    }

    private Set<Type> buildTypeClosure(Class<?> clazz) {
        Set<Type> closure = new HashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            closure.add(current);
            closure.addAll(Arrays.asList(current.getInterfaces()));
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
        return AnnotationsEnum.hasScopeAnnotation(annotationType) ||
                AnnotationsEnum.hasNormalScopeAnnotation(annotationType);
    }

    private boolean isCdiInheritableTypeAnnotation(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return false;
        }
        if (isScopeAnnotation(annotationType)) {
            return true;
        }
        return AnnotationsEnum.hasQualifierAnnotation(annotationType)
                || AnnotationsEnum.hasStereotypeAnnotation(annotationType)
                || AnnotationsEnum.hasInterceptorBindingAnnotation(annotationType);
    }
}
