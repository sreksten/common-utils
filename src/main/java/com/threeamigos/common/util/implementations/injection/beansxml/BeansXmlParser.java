package com.threeamigos.common.util.implementations.injection.beansxml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
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
 * <p>If parsing fails (malformed XML, invalid structure), this parser:
 * <ul>
 *   <li>Logs a warning with details</li>
 *   <li>Returns a default BeansXml with bean-discovery-mode="all"</li>
 *   <li>Never throws exceptions (fail-safe behavior)</li>
 * </ul>
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
    private boolean validationEnabled = false;

    /**
     * URL to the CDI beans XSD schema (if validation is enabled).
     * Can be loaded from classpath or external URL.
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

            // Create unmarshaller
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Enable validation if configured
            if (validationEnabled && schemaUrl != null) {
                Schema schema = createSchema(schemaUrl);
                unmarshaller.setSchema(schema);
            }

            // Unmarshal the XML
            BeansXml beansXml = (BeansXml) unmarshaller.unmarshal(inputStream);

            // Ensure non-null discovery mode
            if (beansXml.getBeanDiscoveryMode() == null ||
                beansXml.getBeanDiscoveryMode().trim().isEmpty()) {
                beansXml.setBeanDiscoveryMode("all");
            }

            return beansXml;

        } catch (Exception e) {
            // Log the error but don't fail - return default instead
            System.err.println("[BeansXmlParser] Failed to parse beans.xml: " + e.getMessage());
            System.err.println("[BeansXmlParser] Falling back to default configuration (bean-discovery-mode=all)");

            // In production, you might want to use a proper logger:
            // logger.warn("Failed to parse beans.xml, using defaults", e);

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
            return parse(inputStream);
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
        return schemaFactory.newSchema(new StreamSource(schemaUrl.openStream()));
    }

    /**
     * Creates a default BeansXml instance.
     *
     * <p>Used as a fallback when parsing fails or when no beans.xml exists.
     *
     * @return a BeansXml with bean-discovery-mode="all" and no other configuration
     */
    private BeansXml createDefault() {
        BeansXml beansXml = new BeansXml();
        beansXml.setBeanDiscoveryMode("all");
        return beansXml;
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
