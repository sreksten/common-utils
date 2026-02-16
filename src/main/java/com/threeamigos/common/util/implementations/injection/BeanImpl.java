package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BeanImpl<T> implements Bean<T> {

    // Bean
    private final Class<T> beanClass;
    private final Set<InjectionPoint> injectionPoints = new HashSet<>();

    // BeanAttributes
    private String name;
    private final Set<Annotation> qualifiers = new HashSet<>();
    private Class<? extends Annotation> scope;
    private final Set<Class<? extends Annotation>> stereotypes = new HashSet<>();
    private final Set<Type> types = new HashSet<>();
    private final boolean alternative;

    // Validation state
    private boolean hasValidationErrors = false;

    public BeanImpl(Class<T> beanClass, boolean alternative) {
        this.beanClass = beanClass;
        this.name = "";
        this.scope = null;
        this.alternative = alternative;
    }

    @Override
    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.unmodifiableSet(injectionPoints);
    }

    public void addInjectionPoint(InjectionPoint injectionPoint) {
        injectionPoints.add(injectionPoint);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name == null) ? "" : name;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.unmodifiableSet(qualifiers);
    }

    public void setQualifiers(Set<Annotation> qualifiers) {
        this.qualifiers.clear();
        if (qualifiers != null) {
            this.qualifiers.addAll(qualifiers);
        }
    }

    public void addQualifier(Annotation qualifier) {
        if (qualifier != null) {
            this.qualifiers.add(qualifier);
        }
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    public void setScope(Class<? extends Annotation> scope) {
        this.scope = scope;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.unmodifiableSet(stereotypes);
    }

    public void setStereotypes(Set<Class<? extends Annotation>> stereotypes) {
        this.stereotypes.clear();
        if (stereotypes != null) {
            this.stereotypes.addAll(stereotypes);
        }
    }

    public void addStereotype(Class<? extends Annotation> stereotype) {
        if (stereotype != null) {
            this.stereotypes.add(stereotype);
        }
    }

    @Override
    public Set<Type> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    public void setTypes(Set<Type> types) {
        this.types.clear();
        if (types != null) {
            this.types.addAll(types);
        }
    }

    public void addType(Type type) {
        if (type != null) {
            this.types.add(type);
        }
    }

    @Override
    public boolean isAlternative() {
        return alternative;
    }

    @Override
    //FIXME
    public T create(CreationalContext<T> creationalContext) {
        return null;
    }

    @Override
    //FIXME
    public void destroy(T t, CreationalContext<T> creationalContext) {

    }

    /**
     * Returns whether this bean has validation errors.
     * A bean with validation errors should not be used for dependency resolution,
     * but the error should only be reported if the bean is actually needed.
     *
     * @return true if the bean has validation errors, false otherwise
     */
    public boolean hasValidationErrors() {
        return hasValidationErrors;
    }

    /**
     * Marks this bean as having validation errors.
     * This should be called during validation if any CDI constraint is violated.
     */
    public void setHasValidationErrors(boolean hasValidationErrors) {
        this.hasValidationErrors = hasValidationErrors;
    }

}