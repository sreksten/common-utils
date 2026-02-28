package com.threeamigos.common.util.implementations.injection.resolution;

import com.threeamigos.common.util.implementations.injection.util.RawTypeExtractor;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.inject.*;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;

/**
 * Generic wrapper implementing CDI {@link Instance} interface for lazy and programmatic
 * bean resolution. This class provides a reusable implementation that can work with
 * different dependency resolution strategies.
 *
 * <p>The wrapper supports all Instance operations:
 * <ul>
 *   <li>{@link #get()} - Lazily retrieves an instance</li>
 *   <li>{@link #select(Annotation...)} - Refines selection with additional qualifiers</li>
 *   <li>{@link #isAmbiguous()} - Checks if multiple implementations exist</li>
 *   <li>{@link #isUnsatisfied()} - Checks if no implementations exist</li>
 *   <li>{@link #iterator()} - Iterates over all matching implementations</li>
 *   <li>{@link #destroy(Object)} - Explicitly invokes {@link jakarta.annotation.PreDestroy} on an instance</li>
 *   <li>{@link #getHandle()} - Returns a Handle for explicit lifecycle management</li>
 *   <li>{@link #handles()} - Returns all Handles for matching beans</li>
 * </ul>
 *
 * <p>This implementation is generic and delegates actual bean resolution to a
 * {@link ResolutionStrategy} provided at construction time. This allows it to work
 * with both InjectorImpl and BeanResolver approaches.
 *
 * @param <T> the type of instances this Instance provides
 * @author Stefano Reksten
 * @see jakarta.enterprise.inject.Instance
 */
public class InstanceImpl<T> implements Instance<T> {

    private final Class<T> type;
    private final Collection<Annotation> qualifiers;
    private final ResolutionStrategy<T> resolutionStrategy;
    private final Function<Class<? extends T>, Bean<? extends T>> beanLookup;

    @SuppressWarnings("unchecked")
    private <U extends T> Function<Class<? extends U>, Bean<? extends U>> adaptBeanLookup() {
        if (beanLookup == null) {
            return null;
        }
        return clazz -> {
            Bean<? extends T> bean = beanLookup.apply((Class<? extends T>) clazz);
            return (Bean<? extends U>) bean;
        };
    }

    /**
     * Strategy interface for resolving beans and instances.
     * This allows InstanceWrapper to work with different resolution mechanisms.
     *
     * @param <T> the type being resolved
     */
    public interface ResolutionStrategy<T> {
        /**
         * Resolves and creates a single instance of the specified type with qualifiers.
         *
         * @param type the class to resolve
         * @param qualifiers the qualifiers to match
         * @return a fully injected instance
         * @throws Exception if resolution fails
         */
        T resolveInstance(Class<T> type, Collection<Annotation> qualifiers) throws Exception;

        /**
         * Resolves all implementation classes that match the type and qualifiers.
         *
         * @param type the type to resolve
         * @param qualifiers the qualifiers to match
         * @return collection of matching implementation classes
         * @throws Exception if resolution fails
         */
        Collection<Class<? extends T>> resolveImplementations(Class<T> type, Collection<Annotation> qualifiers) throws Exception;

        /**
         * Invokes @PreDestroy lifecycle methods on the given instance.
         *
         * @param instance the instance to destroy
         * @throws InvocationTargetException if @PreDestroy method throws
         * @throws IllegalAccessException if @PreDestroy method cannot be accessed
         */
        void invokePreDestroy(T instance) throws InvocationTargetException, IllegalAccessException;
    }

    /**
     * Creates an Instance wrapper with the specified resolution strategy.
     *
     * @param type the class of instances to provide
     * @param qualifiers the qualifiers to use for instance resolution
     * @param resolutionStrategy the strategy for resolving beans
     */
    public InstanceImpl(Class<T> type,
                 Collection<Annotation> qualifiers,
                 ResolutionStrategy<T> resolutionStrategy) {
        this(type, qualifiers, resolutionStrategy, null);
    }

