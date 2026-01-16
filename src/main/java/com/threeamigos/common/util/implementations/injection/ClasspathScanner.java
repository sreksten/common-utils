package com.threeamigos.common.util.implementations.injection;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Retrieves all .class files from a set of packages, both on a filesystem or from a JAR file.
 *
 * @author Stefano Reksten
 */
class ClasspathScanner {

    /**
     * All packages to scan for concrete implementations. If empty, will scan all packages.
     */
    private final Collection<String> packagesToScan = new ArrayList<>();
    /**
     * A cache to avoid browsing the classpath multiple times
     */
    private List<Class<?>> classesCache = null;

    ClasspathScanner(String ... packageNames) {
        packagesToScan.addAll(Arrays.asList(packageNames));
    }

    List<Class<?>> getAllClasses(ClassLoader classLoader) throws ClassNotFoundException, IOException {
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
        if (!directory.exists() || !directory.isDirectory()) {
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
