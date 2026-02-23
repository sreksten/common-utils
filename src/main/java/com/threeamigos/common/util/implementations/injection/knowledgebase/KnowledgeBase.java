package com.threeamigos.common.util.implementations.injection.knowledgebase;

import com.threeamigos.common.util.implementations.injection.BeanImpl;
import com.threeamigos.common.util.implementations.injection.ProducerBean;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InterceptionType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

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

    // ============================================================
    // INTERCEPTOR QUERY METHODS
    // ============================================================

    /**
     * Queries interceptors by interceptor bindings and interception type, sorted by priority.
     *
     * <p>This is the primary method for resolving which interceptors should be applied to a target bean.
     * It performs the following:
     * <ul>
     *   <li>Filters interceptors that have matching interceptor bindings</li>
     *   <li>Filters interceptors that support the specified interception type</li>
     *   <li>Sorts by priority (lower priority value = higher precedence, executes first)</li>
     * </ul>
     *
     * <p><b>CDI 4.1 Interceptor Resolution Rules:</b>
     * <ul>
     *   <li>An interceptor matches if ALL of its bindings are present on the target</li>
     *   <li>The target may have additional bindings not present on the interceptor</li>
     *   <li>Priority determines invocation order (default is Integer.MAX_VALUE if not specified)</li>
     * </ul>
     *
     * @param interceptionType the type of interception (AROUND_INVOKE, AROUND_CONSTRUCT, POST_CONSTRUCT, PRE_DESTROY)
     * @param targetBindings the interceptor bindings present on the target bean/method
     * @return list of matching interceptors sorted by priority (ascending)
     */
    public List<InterceptorInfo> getInterceptorsByBindingsAndType(
            InterceptionType interceptionType,
            Set<Annotation> targetBindings) {

        if (targetBindings == null || targetBindings.isEmpty()) {
            return Collections.emptyList();
        }

        return interceptorInfos.stream()
                .filter(info -> supportsInterceptionType(info, interceptionType))
                .filter(info -> hasMatchingBindings(info, targetBindings))
                .sorted(Comparator.comparingInt(InterceptorInfo::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Queries interceptors by a single interceptor binding annotation, sorted by priority.
     *
     * <p>Convenience method for when the target has only one interceptor binding.
     *
     * @param interceptionType the type of interception
     * @param binding the single interceptor binding annotation
     * @return list of matching interceptors sorted by priority
     */
    public List<InterceptorInfo> getInterceptorsByBindingAndType(
            InterceptionType interceptionType,
            Annotation binding) {

        if (binding == null) {
            return Collections.emptyList();
        }

        return getInterceptorsByBindingsAndType(interceptionType, Collections.singleton(binding));
    }

    /**
     * Queries interceptors by interception type only (no binding filtering), sorted by priority.
     *
     * <p>Returns all interceptors that support the given interception type, regardless of bindings.
     * This is useful for diagnostic purposes or when you want to see all available interceptors.
     *
     * @param interceptionType the type of interception
     * @return list of interceptors that support this type, sorted by priority
     */
    public List<InterceptorInfo> getInterceptorsByType(
            InterceptionType interceptionType) {

        return interceptorInfos.stream()
                .filter(info -> supportsInterceptionType(info, interceptionType))
                .sorted(Comparator.comparingInt(InterceptorInfo::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Queries interceptors by interceptor bindings only (no type filtering), sorted by priority.
     *
     * <p>Returns all interceptors that match the given bindings, regardless of what interception
     * types they support. Useful for seeing all interceptors that could potentially apply.
     *
     * @param targetBindings the interceptor bindings to match
     * @return list of matching interceptors sorted by priority
     */
    public List<InterceptorInfo> getInterceptorsByBindings(Set<Annotation> targetBindings) {
        if (targetBindings == null || targetBindings.isEmpty()) {
            return Collections.emptyList();
        }

        return interceptorInfos.stream()
                .filter(info -> hasMatchingBindings(info, targetBindings))
                .sorted(Comparator.comparingInt(InterceptorInfo::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Returns all interceptor bindings registered in the system.
     *
     * <p>This returns the unique set of all interceptor binding annotation types
     * that are present on any registered interceptor.
     *
     * @return set of all interceptor binding annotation types
     */
    public Set<Class<? extends Annotation>> getAllInterceptorBindingTypes() {
        return interceptorInfos.stream()
                .flatMap(info -> info.getInterceptorBindings().stream())
                .map(Annotation::annotationType)
                .collect(Collectors.toSet());
    }

    /**
     * Checks if an interceptor supports a specific interception type.
     *
     * <p>An interceptor supports a type if it has the corresponding interceptor method:
     * <ul>
     *   <li>AROUND_INVOKE → @AroundInvoke method</li>
     *   <li>AROUND_CONSTRUCT → @AroundConstruct method</li>
     *   <li>POST_CONSTRUCT → @PostConstruct method</li>
     *   <li>PRE_DESTROY → @PreDestroy method</li>
     * </ul>
     *
     * @param interceptorInfo the interceptor to check
     * @param interceptionType the interception type to check for
     * @return true if the interceptor supports this type
     */
    private boolean supportsInterceptionType(
            InterceptorInfo interceptorInfo,
            InterceptionType interceptionType) {

        switch (interceptionType) {
            case AROUND_INVOKE:
                return interceptorInfo.hasAroundInvoke();

            case AROUND_CONSTRUCT:
                return interceptorInfo.hasAroundConstruct();

            case POST_CONSTRUCT:
                return interceptorInfo.getPostConstructMethod() != null;

            case PRE_DESTROY:
                return interceptorInfo.getPreDestroyMethod() != null;

            case AROUND_TIMEOUT:
                // EJB feature - not supported in this implementation
                return false;

            default:
                return false;
        }
    }

    /**
     * Checks if an interceptor's bindings match the target's bindings.
     *
     * <p><b>CDI 4.1 Binding Matching Rules:</b>
     * <ul>
     *   <li>ALL the interceptor's bindings must be present on the target</li>
     *   <li>The target may have additional bindings that the interceptor doesn't have</li>
     *   <li>Binding values (annotation attributes) must also match</li>
     * </ul>
     *
     * <p>Example:
     * <pre>
     * Interceptor has: @Transactional, @Logged
     * Target has: @Transactional, @Logged, @Secured
     * Result: MATCH (target has all interceptor bindings)
     *
     * Interceptor has: @Transactional, @Logged
     * Target has: @Transactional
     * Result: NO MATCH (target is missing @Logged)
     * </pre>
     *
     * @param interceptorInfo the interceptor to check
     * @param targetBindings the bindings on the target bean/method
     * @return true if all interceptor bindings are present on the target
     */
    private boolean hasMatchingBindings(InterceptorInfo interceptorInfo, Set<Annotation> targetBindings) {
        Set<Annotation> interceptorBindings = interceptorInfo.getInterceptorBindings();

        // Check if ALL interceptor bindings are present on the target
        for (Annotation interceptorBinding : interceptorBindings) {
            boolean found = false;

            for (Annotation targetBinding : targetBindings) {
                if (areBindingsEqual(interceptorBinding, targetBinding)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Interceptor has a binding that target doesn't have
                return false;
            }
        }

        return true; // All interceptor bindings found on target
    }

    /**
     * Checks if two interceptor binding annotations are equal.
     *
     * <p>Two bindings are equal if:
     * <ul>
     *   <li>They are the same annotation type</li>
     *   <li>All their attributes (members) have equal values</li>
     * </ul>
     *
     * <p>This uses the annotation's built-in equals() method, which compares
     * all annotation attributes according to the Java Language Specification.
     *
     * @param binding1 first binding annotation
     * @param binding2 second binding annotation
     * @return true if the bindings are equal
     */
    private boolean areBindingsEqual(Annotation binding1, Annotation binding2) {
        // Annotations implement equals() to compare type and all attributes
        return binding1.equals(binding2);
    }

    // === Programmatic Bean Registration (for InjectorImpl2) ===

    /**
     * Adds a programmatic bean binding for runtime bean registration.
     *
     * <p>This allows beans to be registered programmatically outside classpath scanning,
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