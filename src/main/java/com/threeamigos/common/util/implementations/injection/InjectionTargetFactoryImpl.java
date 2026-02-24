package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.spievents.AnnotatedTypeConfiguratorImpl;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.*;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Factory for creating InjectionTarget instances.
 *
 * <p>This factory creates InjectionTarget objects that handle the lifecycle
 * of a bean instance: instantiation, dependency injection, and lifecycle callbacks.
 *
 * <p><b>Usage in Portable Extensions:</b>
 * <pre>{@code
 * public class MyExtension implements Extension {
 *     void processAnnotatedType(@Observes ProcessAnnotatedType<MyBean> event, BeanManager bm) {
 *         AnnotatedType<MyBean> type = event.getAnnotatedType();
 *         InjectionTargetFactory<MyBean> factory = bm.getInjectionTargetFactory(type);
 *
 *         // Optionally configure before creating
 *         factory.configure().add(new MyQualifierLiteral());
 *
 *         // Create injection target
 *         InjectionTarget<MyBean> it = factory.createInjectionTarget(null);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the bean type
 * @author Stefano Reksten
 */
public class InjectionTargetFactoryImpl<T> implements InjectionTargetFactory<T> {

    private final AnnotatedType<T> annotatedType;
    private final BeanManager beanManager;
    private AnnotatedTypeConfigurator<T> configurator;

    /**
     * Creates an injection target factory.
     *
     * @param annotatedType the annotated type
     * @param beanManager the bean manager
     */
    public InjectionTargetFactoryImpl(AnnotatedType<T> annotatedType, BeanManager beanManager) {
        this.annotatedType = annotatedType;
        this.beanManager = beanManager;
    }

    /**
     * Creates an InjectionTarget for the configured type.
     *
     * <p>The InjectionTarget handles:
     * <ul>
     *   <li>Instance creation via constructor</li>
     *   <li>Field and method injection</li>
     *   <li>@PostConstruct callbacks</li>
     *   <li>@PreDestroy callbacks</li>
     * </ul>
     *
     * @param bean the bean (can be null for synthetic beans)
     * @return the injection target
     */
    @Override
    public InjectionTarget<T> createInjectionTarget(Bean<T> bean) {
        // Get the configured type (if configure() was called)
        AnnotatedType<T> finalType = annotatedType;
        if (configurator != null && configurator instanceof AnnotatedTypeConfiguratorImpl) {
            finalType = ((AnnotatedTypeConfiguratorImpl<T>) configurator).complete();
        }

        return new InjectionTargetImpl<>(finalType, beanManager);
    }

    /**
     * Returns a configurator for modifying the annotated type before creating the injection target.
     *
     * <p>This allows extensions to add/remove annotations before the InjectionTarget is created.
     *
     * @return the configurator
     */
    @Override
    public AnnotatedTypeConfigurator<T> configure() {
        if (configurator == null) {
            configurator = new AnnotatedTypeConfiguratorImpl<>(annotatedType);
        }
        return configurator;
    }

    /**
     * Implementation of InjectionTarget that handles bean lifecycle.
     */
    private static class InjectionTargetImpl<T> implements InjectionTarget<T> {
        private final AnnotatedType<T> annotatedType;
        private final BeanManager beanManager;
        private final Set<InjectionPoint> injectionPoints;

        public InjectionTargetImpl(AnnotatedType<T> annotatedType, BeanManager beanManager) {
            this.annotatedType = annotatedType;
            this.beanManager = beanManager;
            this.injectionPoints = discoverInjectionPoints();
        }

        /**
         * Produces a new instance by calling the constructor.
         *
         * @param ctx the creational context
         * @return the new instance
         */
        @Override
        public T produce(CreationalContext<T> ctx) {
            try {
                Class<T> javaClass = annotatedType.getJavaClass();

                // Find constructor (prefer @Inject, fallback to no-args)
                Constructor<T> constructor = findConstructor(javaClass);
                constructor.setAccessible(true);

                // Resolve constructor parameters
                Object[] args = resolveConstructorParameters(constructor, ctx);

                // Create instance
                return constructor.newInstance(args);

            } catch (Exception e) {
                throw new RuntimeException("Failed to produce instance of " +
                    annotatedType.getJavaClass().getName(), e);
            }
        }

        /**
         * Injects dependencies into an existing instance.
         *
         * @param instance the instance
         * @param ctx the creational context
         */
        @Override
        public void inject(T instance, CreationalContext<T> ctx) {
            try {
                Class<?> clazz = instance.getClass();

                // Inject fields
                for (Field field : clazz.getDeclaredFields()) {
                    if (hasInjectAnnotation(field)) {
                        field.setAccessible(true);
                        Object value = beanManager.getInjectableReference(
                            createInjectionPoint(field), ctx);
                        field.set(instance, value);
                    }
                }

                // Inject methods
                for (Method method : clazz.getDeclaredMethods()) {
                    if (hasInjectAnnotation(method)) {
                        method.setAccessible(true);
                        Object[] args = resolveMethodParameters(method, ctx);
                        method.invoke(instance, args);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to inject dependencies into " +
                    instance.getClass().getName(), e);
            }
        }

        /**
         * Invokes @PostConstruct callback.
         *
         * @param instance the instance
         */
        @Override
        public void postConstruct(T instance) {
            try {
                Class<?> clazz = instance.getClass();

                // Find and invoke @PostConstruct method
                for (Method method : clazz.getDeclaredMethods()) {
                    if (hasPostConstructAnnotation(method)) {
                        method.setAccessible(true);
                        method.invoke(instance);
                        return;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke @PostConstruct on " +
                    instance.getClass().getName(), e);
            }
        }

        /**
         * Invokes @PreDestroy callback.
         *
         * @param instance the instance
         */
        @Override
        public void preDestroy(T instance) {
            try {
                Class<?> clazz = instance.getClass();

                // Find and invoke @PreDestroy method
                for (Method method : clazz.getDeclaredMethods()) {
                    if (hasPreDestroyAnnotation(method)) {
                        method.setAccessible(true);
                        method.invoke(instance);
                        return;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke @PreDestroy on " +
                    instance.getClass().getName(), e);
            }
        }

        /**
         * Disposes of the instance (no-op for managed beans).
         *
         * @param instance the instance
         */
        @Override
        public void dispose(T instance) {
            // For managed beans, disposal is handled via preDestroy
            // For producers, this would call the disposer method
        }

        /**
         * Returns all injection points for this target.
         *
         * @return set of injection points
         */
        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return injectionPoints;
        }

        // Helper methods

        private Set<InjectionPoint> discoverInjectionPoints() {
            Set<InjectionPoint> points = new HashSet<>();
            Class<T> javaClass = annotatedType.getJavaClass();

            // Constructor parameters
            for (Constructor<?> constructor : javaClass.getDeclaredConstructors()) {
                if (hasInjectAnnotation(constructor)) {
                    for (java.lang.reflect.Parameter param : constructor.getParameters()) {
                        points.add(new InjectionPointImpl(param, null));
                    }
                }
            }

            // Fields
            for (Field field : javaClass.getDeclaredFields()) {
                if (hasInjectAnnotation(field)) {
                    points.add(new InjectionPointImpl(field, null));
                }
            }

            // Method parameters
            for (Method method : javaClass.getDeclaredMethods()) {
                if (hasInjectAnnotation(method)) {
                    for (java.lang.reflect.Parameter param : method.getParameters()) {
                        points.add(new InjectionPointImpl(param, null));
                    }
                }
            }

            return points;
        }

        @SuppressWarnings("unchecked")
        private Constructor<T> findConstructor(Class<T> clazz) throws NoSuchMethodException {
            // Look for @Inject constructor
            for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                if (hasInjectAnnotation(ctor)) {
                    return (Constructor<T>) ctor;
                }
            }

            // Fallback to no-args constructor
            return clazz.getDeclaredConstructor();
        }

        private Object[] resolveConstructorParameters(Constructor<T> constructor, CreationalContext<T> ctx) {
            java.lang.reflect.Parameter[] params = constructor.getParameters();
            Object[] args = new Object[params.length];

            for (int i = 0; i < params.length; i++) {
                InjectionPoint ip = new InjectionPointImpl(params[i], null);
                args[i] = beanManager.getInjectableReference(ip, ctx);
            }

            return args;
        }

        private Object[] resolveMethodParameters(Method method, CreationalContext<T> ctx) {
            java.lang.reflect.Parameter[] params = method.getParameters();
            Object[] args = new Object[params.length];

            for (int i = 0; i < params.length; i++) {
                InjectionPoint ip = new InjectionPointImpl(params[i], null);
                args[i] = beanManager.getInjectableReference(ip, ctx);
            }

            return args;
        }

        private InjectionPoint createInjectionPoint(Field field) {
            return new InjectionPointImpl(field, null);
        }

        private boolean hasInjectAnnotation(java.lang.reflect.AnnotatedElement element) {
            return element.isAnnotationPresent(jakarta.inject.Inject.class);
        }

        private boolean hasPostConstructAnnotation(Method method) {
            return method.isAnnotationPresent(jakarta.annotation.PostConstruct.class);
        }

        private boolean hasPreDestroyAnnotation(Method method) {
            return method.isAnnotationPresent(jakarta.annotation.PreDestroy.class);
        }
    }
}
