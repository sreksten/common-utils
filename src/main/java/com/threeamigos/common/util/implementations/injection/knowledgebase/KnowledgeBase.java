package com.threeamigos.common.util.implementations.injection.knowledgebase;

import com.threeamigos.common.util.implementations.injection.BeanImpl;
import com.threeamigos.common.util.implementations.injection.ProducerBean;
import jakarta.enterprise.inject.spi.Bean;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KnowledgeBase {

    // Use Set to prevent duplicate class registrations
    private final Set<Class<?>> classes = ConcurrentHashMap.newKeySet();
    private final Collection<Bean<?>> beans = new ConcurrentLinkedQueue<>();

    private final Map<Class<?>, Constructor<?>> constructorsMap = new ConcurrentHashMap<>();

    // Producer/Disposer tracking
    // ProducerBeans are also added to the beans collection, but we keep separate reference for convenience
    private final Collection<ProducerBean<?>> producerBeans = new ConcurrentLinkedQueue<>();

    // Interceptor/Decorator tracking (legacy - for backward compatibility)
    private final Collection<Class<?>> interceptors = new ConcurrentLinkedQueue<>();
    private final Collection<Class<?>> decorators = new ConcurrentLinkedQueue<>();

    // Enhanced tracking with full metadata
    private final Collection<InterceptorInfo> interceptorInfos = new ConcurrentLinkedQueue<>();
    private final Collection<DecoratorInfo> decoratorInfos = new ConcurrentLinkedQueue<>();
    private final Collection<ObserverMethodInfo> observerMethodInfos = new ConcurrentLinkedQueue<>();

    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> definitionErrors = new ArrayList<>();
    private final List<String> injectionErrors = new ArrayList<>();

    public void add(Class<?> clazz) {
        classes.add(clazz);
    }

    public Collection<Class<?>> getClasses() {
        return classes;
    }

    public void addBean(Bean<?> bean) {
        beans.add(bean);
    }

    public Collection<Bean<?>> getBeans() {
        return beans;
    }

    public <T> void addConstructor(Class<T> clazz, Constructor<T> constructor) {
        constructorsMap.put(clazz, constructor);
    }

    @SuppressWarnings("unchecked")
    public <T> Constructor<T> getConstructor(Class<T> clazz) {
        return (Constructor<T>)constructorsMap.get(clazz);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addDefinitionError(String error) {
        definitionErrors.add(error);
    }

    public List<String> getDefinitionErrors() {
        return definitionErrors;
    }

    public void addInjectionError(String error) {
        injectionErrors.add(error);
    }

    public List<String> getInjectionErrors() {
        return injectionErrors;
    }

    /**
     * Checks if there are any critical errors that would prevent application startup.
     * This includes definition errors, injection errors, and general errors.
     *
     * @return true if there are any errors that should stop the application
     */
    public boolean hasErrors() {
        return !definitionErrors.isEmpty() || !injectionErrors.isEmpty() || !errors.isEmpty();
    }

    /**
     * Returns all beans that have validation errors.
     * These beans were discovered but failed validation.
     * The application should only fail if these beans are actually needed for injection.
     *
     * @return collection of beans with validation errors
     */
    public Collection<Bean<?>> getBeansWithValidationErrors() {
        List<Bean<?>> beansWithErrors = new ArrayList<>();
        for (Bean<?> bean : beans) {
            if (bean instanceof BeanImpl && ((BeanImpl<?>) bean).hasValidationErrors()) {
                beansWithErrors.add(bean);
            }
        }
        return beansWithErrors;
    }

    /**
     * Returns all beans that are valid (no validation errors).
     * These beans are candidates for dependency injection.
     *
     * @return collection of valid beans
     */
    public Collection<Bean<?>> getValidBeans() {
        List<Bean<?>> validBeans = new ArrayList<>();
        for (Bean<?> bean : beans) {
            if (!(bean instanceof BeanImpl) || !((BeanImpl<?>) bean).hasValidationErrors()) {
                validBeans.add(bean);
            }
        }
        return validBeans;
    }

    // Producer/Disposer methods

    /**
     * Adds a ProducerBean to the knowledge base.
     * ProducerBeans are also added to the general beans collection.
     */
    public void addProducerBean(ProducerBean<?> producerBean) {
        producerBeans.add(producerBean);
        beans.add(producerBean); // Also add to general bean collection
    }

    /**
     * Returns all producer beans (convenience method).
     */
    public Collection<ProducerBean<?>> getProducerBeans() {
        return producerBeans;
    }

    // Interceptor/Decorator methods (legacy)

    public void addInterceptor(Class<?> interceptorClass) {
        interceptors.add(interceptorClass);
    }

    public Collection<Class<?>> getInterceptors() {
        return interceptors;
    }

    public void addDecorator(Class<?> decoratorClass) {
        decorators.add(decoratorClass);
    }

    public Collection<Class<?>> getDecorators() {
        return decorators;
    }

    // Enhanced Interceptor/Decorator/Observer methods

    /**
     * Adds fully validated interceptor metadata to the knowledge base.
     * This should be called after validating the interceptor class.
     *
     * @param interceptorInfo the validated interceptor metadata
     */
    public void addInterceptorInfo(InterceptorInfo interceptorInfo) {
        interceptorInfos.add(interceptorInfo);
    }

    /**
     * Returns all validated interceptors with full metadata.
     * Use this for building interceptor chains.
     *
     * @return collection of interceptor metadata
     */
    public Collection<InterceptorInfo> getInterceptorInfos() {
        return interceptorInfos;
    }

    /**
     * Adds fully validated decorator metadata to the knowledge base.
     * This should be called after validating the decorator class.
     *
     * @param decoratorInfo the validated decorator metadata
     */
    public void addDecoratorInfo(DecoratorInfo decoratorInfo) {
        decoratorInfos.add(decoratorInfo);
    }

    /**
     * Returns all validated decorators with full metadata.
     * Use this for building decorator chains.
     *
     * @return collection of decorator metadata
     */
    public Collection<DecoratorInfo> getDecoratorInfos() {
        return decoratorInfos;
    }

    /**
     * Adds fully validated observer method metadata to the knowledge base.
     * This should be called after validating the observer method.
     *
     * @param observerMethodInfo the validated observer method metadata
     */
    public void addObserverMethodInfo(ObserverMethodInfo observerMethodInfo) {
        observerMethodInfos.add(observerMethodInfo);
    }

    /**
     * Returns all validated observer methods with full metadata.
     * Use this for event firing and observer invocation.
     *
     * @return collection of observer method metadata
     */
    public Collection<ObserverMethodInfo> getObserverMethodInfos() {
        return observerMethodInfos;
    }

    // === Programmatic Bean Registration (for InjectorImpl2) ===

    /**
     * Adds a programmatic bean binding for runtime bean registration.
     *
     * <p>This allows beans to be registered programmatically outside of classpath scanning,
     * useful for testing, dynamic configuration, and third-party library integration.
     *
     * @param type the interface or abstract type
     * @param qualifiers the qualifiers for this binding
     * @param bean the bean implementation
     */
    public void addProgrammaticBean(java.lang.reflect.Type type, java.util.Collection<java.lang.annotation.Annotation> qualifiers, jakarta.enterprise.inject.spi.Bean<?> bean) {
        beans.add(bean);
    }

    /**
     * Enables an alternative bean at runtime.
     *
     * <p>This activates an @Alternative bean programmatically, useful for feature flags
     * and runtime environment detection.
     *
     * @param alternativeClass the alternative bean class to enable
     */
    public void enableAlternative(Class<?> alternativeClass) {
        // Find the bean for this class and mark it as enabled
        for (jakarta.enterprise.inject.spi.Bean<?> bean : beans) {
            if (bean.getBeanClass().equals(alternativeClass) && bean.isAlternative()) {
                // Alternative is already in beans collection, no action needed
                // Priority-based resolution will handle selection
                return;
            }
        }
        throw new IllegalArgumentException(
                "No alternative bean found for class: " + alternativeClass.getName());
    }
}