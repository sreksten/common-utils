package com.threeamigos.common.util.implementations.injection.spievents;

import com.threeamigos.common.util.implementations.injection.literals.AnyLiteral;
import com.threeamigos.common.util.implementations.injection.literals.DefaultLiteral;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.configurator.InjectionPointConfigurator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link InjectionPointConfigurator} used for ProcessInjectionPoint events.
 */
public class InjectionPointConfiguratorImpl implements InjectionPointConfigurator {

    private final InjectionPoint original;
    private Type type;
    private final Set<Annotation> qualifiers;
    private boolean isDelegate;
    private boolean isTransient;

    public InjectionPointConfiguratorImpl(InjectionPoint original) {
        this.original = original;
        this.type = original.getType();
        this.qualifiers = new HashSet<>(original.getQualifiers());
        this.isDelegate = original.isDelegate();
        this.isTransient = original.isTransient();
    }

    @Override
    public InjectionPointConfigurator type(Type type) {
        if (type != null) {
            this.type = type;
        }
        return this;
    }

    @Override
    public InjectionPointConfigurator addQualifier(Annotation qualifier) {
        if (qualifier != null) {
            qualifiers.add(qualifier);
        }
        return this;
    }

    @Override
    public InjectionPointConfigurator addQualifiers(Annotation... qualifiers) {
        if (qualifiers != null) {
            Arrays.stream(qualifiers)
                  .filter(q -> q != null)
                  .forEach(this.qualifiers::add);
        }
        return this;
    }

    @Override
    public InjectionPointConfigurator addQualifiers(Set<Annotation> qualifiers) {
        if (qualifiers != null) {
            qualifiers.stream()
                      .filter(q -> q != null)
                      .forEach(this.qualifiers::add);
        }
        return this;
    }

    @Override
    public InjectionPointConfigurator qualifiers(Annotation... qualifiers) {
        this.qualifiers.clear();
        if (qualifiers != null) {
            addQualifiers(qualifiers);
        }
        return ensureDefaultQualifiers();
    }

    @Override
    public InjectionPointConfigurator qualifiers(Set<Annotation> qualifiers) {
        this.qualifiers.clear();
        if (qualifiers != null) {
            addQualifiers(qualifiers);
        }
        return ensureDefaultQualifiers();
    }

    @Override
    public InjectionPointConfigurator delegate(boolean delegate) {
        this.isDelegate = delegate;
        return this;
    }

    @Override
    public InjectionPointConfigurator transientField(boolean transientField) {
        this.isTransient = transientField;
        return this;
    }

    /**
        * Materializes the configured {@link InjectionPoint}.
        */
    public InjectionPoint complete() {
        ensureDefaultQualifiers();
        return new ConfiguredInjectionPoint(original, type, qualifiers, isDelegate, isTransient);
    }

    private InjectionPointConfigurator ensureDefaultQualifiers() {
        boolean hasDefault = qualifiers.stream()
                .anyMatch(q -> q.annotationType().getName()
                        .equals(jakarta.enterprise.inject.Default.class.getName()));
        boolean hasAny = qualifiers.stream()
                .anyMatch(q -> q.annotationType().getName()
                        .equals(jakarta.enterprise.inject.Any.class.getName()));

        if (!hasDefault) {
            qualifiers.add(new DefaultLiteral());
        }
        if (!hasAny) {
            qualifiers.add(new AnyLiteral());
        }
        return this;
    }
}
