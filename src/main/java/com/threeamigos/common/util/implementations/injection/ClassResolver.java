package com.threeamigos.common.util.implementations.injection;
import com.threeamigos.common.util.annotations.injection.Alternative;
import com.threeamigos.common.util.implementations.injection.exceptions.AlternativeNotFoundException;
import com.threeamigos.common.util.implementations.injection.exceptions.AmbiguousImplementationFoundException;
import com.threeamigos.common.util.implementations.injection.exceptions.ConcreteClassNotFoundException;
import com.threeamigos.common.util.implementations.injection.exceptions.ImplementationNotFoundException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassResolver {

    private final Map<String, List<Class<?>>> classesCache = new HashMap<>();
    private final Map<Class<?>, Collection<Class<?>>> resolvedClasses = new HashMap<>();

    public <T> Class<? extends T> resolveImplementation(Class<T> abstractClass, String packageName, String identifier) throws Exception {
        return resolveImplementation(Thread.currentThread().getContextClassLoader(), abstractClass, packageName, identifier);
    }

    public <T> Collection<Class<? extends T>> resolveImplementations(Class<T> abstractClass, String packageName) throws Exception {
        return resolveImplementations(Thread.currentThread().getContextClassLoader(), abstractClass, packageName);
    }

    /*
     * package-private to run the tests for a jar file
     */
    @SuppressWarnings("unchecked")
    <T> Class<? extends T> resolveImplementation(ClassLoader classLoader, Class<T> abstractClass, String packageName, String identifier) throws Exception {
        if (!abstractClass.isInterface() && !Modifier.isAbstract(abstractClass.getModifiers())) {
            return abstractClass;
        }

        List<Class<?>> allPackageClasses = getAllPackageClasses(classLoader, packageName);

        List<Class<? extends T>> candidates = new ArrayList<>();

        for (Class<?> clazz : allPackageClasses) {
            if (abstractClass.isAssignableFrom(clazz) && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {

                Alternative alternative = clazz.getAnnotation(Alternative.class);

                if (identifier != null) {
                    // If we have an identifier, only look for matching Alternatives
                    if (alternative != null && alternative.value().equals(identifier)) {
                        return (Class<? extends T>) clazz;
                    }
                } else {
                    // If no identifier, skip all Alternatives
                    if (alternative == null) {
                        candidates.add((Class<? extends T>) clazz);
                    }
                }
            }
        }

        if (identifier != null) {
            throw new AlternativeNotFoundException("No @Alternative found with value " + identifier + " for " + abstractClass.getName());
        }

        if (candidates.isEmpty()) {
            if (abstractClass.isInterface()) {
                throw new ImplementationNotFoundException("No implementation found for " + abstractClass.getName());
            } else {
                throw new ConcreteClassNotFoundException("No concrete class found for " + abstractClass.getName());
            }
        } else if (candidates.size() > 1) {
            if (abstractClass.isInterface()) {
                throw new AmbiguousImplementationFoundException("More than one implementation found for " + abstractClass.getName() + ": " + candidates.stream().map(Class::getName).reduce((a, b) -> a + ", " + b).get());
            } else {
                throw new AmbiguousImplementationFoundException("More than one concrete class found for " + abstractClass.getName() + ": " + candidates.stream().map(Class::getName).reduce((a, b) -> a + ", " + b).get());
            }
        }
        return candidates.get(0);
    }

    /*
     * package-private to run the tests for a jar file
     */
    @SuppressWarnings("unchecked")
    <T> Collection<Class<? extends T>> resolveImplementations(ClassLoader classLoader, Class<T> abstractClass, String packageName) throws Exception {
        if (!abstractClass.isInterface() && !Modifier.isAbstract(abstractClass.getModifiers())) {
            return Collections.singletonList(abstractClass);
        }

        List<Class<?>> allPackageClasses = getAllPackageClasses(classLoader, packageName);

        List<Class<? extends T>> candidates = new ArrayList<>();

        for (Class<?> clazz : allPackageClasses) {
            if (abstractClass.isAssignableFrom(clazz) && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                candidates.add((Class<? extends T>) clazz);
            }
        }
        return candidates;
    }

    private List<Class<?>> getAllPackageClasses(ClassLoader classLoader, String packageName) throws ClassNotFoundException, IOException {
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
        System.out.println("Found " + classes.size() + " classes in package \"" + packageName + "\"");
        return classes;
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

    List<Class<?>> findClassesInJar(ClassLoader classLoader, URL jarUrl, String packageName) throws IOException, ClassNotFoundException {
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