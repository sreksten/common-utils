package com.threeamigos.common.util.implementations.injection.discovery;

import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Detects bean archive mode (EXPLICIT/IMPLICIT/NONE) for JAR files and directories
 * by examining META-INF/beans.xml according to CDI 4.1 specification.
 *
 * <p><b>CDI 4.1 Bean Archive Types:</b>
 * <ul>
 *   <li><b>Explicit bean archive:</b> Contains beans.xml with bean-discovery-mode="all" (or no mode attribute).
 *       All classes with suitable constructors are beans.</li>
 *   <li><b>Implicit bean archive:</b> Contains beans.xml with bean-discovery-mode="annotated", OR
 *       no beans.xml but has classes with bean-defining annotations. Only annotated classes are beans.</li>
 *   <li><b>Not a bean archive:</b> Contains beans.xml with bean-discovery-mode="none". No beans discovered.</li>
 * </ul>
 *
 * <p><b>Enhancement (v1.16):</b> Now uses JAXB-based {@link BeansXmlParser} to parse
 * the complete beans.xml structure, including alternatives, interceptors, decorators, scan, and trim.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * BeanArchiveDetector detector = new BeanArchiveDetector();
 * BeanArchiveMode mode = detector.detectArchiveMode(jarFileOrDirectory);
 * BeansXml beansXml = detector.getBeansXml(jarFileOrDirectory); // Get full configuration
 * }</pre>
 *
 * @author Stefano Reksten
 */
public class BeanArchiveDetector {

    private static final String BEANS_XML_PATH = "META-INF/beans.xml";

    /**
     * Cache of already detected bean archive modes.
     * Key: canonical path of JAR file or directory
     * Value: detected BeanArchiveMode
     */
    private final Map<String, BeanArchiveMode> archiveModeCache = new ConcurrentHashMap<>();

    /**
     * Cache of parsed beans.xml configurations.
     * Key: canonical path of JAR file or directory
     * Value: parsed BeansXml object
     */
    private final Map<String, BeansXml> beansXmlCache = new ConcurrentHashMap<>();

    /**
     * Parser for beans.xml files.
     */
    private final BeansXmlParser beansXmlParser = new BeansXmlParser();

    /**
     * Detects the bean archive mode for a JAR file.
     *
     * @param jarFile the JAR file to examine
     * @return the detected bean archive mode
     */
    public BeanArchiveMode detectArchiveMode(File jarFile) {
        if (jarFile == null || !jarFile.exists()) {
            return BeanArchiveMode.IMPLICIT; // Default for non-existent archives
        }

        try {
            String canonicalPath = jarFile.getCanonicalPath();
            return archiveModeCache.computeIfAbsent(canonicalPath, path -> {
                if (jarFile.isFile() && jarFile.getName().endsWith(".jar")) {
                    return detectJarArchiveMode(jarFile);
                } else if (jarFile.isDirectory()) {
                    return detectDirectoryArchiveMode(jarFile);
                } else {
                    return BeanArchiveMode.IMPLICIT;
                }
            });
        } catch (Exception e) {
            // If we can't determine, default to IMPLICIT (safest option)
            return BeanArchiveMode.IMPLICIT;
        }
    }

