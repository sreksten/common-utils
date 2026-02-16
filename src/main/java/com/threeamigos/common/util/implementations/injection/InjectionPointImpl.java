package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InjectionPointImpl<T> implements InjectionPoint {

    private final Member member;
    private final Bean<T> bean;
    private final Set<Annotation> qualifiers = new HashSet<>();

    public InjectionPointImpl(Member member, Bean<T> bean) {
        this.member = member;
        this.bean = bean;
    }

    @Override
    public Type getType() {
        return null;
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
