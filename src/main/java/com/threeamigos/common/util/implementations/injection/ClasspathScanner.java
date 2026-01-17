package com.threeamigos.common.util.implementations.injection;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scans the classpath for classes in specified packages.
 *
 * <p>Discovers all .class files from both filesystem directories and JAR files
 * within the specified package(s). If no packages are specified, scans the
 * entire classpath.
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * ClasspathScanner scanner = new ClasspathScanner("com.example", "com.other");
 * List<Class<?>> classes = scanner.getAllClasses(classLoader);
 * </pre>
 *
 * <p><b>Caching:</b> Results are cached after the first call to
 * {@link #getAllClasses(ClassLoader)}. The cache does NOT consider the
 * ClassLoader parameter - subsequent calls return the cached results regardless
 * of the ClassLoader passed.
 *
 * <p><b>ClassLoader Constraint:</b> This scanner is designed for single
 * ClassLoader use. If you need to scan with different ClassLoaders, create
 * separate ClasspathScanner instances.
 *
 * <p><b>Thread Safety:</b> This class is NOT thread-safe. External
 * synchronization is required for concurrent access, or use separate instances
 * per thread.
 *
 * <p><b>Package Names:</b>
 * <ul>
 *   <li>Standard format: "com.example.package"</li>
 *   <li>Empty string or null: Scans entire classpath</li>
 *   <li>Multiple packages: All are scanned and combined</li>
 * </ul>
 *
 * <p><b>Filtering:</b> Automatically excludes:
 * <ul>
 *   <li>META-INF entries</li>
 *   <li>module-info.class files</li>
 *   <li>Classes that cannot be loaded (missing dependencies)</li>
 * </ul>
 *
 * @author Stefano Reksten
 */
class ClasspathScanner {

    private static final String CLASS_EXTENSION = ".class";
    private static final int CLASS_EXTENSION_LENGTH = 6;
    private static final String MODULE_INFO_CLASS = "module-info.class";
    private static final String META_INF = "META-INF";
    private static final String ROOT_PACKAGE = "";
    private static final String FILE_PROTOCOL = "file";
    private static final String JAR_PROTOCOL = "jar";
    /**
     * All packages to scan for concrete implementations. If empty, will scan all packages.
     */
    private final Collection<String> packagesToScan = new ArrayList<>();
    /**
     * A cache to avoid browsing the classpath multiple times
     */
    private List<Class<?>> classesCache = null;

    private ClassLoader cachedClassLoader;

    ClasspathScanner(String ... packageNames) {
        for (String pkg : packageNames) {
            if (pkg != null && !pkg.isEmpty()) {
                validatePackageName(pkg);
            }
        }
        packagesToScan.addAll(Arrays.asList(packageNames));
    }

    private void validatePackageName(String packageName) {
        if (!packageName.matches("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*")) {
            throw new IllegalArgumentException("Invalid package name: " + packageName);
        }
    }

    List<Class<?>> getAllClasses(ClassLoader classLoader) throws ClassNotFoundException, IOException {
        if (classesCache == null) {
            cachedClassLoader = classLoader;
            classesCache = new ArrayList<>();
            getClasses(classLoader);
        } else if (cachedClassLoader != classLoader) {
            throw new IllegalStateException("Cannot scan classpath with different ClassLoaders");
        }
        return Collections.unmodifiableList(classesCache);
    }

    private void getClasses(ClassLoader classLoader) throws ClassNotFoundException, IOException {
        packagesToScan.removeIf(Objects::isNull);
        if (packagesToScan.isEmpty()) {
            packagesToScan.add(ROOT_PACKAGE);
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
        if (resource.getProtocol().equals(FILE_PROTOCOL)) {
            return findClassesInDirectory(classLoader, new File(resource.getFile()), packageName);
        } else if (resource.getProtocol().equals(JAR_PROTOCOL)) {
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

        // listFiles() CAN return null on I/O errors or permission issues
        if (files == null) {
            // Could log warning here if logging is available
            return Collections.emptyList();
        }

        for (File file : files) {
            String prefix = packageName.isEmpty() ? "" : packageName + ".";
            if (file.isDirectory()) {
                classes.addAll(findClassesInDirectory(classLoader, file, prefix + file.getName()));
            } else if (file.getName().endsWith(CLASS_EXTENSION)) {
                String className = prefix + file.getName().substring(0, file.getName().length() - CLASS_EXTENSION_LENGTH);
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
                if (!name.startsWith(META_INF) && name.startsWith(packagePath) &&
                        name.endsWith(CLASS_EXTENSION) && !name.endsWith(MODULE_INFO_CLASS)) {
                    String className = name.replace('/', '.').substring(0, name.length() - CLASS_EXTENSION_LENGTH);
                    try {
                        classes.add(Class.forName(className, false, classLoader));
                    } catch (NoClassDefFoundError | ClassNotFoundException e) {
                        // Skip classes with missing dependencies or those that can't be loaded
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to read JAR file: " + jarFilePath, e);
        }
        return classes;
    }
}
