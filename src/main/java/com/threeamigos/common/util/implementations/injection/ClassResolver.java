package com.threeamigos.common.util.implementations.injection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.enterprise.inject.Instance;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ClassResolver is responsible for resolving concrete implementations of abstract classes or interfaces.<br/>
 * Given an abstract class, a package name, and an optional identifier, it returns the concrete
 * implementation(s) of the abstract class. If no concrete implementation is found, it throws an
 * ImplementationNotFoundException. IF the class is a concrete class, it just returns the class itself.<br/>
 * The Unit Test for this class gives information about the expected behavior and edge cases.<br/>
 * This class, however, is to be used by the Injector class.
 *
 * @author Stefano Reksten
 */
class ClassResolver {

    /**
     * First cache - to avoid browsing the classpath multiple times for the same package
     */
    private final Map<String, List<Class<?>>> classesCache = new HashMap<>();
    /**
     * Second cache - to avoid scanning the classes multiple times for the same interface or class
     */
    private final Map<Class<?>, Collection<Class<?>>> resolvedClasses = new HashMap<>();

    private final Set<Class<?>> enabledAlternatives = new HashSet<>();

    void enableAlternative(Class<?> alternativeClass) {
        enabledAlternatives.add(alternativeClass);
    }

    /**
     * Resolves an abstract class or an interface returning the concrete class that implements the interface or
     * that extends the abstract class. When more implementations are present, the default implementation should not
     * be annotated, while alternative implementations should be marked with {@link Named @Named}.<br/>
     * If the identifier is specified, it will look for that particular alternative; otherwise the main implementation
     * will be returned.
     *
     * @param abstractClass the class to resolve
     * @param packageName package name used to reduce scanning
     * @param qualifier a @Named annotation value
     * @return the resolved class
     * @param <T> type of the class to resolve
     */
    <T> Class<? extends T> resolveImplementation(@NonNull Class<T> abstractClass,
                                                        @Nullable String packageName,
                                                        @Nullable Annotation qualifier) throws Exception {
        return resolveImplementation(Thread.currentThread().getContextClassLoader(), abstractClass, packageName, qualifier);
    }

    /**
     * Resolves an abstract class or an interface returning all concrete classes that implement the interface or
     * extend the abstract class. Used for the {@link Instance} interface.
     *
     * @param abstractClass the class to resolve
     * @param packageName package name used to reduce scanning
     * @return the resolved classes
     * @param <T> type of the class to resolve
     */
    <T> Collection<Class<? extends T>> resolveImplementations(@NonNull Class<T> abstractClass,
                                                                     @Nullable String packageName) throws Exception {
        return resolveImplementations(Thread.currentThread().getContextClassLoader(), abstractClass, packageName);
    }

    /*
     * package-private to run the tests
     */
    <T> Class<? extends T> resolveImplementation(ClassLoader classLoader, Class<T> abstractClass, String packageName, Annotation qualifier) throws Exception {
        /*
         * If we have a concrete class, return that class.
         */
        if (!abstractClass.isInterface() && !Modifier.isAbstract(abstractClass.getModifiers())) {
            return abstractClass;
        }

        Collection<Class<?extends T>> resolvedClasses = resolveImplementations(classLoader, abstractClass, packageName);

        // 1. Check for enabled @Alternatives first (Global Override)
        for (Class<? extends T> clazz : resolvedClasses) {
            if (enabledAlternatives.contains(clazz)) {
                return clazz;
            }
        }

        // 2. Filter out INACTIVE alternatives before looking for standard candidates
        List<Class<? extends T>> activeClasses = new ArrayList<>();
        for (Class<? extends T> clazz : resolvedClasses) {
            boolean isAlternative = clazz.isAnnotationPresent(Alternative.class);
            // Include if it's NOT an alternative, OR if it's an alternative that we've already checked (but wasn't enabled)
            // Actually, simply: if it's an alternative and NOT in enabledAlternatives, skip it.
            if (isAlternative && !enabledAlternatives.contains(clazz)) {
                continue;
            }
            activeClasses.add(clazz);
        }

        // 3. Check for @Qualifier / @Named annotations (Local Override)
        List<Class<? extends T>> candidates = new ArrayList<>();
        for (Class<? extends T> clazz : activeClasses) {
            Named named = clazz.getAnnotation(Named.class);
            if (qualifier != null) {
                // Check if the class has the exact same qualifier
                Annotation found = clazz.getAnnotation(qualifier.annotationType());
                if (found != null && found.equals(qualifier)) {
                    return clazz;
                }
            } else {
                // If no qualifier requested, look for classes WITHOUT any @Qualifier annotations
                boolean hasQualifier = Arrays.stream(clazz.getAnnotations())
                        .anyMatch(a -> a.annotationType().isAnnotationPresent(Qualifier.class));
                if (!hasQualifier) {
                    candidates.add(clazz);
                }
            }
        }

        if (qualifier != null) {
            throw new UnsatisfiedResolutionException("No implementation found with qualifier " + qualifier + " for " + abstractClass.getName());
        }

        // 3. Return the standard implementation (if any)
        if (candidates.isEmpty()) {
            throw new UnsatisfiedResolutionException("No implementation found for " + abstractClass.getName());
        } else if (candidates.size() > 1) {
            String candidatesAsList = candidates.stream().map(Class::getName).reduce((a, b) -> a + ", " + b).get();
            throw new AmbiguousResolutionException("More than one implementation found for " + abstractClass.getName() + ": " + candidatesAsList);
        }
        return candidates.get(0);
    }

