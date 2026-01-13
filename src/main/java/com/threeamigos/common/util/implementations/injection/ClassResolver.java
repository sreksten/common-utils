package com.threeamigos.common.util.implementations.injection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.enterprise.inject.*;
import javax.inject.Named;
import javax.inject.Qualifier;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

    /**
     * All packages to scan for concrete implementations. If empty, will scan all packages.
     */
    private final Collection<String> packagesToScan = new ArrayList<>();
    /**
     * First cache - to avoid browsing the classpath multiple times for the same package
     */
    private List<Class<?>> classesCache = null;
    /**
     * Second cache - to avoid scanning the classes multiple times for the same interface or class
     */
    private final Map<Type, Collection<Class<?>>> resolvedClasses = new HashMap<>();
    /**
     * Custom mappings to bind a type and qualifiers to a specific implementation.
     */
    private final Map<MappingKey, Class<?>> bindings = new HashMap<>();
    /**
     * A collection of Alternatives that can be used instead of the actual implementations.
     */
    private final Set<Class<?>> enabledAlternatives = new HashSet<>();

    ClassResolver(String ... packageNames) {
        packagesToScan.addAll(Arrays.asList(packageNames));
    }

    void bind(Type type, Collection<Annotation> qualifiers, Class<?> implementation) {
        bindings.put(new MappingKey(type, qualifiers), implementation);
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

        List<Class<? extends T>> candidates = new ArrayList<>();

        /*
         * Look for a cache-hit
         */
        if (resolvedClasses.containsKey(abstractClass)) {
            resolvedClasses.get(abstractClass).forEach(c -> candidates.add((Class<? extends T>)c));
        } else {
            List<Class<?>> allClasses = getAllClasses(classLoader);

            for (Class<?> candidate : allClasses) {
                if (isNotInterfaceOrAbstract(candidate) && isAssignable(abstractClass, candidate)) {
                    candidates.add((Class<? extends T>) candidate);
                }
            }
            resolvedClasses.put(abstractClass, new ArrayList<>(candidates));
        }
        return candidates;
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

    boolean isAssignable(Type targetType, Class<?> candidate) {
        if (targetType instanceof Class<?>) {
            return ((Class<?>) targetType).isAssignableFrom(candidate);
        }

        if (targetType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) targetType;
            Class<?> rawTarget = (Class<?>) pt.getRawType();

            if (!rawTarget.isAssignableFrom(candidate)) {
                return false;
            }

            // 1. Check all interfaces of the candidate
            for (Type itf : candidate.getGenericInterfaces()) {
                if (typesMatch(targetType, itf)) {
                    return true;
                }
            }

            // 2. Check superclass hierarchy
            Type superType = candidate.getGenericSuperclass();
            if (superType != null && superType != Object.class) {
                if (typesMatch(targetType, superType)) {
                    return true;
                }
                return isAssignable(targetType, candidate.getSuperclass());
            }

            // Fallback: if we match the raw type, and it's a simple match, consider it assignable
            return typesMatch(targetType, candidate);
        }

        if (targetType instanceof GenericArrayType ||
                targetType instanceof TypeVariable ||
                targetType instanceof WildcardType) {
            Class<?> rawTargetType = RawTypeExtractor.getRawType(targetType);
            return rawTargetType.isAssignableFrom(candidate);
        }

        return false;
    }

    boolean typesMatch(Type target, Type candidate) {
        if (target.equals(candidate)) {
            return true;
        }

        // If the target is a TypeVariable (like T), it matches its bound (usually Object)
        if (target instanceof TypeVariable) {
            return true;
        }

        if (target instanceof ParameterizedType && candidate instanceof ParameterizedType) {
            ParameterizedType pt1 = (ParameterizedType) target;
            ParameterizedType pt2 = (ParameterizedType) candidate;

            if (!pt1.getRawType().equals(pt2.getRawType())) {
                return false;
            }

            Type[] args1 = pt1.getActualTypeArguments();
            Type[] args2 = pt2.getActualTypeArguments();

            if (args1.length != args2.length) {
                return false;
            }

            for (int i = 0; i < args1.length; i++) {
                if (!typeArgsMatch(args1[i], args2[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    boolean typeArgsMatch(Type t1, Type t2) {
        if (t1.equals(t2) ||
                t1 instanceof WildcardType ||
                t2 instanceof WildcardType ||
                t1 instanceof TypeVariable ||
                t2 instanceof TypeVariable) {
            return true;
        }
        return RawTypeExtractor.getRawType(t1).isAssignableFrom(RawTypeExtractor.getRawType(t2));
    }

    // From now on, package scans to search for classes.

    private List<Class<?>> getAllClasses(ClassLoader classLoader) throws ClassNotFoundException, IOException {
        if (classesCache == null) {
            classesCache = new ArrayList<>();
            getClasses(classLoader);
        }
        return classesCache;
    }

    private void getClasses(ClassLoader classLoader) throws ClassNotFoundException, IOException {
        packagesToScan.removeIf(Objects::isNull);
        if (packagesToScan.isEmpty()) {
            packagesToScan.add("");
        }
        for (String packageName : packagesToScan) {
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                classesCache.addAll(getClassesFromResource(classLoader, resources.nextElement(), packageName));
            }
        }
    }

    List<Class<?>> getClassesFromResource(ClassLoader classLoader, URL resource, String packageName) throws ClassNotFoundException, IOException{
        if (resource.getProtocol().equals("file")) {
            return findClassesInDirectory(classLoader, new File(resource.getFile()), packageName);
        } else if (resource.getProtocol().equals("jar")) {
            return findClassesInJar(classLoader, resource, packageName);
        } else {
            return Collections.emptyList();
        }
    }

    List<Class<?>> findClassesInDirectory(ClassLoader classLoader, File directory, String packageName) throws ClassNotFoundException {
        if (!directory.exists()) {
            return Collections.emptyList();
        }
        if (!directory.isDirectory()) {
            return Collections.emptyList();
        }

        List<Class<?>> classes = new ArrayList<>();

        File[] files = directory.listFiles();
        // Being a directory, it can't return null, but to avoid false positives from the static checker...
        for (File file : Objects.requireNonNull(files)) {
            String prefix = packageName.isEmpty() ? "" : packageName + ".";
            if (file.isDirectory()) {
                    classes.addAll(findClassesInDirectory(classLoader, file, prefix + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    String className = prefix + file.getName().substring(0, file.getName().length() - 6);
                    Class<?> clazz = Class.forName(className, false, classLoader);
                    classes.add(clazz);
                }
        }
        return classes;
    }

    List<Class<?>> findClassesInJar(ClassLoader classLoader, URL jarUrl, String packageName) throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        // Extract the file path properly handling 'jar:file': and '!'
        String urlString = jarUrl.toString();
        String jarFilePath = urlString.substring(urlString.indexOf("file:"), urlString.indexOf("!"));

        File jarFile;
        try {
            jarFile = new File(new URL(jarFilePath).toURI());
        } catch (Exception e) {
            // Fallback for non-standard URI formats
            jarFile = new File(jarFilePath.replace("file:", ""));
        }
        String packagePath = packageName.replace('.', '/');

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("META-INF") && name.startsWith(packagePath) &&
                        name.endsWith(".class") && !name.endsWith("module-info.class")) {
                    String className = name.replace('/', '.').substring(0, name.length() - 6);
                    try {
                        classes.add(Class.forName(className, false, classLoader));
                    } catch (NoClassDefFoundError | ClassNotFoundException e) {
                        // Skip classes with missing dependencies or those that can't be loaded
                    }
                }
            }
        }
        return classes;
    }
}
