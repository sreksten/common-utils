package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.inject.Vetoed;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scans the classpath for classes in specified packages.
 *
 * <p>Discovers all .class files from both filesystem directories and JAR files within the specified package(s).
 * If no packages are specified, scans the entire classpath.</p>
 * Scan results are passed to a {@link ClassConsumer} for further processing (e.g., JSR-330 compliance checks).</p>
 *
 * <p><b>Filtering:</b> Automatically excludes:
 * <ul>
 *   <li>META-INF entries</li>
 *   <li>module-info.class files</li>
 *   <li>Classes that cannot be loaded (missing dependencies)</li>
 *   <li>Classes in packages annotated with @Vetoed (CDI 4.1)</li>
 * </ul>
 *
 * <p><b>@Vetoed Package Support (CDI 4.1):</b>
 * Packages can be vetoed by annotating their {@code package-info.java} with {@code @Vetoed}.
 * All classes in vetoed packages and their subpackages are automatically excluded from bean discovery.
 *
 * @author Stefano Reksten
 */
class ParallelClasspathScanner {

    private static final String CLASS_EXTENSION = ".class";
    private static final int CLASS_EXTENSION_LENGTH = 6;
    private static final String MODULE_INFO_CLASS = "module-info.class";
    private static final String PACKAGE_INFO_CLASS = "package-info.class";
    private static final String META_INF = "META-INF";
    private static final String ROOT_PACKAGE = "";
    private static final String FILE_PROTOCOL = "file";
    private static final String JAR_PROTOCOL = "jar";

    /**
     * Cache of vetoed packages. Thread-safe for concurrent scanning.
     * Key: package name, Value: true if vetoed
     */
    private final Map<String, Boolean> vetoedPackages = new ConcurrentHashMap<>();

    /**
     * Set to track already-scanned classes to avoid duplicates.
     * This prevents the same class from being added multiple times when it appears
     * in multiple JARs (e.g., javax.inject-tck and jakarta.inject-tck both contain
     * the same org.atinject.tck classes).
     */
    private final Set<String> scannedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Detector for bean archive modes (EXPLICIT/IMPLICIT/NONE based on beans.xml).
     */
    private final BeanArchiveDetector beanArchiveDetector = new BeanArchiveDetector();

    ParallelClasspathScanner(ClassLoader classLoader,
                     ClassConsumer sink,
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
                                ClassConsumer sink) throws ClassNotFoundException, IOException {
        if (resource.getProtocol().equals(FILE_PROTOCOL)) {
            findClassesInDirectory(classLoader, new File(resource.getFile()), packageName, sink);
        } else if (resource.getProtocol().equals(JAR_PROTOCOL)) {
            findClassesInJar(classLoader, resource, packageName, sink);
        } else {
            throw new IllegalArgumentException("Unsupported protocol: " + resource.getProtocol());
        }
    }

