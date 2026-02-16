package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.collections.Cache;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.enterprise.inject.*;
import javax.inject.Named;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves concrete implementations for abstract types in a dependency injection framework.
 *
 * <p>This class implements JSR 330/346 dependency injection semantics, with the following
 * resolution priority:
 * <p><b>Resolution Priority (closer to CDI specification):</b>
 * <ol>
 *   <li>Enabled alternatives ({@code @Alternative} annotation) - highest priority
 *   <li>Custom bindings via bind() method
 *   <li>Qualifier-based resolution ({@code @Named}, {@code @Any}, {@code @Default}, custom qualifiers)
 *   <li>Standard implementation (no qualifiers or @Default)
 * </ol>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. Concurrent resolution
 * of the same type will result in only one classpath scan, with other threads
 * receiving the cached result from the {@link Cache}.
 *
 * <p><b>Caching:</b> Resolution results are cached using a thread-safe LRU cache
 * with bounded size. This prevents redundant classpath scanning while ensuring
 * memory bounds.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * ClassResolver resolver = new ClassResolver("com.example");
 *
 * // Simple resolution
 * Class<? extends MyService> impl = resolver.resolveImplementation(MyService.class, null);
 *
 * // With qualifier
 * Collection<Annotation> qualifiers = Collections.singleton(new NamedLiteral("production"));
 * Class<? extends MyService> impl = resolver.resolveImplementation(MyService.class, qualifiers);
 *
 * // Custom binding
 * resolver.bind(MyService.class, null, ProductionService.class);
 *
 * // Enable alternative
 * resolver.enableAlternative(TestService.class);
 *
 * // Get all implementations
 * Collection<Class<? extends MyService>> allImpls = resolver.resolveImplementations(MyService.class);
 * }</pre>
 *
 * <p>Checked and commented with Claude.
 *
 * @author Stefano Reksten
 * @see javax.inject.Named
 * @see javax.enterprise.inject.Alternative
 * @see javax.inject.Qualifier
 * @see Cache
 */
class ClassResolver {

    private final KnowledgeBase knowledgeBase;
    private final TypeChecker typeChecker;
    /**
     * Second cache - to avoid scanning the classes multiple times for the same interface or class
     */
    private final Cache<Type, Collection<Class<?>>> resolvedClasses = new Cache<>();
    /**
     * Custom mappings to bind a type and qualifiers to a specific implementation.
     */
    private final Map<MappingKey, Class<?>> bindings = new ConcurrentHashMap<>();
    /**
     * See comments in setter.
     */
    private volatile boolean bindingsOnly;
    /**
     * A collection of Alternatives that can be used instead of the actual implementations.
     */
    private final Set<Class<?>> enabledAlternatives = ConcurrentHashMap.newKeySet();

    /**
     * Package-private because intended to be used by InjectorImpl only
     * @param knowledgeBase a KnowledgeBase instance
     */
    ClassResolver(@Nonnull KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
        this.typeChecker = new TypeChecker();
    }

