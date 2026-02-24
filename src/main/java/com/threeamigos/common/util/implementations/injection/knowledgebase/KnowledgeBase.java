package com.threeamigos.common.util.implementations.injection.knowledgebase;

import com.threeamigos.common.util.implementations.injection.BeanImpl;
import com.threeamigos.common.util.implementations.injection.ProducerBean;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
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

    // Programmatically registered stereotypes (via BeforeBeanDiscovery.addStereotype)
    // Maps stereotype class -> set of meta-annotations that define the stereotype
    private final Map<Class<? extends Annotation>, Set<Annotation>> registeredStereotypes = new ConcurrentHashMap<>();

    // Programmatically registered qualifiers (via BeforeBeanDiscovery.addQualifier)
    private final Set<Class<? extends Annotation>> registeredQualifiers = ConcurrentHashMap.newKeySet();

    // Programmatically registered scopes (via BeforeBeanDiscovery.addScope)
    // Maps scope annotation class -> ScopeMetadata containing normal/passivating flags
    private final Map<Class<? extends Annotation>, ScopeMetadata> registeredScopes = new ConcurrentHashMap<>();

    // Programmatically registered interceptor bindings (via BeforeBeanDiscovery.addInterceptorBinding)
    // Maps interceptor binding class -> set of meta-annotations that define the binding
    private final Map<Class<? extends Annotation>, Set<Annotation>> registeredInterceptorBindings = new ConcurrentHashMap<>();

    // Programmatically registered annotated types (via BeforeBeanDiscovery.addAnnotatedType)
    // Maps ID -> AnnotatedType for synthetic types added by extensions
    private final Map<String, jakarta.enterprise.inject.spi.AnnotatedType<?>> registeredAnnotatedTypes = new ConcurrentHashMap<>();

    // Programmatically registered synthetic observer methods (via AfterBeanDiscovery.addObserverMethod)
    private final Collection<jakarta.enterprise.inject.spi.ObserverMethod<?>> syntheticObserverMethods = new ConcurrentLinkedQueue<>();

    // beans.xml configurations from all scanned archives
    // Collection is used instead of merging because each archive may have different configurations
    private final Collection<BeansXml> beansXmlConfigurations = new ConcurrentLinkedQueue<>();

    // Vetoed types (types vetoed by extensions during ProcessAnnotatedType)
    private final Set<Class<?>> vetoedTypes = ConcurrentHashMap.newKeySet();

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
        // Use AnnotationComparator to respect @Nonbinding members
        // According to CDI 4.1 spec, interceptor binding members marked with @Nonbinding
        // must be ignored when comparing bindings for interceptor resolution
        return com.threeamigos.common.util.implementations.injection.AnnotationComparator.equals(binding1, binding2);
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

    /**
     * Registers a stereotype programmatically with its meta-annotations.
     *
     * <p>This is called by BeforeBeanDiscovery.addStereotype() to register stereotypes
     * that are not defined via @Stereotype annotation.
     *
     * @param stereotype the stereotype annotation class
     * @param stereotypeDef the meta-annotations that define the stereotype (scope, qualifiers, interceptor bindings, etc.)
     */
    public void addStereotype(Class<? extends Annotation> stereotype, Annotation... stereotypeDef) {
        if (stereotype == null) {
            throw new IllegalArgumentException("Stereotype cannot be null");
        }

        Set<Annotation> definitions = new HashSet<>();
        if (stereotypeDef != null) {
            definitions.addAll(Arrays.asList(stereotypeDef));
        }

        registeredStereotypes.put(stereotype, definitions);

        System.out.println("[KnowledgeBase] Registered stereotype: " + stereotype.getSimpleName() +
                          " with " + definitions.size() + " meta-annotation(s)");
    }

    /**
     * Checks if a given annotation type is a registered stereotype.
     *
     * @param annotationType the annotation type to check
     * @return true if it's a programmatically registered stereotype
     */
    public boolean isRegisteredStereotype(Class<? extends Annotation> annotationType) {
        return registeredStereotypes.containsKey(annotationType);
    }

    /**
     * Gets the stereotype definition (meta-annotations) for a registered stereotype.
     *
     * @param stereotype the stereotype annotation class
     * @return set of meta-annotations, or null if not registered
     */
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype) {
        return registeredStereotypes.get(stereotype);
    }

    /**
     * Gets all registered stereotypes.
     *
     * @return map of stereotype class to their definitions
     */
    public Map<Class<? extends Annotation>, Set<Annotation>> getRegisteredStereotypes() {
        return Collections.unmodifiableMap(registeredStereotypes);
    }

    /**
     * Registers a qualifier programmatically.
     *
     * <p>This is called by BeforeBeanDiscovery.addQualifier() to register qualifiers
     * that are not defined via @Qualifier annotation.
     *
     * @param qualifier the qualifier annotation class
     */
    public void addQualifier(Class<? extends Annotation> qualifier) {
        if (qualifier == null) {
            throw new IllegalArgumentException("Qualifier cannot be null");
        }

        registeredQualifiers.add(qualifier);

        System.out.println("[KnowledgeBase] Registered qualifier: " + qualifier.getSimpleName());
    }

    /**
     * Checks if a given annotation type is a registered qualifier.
     *
     * @param annotationType the annotation type to check
     * @return true if it's a programmatically registered qualifier
     */
    public boolean isRegisteredQualifier(Class<? extends Annotation> annotationType) {
        return registeredQualifiers.contains(annotationType);
    }

    /**
     * Gets all registered qualifiers.
     *
     * @return set of registered qualifier annotation classes
     */
    public Set<Class<? extends Annotation>> getRegisteredQualifiers() {
        return Collections.unmodifiableSet(registeredQualifiers);
    }

    /**
     * Registers a scope programmatically with its characteristics.
     *
     * <p>This is called by BeforeBeanDiscovery.addScope() to register scopes
     * that are not defined via @NormalScope or pseudo-scope annotations.
     *
     * @param scopeType the scope annotation class
     * @param normal whether it's a normal scope (true) or pseudo-scope (false)
     * @param passivating whether instances in this scope can be passivated (serialized)
     */
    public void addScope(Class<? extends Annotation> scopeType, boolean normal, boolean passivating) {
        if (scopeType == null) {
            throw new IllegalArgumentException("Scope type cannot be null");
        }

        ScopeMetadata metadata = new ScopeMetadata(scopeType, normal, passivating);
        registeredScopes.put(scopeType, metadata);

        System.out.println("[KnowledgeBase] Registered scope: " + scopeType.getSimpleName() +
                          " (normal=" + normal + ", passivating=" + passivating + ")");
    }

    /**
     * Checks if a given annotation type is a registered scope.
     *
     * @param annotationType the annotation type to check
     * @return true if it's a programmatically registered scope
     */
    public boolean isRegisteredScope(Class<? extends Annotation> annotationType) {
        return registeredScopes.containsKey(annotationType);
    }

    /**
     * Gets the scope metadata for a registered scope.
     *
     * @param scopeType the scope annotation class
     * @return scope metadata, or null if not registered
     */
    public ScopeMetadata getScopeMetadata(Class<? extends Annotation> scopeType) {
        return registeredScopes.get(scopeType);
    }

    /**
     * Gets all registered scopes.
     *
     * @return map of scope class to their metadata
     */
    public Map<Class<? extends Annotation>, ScopeMetadata> getRegisteredScopes() {
        return Collections.unmodifiableMap(registeredScopes);
    }

    /**
     * Registers an interceptor binding programmatically with its meta-annotations.
     *
     * <p>This is called by BeforeBeanDiscovery.addInterceptorBinding() to register
     * interceptor bindings that are not defined via @InterceptorBinding annotation.
     *
     * @param bindingType the interceptor binding annotation class
     * @param bindingTypeDef the meta-annotations that define the binding
     */
    public void addInterceptorBinding(Class<? extends Annotation> bindingType, Annotation... bindingTypeDef) {
        if (bindingType == null) {
            throw new IllegalArgumentException("Interceptor binding type cannot be null");
        }

        Set<Annotation> definitions = new HashSet<>();
        if (bindingTypeDef != null) {
            definitions.addAll(Arrays.asList(bindingTypeDef));
        }

        registeredInterceptorBindings.put(bindingType, definitions);

        System.out.println("[KnowledgeBase] Registered interceptor binding: " + bindingType.getSimpleName() +
                          " with " + definitions.size() + " meta-annotation(s)");
    }

    /**
     * Checks if a given annotation type is a registered interceptor binding.
     *
     * @param annotationType the annotation type to check
     * @return true if it's a programmatically registered interceptor binding
     */
    public boolean isRegisteredInterceptorBinding(Class<? extends Annotation> annotationType) {
        return registeredInterceptorBindings.containsKey(annotationType);
    }

    /**
     * Gets the interceptor binding definition (meta-annotations) for a registered binding.
     *
     * @param bindingType the interceptor binding annotation class
     * @return set of meta-annotations, or null if not registered
     */
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> bindingType) {
        return registeredInterceptorBindings.get(bindingType);
    }

    /**
     * Gets all registered interceptor bindings.
     *
     * @return map of interceptor binding class to their definitions
     */
    public Map<Class<? extends Annotation>, Set<Annotation>> getRegisteredInterceptorBindings() {
        return Collections.unmodifiableMap(registeredInterceptorBindings);
    }

    /**
     * Registers an annotated type programmatically.
     *
     * <p>This is called by BeforeBeanDiscovery.addAnnotatedType() to register synthetic
     * types added by extensions that should be processed during bean discovery.
     *
     * @param type the annotated type to register
     * @param id the unique identifier for this registration
     */
    public void addAnnotatedType(jakarta.enterprise.inject.spi.AnnotatedType<?> type, String id) {
        if (type == null) {
            throw new IllegalArgumentException("Annotated type cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        if (registeredAnnotatedTypes.containsKey(id)) {
            throw new IllegalArgumentException("Annotated type with ID '" + id + "' already registered");
        }

        registeredAnnotatedTypes.put(id, type);

        System.out.println("[KnowledgeBase] Registered annotated type: " + type.getJavaClass().getName() +
                          " with ID: " + id);
    }

    /**
     * Gets a registered annotated type by ID.
     *
     * @param id the unique identifier
     * @return the annotated type, or null if not found
     */
    public jakarta.enterprise.inject.spi.AnnotatedType<?> getRegisteredAnnotatedType(String id) {
        return registeredAnnotatedTypes.get(id);
    }

    /**
     * Gets all registered annotated types.
     *
     * @return map of ID to annotated type
     */
    public Map<String, jakarta.enterprise.inject.spi.AnnotatedType<?>> getRegisteredAnnotatedTypes() {
        return Collections.unmodifiableMap(registeredAnnotatedTypes);
    }

    /**
     * Registers a synthetic observer method.
     *
     * <p>This is called by AfterBeanDiscovery.addObserverMethod() to register observer methods
     * created programmatically by extensions (not discovered from bean classes).
     *
     * @param observerMethod the synthetic observer method to register
     */
    public void addSyntheticObserverMethod(jakarta.enterprise.inject.spi.ObserverMethod<?> observerMethod) {
        if (observerMethod == null) {
            throw new IllegalArgumentException("Observer method cannot be null");
        }

        syntheticObserverMethods.add(observerMethod);

        System.out.println("[KnowledgeBase] Registered synthetic observer method: " +
                          "observedType=" + observerMethod.getObservedType() +
                          ", async=" + observerMethod.isAsync());
    }

    /**
     * Gets all synthetic observer methods.
     *
     * @return collection of synthetic observer methods
     */
    public Collection<jakarta.enterprise.inject.spi.ObserverMethod<?>> getSyntheticObserverMethods() {
        return syntheticObserverMethods;
    }

    /**
     * Adds a beans.xml configuration from a scanned archive.
     *
     * <p>This is called during bean discovery to collect all beans.xml files found
     * in the classpath. Each archive (JAR or directory) may have its own beans.xml
     * with different alternatives, interceptors, decorators, and scan exclusions.
     *
     * @param beansXml the parsed beans.xml configuration
     */
    public void addBeansXml(BeansXml beansXml) {
        if (beansXml == null) {
            throw new IllegalArgumentException("BeansXml cannot be null");
        }

        // Only add non-empty configurations to avoid clutter
        if (!beansXml.isEmpty()) {
            beansXmlConfigurations.add(beansXml);
            System.out.println("[KnowledgeBase] Registered beans.xml configuration: " + beansXml);
        }
    }

    /**
     * Gets all beans.xml configurations from all scanned archives.
     *
     * @return collection of BeansXml objects
     */
    public Collection<BeansXml> getBeansXmlConfigurations() {
        return Collections.unmodifiableCollection(beansXmlConfigurations);
    }

    /**
     * Checks if a class or stereotype is enabled as an alternative in any beans.xml.
     *
     * <p>CDI 4.1 Section 5.1.2: Alternatives can be enabled via:
     * <ul>
     *   <li>@Priority annotation on the class (preferred in CDI 4.1)</li>
     *   <li>beans.xml &lt;alternatives&gt; section (traditional method)</li>
     * </ul>
     *
     * @param className the fully qualified class name to check
     * @return true if the class/stereotype is declared in any beans.xml alternatives section
     */
    public boolean isAlternativeEnabledInBeansXml(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }

        for (BeansXml beansXml : beansXmlConfigurations) {
            if (beansXml.getAlternatives() != null) {
                // Check if it's listed as an alternative class
                if (beansXml.getAlternatives().getClasses().contains(className)) {
                    return true;
                }
                // Check if it's listed as an alternative stereotype
                if (beansXml.getAlternatives().getStereotypes().contains(className)) {
                    return true;
                }
            }
        }

        return false;
    }

    // ==================== Vetoed Types Management ====================

    /**
     * Marks a type as vetoed by an extension during ProcessAnnotatedType.
     * Vetoed types should not become beans.
     *
     * @param clazz the class to veto
     */
    public void vetoType(Class<?> clazz) {
        vetoedTypes.add(clazz);
    }

    /**
     * Checks if a type was vetoed by an extension.
     *
     * @param clazz the class to check
     * @return true if the type was vetoed
     */
    public boolean isTypeVetoed(Class<?> clazz) {
        return vetoedTypes.contains(clazz);
    }

    /**
     * Returns all vetoed types.
     *
     * @return set of vetoed types
     */
    public Set<Class<?>> getVetoedTypes() {
        return Collections.unmodifiableSet(vetoedTypes);
    }
}