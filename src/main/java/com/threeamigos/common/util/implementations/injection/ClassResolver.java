package com.threeamigos.common.util.implementations.injection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.enterprise.inject.*;
import javax.inject.Named;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ClassResolver is responsible for resolving concrete implementations of abstract classes or interfaces.<br/>
 * Given an abstract class, a package name, and an optional identifier, it returns the concrete
 * implementation(s) of the abstract class.<br/>
 * The Unit Test for this class gives information about the expected behavior and edge cases.<br/>
 * This class, however, is to be used by the implementation of the Injector class.
 *
 * @author Stefano Reksten
 */
class ClassResolver {

    private final ClasspathScanner classpathScanner;
    private final TypeChecker typeChecker;
    /**
     * Second cache - to avoid scanning the classes multiple times for the same interface or class
     */
    private final Cache<Type, Collection<Class<?>>> resolvedClasses = new Cache<>();
    /**
     * Custom mappings to bind a type and qualifiers to a specific implementation.
     */
    private final Map<MappingKey, Class<?>> bindings = new HashMap<>();
    /**
     * Whether the resolve process should stop at bound classes or scan the classpath
     */
    private boolean bindingsOnly;
    /**
     * A collection of Alternatives that can be used instead of the actual implementations.
     */
    private final Set<Class<?>> enabledAlternatives = new HashSet<>();

    ClassResolver(String ... packageNames) {
        classpathScanner = new ClasspathScanner(packageNames);
        typeChecker = new TypeChecker();
    }

    ClassResolver(ClasspathScanner classpathScanner, TypeChecker typeChecker) {
        this.classpathScanner = classpathScanner;
        this.typeChecker = typeChecker;
    }

    void bind(Type type, Collection<Annotation> qualifiers, Class<?> implementation) {
        if (!typeChecker.isAssignable(type, implementation)) {
            throw new IllegalArgumentException("Cannot bind " + implementation.getName() +
                    " to " + type.getClass().getName() + " because they are not assignable");
        }
        bindings.put(new MappingKey(type, qualifiers), implementation);
    }

    void setBindingsOnly(boolean bindingsOnly) {
        this.bindingsOnly = bindingsOnly;
    }

    void enableAlternative(Class<?> alternativeClass) {
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
    <T> Class<? extends T> resolveImplementation(@NonNull Type typeToResolve,
                                                 @Nullable Collection<Annotation> qualifiers) throws Exception {
        return resolveImplementation(Thread.currentThread().getContextClassLoader(), typeToResolve, qualifiers);
    }

    @SuppressWarnings("unchecked")
    <T> Class<? extends T> resolveImplementation(ClassLoader classLoader, Type typeToResolve,
                                                 @Nullable Collection<Annotation> qualifiers) throws Exception {

        // Check custom mappings first
        MappingKey key = new MappingKey(typeToResolve, qualifiers);
        if (bindings.containsKey(key)) {
            return (Class<? extends T>) bindings.get(key);
        } else if (bindingsOnly) {
            if (qualifiers != null && !qualifiers.isEmpty()) {
                throw new UnsatisfiedResolutionException("No implementation found with qualifiers " +
                        qualifiers.stream().map(Annotation::toString).collect(Collectors.joining(", ")) +
                        " for " + typeToResolve.getClass().getName());
            } else {
                throw new UnsatisfiedResolutionException("No implementation found for " + typeToResolve.getClass().getName());
            }
        }

        Class<?> rawType = RawTypeExtractor.getRawType(typeToResolve);

        // If we have a concrete class and the qualifier is Default, return that class.
        boolean isDefault = qualifiers == null ||
                qualifiers.isEmpty() ||
                qualifiers.stream().anyMatch(q -> q instanceof DefaultLiteral);

        if (isDefault && (isNotInterfaceOrAbstract(rawType) || rawType.isArray())) {
            return (Class<? extends T>)rawType;
        }

        // Search for all possible implementations
        Collection<Class<? extends T>> resolvedClasses = resolveImplementations(classLoader, typeToResolve);

        // If one of the implementations is an enabled Alternative, return that class
        for (Class<? extends T> clazz : resolvedClasses) {
            if (enabledAlternatives.contains(clazz)) {
                return clazz;
            }
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
            throw new UnsatisfiedResolutionException("No implementation found with qualifiers " +
                    qualifiers.stream().map(Annotation::toString).collect(Collectors.joining(", ")) +
                    " for " + typeToResolve.getClass().getName());
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
            throw new UnsatisfiedResolutionException("No implementation found for " + typeToResolve.getClass().getName());
        } else if (candidates.size() > 1) {
            String candidatesAsList = candidates.stream().map(Class::getName).reduce((a, b) -> a + ", " + b).get();
            throw new AmbiguousResolutionException("More than one implementation found for " + typeToResolve.getClass().getName() +
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
    <T> Collection<Class<? extends T>> resolveImplementations(@NonNull Type typeToResolve) throws Exception {
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
    <T> Collection<Class<? extends T>> resolveImplementations(@NonNull Type typeToResolve,
                                                              @Nullable Collection<Annotation> qualifiers) throws Exception {
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
    <T> Collection<Class<? extends T>> resolveImplementations(ClassLoader classLoader, Type abstractClass) throws Exception {
        Collection<Class<?>> cached = resolvedClasses.computeIfAbsent(abstractClass, () -> {
            List<Class<?>> candidates = new ArrayList<>();
            try {
                List<Class<?>> allClasses = classpathScanner.getAllClasses(classLoader);
                for (Class<?> candidate : allClasses) {
                    if (isNotInterfaceOrAbstract(candidate) && typeChecker.isAssignable(abstractClass, candidate)) {
                        candidates.add(candidate);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve implementations for " + abstractClass, e);
            }
            return new ArrayList<>(candidates);
        });

        // Convert to properly typed collection
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
}
