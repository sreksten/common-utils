package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.spi.SyntheticBean;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Implementation of BeanConfigurator for building synthetic beans programmatically.
 *
 * <p>This fluent API allows portable extensions to create beans during the
 * AfterBeanDiscovery phase without requiring classpath scanning or annotations.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * public class MyExtension implements Extension {
 *     public void addCustomBean(@Observes AfterBeanDiscovery event) {
 *         event.<MyService>addBean()
 *             .beanClass(MyService.class)
 *             .types(MyService.class, Object.class)
 *             .qualifiers(new NamedLiteral("myService"))
 *             .scope(ApplicationScoped.class)
 *             .createWith(ctx -> new MyService())
 *             .destroyWith((instance, ctx) -> instance.cleanup());
 *     }
 * }
 * }</pre>
 *
 * @param <T> the bean type
 * @see jakarta.enterprise.inject.spi.configurator.BeanConfigurator
 */
public class BeanConfiguratorImpl<T> implements BeanConfigurator<T> {

    private final KnowledgeBase knowledgeBase;

    // Bean configuration
    private Class<?> beanClass;
    private final Set<Type> types = new LinkedHashSet<>();
    private final Set<Annotation> qualifiers = new LinkedHashSet<>();
    private Class<? extends Annotation> scope = Dependent.class;
    private String name;
    private String id;  // Bean identifier
    private final Set<Class<? extends Annotation>> stereotypes = new LinkedHashSet<>();
    private boolean alternative = false;
    private Integer priority;

    // Callbacks
    private Function<CreationalContext<T>, T> createCallback;
    private BiConsumer<T, CreationalContext<T>> destroyCallback;

    // Injection points
    private final Set<InjectionPoint> injectionPoints = new LinkedHashSet<>();

    public BeanConfiguratorImpl(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public BeanConfigurator<T> beanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
        return this;
    }

