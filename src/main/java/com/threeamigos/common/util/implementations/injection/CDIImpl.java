package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Implementation of jakarta.enterprise.inject.spi.CDI for static container access.
 *
 * <p>This class provides the CDI.current() entry point for obtaining a BeanManager
 * reference when injection is not available.
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * BeanManager manager = CDI.current().getBeanManager();
 * MyBean bean = CDI.current().select(MyBean.class).get();
 * </pre>
 *
 * <p><b>CDI 4.1 Compliance:</b>
 * <ul>
 *   <li>Section 12.1: Accessing the BeanManager statically via CDI.current()</li>
 *   <li>Section 5.6: Programmatic lookup via Instance<T> methods</li>
 * </ul>
 *
 * @author Stefano Reksten
 */
public class CDIImpl extends CDI<Object> {

    private final BeanManagerImpl beanManager;
    private final Instance<Object> rootInstance;

    /**
     * Creates a new CDI implementation wrapping the given BeanManager.
     *
     * @param beanManager The BeanManager to wrap
     */
    public CDIImpl(BeanManagerImpl beanManager) {
        if (beanManager == null) {
            throw new IllegalArgumentException("beanManager cannot be null");
        }
        this.beanManager = beanManager;

        // Create root Instance<Object> for programmatic lookup
        // This gives us access to all beans in the container
        this.rootInstance = beanManager.createInstance();
    }

    /**
     * Returns the BeanManager for programmatic CDI access.
     *
     * <p>Application components which cannot obtain a BeanManager reference
     * via injection nor JNDI lookup can get the reference from the
     * jakarta.enterprise.inject.spi.CDI class via a static method call:
     * <pre>
     * BeanManager manager = CDI.current().getBeanManager();
     * </pre>
     *
     * @return The BeanManager
     */
    @Override
    public BeanManager getBeanManager() {
        return beanManager;
    }

    // ========================================================================
    // Instance<T> Implementation - Delegate to root instance
    // ========================================================================

    /**
     * Obtains a child Instance for the given required type and qualifiers.
     *
     * @param subtype The required type
     * @param qualifiers The required qualifiers
     * @param <U> The type
     * @return The child Instance
     */
    @Override
    public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        return rootInstance.select(subtype, qualifiers);
    }

    /**
     * Obtains a child Instance for the given required type and qualifiers.
     *
     * @param subtype The required type
     * @param qualifiers The required qualifiers
     * @param <U> The type
     * @return The child Instance
     */
    @Override
    public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        return rootInstance.select(subtype, qualifiers);
    }

    /**
     * Determines if there is more than one bean that matches the required type and qualifiers
     * and is eligible for injection.
     *
     * @return true if there is more than one matching bean
     */
    @Override
    public boolean isAmbiguous() {
        return rootInstance.isAmbiguous();
    }

    /**
     * Determines if there is no bean that matches the required type and qualifiers
     * and is eligible for injection.
     *
     * @return true if there is no matching bean
     */
    @Override
    public boolean isUnsatisfied() {
        return rootInstance.isUnsatisfied();
    }

    /**
     * Determines if there is exactly one bean that matches the required type and qualifiers
     * and is eligible for injection.
     *
     * @return true if there is exactly one matching bean
     */
    @Override
    public boolean isResolvable() {
        return rootInstance.isResolvable();
    }

    /**
     * When called, provides back the instance associated with this Instance.
     *
     * @return The instance
     * @throws jakarta.enterprise.inject.UnsatisfiedResolutionException if no bean matches
     * @throws jakarta.enterprise.inject.AmbiguousResolutionException if multiple beans match
     */
    @Override
    public Object get() {
        return rootInstance.get();
    }

    /**
     * Returns an Iterator over all beans that match the required type and qualifiers.
     *
     * @return Iterator of matching bean instances
     */
    @Override
    public Iterator<Object> iterator() {
        return rootInstance.iterator();
    }

    /**
     * Returns a Stream over all beans that match the required type and qualifiers.
     *
     * @return Stream of matching bean instances
     */
    @Override
    public Stream<Object> stream() {
        return rootInstance.stream();
    }

    /**
     * Obtains an Instance.Handle for the bean instance.
     *
     * @return The Handle
     */
    @Override
    public Handle<Object> getHandle() {
        return rootInstance.getHandle();
    }

    /**
     * Obtains Handles for all beans that match the required type and qualifiers.
     *
     * @return Iterable of Handles
     */
    @Override
    public Iterable<? extends Handle<Object>> handles() {
        return rootInstance.handles();
    }

    /**
     * Returns a Stream of Handles for all beans that match the required type and qualifiers.
     *
     * @return Stream of Handles
     */
    @Override
    public Stream<? extends Handle<Object>> handlesStream() {
        return rootInstance.handlesStream();
    }

    /**
     * Explicitly destroys an instance, calling any @PreDestroy methods.
     *
     * @param instance The instance to destroy
     */
    @Override
    public void destroy(Object instance) {
        rootInstance.destroy(instance);
    }

    /**
     * Obtains a child Instance for the given qualifiers.
     *
     * @param qualifiers The qualifiers
     * @return The child Instance
     */
    @Override
    public Instance<Object> select(Annotation... qualifiers) {
        return rootInstance.select(qualifiers);
    }
}