    /**
     * Package-private because intended to be used by InjectorImpl only or unit testing
     * @param knowledgeBase a KnowledgeBase instance
     * @param typeChecker a custom TypeChecker implementation
     */
    ClassResolver(@Nonnull KnowledgeBase knowledgeBase, @Nonnull TypeChecker typeChecker) {
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("ClasspathScanner cannot be null");
        }
        if (typeChecker == null) {
            throw new IllegalArgumentException("TypeChecker cannot be null");
        }
        this.knowledgeBase = knowledgeBase;
        this.typeChecker = typeChecker;
    }

    void bind(@Nonnull Type type, @Nonnull Collection<Annotation> qualifiers, @Nonnull Class<?> implementation) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (implementation == null) {
            throw new IllegalArgumentException("implementation cannot be null");
        }
        if (qualifiers == null) {
            throw new IllegalArgumentException("qualifiers cannot be null");
        }
        if (!typeChecker.isAssignable(type, implementation)) {
            throw new IllegalArgumentException("Cannot bind " + implementation.getName() +
                    " to " + type.getTypeName() + " because they are not assignable");
        }
        bindings.put(new MappingKey(type, qualifiers), implementation);
    }

    /**
     * Whether the resolve process should stop at alternatives and bound classes or scan the classpath.
     * Default is false. You can enable this flag to disable classpath scanning, but you MUST manually
     * do all the bindings!
     *
     * @param bindingsOnly whether the resolve process should stop at alternatives and bound classes or scan the classpath
     */
    void setBindingsOnly(boolean bindingsOnly) {
        this.bindingsOnly = bindingsOnly;
    }

    void enableAlternative(@Nonnull Class<?> alternativeClass) {
        if (alternativeClass == null) {
            throw new IllegalArgumentException("alternativeClass cannot be null");
        }
        if (!alternativeClass.isAnnotationPresent(Alternative.class)) {
            throw new IllegalArgumentException(alternativeClass.getName() + " is not annotated with @Alternative");
        }
        enabledAlternatives.add(alternativeClass);
    }

    /**
     * Resolves an abstract class or an interface returning the concrete class that implements the interface or
     * that extends the abstract class. When more implementations are present, the default implementation should not
     * be annotated, while alternative implementations should be marked with {@link Named @Named}.<br/>
     * If the identifier is specified, it will look for that particular Named class; otherwise the standard
     * implementation will be returned.
     *
     * @param typeToResolve the class to resolve
     * @param qualifiers a @Named annotation value
     * @return the resolved class
     * @param <T> type of the class to resolve
     */
    <T> Class<? extends T> resolveImplementation(@Nonnull Type typeToResolve,
                                                 @Nullable Collection<Annotation> qualifiers) {
        return resolveImplementation(Thread.currentThread().getContextClassLoader(), typeToResolve, qualifiers);
    }

    @SuppressWarnings("unchecked")
    <T> Class<? extends T> resolveImplementation(@Nonnull ClassLoader classLoader, @Nonnull Type typeToResolve,
                                                 @Nullable Collection<Annotation> qualifiers) {
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be null");
        }
        if (typeToResolve == null) {
            throw new IllegalArgumentException("typeToResolve cannot be null");
        }

        // Search for all possible implementations
        Collection<Class<? extends T>> resolvedClasses = resolveImplementations(classLoader, typeToResolve);

        // If one of the implementations is an enabled Alternative, return that class
        List<Class<? extends T>> matchingEnabledAlternatives = new ArrayList<>();
        for (Class<? extends T> clazz : resolvedClasses) {
            if (enabledAlternatives.contains(clazz)) {
                matchingEnabledAlternatives.add(clazz);
            }
        }
        if (matchingEnabledAlternatives.size() == 1) {
            return matchingEnabledAlternatives.get(0);
        } else if (matchingEnabledAlternatives.size() > 1) {
            throw new AmbiguousResolutionException("More than one alternative found for " + typeToResolve.getClass().getName() +
                    ": " + matchingEnabledAlternatives.stream().map(Class::getName).reduce((a, b) -> a + ", " + b).get());
        }

        // Check if we have a custom mapping
        MappingKey key = new MappingKey(typeToResolve, qualifiers);
        if (bindings.containsKey(key)) {
            return (Class<? extends T>) bindings.get(key);
        } else if (bindingsOnly) {
            throw new UnsatisfiedResolutionException(formatUnsatisfiedError(typeToResolve, qualifiers));
        }

        Class<?> rawType = RawTypeExtractor.getRawType(typeToResolve);

        // If we have a concrete class and the qualifier is Default, return that class.
        boolean isDefault = qualifiers == null ||
                qualifiers.isEmpty() ||
                qualifiers.stream().anyMatch(q -> q instanceof DefaultLiteral);

        if (isDefault && (isNotInterfaceOrAbstract(rawType) || rawType.isArray())) {
            return (Class<? extends T>)rawType;
        }

        // Filter out alternatives
        List<Class<? extends T>> activeClasses = resolvedClasses
                .stream()
                .filter(clazz -> !clazz.isAnnotationPresent(Alternative.class))
                .collect(Collectors.toList());

        // Check for @Qualifier / @Named annotations
        if (qualifiers != null && !qualifiers.isEmpty()) {
            for (Class<? extends T> clazz : activeClasses) {
                if (matchesQualifiers(clazz, qualifiers)) {
                    return clazz;
                }
            }
            throw new UnsatisfiedResolutionException(formatUnsatisfiedError(typeToResolve, qualifiers));
        }

        // Filter out @Qualifier / @Named classes
        Function<Class<? extends T>, Boolean> noQualifierFilteringFunction =
                clazz -> Arrays.stream(clazz.getAnnotations())
                        .noneMatch(a -> a.annotationType().isAnnotationPresent(Qualifier.class));

        List<Class<? extends T>> candidates = activeClasses
                .stream()
                .filter(noQualifierFilteringFunction::apply)
                .collect(Collectors.toList());

        // Return the standard implementation (if any)
        if (candidates.isEmpty()) {
            throw new UnsatisfiedResolutionException(formatUnsatisfiedError(typeToResolve, null));
        } else if (candidates.size() > 1) {
            String candidatesAsList = candidates.stream().map(Class::getName).reduce((a, b) -> a + ", " + b).get();
            throw new AmbiguousResolutionException("More than one implementation found for " + typeToResolve.getTypeName() +
                    ": " + candidatesAsList);
        }
        return candidates.get(0);
    }

    /**
     * Resolves an abstract class or an interface returning all concrete classes that implement the interface or
     * extend the abstract class. Used for the {@link Instance} interface.
     *
     * @param typeToResolve the class to resolve
     * @return the resolved classes
     * @param <T> type of the class to resolve
     */
    <T> Collection<Class<? extends T>> resolveImplementations(@Nonnull Type typeToResolve) {
        return resolveImplementations(Thread.currentThread().getContextClassLoader(), typeToResolve);
    }

    /**
     * Resolves an abstract class or an interface returning all concrete classes that implement the interface or
     * extend the abstract class and that match the provided qualifiers.
     *
     * @param typeToResolve the class to resolve
     * @param qualifiers the qualifiers to match
     * @return the resolved classes
     * @param <T> type of the class to resolve
     */
    <T> Collection<Class<? extends T>> resolveImplementations(@Nonnull Type typeToResolve,
                                                              @Nullable Collection<Annotation> qualifiers) {
        Collection<Class<? extends T>> resolvedClasses =
                resolveImplementations(Thread.currentThread().getContextClassLoader(), typeToResolve);

        List<Class<? extends T>> activeClasses = resolvedClasses.stream()
                .filter(clazz -> enabledAlternatives.contains(clazz) || !clazz.isAnnotationPresent(Alternative.class))
                .collect(Collectors.toList());

        if (qualifiers == null || qualifiers.isEmpty()) {
            return activeClasses;
        }

        return activeClasses.stream()
                .filter(clazz -> matchesQualifiers(clazz, qualifiers))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    <T> Collection<Class<? extends T>> resolveImplementations(@Nonnull ClassLoader classLoader, @Nonnull Type typeToResolve) {
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be null");
        }
        if (typeToResolve == null) {
            throw new IllegalArgumentException("typeToResolve cannot be null");
        }

        Collection<Class<?>> cached = resolvedClasses.computeIfAbsent(typeToResolve, () -> {
            List<Class<?>> candidates = new ArrayList<>();
            try {
                Collection<Class<?>> allClasses = knowledgeBase.getClasses();
                for (Class<?> candidate : allClasses) {
                    if (isNotInterfaceOrAbstract(candidate) && typeChecker.isAssignable(typeToResolve, candidate)) {
                        candidates.add(candidate);
                    }
                }
            } catch (Exception e) {
                throw new ResolutionException("Failed to resolve implementations for " + typeToResolve, e);
            }
            return new ArrayList<>(candidates);
        });

        // Convert to a properly typed collection
        List<Class<? extends T>> result = new ArrayList<>();
        for (Class<?> clazz : cached) {
            result.add((Class<? extends T>) clazz);
        }
        return result;
    }

    private boolean isNotInterfaceOrAbstract(Class<?> clazz) {
        return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
    }

    private boolean matchesQualifiers(Class<?> clazz, Collection<Annotation> qualifiers) {
        return qualifiers.stream().allMatch(qualifier -> {
            Annotation actual = clazz.getAnnotation(qualifier.annotationType());
            if (actual != null) {
                return qualifier.equals(actual);
            }
            if (qualifier instanceof Default) {
                return Arrays.stream(clazz.getAnnotations())
                        .noneMatch(a -> a.annotationType().isAnnotationPresent(Qualifier.class));
            }
            // @Any matches everything
            return qualifier instanceof Any;
        });
    }

    /**
     * Formats a collection of qualifiers into a human-readable string for error messages.
     *
     * @param qualifiers the collection of qualifiers to format
     * @return a comma-separated string representation of the qualifiers
     */
    private String formatQualifiers(Collection<Annotation> qualifiers) {
        return qualifiers.stream()
                .map(Annotation::toString)
                .collect(Collectors.joining(", "));
    }

    /**
     * Creates a formatted error message for unsatisfied resolution exceptions.
     *
     * @param type the type that could not be resolved
     * @param qualifiers the qualifiers that were specified (can be null or empty)
     * @return a formatted error message
     */
    private String formatUnsatisfiedError(Type type, Collection<Annotation> qualifiers) {
        if (qualifiers != null && !qualifiers.isEmpty()) {
            return "No implementation found with qualifiers " +
                    formatQualifiers(qualifiers) + " for " + type.getTypeName();
        }
        return "No implementation found for " + type.getTypeName();
    }

    void clearCaches() {
        resolvedClasses.clear();
    }
}