    @Override
    public BeanConfigurator<T> addType(Type type) {
        if (type != null) {
            this.types.add(type);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addType(jakarta.enterprise.util.TypeLiteral<?> typeLiteral) {
        if (typeLiteral != null) {
            this.types.add(typeLiteral.getType());
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> types(Type... types) {
        this.types.clear();
        if (types != null) {
            this.types.addAll(Arrays.asList(types));
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> types(Set<Type> types) {
        this.types.clear();
        if (types != null) {
            this.types.addAll(types);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addTypes(Set<Type> types) {
        if (types != null) {
            this.types.addAll(types);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addTypes(Type... types) {
        if (types != null) {
            this.types.addAll(Arrays.asList(types));
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addTransitiveTypeClosure(Type type) {
        if (type != null) {
            // Add the type itself
            this.types.add(type);

            // Add all supertypes if it's a class
            if (type instanceof Class<?>) {
                Class<?> clazz = (Class<?>) type;
                Class<?> current = clazz.getSuperclass();
                while (current != null && current != Object.class) {
                    this.types.add(current);
                    current = current.getSuperclass();
                }

                // Add interfaces
                for (Class<?> iface : clazz.getInterfaces()) {
                    this.types.add(iface);
                }
            }

            // Always add Object
            this.types.add(Object.class);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> scope(Class<? extends Annotation> scope) {
        this.scope = scope != null ? scope : Dependent.class;
        return this;
    }

    @Override
    public BeanConfigurator<T> addQualifier(Annotation qualifier) {
        if (qualifier != null) {
            this.qualifiers.add(qualifier);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> qualifiers(Annotation... qualifiers) {
        this.qualifiers.clear();
        if (qualifiers != null) {
            this.qualifiers.addAll(Arrays.asList(qualifiers));
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> qualifiers(Set<Annotation> qualifiers) {
        this.qualifiers.clear();
        if (qualifiers != null) {
            this.qualifiers.addAll(qualifiers);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addQualifiers(Set<Annotation> qualifiers) {
        if (qualifiers != null) {
            this.qualifiers.addAll(qualifiers);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addQualifiers(Annotation... qualifiers) {
        if (qualifiers != null) {
            this.qualifiers.addAll(Arrays.asList(qualifiers));
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addStereotype(Class<? extends Annotation> stereotype) {
        if (stereotype != null) {
            this.stereotypes.add(stereotype);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> stereotypes(Set<Class<? extends Annotation>> stereotypes) {
        this.stereotypes.clear();
        if (stereotypes != null) {
            this.stereotypes.addAll(stereotypes);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addStereotypes(Set<Class<? extends Annotation>> stereotypes) {
        if (stereotypes != null) {
            this.stereotypes.addAll(stereotypes);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public BeanConfigurator<T> id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public BeanConfigurator<T> alternative(boolean alternative) {
        this.alternative = alternative;
        return this;
    }

    @Override
    public BeanConfigurator<T> priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public BeanConfigurator<T> addInjectionPoint(InjectionPoint injectionPoint) {
        if (injectionPoint != null) {
            this.injectionPoints.add(injectionPoint);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> injectionPoints(InjectionPoint... injectionPoints) {
        this.injectionPoints.clear();
        if (injectionPoints != null) {
            this.injectionPoints.addAll(Arrays.asList(injectionPoints));
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> injectionPoints(Set<InjectionPoint> injectionPoints) {
        this.injectionPoints.clear();
        if (injectionPoints != null) {
            this.injectionPoints.addAll(injectionPoints);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addInjectionPoints(Set<InjectionPoint> injectionPoints) {
        if (injectionPoints != null) {
            this.injectionPoints.addAll(injectionPoints);
        }
        return this;
    }

    @Override
    public BeanConfigurator<T> addInjectionPoints(InjectionPoint... injectionPoints) {
        if (injectionPoints != null) {
            this.injectionPoints.addAll(Arrays.asList(injectionPoints));
        }
        return this;
    }

    @Override
    public <U extends T> BeanConfigurator<U> createWith(Function<CreationalContext<U>, U> callback) {
        @SuppressWarnings("unchecked")
        BeanConfiguratorImpl<U> configurator = (BeanConfiguratorImpl<U>) this;
        configurator.createCallback = callback;
        return configurator;
    }

    @Override
    public <U extends T> BeanConfigurator<U> produceWith(Function<jakarta.enterprise.inject.Instance<Object>, U> callback) {
        // Convert Instance-based callback to CreationalContext-based
        @SuppressWarnings("unchecked")
        BeanConfiguratorImpl<U> configurator = (BeanConfiguratorImpl<U>) this;
        configurator.createCallback = ctx -> {
            // For now, we don't have Instance available in this context
            // Extensions should use createWith() instead
            throw new UnsupportedOperationException(
                "produceWith() not yet supported. Use createWith(Function<CreationalContext<T>, T>) instead"
            );
        };
        return configurator;
    }

    @Override
    public BeanConfigurator<T> destroyWith(BiConsumer<T, CreationalContext<T>> callback) {
        this.destroyCallback = callback;
        return this;
    }

    @Override
    public BeanConfigurator<T> disposeWith(BiConsumer<T, jakarta.enterprise.inject.Instance<Object>> callback) {
        // Convert Instance-based callback to CreationalContext-based
        this.destroyCallback = (instance, ctx) -> {
            // For now, we don't have Instance available in this context
            // Extensions should use destroyWith() instead
            throw new UnsupportedOperationException(
                "disposeWith() not yet supported. Use destroyWith(BiConsumer<T, CreationalContext<T>>) instead"
            );
        };
        return this;
    }

    @Override
    public <U extends T> BeanConfigurator<U> read(jakarta.enterprise.inject.spi.AnnotatedType<U> type) {
        if (type != null) {
            // Read configuration from AnnotatedType
            this.beanClass = type.getJavaClass();
            this.types.clear();
            this.types.add(type.getJavaClass());

            // Add all supertypes
            Class<?> current = type.getJavaClass().getSuperclass();
            while (current != null && current != Object.class) {
                this.types.add(current);
                current = current.getSuperclass();
            }

            // Add interfaces
            for (Class<?> iface : type.getJavaClass().getInterfaces()) {
                this.types.add(iface);
            }

            // Always add Object
            this.types.add(Object.class);
        }
        @SuppressWarnings("unchecked")
        BeanConfigurator<U> result = (BeanConfigurator<U>) this;
        return result;
    }

    @Override
    public BeanConfigurator<T> read(jakarta.enterprise.inject.spi.BeanAttributes<?> beanAttributes) {
        if (beanAttributes != null) {
            // Read attributes from BeanAttributes
            this.types.clear();
            this.types.addAll(beanAttributes.getTypes());

            this.qualifiers.clear();
            this.qualifiers.addAll(beanAttributes.getQualifiers());

            this.scope = beanAttributes.getScope();
            this.name = beanAttributes.getName();

            this.stereotypes.clear();
            this.stereotypes.addAll(beanAttributes.getStereotypes());

            this.alternative = beanAttributes.isAlternative();
        }
        return this;
    }

    /**
     * Completes the bean configuration and registers the synthetic bean.
     * This method is called automatically when the BeanConfigurator goes out of scope
     * in the extension's observer method.
     */
    public void complete() {
        // Validate required fields
        if (beanClass == null) {
            throw new IllegalStateException("beanClass must be set for synthetic bean");
        }

        if (createCallback == null) {
            throw new IllegalStateException(
                "createWith() callback must be set for synthetic bean " + beanClass.getName()
            );
        }

        // Apply defaults
        if (types.isEmpty()) {
            types.add(beanClass);
            types.add(Object.class);
        }

        if (qualifiers.isEmpty()) {
            // Add @Default qualifier if none specified
            qualifiers.add(new Default.Literal());
        }

        // Build the synthetic bean
        SyntheticBean<T> syntheticBean = new SyntheticBean<>(
            beanClass,
            types,
            qualifiers,
            scope,
            name,
            stereotypes,
            alternative,
            priority,
            createCallback,
            destroyCallback,
            injectionPoints
        );

        // Register with knowledge base
        knowledgeBase.addBean(syntheticBean);

        System.out.println("[BeanConfigurator] Created synthetic bean: " +
                          beanClass.getSimpleName() +
                          " with scope @" + scope.getSimpleName());
    }
}
