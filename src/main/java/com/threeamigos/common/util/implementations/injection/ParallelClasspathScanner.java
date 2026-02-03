package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scans the classpath for classes in specified packages.
 *
 * <p>Discovers all .class files from both filesystem directories and JAR files within the specified package(s).
 * If no packages are specified, scans the entire classpath.</p>
 * Scan results are passed to a {@link ClasspathScannerSink} for further processing (e.g., JSR-330 compliance checks).</p>
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
class ParallelClasspathScanner {

    private static final String CLASS_EXTENSION = ".class";
    private static final int CLASS_EXTENSION_LENGTH = 6;
    private static final String MODULE_INFO_CLASS = "module-info.class";
    private static final String META_INF = "META-INF";
    private static final String ROOT_PACKAGE = "";
    private static final String FILE_PROTOCOL = "file";
    private static final String JAR_PROTOCOL = "jar";

    ParallelClasspathScanner(ClassLoader classLoader,
                     ClasspathScannerSink sink,
                     String... packageNames) throws IOException, ClassNotFoundException {
        Objects.requireNonNull(sink, "sink cannot be null");
        Objects.requireNonNull(packageNames, "packageNames cannot be null");

        Collection<String> packageList = sanitizePackages(packageNames);

        for (String packageName : packageList) {
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                getClassesFromResource(classLoader, resource, packageName, sink);
            }
        }
    }

    private Collection<String> sanitizePackages(String... packageNames) {
        List<String> packages = new ArrayList<>();
        for (String pkg : packageNames) {
            if (pkg != null && !pkg.isEmpty()) {
                validatePackageName(pkg);
                packages.add(pkg);
            }
        }
        if (packages.isEmpty()) {
            packages.add(ROOT_PACKAGE);
        }
        return packages;
    }

    private void validatePackageName(String packageName) {
        if (!packageName.matches("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*")) {
            throw new IllegalArgumentException("Invalid package name: " + packageName);
        }
    }

    void getClassesFromResource(ClassLoader classLoader, URL resource, String packageName,
                                ClasspathScannerSink sink) throws ClassNotFoundException, IOException {
        if (resource.getProtocol().equals(FILE_PROTOCOL)) {
            findClassesInDirectory(classLoader, new File(resource.getFile()), packageName, sink);
        } else if (resource.getProtocol().equals(JAR_PROTOCOL)) {
            findClassesInJar(classLoader, resource, packageName, sink);
        } else {
            throw new IllegalArgumentException("Unsupported protocol: " + resource.getProtocol());
        }
    }

    void findClassesInDirectory(ClassLoader classLoader, File directory, String packageName,
                                ClasspathScannerSink sink) throws ClassNotFoundException {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();

        // listFiles() CAN return null on I/O errors or permission issues
        if (files == null) {
            // Could log a warning here if logging is available
            return;
        }

        for (File file : files) {
            String prefix = packageName.isEmpty() ? "" : packageName + ".";
            if (file.isDirectory()) {
                findClassesInDirectory(classLoader, file, prefix + file.getName(), sink);
            } else if (file.getName().endsWith(CLASS_EXTENSION)) {
                String className = prefix + file.getName().substring(0, file.getName().length() - CLASS_EXTENSION_LENGTH);
                Class<?> clazz = Class.forName(className, false, classLoader);
                sink.add(clazz);
            }
        }
    }

    void findClassesInJar(ClassLoader classLoader, URL jarUrl, String packageName, ClasspathScannerSink sink) throws IOException {
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
                        sink.add(Class.forName(className, false, classLoader));
                    } catch (NoClassDefFoundError | ClassNotFoundException e) {
                        // Skip classes with missing dependencies or those that can't be loaded
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to read JAR file: " + jarFilePath, e);
        }
    }
}
