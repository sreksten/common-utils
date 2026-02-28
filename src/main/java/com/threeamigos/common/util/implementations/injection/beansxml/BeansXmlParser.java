package com.threeamigos.common.util.implementations.injection.beansxml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parser for CDI 4.1 beans.xml files using JAXB.
 *
 * <p>This parser converts beans.xml files into structured {@link BeansXml} objects
 * according to the Jakarta CDI 4.0/4.1 specification.
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>✅ Full CDI 4.1 beans.xml support (all elements)</li>
 *   <li>✅ JAXB-based unmarshalling (type-safe)</li>
 *   <li>✅ XSD validation (optional but recommended)</li>
 *   <li>✅ Thread-safe with parser caching</li>
 *   <li>✅ Handles both javax and jakarta namespaces</li>
 *   <li>✅ Graceful error handling</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * BeansXmlParser parser = new BeansXmlParser();
 *
 * // Parse from InputStream
 * try (InputStream is = getClass().getResourceAsStream("/META-INF/beans.xml")) {
 *     BeansXml beansXml = parser.parse(is);
 *     System.out.println("Discovery mode: " + beansXml.getBeanDiscoveryMode());
 *     System.out.println("Alternatives: " + beansXml.getAlternatives().getClasses());
 * }
 *
 * // Parse with validation
 * BeansXml validated = parser.parseWithValidation(is, schemaUrl);
 * }</pre>
 *
     * <h2>Error Handling:</h2>
     * <p>If parsing fails (malformed XML, invalid structure), this parser returns
     * a default BeansXml with bean-discovery-mode="annotated" and no other config.
 *
 * @author Stefano Reksten
 * @see BeansXml
 */
public class BeansXmlParser {

    /**
     * JAXB context cache for performance.
     * Creating JAXBContext is expensive, so we cache it per thread/parser.
     */
    private static final ConcurrentHashMap<Class<?>, JAXBContext> jaxbContextCache =
        new ConcurrentHashMap<>();

    /**
     * Whether to enable XSD validation during parsing.
     * Default: false (for performance and backward compatibility).
     */
    private boolean validationEnabled;

    /**
     * URL to the CDI beans XSD schema (if validation is enabled).
     * Can be loaded from the classpath or an external URL.
     */
    private URL schemaUrl;

    /**
     * Creates a new BeansXmlParser with validation disabled.
     */
    public BeansXmlParser() {
        this(false);
    }

    /**
     * Creates a new BeansXmlParser with configurable validation.
     *
     * @param validationEnabled whether to enable XSD validation
     */
    public BeansXmlParser(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    /**
     * Enables XSD schema validation with a custom schema URL.
     *
     * @param schemaUrl the URL to the beans.xsd schema file
     */
    public void setSchemaUrl(URL schemaUrl) {
        this.schemaUrl = schemaUrl;
        this.validationEnabled = true;
    }

    /**
     * Parses a beans.xml file from an InputStream.
     *
     * <p>This method uses JAXB to unmarshal the XML into a {@link BeansXml} object.
     *
     * @param inputStream the input stream containing beans.xml content
     * @return the parsed BeansXml object, or a default instance if parsing fails
     */
    public BeansXml parse(InputStream inputStream) {
        if (inputStream == null) {
            return createDefault();
        }

        try {
            return parseInternal(inputStream);
        } catch (Exception e) {
            return createDefault();
        }
    }

    /**
     * Parses a beans.xml file with XSD validation.
     *
     * <p>This method validates the XML against the CDI beans schema before unmarshalling.
     * If validation fails, an exception is thrown.
     *
     * @param inputStream the input stream containing beans.xml content
     * @param schemaUrl the URL to the XSD schema file
     * @return the parsed and validated BeansXml object
     * @throws BeansXmlParseException if parsing or validation fails
     */
    public BeansXml parseWithValidation(InputStream inputStream, URL schemaUrl)
            throws BeansXmlParseException {
        try {
            setSchemaUrl(schemaUrl);
            return parseInternal(inputStream);
        } catch (Exception e) {
            throw new BeansXmlParseException("Failed to parse and validate beans.xml", e);
        }
    }

    /**
     * Creates an XSD schema from a URL.
     *
     * @param schemaUrl the URL to the schema file
     * @return the compiled Schema object
     * @throws Exception if schema creation fails
     */
    private Schema createSchema(URL schemaUrl) throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        try (InputStream is = schemaUrl.openStream()) {
            return schemaFactory.newSchema(new StreamSource(is));
        }
    }

    /**
     * Creates a default BeansXml instance.
     *
     * <p>Used as a fallback when parsing fails or when no beans.xml exists.
     *
     * @return a BeansXml with bean-discovery-mode="annotated" and no other configuration
     */
    private BeansXml createDefault() {
        return new BeansXml();
    }

    private void ensureValidDiscoveryMode(BeansXml beansXml) throws BeansXmlParseException {
        String mode = beansXml.getBeanDiscoveryMode();
        switch (mode) {
            case "all":
            case "annotated":
            case "none":
                return;
            default:
                throw new BeansXmlParseException(
                    "Invalid bean-discovery-mode '" + mode + "'. Allowed: all, annotated, none.", null);
        }
    }

    private BeansXml parseInternal(InputStream inputStream) throws Exception {
        // Get or create JAXB context (cached for performance)
        JAXBContext jaxbContext = jaxbContextCache.computeIfAbsent(
            BeansXml.class,
            clazz -> {
                try {
                    return JAXBContext.newInstance(BeansXml.class);
                } catch (JAXBException e) {
                    throw new RuntimeException("Failed to create JAXB context for BeansXml", e);
                }
            }
        );

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        boolean stripVendorScan = validationEnabled && schemaUrl != null;

        if (validationEnabled && schemaUrl != null) {
            Schema schema = createSchema(schemaUrl);
            unmarshaller.setSchema(schema);
        }

        try (InputStream normalized = normalizeNamespaces(inputStream, stripVendorScan)) {
            BeansXml beansXml = (BeansXml) unmarshaller.unmarshal(normalized);
            ensureValidDiscoveryMode(beansXml);
            return beansXml;
        }
    }

    private InputStream normalizeNamespaces(InputStream inputStream, boolean stripVendorScan) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        String xml = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        String normalized = xml
            .replace("http://xmlns.jcp.org/xml/ns/javaee", "https://jakarta.ee/xml/ns/jakartaee")
            .replace("http://java.sun.com/xml/ns/javaee", "https://jakarta.ee/xml/ns/jakartaee");
        if (stripVendorScan) {
            normalized = normalized.replaceAll("(?s)<scan>.*?</scan>", "");
        }
        return new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Clears the JAXB context cache.
     *
     * <p>Useful for testing or when you need to free memory.
     * In normal operation, the cache improves performance significantly.
     */
    public static void clearCache() {
        jaxbContextCache.clear();
    }

    // ============================================
    // Exception Class
    // ============================================

    /**
     * Exception thrown when beans.xml parsing fails with validation enabled.
     */
    public static class BeansXmlParseException extends Exception {
        public BeansXmlParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