    /*
     * package-private to run the tests
     */
    @SuppressWarnings("unchecked")
    <T> Collection<Class<? extends T>> resolveImplementations(ClassLoader classLoader, Class<T> abstractClass, String packageName) throws Exception {
        /*
         * If we have a concrete class, return that class.
         */
        if (!abstractClass.isInterface() && !Modifier.isAbstract(abstractClass.getModifiers())) {
            return Collections.singletonList(abstractClass);
        }

        List<Class<? extends T>> candidates = new ArrayList<>();

        /*
         * Look for a cache-hit
         */
        if (resolvedClasses.containsKey(abstractClass)) {
            resolvedClasses.get(abstractClass).forEach(c -> candidates.add((Class<? extends T>)c));
        } else {
            List<Class<?>> allPackageClasses = getAllPackageClasses(classLoader, packageName);

            for (Class<?> clazz : allPackageClasses) {
                if (abstractClass.isAssignableFrom(clazz) && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                    candidates.add((Class<? extends T>) clazz);
                }
            }
            List<Class<?>> mapValue = new ArrayList<>(candidates);
            resolvedClasses.put(abstractClass, mapValue);
        }
        return candidates;
    }

    private List<Class<?>> getAllPackageClasses(ClassLoader classLoader, String packageName) throws ClassNotFoundException, IOException {
        /*
         * Look for a cache-hit
         */
        List<Class<?>> allPackageClasses = classesCache.get(packageName);
        if (allPackageClasses == null) {
            allPackageClasses = getClasses(classLoader, packageName);
            classesCache.put(packageName, allPackageClasses);
        }
        return allPackageClasses;
    }

    private List<Class<?>> getClasses(ClassLoader classLoader, String packageName) throws ClassNotFoundException, IOException {
        if (packageName == null) {
            packageName = "";
        }
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<Class<?>> classes = new ArrayList<>();
        while (resources.hasMoreElements()) {
            classes.addAll(getClassesFromResource(classLoader, resources.nextElement(), packageName));
        }
        return classes;
    }

    /*
     * package-private to run the tests
     */
    List<Class<?>> getClassesFromResource(ClassLoader classLoader, URL resource, String packageName) throws ClassNotFoundException, IOException{
        if (resource.getProtocol().equals("file")) {
            return findClassesInDirectory(classLoader, new File(resource.getFile()), packageName);
        } else if (resource.getProtocol().equals("jar")) {
            return findClassesInJar(classLoader, resource, packageName);
        } else {
            return Collections.emptyList();
        }
    }

    /*
     * package-private to run the tests
     */
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

    /*
     * package-private to run the tests
     */
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
                if (!name.startsWith("META-INF") && name.startsWith(packagePath) && name.endsWith(".class") && !name.endsWith("module-info.class")) {
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