    /**
     * Detects the bean archive mode for a JAR file by examining its beans.xml.
     *
     * @param jarFile the JAR file
     * @return the detected mode
     */
    private BeanArchiveMode detectJarArchiveMode(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            ZipEntry beansXmlEntry = jar.getEntry(BEANS_XML_PATH);

            if (beansXmlEntry == null) {
                // No beans.xml found - this is an implicit bean archive
                // (will only discover classes with bean-defining annotations)
                return BeanArchiveMode.IMPLICIT;
            }

            // beans.xml exists - parse it to determine mode
            try (InputStream is = jar.getInputStream(beansXmlEntry)) {
                BeansXml beansXml = beansXmlParser.parse(is);
                cacheBeansXml(jarFile, beansXml);
                return determineMode(beansXml);
            }
        } catch (Exception e) {
            // If we can't read beans.xml, default to IMPLICIT
            return BeanArchiveMode.IMPLICIT;
        }
    }

    /**
     * Detects the bean archive mode for a directory by examining its beans.xml.
     *
     * @param directory the directory (typically in classpath)
     * @return the detected mode
     */
    private BeanArchiveMode detectDirectoryArchiveMode(File directory) {
        File beansXmlFile = new File(directory, BEANS_XML_PATH);

        if (!beansXmlFile.exists()) {
            // No beans.xml found - this is an implicit bean archive
            return BeanArchiveMode.IMPLICIT;
        }

        // beans.xml exists - parse it to determine mode
        try (InputStream is = beansXmlFile.toURI().toURL().openStream()) {
            BeansXml beansXml = beansXmlParser.parse(is);
            cacheBeansXml(directory, beansXml);
            return determineMode(beansXml);
        } catch (Exception e) {
            // If we can't read beans.xml, default to IMPLICIT
            return BeanArchiveMode.IMPLICIT;
        }
    }

    /**
     * Detects bean archive mode from a URL (handles both JAR and directory URLs).
     *
     * @param resourceUrl the URL to check (e.g., jar:file:/path/to/lib.jar!/META-INF/beans.xml)
     * @return the detected mode
     */
    BeanArchiveMode detectArchiveModeFromUrl(URL resourceUrl) {
        if (resourceUrl == null) {
            return BeanArchiveMode.IMPLICIT;
        }

        String protocol = resourceUrl.getProtocol();

        try {
            if ("jar".equals(protocol)) {
                // Extract JAR file path from jar:file:/path/to/lib.jar!/some/path
                String urlString = resourceUrl.toString();
                int jarSeparator = urlString.indexOf("!/");
                if (jarSeparator != -1) {
                    String jarFilePath = urlString.substring(urlString.indexOf("file:"), jarSeparator);
                    File jarFile = new File(new URL(jarFilePath).toURI());
                    return detectArchiveMode(jarFile);
                }
            } else if ("file".equals(protocol)) {
                // File URL - find the root directory
                File file = new File(resourceUrl.toURI());
                // Navigate up to find the classpath root (where META-INF would be)
                File directory = findClasspathRoot(file);
                if (directory != null) {
                    return detectArchiveMode(directory);
                }
            }
        } catch (Exception e) {
            // Fall through to default
        }

        return BeanArchiveMode.IMPLICIT;
    }

    /**
     * Finds the classpath root directory by navigating up from a file.
     * The classpath root is where META-INF directory exists.
     *
     * @param file a file within the classpath
     * @return the classpath root directory, or null if not found
     */
    private File findClasspathRoot(File file) {
        File current = file.isDirectory() ? file : file.getParentFile();

        while (current != null) {
            // Check if META-INF exists at this level
            File metaInf = new File(current, "META-INF");
            if (metaInf.exists() && metaInf.isDirectory()) {
                return current;
            }
            current = current.getParentFile();
        }

        return null;
    }

    /**
     * Parses beans.xml using JAXB and extracts the bean-discovery-mode.
     *
     * <p>CDI 4.1 bean-discovery-mode values:
     * <ul>
     *   <li>"all" or missing → EXPLICIT (all classes are beans)</li>
     *   <li>"annotated" → IMPLICIT (only annotated classes are beans)</li>
     *   <li>"none" → Not a bean archive (skip entirely)</li>
     * </ul>
     *
     * @param inputStream the beans.xml input stream
     * @return the detected mode
     */
    private BeanArchiveMode parseBeanDiscoveryMode(InputStream inputStream) {
        try {
            BeansXml beansXml = beansXmlParser.parse(inputStream);
            // parseBeanDiscoveryMode is used when we cannot associate with a concrete file path
            return determineMode(beansXml);
        } catch (Exception e) {
            return BeanArchiveMode.EXPLICIT;
        }
    }

    private BeanArchiveMode determineMode(BeansXml beansXml) {
        if (beansXml == null) {
            return BeanArchiveMode.IMPLICIT;
        }

        String discoveryMode = beansXml.getBeanDiscoveryMode();
        BeanArchiveMode baseMode;

        if (discoveryMode == null || discoveryMode.trim().isEmpty()) {
            baseMode = BeanArchiveMode.EXPLICIT;
        } else {
            switch (discoveryMode.trim().toLowerCase()) {
                case "all":
                    baseMode = BeanArchiveMode.EXPLICIT;
                    break;
                case "annotated":
                    baseMode = BeanArchiveMode.IMPLICIT;
                    break;
                case "none":
                    baseMode = BeanArchiveMode.NONE;
                    break;
                default:
                    baseMode = BeanArchiveMode.EXPLICIT;
            }
        }

        // <trim/> turns explicit discovery into trimmed explicit (behaves like implicit)
        if (beansXml.isTrimEnabled() && baseMode == BeanArchiveMode.EXPLICIT) {
            return BeanArchiveMode.TRIMMED;
        }
        return baseMode;
    }

    /**
     * Gets the fully parsed beans.xml configuration for a JAR file or directory.
     *
     * <p>This method returns the complete beans.xml structure, including:
     * <ul>
     *   <li>bean-discovery-mode</li>
     *   <li>alternatives (classes and stereotypes)</li>
     *   <li>interceptors (ordered list)</li>
     *   <li>decorators (ordered list)</li>
     *   <li>scan exclusions</li>
     *   <li>trim setting</li>
     * </ul>
     *
     * @param jarFile the JAR file or directory to examine
     * @return the parsed BeansXml object, or a default instance if no beans.xml exists
     */
    BeansXml getBeansXml(File jarFile) {
        if (jarFile == null || !jarFile.exists()) {
            return new BeansXml(); // Default with bean-discovery-mode="all"
        }

        try {
            String canonicalPath = jarFile.getCanonicalPath();
            return beansXmlCache.computeIfAbsent(canonicalPath, path -> {
                if (jarFile.isFile() && jarFile.getName().endsWith(".jar")) {
                    return parseBeansXmlFromJar(jarFile);
                } else if (jarFile.isDirectory()) {
                    return parseBeansXmlFromDirectory(jarFile);
                } else {
                    return new BeansXml();
                }
            });
        } catch (Exception e) {
            return new BeansXml();
        }
    }

    /**
     * Parses beans.xml from a JAR file.
     *
     * @param jarFile the JAR file
     * @return the parsed BeansXml object
     */
    private BeansXml parseBeansXmlFromJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            ZipEntry beansXmlEntry = jar.getEntry(BEANS_XML_PATH);

            if (beansXmlEntry == null) {
                return new BeansXml(); // No beans.xml
            }

            try (InputStream is = jar.getInputStream(beansXmlEntry)) {
                BeansXml beansXml = beansXmlParser.parse(is);
                cacheBeansXml(jarFile, beansXml);
                // Update the archive mode cache with the effective mode (including trim)
                archiveModeCache.put(jarFile.getCanonicalPath(), determineMode(beansXml));
                return beansXml;
            }
        } catch (Exception e) {
            return new BeansXml();
        }
    }

    /**
     * Parses beans.xml from a directory.
     *
     * @param directory the directory
     * @return the parsed BeansXml object
     */
    private BeansXml parseBeansXmlFromDirectory(File directory) {
        File beansXmlFile = new File(directory, BEANS_XML_PATH);

        if (!beansXmlFile.exists()) {
            return new BeansXml(); // No beans.xml
        }

        try {
            try (InputStream is = beansXmlFile.toURI().toURL().openStream()) {
                BeansXml beansXml = beansXmlParser.parse(is);
                cacheBeansXml(directory, beansXml);
                archiveModeCache.put(directory.getCanonicalPath(), determineMode(beansXml));
                return beansXml;
            }
        } catch (Exception e) {
            return new BeansXml();
        }
    }

    private void cacheBeansXml(File archiveRoot, BeansXml beansXml) {
        if (archiveRoot == null || beansXml == null) {
            return;
        }
        try {
            beansXmlCache.putIfAbsent(archiveRoot.getCanonicalPath(), beansXml);
        } catch (Exception ignored) {
            // ignore cache write failures
        }
    }

    /**
     * Clears the internal caches. Useful for testing or reloading.
     */
    void clearCache() {
        archiveModeCache.clear();
        beansXmlCache.clear();
    }
}
