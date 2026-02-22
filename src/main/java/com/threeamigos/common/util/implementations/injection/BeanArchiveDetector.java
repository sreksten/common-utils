package com.threeamigos.common.util.implementations.injection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
 * <p><b>Usage:</b>
 * <pre>{@code
 * BeanArchiveDetector detector = new BeanArchiveDetector();
 * BeanArchiveMode mode = detector.detectArchiveMode(jarFileOrDirectory);
 * }</pre>
 *
 * @author Stefano Reksten
 */
class BeanArchiveDetector {

    private static final String BEANS_XML_PATH = "META-INF/beans.xml";

    /**
     * Cache of already detected bean archive modes.
     * Key: canonical path of JAR file or directory
     * Value: detected BeanArchiveMode
     */
    private final Map<String, BeanArchiveMode> archiveModeCache = new ConcurrentHashMap<>();

    /**
     * Detects the bean archive mode for a JAR file.
     *
     * @param jarFile the JAR file to examine
     * @return the detected bean archive mode
     */
    BeanArchiveMode detectArchiveMode(File jarFile) {
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
                return parseBeanDiscoveryMode(is);
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
        try {
            return parseBeanDiscoveryMode(beansXmlFile.toURI().toURL().openStream());
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
     * Parses beans.xml to extract the bean-discovery-mode attribute.
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
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Disable external entity processing for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            Element root = doc.getDocumentElement();
            if (root == null) {
                // Empty or malformed beans.xml - default to EXPLICIT per CDI 1.1+ spec
                return BeanArchiveMode.EXPLICIT;
            }

            String discoveryMode = root.getAttribute("bean-discovery-mode");

            if (discoveryMode == null || discoveryMode.trim().isEmpty()) {
                // No bean-discovery-mode attribute - defaults to "all" (EXPLICIT)
                return BeanArchiveMode.EXPLICIT;
            }

            switch (discoveryMode.trim().toLowerCase()) {
                case "all":
                    return BeanArchiveMode.EXPLICIT;
                case "annotated":
                    return BeanArchiveMode.IMPLICIT;
                case "none":
                    return BeanArchiveMode.NONE;
                default:
                    // Unknown mode - default to EXPLICIT for safety
                    return BeanArchiveMode.EXPLICIT;
            }
        } catch (Exception e) {
            // If parsing fails, default to EXPLICIT (more permissive)
            return BeanArchiveMode.EXPLICIT;
        }
    }

    /**
     * Clears the internal cache. Useful for testing or reloading.
     */
    void clearCache() {
        archiveModeCache.clear();
    }
}
