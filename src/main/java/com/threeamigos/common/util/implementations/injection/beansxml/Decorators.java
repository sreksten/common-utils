package com.threeamigos.common.util.implementations.injection.beansxml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class for the &lt;decorators&gt; section of beans.xml.
 *
 * <p>CDI 4.1 Section 8.2: Decorators can be enabled and ordered via beans.xml.
 * The order in the list determines decorator precedence - first entry has highest priority.
 *
 * <h2>Example XML:</h2>
 * <pre>{@code
 * <decorators>
 *     <class>com.example.TimingDecorator</class>
 *     <class>com.example.CachingDecorator</class>
 *     <class>com.example.LoggingDecorator</class>
 * </decorators>
 * }</pre>
 *
 * <h2>Decorator Ordering:</h2>
 * <p>In the example above:
 * <ol>
 *   <li>TimingDecorator wraps first (outermost)</li>
 *   <li>CachingDecorator wraps second</li>
 *   <li>LoggingDecorator wraps last (innermost, closest to delegate)</li>
 * </ol>
 *
 * <h2>Decorators vs Interceptors:</h2>
 * <ul>
 *   <li><b>Decorators</b> - Implement business interfaces, type-safe delegation</li>
 *   <li><b>Interceptors</b> - Generic cross-cutting concerns, annotation-based</li>
 * </ul>
 *
 * <p><b>Note:</b> CDI 4.1 prefers @Priority annotation over beans.xml for decorator ordering.
 * However, beans.xml decorator ordering is still part of the specification.
 *
 * <p><b>Priority Rules:</b>
 * <ul>
 *   <li>@Priority on decorator class takes precedence over beans.xml</li>
 *   <li>If both @Priority and beans.xml are used, @Priority wins</li>
 *   <li>Decorators not listed in beans.xml and without @Priority are disabled</li>
 * </ul>
 *
 * @author Stefano Reksten
 * @see BeansXml
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Decorators {

    /**
     * Ordered list of decorator classes to enable.
     *
     * <p>Each entry is a fully-qualified class name (e.g., "com.example.TimingDecorator").
     *
     * <p>The order in this list determines decorator wrapping order:
     * <ul>
     *   <li>First entry = outermost decorator (highest priority)</li>
     *   <li>Last entry = innermost decorator (closest to actual delegate)</li>
     * </ul>
     *
     * <p><b>Execution Flow:</b>
     * <pre>
     * Client → Decorator1 → Decorator2 → Decorator3 → Actual Bean (Delegate)
     *                                                ← Return Value
     * </pre>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @Decorator
     * public class TimingDecorator implements PaymentService {
     *     @Inject @Delegate PaymentService delegate;
     *
     *     public void processPayment(Payment p) {
     *         long start = System.currentTimeMillis();
     *         delegate.processPayment(p); // Call next decorator or actual bean
     *         long duration = System.currentTimeMillis() - start;
     *         log.info("Payment processed in " + duration + "ms");
     *     }
     * }
     * }</pre>
     */
    @XmlElement(name = "class")
    private List<String> classes = new ArrayList<>();

    // ============================================
    // Getters and Setters
    // ============================================

    public List<String> getClasses() {
        return classes;
    }

    public void setClasses(List<String> classes) {
        this.classes = classes;
    }

    // ============================================
    // Convenience Methods
    // ============================================

    /**
     * Checks if any decorators are configured.
     *
     * @return true if at least one decorator is configured
     */
    public boolean isEmpty() {
        return classes.isEmpty();
    }

    /**
     * Gets the priority/order of a specific decorator class.
     *
     * <p>Lower index = higher priority (outermost decorator).
     *
     * @param className the fully-qualified decorator class name
     * @return the index (0-based), or -1 if not found
     */
    public int getOrder(String className) {
        return classes.indexOf(className);
    }

    @Override
    public String toString() {
        return "Decorators{" +
               "classes=" + classes.size() +
               '}';
    }
}