    public InstanceImpl(Class<T> type,
                 Collection<Annotation> qualifiers,
                 ResolutionStrategy<T> resolutionStrategy,
                 Function<Class<? extends T>, Bean<? extends T>> beanLookup) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers cannot be null");
        this.resolutionStrategy = Objects.requireNonNull(resolutionStrategy, "resolutionStrategy cannot be null");
        this.beanLookup = beanLookup;
    }

    @Override
    public T get() {
        try {
            return resolutionStrategy.resolveInstance(type, qualifiers);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject " + type.getName(), e);
        }
    }

    @Override
    public Instance<T> select(Annotation... annotations) {
        return new InstanceImpl<>(type, mergeQualifiers(qualifiers, annotations), resolutionStrategy, beanLookup);
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... annotations) {
        @SuppressWarnings("unchecked")
        ResolutionStrategy<U> castStrategy = (ResolutionStrategy<U>) resolutionStrategy;
        return new InstanceImpl<>(subtype, mergeQualifiers(qualifiers, annotations), castStrategy, adaptBeanLookup());
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... annotations) {
        // Extract the raw class from the TypeLiteral to maintain compatibility
        @SuppressWarnings("unchecked")
        Class<U> rawType = (Class<U>) RawTypeExtractor.getRawType(subtype.getType());
        @SuppressWarnings("unchecked")
        ResolutionStrategy<U> castStrategy = (ResolutionStrategy<U>) resolutionStrategy;
        return new InstanceImpl<>(rawType, mergeQualifiers(qualifiers, annotations), castStrategy, adaptBeanLookup());
    }

    @Override
    public boolean isUnsatisfied() {
        try {
            return resolutionStrategy.resolveImplementations(type, qualifiers).isEmpty();
        } catch (Exception e) {
            return true; // treating an Exception as unsatisfied
        }
    }

    @Override
    public boolean isAmbiguous() {
        try {
            return resolutionStrategy.resolveImplementations(type, qualifiers).size() > 1;
        } catch (Exception e) {
            return false; // If we can't resolve the class, it's not ambiguous (it's unsatisfied)
        }
    }

    @Override
    public void destroy(T instance) {
        try {
            if (instance != null) {
                resolutionStrategy.invokePreDestroy(instance);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke @PreDestroy on " + type.getName(), e);
        }
    }

    @Override
    public Handle<T> getHandle() {
        // Per CDI spec: get the single bean or throw exception if ambiguous/unsatisfied
        try {
            Collection<Class<? extends T>> implementations = resolutionStrategy.resolveImplementations(type, qualifiers);

            if (implementations.isEmpty()) {
                throw new UnsatisfiedResolutionException(
                    "No bean found for type " + type.getName() + " with qualifiers " + qualifiers);
            }

            if (implementations.size() > 1) {
                throw new AmbiguousResolutionException(
                    "Multiple beans found for type " + type.getName() + " with qualifiers " + qualifiers);
            }

            Class<? extends T> resolvedClass = implementations.iterator().next();
            return createHandle(resolvedClass);

        } catch (UnsatisfiedResolutionException | AmbiguousResolutionException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get handle for " + type.getName(), e);
        }
    }

    @Override
    public Iterable<? extends Handle<T>> handles() {
        // Per CDI spec: return handles for ALL matching beans
        try {
            Collection<Class<? extends T>> implementations = resolutionStrategy.resolveImplementations(type, qualifiers);

            List<Handle<T>> handleList = new ArrayList<>();
            for (Class<? extends T> implClass : implementations) {
                handleList.add(createHandle(implClass));
            }

            return handleList;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get handles for " + type.getName(), e);
        }
    }

    /**
     * Creates a Handle implementation for a specific bean class.
     * The Handle provides lazy access to the bean instance and lifecycle management.
     */
    private Handle<T> createHandle(Class<? extends T> beanClass) {
        return new Handle<T>() {
            private T instance;
            private boolean destroyed = false;

            @Override
            public T get() {
                if (destroyed) {
                    throw new IllegalStateException("Handle has been destroyed for " + beanClass.getName());
                }

                // Lazy initialization - create instance only when first needed
                if (instance == null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<T> classToResolve = (Class<T>) beanClass;
                        instance = resolutionStrategy.resolveInstance(classToResolve, qualifiers);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance of " + beanClass.getName(), e);
                    }
                }

                return instance;
            }

            @Override
            public jakarta.enterprise.inject.spi.Bean<T> getBean() {
                if (beanLookup != null) {
                    @SuppressWarnings("unchecked")
                    Bean<T> bean = (Bean<T>) beanLookup.apply(beanClass);
                    if (bean != null) {
                        return bean;
                    }
                }

                // Fallback: synthesize a lightweight BeanImpl with available metadata
                BeanImpl<T> fallback = new BeanImpl<>((Class<T>) beanClass, false);
                if (!qualifiers.isEmpty()) {
                    fallback.setQualifiers(new HashSet<>(qualifiers));
                }

                Set<java.lang.reflect.Type> types = new HashSet<>();
                types.add(beanClass);
                fallback.setTypes(types);
                return fallback;
            }

            @Override
            public void destroy() {
                if (!destroyed && instance != null) {
                    try {
                        resolutionStrategy.invokePreDestroy(instance);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to destroy instance of " + beanClass.getName(), e);
                    }
                    instance = null;
                    destroyed = true;
                }
                // Per spec: multiple calls to destroy() are no-ops
            }

            @Override
            public void close() {
                // Per spec: close() delegates to destroy()
                destroy();
            }
        };
    }

    @Override
    public @Nonnull Iterator<T> iterator() {
        try {
            Collection<Class<? extends T>> classes = resolutionStrategy.resolveImplementations(type, qualifiers);

            List<T> instances = new ArrayList<>();
            for (Class<? extends T> clazz : classes) {
                instances.add(resolutionStrategy.resolveInstance((Class<T>) clazz, qualifiers));
            }
            return instances.iterator();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve implementations", e);
        }
    }

    /**
     * Merges qualifier annotations, giving precedence to new annotations. This is used when
     * {@link Instance#select(Annotation...)} is called to refine the qualifier set.
     *
     * <p>Merge Rules:
     * <ul>
     *   <li>New annotations override existing ones of the same type</li>
     *   <li>If specific qualifiers are added, the {@link jakarta.enterprise.inject.Default @Default} qualifier is removed</li>
     *   <li>Returns the existing collection unchanged if no new annotations are provided</li>
     * </ul>
     *
     * @param existing the existing qualifier annotations
     * @param newAnnotations new qualifier annotations to add/override
     * @return merged collection of qualifiers
     */
    private Collection<Annotation> mergeQualifiers(Collection<Annotation> existing, Annotation... newAnnotations) {
        if (newAnnotations == null || newAnnotations.length == 0) {
            return existing;
        }

        Map<Class<? extends Annotation>, Annotation> merged = new HashMap<>();
        // Start with existing
        for (Annotation a : existing) {
            merged.put(a.annotationType(), a);
        }
        // Overwrite/Add new ones
        for (Annotation a : newAnnotations) {
            merged.put(a.annotationType(), a);
        }

        // If we now have specific qualifiers, remove the @Default literal if it exists
        if (merged.size() > 1) {
            merged.remove(jakarta.enterprise.inject.Default.class);
        }

        return new ArrayList<>(merged.values());
    }
}
