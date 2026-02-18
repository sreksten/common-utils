package com.threeamigos.common.util.implementations.injection;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InjectionPointImpl<T> implements InjectionPoint {

    private final Member member;
    private final Bean<T> bean;
    private final Type type;
    private final Set<Annotation> qualifiers = new HashSet<>();

    public InjectionPointImpl(Field field, Bean<T> bean) {
        this.member = field;
        this.bean = bean;
        this.type = field.getGenericType();
        collectQualifiers(field.getAnnotations());
    }

    public InjectionPointImpl(Parameter parameter, Bean<T> bean) {
        this.member = parameter.getDeclaringExecutable();
        this.bean = bean;
        this.type = parameter.getParameterizedType();
        collectQualifiers(parameter.getAnnotations());
    }

    private void collectQualifiers(Annotation[] annotations) {
        for (Annotation ann : annotations) {
            if (hasQualifierAnnotation(ann.annotationType())) {
                qualifiers.add(ann);
            }
        }

        // CDI defaulting rules: if no qualifier present, add @Default; always include @Any
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
        qualifiers.add(new AnyLiteral());
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.unmodifiableSet(qualifiers);
    }

    public void addQualifier(Annotation qualifier) {
        qualifiers.add(qualifier);
    }

    @Override
    public Bean<T> getBean() {
        return bean;
    }

    @Override
    public Member getMember() {
        return member;
    }

    @Override
    public Annotated getAnnotated() {
        return null;
    }

    @Override
    public boolean isDelegate() {
        return false;
    }

    @Override
    public boolean isTransient() {
        return false;
    }
}
