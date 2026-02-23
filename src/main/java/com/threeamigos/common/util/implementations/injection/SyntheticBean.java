package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Implementation of Bean for synthetic beans created programmatically via BeanConfigurator.
 * Synthetic beans are not discovered via classpath scanning but are registered by
 * portable extensions during the AfterBeanDiscovery phase.
 *
 * @param <T> the bean type
 * @author CDI Container
 */
public class SyntheticBean<T> implements Bean<T> {

    // Bean attributes
    private final Class<?> beanClass;
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;
    private final Class<? extends Annotation> scope;
    private final String name;
    private final Set<Class<? extends Annotation>> stereotypes;
    private final boolean alternative;
    private final Integer priority;

    // Creation/destruction callbacks
    private final Function<CreationalContext<T>, T> createCallback;
    private final BiConsumer<T, CreationalContext<T>> destroyCallback;

    // Injection points
    private final Set<InjectionPoint> injectionPoints;

    /**
     * Constructor for building synthetic beans.
     * Package-private to allow construction from BeanConfiguratorImpl.
     */
    public SyntheticBean(
            Class<?> beanClass,
            Set<Type> types,
            Set<Annotation> qualifiers,
            Class<? extends Annotation> scope,
            String name,
            Set<Class<? extends Annotation>> stereotypes,
            boolean alternative,
            Integer priority,
            Function<CreationalContext<T>, T> createCallback,
            BiConsumer<T, CreationalContext<T>> destroyCallback,
            Set<InjectionPoint> injectionPoints) {

        this.beanClass = beanClass;
        this.types = Collections.unmodifiableSet(new HashSet<>(types));
        this.qualifiers = Collections.unmodifiableSet(new HashSet<>(qualifiers));
        this.scope = scope;
        this.name = name;
        this.stereotypes = Collections.unmodifiableSet(new HashSet<>(stereotypes));
        this.alternative = alternative;
        this.priority = priority;
        this.createCallback = createCallback;
        this.destroyCallback = destroyCallback;
        this.injectionPoints = Collections.unmodifiableSet(new HashSet<>(injectionPoints));
    }

    @Override
    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return stereotypes;
    }

    @Override
    public boolean isAlternative() {
        return alternative;
    }

    /**
     * Returns the priority for alternative ordering.
     * This is a non-standard extension to support @Priority on alternatives.
     */
    public Integer getPriority() {
        return priority;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return injectionPoints;
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        if (createCallback == null) {
            throw new IllegalStateException(
                "Synthetic bean " + beanClass.getName() + " has no create callback defined"
            );
        }
        return createCallback.apply(creationalContext);
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        if (destroyCallback != null) {
            destroyCallback.accept(instance, creationalContext);
        }

        // Always release the creational context
        if (creationalContext != null) {
            creationalContext.release();
        }
    }

    @Override
    public String toString() {
        return "SyntheticBean{" +
               "beanClass=" + beanClass.getName() +
               ", types=" + types +
               ", qualifiers=" + qualifiers +
               ", scope=" + scope.getSimpleName() +
               ", name='" + name + '\'' +
               ", alternative=" + alternative +
               '}';
    }
}