    void findClassesInDirectory(ClassLoader classLoader, File directory, String packageName,
                                ClassConsumer sink) throws ClassNotFoundException {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        // Check if the package is vetoed (skip the entire package and subpackages)
        if (isPackageVetoed(classLoader, packageName)) {
            return;
        }

        // Detect bean archive mode for this directory (check for META-INF/beans.xml)
        BeanArchiveMode archiveMode = beanArchiveDetector.detectArchiveMode(findArchiveRoot(directory));

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
            } else if (file.getName().endsWith(CLASS_EXTENSION) && !file.getName().equals(PACKAGE_INFO_CLASS)) {
                String className = prefix + file.getName().substring(0, file.getName().length() - CLASS_EXTENSION_LENGTH);
                // Only process if we haven't seen this class before (avoid duplicates from multiple JARs)
                if (scannedClasses.add(className)) {
                    try {
                        Class<?> clazz = Class.forName(className, false, classLoader);
                        sink.add(clazz, archiveMode);
                    } catch (NoClassDefFoundError | ClassNotFoundException e) {
                        // Skip classes with missing dependencies or those that can't be loaded; continue scanning
                    }
                }
            }
        }
    }

    /**
     * Finds the archive root (where META-INF would be located) by navigating up from a directory.
     *
     * @param directory a directory within the classpath
     * @return the archive root directory
     */
    private File findArchiveRoot(File directory) {
        File current = directory;
        // Navigate up to find where META-INF exists or use the original directory
        while (current != null && current.getParentFile() != null) {
            File metaInf = new File(current, "META-INF");
            if (metaInf.exists() && metaInf.isDirectory()) {
                return current;
            }
            File parentMetaInf = new File(current.getParentFile(), "META-INF");
            if (parentMetaInf.exists() && parentMetaInf.isDirectory()) {
                return current.getParentFile();
            }
            current = current.getParentFile();
        }
        // If we can't find META-INF, return the original directory
        return directory;
    }

    void findClassesInJar(ClassLoader classLoader, URL jarUrl, String packageName, ClassConsumer sink) throws IOException {
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

        // Detect bean archive mode for this JAR (check for META-INF/beans.xml inside JAR)
        BeanArchiveMode archiveMode = beanArchiveDetector.detectArchiveMode(jarFile);

        String packagePath = packageName.replace('.', '/');

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(META_INF) && name.startsWith(packagePath) &&
                        name.endsWith(CLASS_EXTENSION) && !name.endsWith(MODULE_INFO_CLASS) &&
                        !name.endsWith(PACKAGE_INFO_CLASS)) {
                    String className = name.replace('/', '.').substring(0, name.length() - CLASS_EXTENSION_LENGTH);

                    // Extract the package from the class name and check if vetoed
                    String classPackage = getPackageFromClassName(className);
                    if (isPackageVetoed(classLoader, classPackage)) {
                        continue; // Skip classes in vetoed packages
                    }

                    // Only process if we haven't seen this class before (avoid duplicates from multiple JARs)
                    if (scannedClasses.add(className)) {
                        try {
                            sink.add(Class.forName(className, false, classLoader), archiveMode);
                        } catch (NoClassDefFoundError | ClassNotFoundException e) {
                            // Skip classes with missing dependencies or those that can't be loaded
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to read JAR file: " + jarFilePath, e);
        }
    }

    /**
     * Checks if a package is vetoed by examining its package-info class.
     * Results are cached to avoid repeated reflection calls.
     *
     * @param classLoader the class loader to use
     * @param packageName the package name to check
     * @return true if the package (or any parent package) is vetoed
     */
    private boolean isPackageVetoed(ClassLoader classLoader, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }

        // Check cache first
        Boolean cached = vetoedPackages.get(packageName);
        if (cached != null) {
            return cached;
        }

        // Check if this package is directly vetoed
        boolean vetoed = checkPackageAnnotation(classLoader, packageName);

        // If not directly vetoed, check parent packages (CDI 4.1: @Vetoed on parent applies to children)
        if (!vetoed) {
            String parentPackage = getParentPackage(packageName);
            if (parentPackage != null) {
                vetoed = isPackageVetoed(classLoader, parentPackage);
            }
        }

        // Cache result
        vetoedPackages.put(packageName, vetoed);
        return vetoed;
    }

    /**
     * Checks if a specific package has @Vetoed annotation.
     *
     * @param classLoader the class loader to use
     * @param packageName the package name to check
     * @return true if the package-info is annotated with @Vetoed
     */
    private boolean checkPackageAnnotation(ClassLoader classLoader, String packageName) {
        try {
            String packageInfoClass = packageName + ".package-info";
            Class<?> pkgInfo = Class.forName(packageInfoClass, false, classLoader);
            return pkgInfo.isAnnotationPresent(Vetoed.class);
        } catch (ClassNotFoundException e) {
            // No package-info.java exists - package is not vetoed
            return false;
        }
    }

    /**
     * Extracts the parent package name from a given package.
     *
     * @param packageName the package name
     * @return the parent package name, or null if no parent exists
     */
    private String getParentPackage(String packageName) {
        int lastDot = packageName.lastIndexOf('.');
        return lastDot > 0 ? packageName.substring(0, lastDot) : null;
    }

    /**
     * Extracts the package name from a fully qualified class name.
     *
     * @param className the fully qualified class name
     * @return the package name, or empty string if in default package
     */
    private String getPackageFromClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
}
