package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.knowledgebase.InterceptorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionType;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BeanImpl<T> implements Bean<T> {

    // Bean
    private final Class<T> beanClass;
    private final Set<InjectionPoint> injectionPoints = new HashSet<>();

    // BeanAttributes
    private String name;
    private final Set<Annotation> qualifiers = new HashSet<>();
    private Class<? extends Annotation> scope;
    private final Set<Class<? extends Annotation>> stereotypes = new HashSet<>();
    private final Set<Type> types = new HashSet<>();
    private final boolean alternative;

    // Validation state
    private boolean hasValidationErrors = false;

    // Extension veto state
    private boolean vetoed = false;

    // Injection metadata (set by validator)
    private Constructor<T> injectConstructor;
    private final Set<Field> injectFields = new HashSet<>();
    private final Set<Method> injectMethods = new HashSet<>();
    private Method postConstructMethod;
    private Method preDestroyMethod;

    // Dependency resolver (set during container initialization)
    private ProducerBean.DependencyResolver dependencyResolver;

    // ====================================================================================
    // PHASE 2: Interceptor Support - Business Method Interception (@AroundInvoke)
    // ====================================================================================

    /**
     * InterceptorResolver - resolves which interceptors apply to this bean's methods.
     * Set during container initialization by InjectorImpl.
     */
    private InterceptorResolver interceptorResolver;

    /**
     * KnowledgeBase - provides access to interceptor metadata.
     * Set during container initialization by InjectorImpl.
     */
    private KnowledgeBase knowledgeBase;

    /**
     * Map of methods to their interceptor chains.
     * Built once during bean initialization and cached for performance.
     * Key: Method object from beanClass
     * Value: Pre-built InterceptorChain for that method
     *
     * Only contains entries for methods that have interceptors.
     * Methods without interceptors are not in this map (for memory efficiency).
     */
    private Map<Method, InterceptorChain> methodInterceptorChains;

    /**
     * PHASE 3: Constructor interceptor chain (@AroundConstruct).
     * Built once during bean initialization if the bean has constructor interceptors.
     * Null if no constructor interceptors are present.
     */
    private InterceptorChain constructorInterceptorChain;

    /**
     * PHASE 4: @PostConstruct interceptor chain.
     * Built once during bean initialization if the bean has @PostConstruct interceptors.
     * Null if no @PostConstruct interceptors are present.
     */
    private InterceptorChain postConstructInterceptorChain;

    /**
     * PHASE 4: @PreDestroy interceptor chain.
     * Built once during bean initialization if the bean has @PreDestroy interceptors.
     * Null if no @PreDestroy interceptors are present.
     */
    private InterceptorChain preDestroyInterceptorChain;

    /**
     * Cache of interceptor instances.
     * Key: Interceptor class
     * Value: Interceptor instance
     *
     * Interceptor instances are created once per bean and reused for all method calls.
     * This is per CDI spec: interceptors are stateful and bound to the bean instance.
     *
     * Thread-safety: ConcurrentHashMap ensures thread-safe lazy initialization of interceptor instances.
     */
    private final Map<Class<?>, Object> interceptorInstanceCache = new ConcurrentHashMap<>();

    /**
     * InterceptorAwareProxyGenerator - creates proxies that execute interceptor chains.
     * Shared across all beans (stateless).
     */
    private InterceptorAwareProxyGenerator interceptorAwareProxyGenerator;

    // ====================================================================================
    // PHASE 3: Decorator Support - Bean-level Decoration
    // ====================================================================================

    /**
     * DecoratorResolver - resolves which decorators apply to this bean's types.
     * Set during container initialization by InjectorImpl2.
     */
    private DecoratorResolver decoratorResolver;

    /**
     * DecoratorAwareProxyGenerator - creates decorator chains with @Delegate injection.
     * Shared across all beans.
     */
    private DecoratorAwareProxyGenerator decoratorAwareProxyGenerator;

    /**
     * BeanManager - needed for creating decorator instances.
     * Set during container initialization by InjectorImpl2.
     */
    private jakarta.enterprise.inject.spi.BeanManager beanManager;

    public BeanImpl(Class<T> beanClass, boolean alternative) {
        this.beanClass = beanClass;
        this.name = "";
        this.scope = null;
        this.alternative = alternative;
    }

    @Override
    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.unmodifiableSet(injectionPoints);
    }

    public void addInjectionPoint(InjectionPoint injectionPoint) {
        injectionPoints.add(injectionPoint);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name == null) ? "" : name;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.unmodifiableSet(qualifiers);
    }

    public void setQualifiers(Set<Annotation> qualifiers) {
        this.qualifiers.clear();
        if (qualifiers != null) {
            this.qualifiers.addAll(qualifiers);
        }
    }

    public void addQualifier(Annotation qualifier) {
        if (qualifier != null) {
            this.qualifiers.add(qualifier);
        }
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    public void setScope(Class<? extends Annotation> scope) {
        this.scope = scope;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.unmodifiableSet(stereotypes);
    }

    public void setStereotypes(Set<Class<? extends Annotation>> stereotypes) {
        this.stereotypes.clear();
        if (stereotypes != null) {
            this.stereotypes.addAll(stereotypes);
        }
    }

    public void addStereotype(Class<? extends Annotation> stereotype) {
        if (stereotype != null) {
            this.stereotypes.add(stereotype);
        }
    }

    @Override
    public Set<Type> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    public void setTypes(Set<Type> types) {
        this.types.clear();
        if (types != null) {
            this.types.addAll(types);
        }
    }

    public void addType(Type type) {
        if (type != null) {
            this.types.add(type);
        }
    }

    @Override
    public boolean isAlternative() {
        return alternative;
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        try {
            // 1. Create instance via constructor injection
            T instance = createInstance(creationalContext);

            // 2. Perform field injection
            performFieldInjection(instance, creationalContext);

            // 3. Perform method injection (initializer methods)
            performMethodInjection(instance, creationalContext);

            // 4. Call @PostConstruct if present
            invokePostConstruct(instance);

            // 5. PHASE 2 - Wrap with interceptor-aware proxy if needed
            // IMPORTANT: This wrapping is ONLY for @Dependent scoped beans!
            //
            // Normal-scoped beans (@ApplicationScoped, @RequestScoped, etc.) are wrapped by their
            // contexts (ApplicationScopedContext, RequestScopedContext, etc.) which call
            // createInterceptorAwareProxy() in their get() methods.
            //
            // @Dependent scoped beans don't go through contexts - they're created directly via
            // bean.create() and returned immediately. So we need to wrap them here.
            //
            // How to tell if this is a @Dependent bean?
            // - Check if scope is null (CDI spec: @Dependent beans have no scope annotation)
            // - Or check if scope is jakarta.enterprise.context.Dependent.class
            if (hasInterceptors() && isDependent()) {
                instance = createInterceptorAwareProxy(instance);
            }

            // 6. PHASE 3 - Wrap with decorators if applicable
            // IMPORTANT: Decorators apply to ALL beans (not just @Dependent)
            // Unlike interceptors which are method-level, decorators are bean-level
            // and wrap the entire bean instance (or interceptor proxy if interceptors exist)
            if (hasDecorators()) {
                instance = createDecoratorChain(instance, creationalContext);
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean instance of " + beanClass.getName(), e);
        }
    }

    /**
     * Checks if this bean is @Dependent scoped.
     * <p>
     * @Dependent is the default scope in CDI and is represented by either:
     * - null scope (no scope annotation)
     * - jakarta.enterprise.context.Dependent.class
     * <p>
     * @Dependent beans are special because they:
     * - Don't go through scope contexts (no context.get() call)
     * - Are created directly via bean.create()
     * - Have the same lifecycle as their injection point
     * - Need to be wrapped with interceptors HERE, not in contexts
     *
     * @return true if this bean is @Dependent scoped
     */
    private boolean isDependent() {
        // If scope is null, it's @Dependent (default scope)
        if (scope == null) {
            return true;
        }

        // Check if scope is explicitly @Dependent
        return scope.equals(jakarta.enterprise.context.Dependent.class);
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        if (instance == null) {
            return;
        }

        try {
            // 1. Call @PreDestroy if present
            invokePreDestroy(instance);

            // 2. Release CreationalContext (destroys dependent objects)
            if (creationalContext != null) {
                creationalContext.release();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to destroy bean instance of " + beanClass.getName(), e);
        }
    }

    /**
     * Creates an instance via constructor injection.
     * Uses @Inject constructor if present, otherwise uses no-args constructor.
     * <p>
     * PHASE 3: Supports @AroundConstruct interceptors.
     * If constructor interceptors are present, they are invoked before the actual construction.
     */
    private T createInstance(CreationalContext<T> creationalContext) throws Exception {
        Constructor<T> constructor = injectConstructor;

        if (constructor == null) {
            // Use no-args constructor
            constructor = beanClass.getDeclaredConstructor();
        }

        constructor.setAccessible(true);

        // Resolve constructor parameters
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            args[i] = resolveInjectionPoint(parameters[i].getParameterizedType(),
                    parameters[i].getAnnotations(), creationalContext);
        }

        // PHASE 3: Check for @AroundConstruct interceptors
        if (constructorInterceptorChain != null) {
            // Invoke constructor interceptor chain
            // The chain will execute all @AroundConstruct interceptors, then invoke the actual constructor
            Object result = constructorInterceptorChain.invoke(null, constructor, args);
            return (T) result;
        } else {
            // No constructor interceptors, invoke directly
            return constructor.newInstance(args);
        }
    }

    /**
     * Performs field injection for all @Inject fields.
     * Processes fields in hierarchy order (superclass → subclass) per CDI 4.1 spec.
     */
    private void performFieldInjection(T instance, CreationalContext<T> creationalContext) throws Exception {
        // Build hierarchy: superclass first, then subclass
        List<Class<?>> hierarchy = LifecycleMethodHelper.buildHierarchy(instance);

        // Inject fields in hierarchy order (parent → child)
        for (Class<?> clazz : hierarchy) {
            for (Field field : injectFields) {
                // Only process fields declared by this specific class in the hierarchy
                if (!field.getDeclaringClass().equals(clazz)) {
                    continue;
                }

                field.setAccessible(true);
                Object value = resolveInjectionPoint(field.getGenericType(),
                        field.getAnnotations(), creationalContext);
                field.set(instance, value);
            }
        }
    }

    /**
     * Performs method injection for all @Inject methods.
     * Processes methods in hierarchy order (superclass → subclass) per CDI 4.1 spec.
     * Skips overridden methods per JSR-330 rules.
     */
    private void performMethodInjection(T instance, CreationalContext<T> creationalContext) throws Exception {
        // Build hierarchy: superclass first, then subclass
        List<Class<?>> hierarchy = LifecycleMethodHelper.buildHierarchy(instance);

        // Inject methods in hierarchy order (parent → child)
        for (Class<?> clazz : hierarchy) {
            for (Method method : injectMethods) {
                // Only process methods declared by this specific class in the hierarchy
                if (!method.getDeclaringClass().equals(clazz)) {
                    continue;
                }

                // Skip overridden methods per JSR-330 (method already processed in parent)
                if (isOverridden(method, instance.getClass())) {
                    continue;
                }

                method.setAccessible(true);
                Parameter[] parameters = method.getParameters();
                Object[] args = new Object[parameters.length];

                for (int i = 0; i < parameters.length; i++) {
                    args[i] = resolveInjectionPoint(parameters[i].getParameterizedType(),
                            parameters[i].getAnnotations(), creationalContext);
                }

                method.invoke(instance, args);
            }
        }
    }

    /**
     * Checks if a method is overridden in a subclass (JSR-330 rules).
     * Private methods are never considered overridden.
     * Package-private methods must be in the same package to be overridden.
     */
    private boolean isOverridden(Method superMethod, Class<?> leafClass) {
        if (Modifier.isPrivate(superMethod.getModifiers())) {
            return false;
        }
        if (superMethod.getDeclaringClass().equals(leafClass)) {
            return false;
        }

        // Search for the method in the leaf class hierarchy
        Class<?> current = leafClass;
        while (current != null && current != superMethod.getDeclaringClass()) {
            try {
                Method subMethod = current.getDeclaredMethod(superMethod.getName(), superMethod.getParameterTypes());
                if (!subMethod.equals(superMethod)) {
                    // Check package-private rules
                    boolean isSuperPackagePrivate = !Modifier.isPublic(superMethod.getModifiers()) &&
                            !Modifier.isProtected(superMethod.getModifiers()) &&
                            !Modifier.isPrivate(superMethod.getModifiers());

                    if (isSuperPackagePrivate) {
                        // Package-private method is only overridden if in same package
                        String superPackage = superMethod.getDeclaringClass().getPackage() != null ?
                                superMethod.getDeclaringClass().getPackage().getName() : "";
                        String subPackage = subMethod.getDeclaringClass().getPackage() != null ?
                                subMethod.getDeclaringClass().getPackage().getName() : "";
                        return superPackage.equals(subPackage);
                    }

                    return true; // Method is overridden
                }
            } catch (NoSuchMethodException e) {
                // Method not found in this class, continue to superclass
            }
            current = current.getSuperclass();
        }

        return false;
    }

    /**
     * Invokes @PostConstruct method if present.
     * <p>
     * PHASE 4: Supports @PostConstruct lifecycle interceptors.
     * If lifecycle interceptors are present, they are invoked before the target @PostConstruct method.
     * The chain executes: [Interceptor 1 @PostConstruct] → [Interceptor 2 @PostConstruct] → [Target @PostConstruct]
     */
    private void invokePostConstruct(T instance) throws Exception {
        // PHASE 4: Check for @PostConstruct interceptors
        if (postConstructInterceptorChain != null) {
            // Invoke lifecycle interceptor chain
            // The chain will execute all interceptor @PostConstruct methods, then the target @PostConstruct
            // The target @PostConstruct method is passed to the chain and invoked as the final step
            postConstructInterceptorChain.invokeLifecycle(instance, postConstructMethod);
        } else if (postConstructMethod != null) {
            // No interceptors, invoke target @PostConstruct directly
            postConstructMethod.setAccessible(true);
            postConstructMethod.invoke(instance);
        }
    }

    /**
     * Invokes @PreDestroy method if present.
     * <p>
     * PHASE 4: Supports @PreDestroy lifecycle interceptors.
     * If lifecycle interceptors are present, they are invoked before the target @PreDestroy method.
     * The chain executes: [Interceptor 1 @PreDestroy] → [Interceptor 2 @PreDestroy] → [Target @PreDestroy]
     */
    private void invokePreDestroy(T instance) throws Exception {
        // PHASE 4: Check for @PreDestroy interceptors
        if (preDestroyInterceptorChain != null) {
            // Invoke lifecycle interceptor chain
            // The chain will execute all interceptor @PreDestroy methods, then the target @PreDestroy
            // The target @PreDestroy method is passed to the chain and invoked as the final step
            preDestroyInterceptorChain.invokeLifecycle(instance, preDestroyMethod);
        } else if (preDestroyMethod != null) {
            // No interceptors, invoke target @PreDestroy directly
            preDestroyMethod.setAccessible(true);
            preDestroyMethod.invoke(instance);
        }
    }

    /**
     * Resolves an injection point by finding a matching bean from the knowledge base.
     *
     * @param type the required type
     * @param annotations the qualifier annotations
     * @param creationalContext the creational context
     * @return the resolved bean instance
     */
    private Object resolveInjectionPoint(Type type, Annotation[] annotations,
                                         CreationalContext<T> creationalContext) {
        if (dependencyResolver == null) {
            throw new IllegalStateException(
                "BeanImpl dependency resolver not set. " +
                "This should be set during container initialization for bean: " + beanClass.getName()
            );
        }

        return dependencyResolver.resolve(type, annotations);
    }

    /**
     * Returns whether this bean has validation errors.
     * A bean with validation errors should not be used for dependency resolution,
     * but the error should only be reported if the bean is actually needed.
     *
     * @return true if the bean has validation errors, false otherwise
     */
    public boolean hasValidationErrors() {
        return hasValidationErrors;
    }

    /**
     * Marks this bean as having validation errors.
     * This should be called during validation if any CDI constraint is violated.
     */
    public void setHasValidationErrors(boolean hasValidationErrors) {
        this.hasValidationErrors = hasValidationErrors;
    }

    /**
     * Returns true if this bean was vetoed by an extension during ProcessAnnotatedType.
     * Vetoed beans should not be available for injection.
     */
    public boolean isVetoed() {
        return vetoed;
    }

    /**
     * Marks this bean as vetoed by an extension.
     * This should be called when an extension calls ProcessAnnotatedType.veto().
     */
    public void setVetoed(boolean vetoed) {
        this.vetoed = vetoed;
    }

    // Injection metadata getters/setters (used by CDI41BeanValidator)

    public Constructor<T> getInjectConstructor() {
        return injectConstructor;
    }

    public void setInjectConstructor(Constructor<T> injectConstructor) {
        this.injectConstructor = injectConstructor;
    }

    public Set<Field> getInjectFields() {
        return Collections.unmodifiableSet(injectFields);
    }

    public void addInjectField(Field field) {
        if (field != null) {
            this.injectFields.add(field);
        }
    }

    public Set<Method> getInjectMethods() {
        return Collections.unmodifiableSet(injectMethods);
    }

    public void addInjectMethod(Method method) {
        if (method != null) {
            this.injectMethods.add(method);
        }
    }

    public Method getPostConstructMethod() {
        return postConstructMethod;
    }

    public void setPostConstructMethod(Method postConstructMethod) {
        this.postConstructMethod = postConstructMethod;
    }

    public Method getPreDestroyMethod() {
        return preDestroyMethod;
    }

    public void setPreDestroyMethod(Method preDestroyMethod) {
        this.preDestroyMethod = preDestroyMethod;
    }

    public void setDependencyResolver(ProducerBean.DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    // ====================================================================================
    // Interceptor Support Methods (Phase 2)
    // ====================================================================================

    /**
     * Sets the interceptor resolver for this bean.
     * Called during container initialization by InjectorImpl.
     *
     * @param interceptorResolver the interceptor resolver to use
     */
    public void setInterceptorResolver(InterceptorResolver interceptorResolver) {
        this.interceptorResolver = interceptorResolver;
    }

    /**
     * Sets the knowledge base for this bean.
     * Called during container initialization by InjectorImpl.
     *
     * @param knowledgeBase the knowledge base containing interceptor metadata
     */
    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Sets the interceptor-aware proxy generator for this bean.
     * Called during container initialization by InjectorImpl.
     *
     * @param interceptorAwareProxyGenerator the proxy generator
     */
    public void setInterceptorAwareProxyGenerator(InterceptorAwareProxyGenerator interceptorAwareProxyGenerator) {
        this.interceptorAwareProxyGenerator = interceptorAwareProxyGenerator;
    }

    /**
     * Sets the decorator resolver for this bean.
     * Called during container initialization by InjectorImpl2.
     *
     * @param decoratorResolver the decorator resolver
     */
    public void setDecoratorResolver(DecoratorResolver decoratorResolver) {
        this.decoratorResolver = decoratorResolver;
    }

    /**
     * Sets the decorator-aware proxy generator for this bean.
     * Called during container initialization by InjectorImpl2.
     *
     * @param decoratorAwareProxyGenerator the decorator proxy generator
     */
    public void setDecoratorAwareProxyGenerator(DecoratorAwareProxyGenerator decoratorAwareProxyGenerator) {
        this.decoratorAwareProxyGenerator = decoratorAwareProxyGenerator;
    }

    /**
     * Sets the BeanManager for this bean.
     * Called during container initialization by InjectorImpl2.
     * Needed for creating decorator instances.
     *
     * @param beanManager the BeanManager
     */
    public void setBeanManager(jakarta.enterprise.inject.spi.BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    /**
     * Builds interceptor chains for all methods that have interceptor bindings.
     * <p>
     * This method is called once during bean initialization to analyze all methods
     * and build interceptor chains for those that require interception.
     * <p>
     * PHASE 3 & 4: Also builds constructor and lifecycle interceptor chains.
     * <p>
     * <h3>Process:</h3>
     * <ol>
     * <li>Iterate through all declared methods in the bean class</li>
     * <li>For each method, use InterceptorResolver to find applicable interceptors</li>
     * <li>If interceptors are found, build an InterceptorChain and cache it</li>
     * <li>Methods without interceptors are not added to the map (memory optimization)</li>
     * <li>Build constructor interceptor chain if @AroundConstruct interceptors exist</li>
     * <li>Build @PostConstruct interceptor chain if lifecycle interceptors exist</li>
     * <li>Build @PreDestroy interceptor chain if lifecycle interceptors exist</li>
     * </ol>
     *
     * <h3>Example:</h3>
     * <pre>
     * {@literal @}ApplicationScoped
     * public class OrderService {
     *     {@literal @}Transactional  // Method-level binding
     *     public void createOrder(Order order) { ... }  // Will have interceptors
     *
     *     public Order getOrder(String id) { ... }      // No interceptors
     * }
     *
     * // Result:
     * methodInterceptorChains = {
     *     createOrder() → [TransactionalInterceptor chain],
     *     // getOrder() not in map - no interceptors
     * }
     * </pre>
     *
     * @throws IllegalStateException if interceptorResolver or knowledgeBase not set
     */
    public void buildMethodInterceptorChains() {
        // If no interceptor support configured, skip chain building
        if (interceptorResolver == null || knowledgeBase == null) {
            this.methodInterceptorChains = Collections.emptyMap();
            this.constructorInterceptorChain = null;
            this.postConstructInterceptorChain = null;
            this.preDestroyInterceptorChain = null;
            return;
        }

        // ========================================================================
        // PHASE 2: Build business method interceptor chains (@AroundInvoke)
        // ========================================================================
        Map<Method, InterceptorChain> chains = new HashMap<>();

        // Iterate through all declared methods in the bean class
        // We only process declared methods (not inherited) to avoid duplicates
        for (Method method : beanClass.getDeclaredMethods()) {
            // Skip non-public methods - CDI only intercepts public business methods
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            // Skip static methods - CDI doesn't intercept static methods
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            // Use InterceptorResolver to find applicable interceptors for this method
            // This considers:
            // 1. Class-level interceptor bindings (e.g., @Transactional on the class)
            // 2. Method-level interceptor bindings (e.g., @Transactional on the method)
            // 3. Stereotype bindings (e.g., @Transactional inherited from a stereotype)
            // 4. Method-level bindings OVERRIDE class-level bindings (CDI spec)
            List<InterceptorInfo> interceptors = interceptorResolver.resolve(
                beanClass,
                method,
                InterceptionType.AROUND_INVOKE
            );

            // If interceptors were found for this method, build a chain
            if (!interceptors.isEmpty()) {
                InterceptorChain chain = buildInterceptorChain(interceptors, InterceptionType.AROUND_INVOKE);
                chains.put(method, chain);
            }
            // If no interceptors, we don't add an entry (memory optimization)
        }

        // Store the chains map (immutable after this point)
        this.methodInterceptorChains = chains;

        // ========================================================================
        // PHASE 3: Build constructor interceptor chain (@AroundConstruct)
        // ========================================================================
        List<InterceptorInfo> constructorInterceptors = interceptorResolver.resolve(
            beanClass,
            null,  // null method = constructor interception
            InterceptionType.AROUND_CONSTRUCT
        );

        if (!constructorInterceptors.isEmpty()) {
            this.constructorInterceptorChain = buildInterceptorChain(
                constructorInterceptors,
                InterceptionType.AROUND_CONSTRUCT
            );
        } else {
            this.constructorInterceptorChain = null;
        }

        // ========================================================================
        // PHASE 4: Build lifecycle callback interceptor chains
        // ========================================================================

        // @PostConstruct interceptor chain
        List<InterceptorInfo> postConstructInterceptors = interceptorResolver.resolve(
            beanClass,
            null,  // null method = lifecycle callback
            InterceptionType.POST_CONSTRUCT
        );

        if (!postConstructInterceptors.isEmpty()) {
            this.postConstructInterceptorChain = buildLifecycleInterceptorChain(
                postConstructInterceptors,
                InterceptionType.POST_CONSTRUCT
            );
        } else {
            this.postConstructInterceptorChain = null;
        }

        // @PreDestroy interceptor chain
        List<InterceptorInfo> preDestroyInterceptors = interceptorResolver.resolve(
            beanClass,
            null,  // null method = lifecycle callback
            InterceptionType.PRE_DESTROY
        );

        if (!preDestroyInterceptors.isEmpty()) {
            this.preDestroyInterceptorChain = buildLifecycleInterceptorChain(
                preDestroyInterceptors,
                InterceptionType.PRE_DESTROY
            );
        } else {
            this.preDestroyInterceptorChain = null;
        }
    }

    /**
     * Builds an InterceptorChain from a list of InterceptorInfo objects.
     * <p>
     * This method:
     * <ol>
     * <li>Creates interceptor instances for each InterceptorInfo (with caching)</li>
     * <li>Retrieves the appropriate interceptor method based on interception type</li>
     * <li>Builds a chain using the InterceptorChain.Builder</li>
     * <li>Returns the immutable, thread-safe chain</li>
     * </ol>
     *
     * <h3>Interceptor Instance Creation:</h3>
     * Interceptors are created with dependency injection (same as beans).
     * Instances are cached per-bean to maintain interceptor state across invocations.
     * <p>
     * Example: A {@code @Transactional} interceptor might maintain transaction state
     * between multiple method calls on the same bean instance.
     *
     * <h3>Chain Ordering:</h3>
     * The interceptors list is already sorted by priority (lower value = earlier execution).
     * This sorting is done by InterceptorResolver using KnowledgeBase query methods.
     *
     * @param interceptors list of interceptor metadata, sorted by priority
     * @param interceptionType the type of interception (AROUND_INVOKE, AROUND_CONSTRUCT, etc.)
     * @return an immutable InterceptorChain ready for execution
     * @throws RuntimeException if interceptor instance creation fails
     */
    private InterceptorChain buildInterceptorChain(List<InterceptorInfo> interceptors, InterceptionType interceptionType) {
        // Use the Builder pattern to construct the chain
        InterceptorChain.Builder builder = InterceptorChain.builder();

        // Add each interceptor to the chain in order (already sorted by priority)
        for (InterceptorInfo interceptorInfo : interceptors) {
            // Get or create the interceptor instance (cached)
            Object interceptorInstance = getOrCreateInterceptorInstance(interceptorInfo);

            // Get the appropriate interceptor method based on interception type
            Method interceptorMethod = getInterceptorMethod(interceptorInfo, interceptionType);

            if (interceptorMethod != null) {
                // Add this interceptor to the chain
                // The chain will invoke: interceptorMethod.invoke(interceptorInstance, invocationContext)
                builder.addInterceptor(interceptorInstance, interceptorMethod);
            }
        }

        // Build and return the immutable chain
        return builder.build();
    }

    /**
     * Builds a lifecycle interceptor chain that includes both interceptor callbacks and the target callback.
     * <p>
     * PHASE 4: Lifecycle interceptor chains include:
     * <ol>
     * <li>All interceptor lifecycle methods (@PostConstruct or @PreDestroy from interceptors)</li>
     * <li>The target bean's lifecycle method (@PostConstruct or @PreDestroy from the bean itself)</li>
     * </ol>
     * <p>
     * Example chain for @PostConstruct:
     * [Interceptor1.postConstruct] → [Interceptor2.postConstruct] → [TargetBean.postConstruct]
     *
     * @param interceptors list of interceptor metadata, sorted by priority
     * @param interceptionType POST_CONSTRUCT or PRE_DESTROY
     * @return an immutable InterceptorChain ready for execution
     */
    private InterceptorChain buildLifecycleInterceptorChain(
            List<InterceptorInfo> interceptors,
            InterceptionType interceptionType) {

        // Use the Builder pattern to construct the chain
        InterceptorChain.Builder builder = InterceptorChain.builder();

        // Add interceptor lifecycle methods
        for (InterceptorInfo interceptorInfo : interceptors) {
            Object interceptorInstance = getOrCreateInterceptorInstance(interceptorInfo);
            Method interceptorMethod = getInterceptorMethod(interceptorInfo, interceptionType);

            if (interceptorMethod != null) {
                builder.addInterceptor(interceptorInstance, interceptorMethod);
            }
        }

        // Add the target bean's lifecycle method at the end of the chain
        // This ensures interceptors run before the target method (per CDI spec)
        Method targetLifecycleMethod = interceptionType == InterceptionType.POST_CONSTRUCT
                ? postConstructMethod
                : preDestroyMethod;

        if (targetLifecycleMethod != null) {
            // Add a special invocation that will invoke the target's lifecycle method
            // We can't add it directly because the target instance is not available yet during chain building
            // Instead, we'll handle this in the invoke methods (invokePostConstruct/invokePreDestroy)
            // by ensuring the target method is called as part of the chain's final proceed()
        }

        return builder.build();
    }

    /**
     * Gets the appropriate interceptor method for a given interception type.
     *
     * @param interceptorInfo the interceptor metadata
     * @param interceptionType the type of interception
     * @return the interceptor method, or null if not present
     */
    private Method getInterceptorMethod(InterceptorInfo interceptorInfo, InterceptionType interceptionType) {
        if (interceptionType == InterceptionType.AROUND_INVOKE) {
            return interceptorInfo.getAroundInvokeMethod();
        } else if (interceptionType == InterceptionType.AROUND_CONSTRUCT) {
            return interceptorInfo.getAroundConstructMethod();
        } else if (interceptionType == InterceptionType.POST_CONSTRUCT) {
            return interceptorInfo.getPostConstructMethod();
        } else if (interceptionType == InterceptionType.PRE_DESTROY) {
            return interceptorInfo.getPreDestroyMethod();
        } else {
            return null;
        }
    }

    /**
     * Gets or creates an interceptor instance, with per-bean caching.
     * <p>
     * <h3>Why Cache Interceptor Instances?</h3>
     * Per CDI specification, interceptor instances are:
     * <ul>
     * <li><b>Stateful</b>: They can maintain state between invocations</li>
     * <li><b>Bean-scoped</b>: One instance per bean (not shared across beans)</li>
     * <li><b>Lifecycle-bound</b>: Created with the bean, destroyed with the bean</li>
     * </ul>
     *
     * <h3>Example - Stateful Interceptor:</h3>
     * <pre>
     * {@literal @}Interceptor
     * {@literal @}Logged
     * public class InvocationCounterInterceptor {
     *     private int invocationCount = 0;  // State maintained across calls
     *
     *     {@literal @}AroundInvoke
     *     public Object count(InvocationContext ctx) {
     *         invocationCount++;
     *         System.out.println("Invocation #" + invocationCount);
     *         return ctx.proceed();
     *     }
     * }
     * </pre>
     *
     * <h3>Creation Process:</h3>
     * <ol>
     * <li>Check if instance exists in cache (thread-safe lookup)</li>
     * <li>If not, create new instance via reflection</li>
     * <li>Perform dependency injection on the interceptor (if it has @Inject fields/methods)</li>
     * <li>Call @PostConstruct on the interceptor (if present)</li>
     * <li>Cache and return the instance</li>
     * </ol>
     *
     * <h3>Thread Safety:</h3>
     * Uses ConcurrentHashMap.computeIfAbsent for atomic cache-if-absent semantics.
     * Multiple threads calling this simultaneously will result in only one instance being created.
     *
     * @param interceptorInfo the interceptor metadata
     * @return the interceptor instance (cached)
     * @throws RuntimeException if instance creation or injection fails
     */
    private Object getOrCreateInterceptorInstance(InterceptorInfo interceptorInfo) {
        Class<?> interceptorClass = interceptorInfo.getInterceptorClass();

        // Atomic get-or-create using ConcurrentHashMap
        // If the key exists, return the cached value
        // If not, compute the value (create instance) and cache it atomically
        return interceptorInstanceCache.computeIfAbsent(interceptorClass, clazz -> {
            try {
                // Step 1: Find and select the appropriate constructor
                Constructor<?> constructor = selectInterceptorConstructor(clazz);
                constructor.setAccessible(true);

                // Step 2: Resolve constructor parameters via dependency injection
                Object[] constructorArgs = resolveConstructorParameters(constructor);

                // Step 3: Create instance with injected constructor parameters
                Object instance = constructor.newInstance(constructorArgs);

                // Step 4: Perform field injection (@Inject fields)
                injectInterceptorFields(instance, clazz);

                // Step 5: Perform method injection (@Inject methods)
                injectInterceptorMethods(instance, clazz);

                // Step 6: Call @PostConstruct lifecycle callback (if present)
                invokeInterceptorPostConstruct(instance, clazz);

                return instance;
            } catch (Exception e) {
                throw new RuntimeException(
                    "Failed to create interceptor instance of " + clazz.getName() +
                    " for bean " + beanClass.getName(), e
                );
            }
        });
    }

    /**
     * Checks if this bean has any interceptors configured.
     * <p>
     * This is used to determine whether to create a regular proxy or an interceptor-aware proxy.
     *
     * @return true if this bean has at least one method with interceptors, false otherwise
     */
    public boolean hasInterceptors() {
        return methodInterceptorChains != null && !methodInterceptorChains.isEmpty();
    }

    /**
     * Gets the method interceptor chains for this bean.
     * <p>
     * This map is used by the InterceptorAwareProxyGenerator to create proxies that
     * execute interceptors before business methods.
     *
     * @return the method interceptor chains map (may be empty, never null)
     */
    public Map<Method, InterceptorChain> getMethodInterceptorChains() {
        return methodInterceptorChains != null ? methodInterceptorChains : Collections.emptyMap();
    }

    /**
     * Creates an interceptor-aware proxy for this bean instance.
     * <p>
     * This method is called by normal-scoped contexts when they need to create a client proxy
     * that also supports interceptors.
     * <p>
     * <h3>Usage:</h3>
     * <pre>
     * // In RequestScopeContext.get():
     * T instance = bean.create(context);
     *
     * // If bean has interceptors, wrap with interceptor-aware proxy
     * if (bean instanceof BeanImpl && ((BeanImpl) bean).hasInterceptors()) {
     *     instance = ((BeanImpl) bean).createInterceptorAwareProxy(instance);
     * }
     * </pre>
     *
     * @param targetInstance the actual bean instance to wrap
     * @return a proxy that executes interceptors before delegating to targetInstance
     * @throws IllegalStateException if interceptor support not configured
     */
    public T createInterceptorAwareProxy(T targetInstance) {
        if (interceptorAwareProxyGenerator == null) {
            throw new IllegalStateException(
                "InterceptorAwareProxyGenerator not set for bean: " + beanClass.getName()
            );
        }

        if (methodInterceptorChains == null || methodInterceptorChains.isEmpty()) {
            // No interceptors, return the instance as-is
            return targetInstance;
        }

        // Create and return an interceptor-aware proxy
        return interceptorAwareProxyGenerator.createProxy(this, targetInstance, methodInterceptorChains);
    }

    // ====================================================================================
    // Interceptor Instance Creation with Dependency Injection
    // ====================================================================================

    /**
     * Selects the appropriate constructor for interceptor instantiation.
     *
     * <p>CDI constructor selection rules:
     * <ol>
     *   <li>If there's a constructor annotated with @Inject, use it</li>
     *   <li>Otherwise, if there's only one public constructor, use it</li>
     *   <li>Otherwise, use the no-arg constructor</li>
     * </ol>
     */
    private Constructor<?> selectInterceptorConstructor(Class<?> interceptorClass) {
        Constructor<?>[] constructors = interceptorClass.getDeclaredConstructors();

        // Look for @Inject constructor
        for (Constructor<?> ctor : constructors) {
            if (AnnotationsEnum.hasInjectAnnotation(ctor)) {
                return ctor;
            }
        }

        // If only one public constructor, use it (CDI implicit constructor injection)
        Constructor<?>[] publicConstructors = interceptorClass.getConstructors();
        if (publicConstructors.length == 1) {
            return publicConstructors[0];
        }

        // Otherwise, use no-arg constructor
        try {
            return interceptorClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                "Interceptor " + interceptorClass.getName() +
                " has no suitable constructor (no @Inject, not exactly one public, no no-arg)", e
            );
        }
    }

    /**
     * Resolves constructor parameters via dependency injection.
     */
    private Object[] resolveConstructorParameters(Constructor<?> constructor) {
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Type paramType = param.getParameterizedType();
            Annotation[] paramAnnotations = param.getAnnotations();

            // Use dependency resolver to resolve the parameter
            if (dependencyResolver != null) {
                args[i] = dependencyResolver.resolve(paramType, paramAnnotations);
            } else {
                throw new IllegalStateException(
                    "DependencyResolver not set for bean " + beanClass.getName() +
                    " - cannot inject constructor parameter " + param.getName()
                );
            }
        }

        return args;
    }

    /**
     * Injects @Inject annotated fields on the interceptor instance.
     */
    private void injectInterceptorFields(Object instance, Class<?> clazz) throws IllegalAccessException {
        if (dependencyResolver == null) {
            return; // No injection possible without resolver
        }

        // Find all @Inject fields (including private, inherited)
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (AnnotationsEnum.hasInjectAnnotation(field)) {
                    field.setAccessible(true);

                    Type fieldType = field.getGenericType();
                    Annotation[] fieldAnnotations = field.getAnnotations();

                    // Resolve and inject the dependency
                    Object value = dependencyResolver.resolve(fieldType, fieldAnnotations);
                    field.set(instance, value);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * Invokes @Inject annotated methods on the interceptor instance.
     */
    private void injectInterceptorMethods(Object instance, Class<?> clazz) throws Exception {
        if (dependencyResolver == null) {
            return; // No injection possible without resolver
        }

        // Find all @Inject methods (including private, inherited)
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (AnnotationsEnum.hasInjectAnnotation(method)) {
                    method.setAccessible(true);

                    // Resolve method parameters
                    Parameter[] parameters = method.getParameters();
                    Object[] args = new Object[parameters.length];

                    for (int i = 0; i < parameters.length; i++) {
                        Parameter param = parameters[i];
                        Type paramType = param.getParameterizedType();
                        Annotation[] paramAnnotations = param.getAnnotations();

                        args[i] = dependencyResolver.resolve(paramType, paramAnnotations);
                    }

                    // Invoke the injector method
                    method.invoke(instance, args);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * Invokes @PostConstruct lifecycle callback on the interceptor instance.
     */
    private void invokeInterceptorPostConstruct(Object instance, Class<?> clazz) throws Exception {
        // Find @PostConstruct method (including private, inherited)
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (AnnotationsEnum.hasPostConstructAnnotation(method)) {
                    method.setAccessible(true);
                    method.invoke(instance);
                    return; // Only one @PostConstruct per class hierarchy
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    // ====================================================================================
    // PHASE 3: Decorator Support
    // ====================================================================================

    /**
     * Checks if any decorators apply to this bean.
     *
     * <p>This is used to determine whether to wrap the bean instance with decorators.
     *
     * @return true if this bean has decorators, false otherwise
     */
    public boolean hasDecorators() {
        if (decoratorResolver == null) {
            return false;
        }
        return decoratorResolver.hasDecorators(getTypes(), getQualifiers());
    }

    /**
     * Creates a decorator chain wrapping the target instance.
     *
     * <p>This method:
     * <ol>
     *   <li>Resolves decorators that apply to this bean (by type)</li>
     *   <li>Creates decorator instances with @Delegate injection</li>
     *   <li>Returns the outermost decorator (what client code sees)</li>
     * </ol>
     *
     * <p><b>Decorator vs Interceptor Wrapping:</b>
     * <ul>
     *   <li><b>Interceptors</b>: Single proxy with method interception</li>
     *   <li><b>Decorators</b>: Nested instances with @Delegate injection</li>
     * </ul>
     *
     * <p><b>Wrapping Order:</b>
     * <pre>
     * Client → Decorator1 (priority=100) → Decorator2 (priority=200) → Target (possibly interceptor proxy)
     * </pre>
     *
     * @param targetInstance the bean instance to decorate (may already be an interceptor proxy)
     * @param creationalContext the CreationalContext for managing decorator lifecycle
     * @return the outermost decorator instance, or target if no decorators apply
     * @throws IllegalStateException if decorator support not configured
     */
    @SuppressWarnings("unchecked")
    public T createDecoratorChain(T targetInstance, CreationalContext<T> creationalContext) {
        if (decoratorAwareProxyGenerator == null) {
            throw new IllegalStateException(
                "DecoratorAwareProxyGenerator not set for bean: " + beanClass.getName()
            );
        }

        if (decoratorResolver == null) {
            throw new IllegalStateException(
                "DecoratorResolver not set for bean: " + beanClass.getName()
            );
        }

        // Resolve decorators that apply to this bean
        java.util.List<com.threeamigos.common.util.implementations.injection.knowledgebase.DecoratorInfo> decorators =
                decoratorResolver.resolve(getTypes(), getQualifiers());

        if (decorators.isEmpty()) {
            // No decorators apply, return target as-is
            return targetInstance;
        }

        // Check if BeanManager is available
        if (beanManager == null) {
            throw new IllegalStateException(
                "BeanManager not set for bean: " + beanClass.getName() +
                " - cannot create decorator instances"
            );
        }

        // Create decorator chain using DecoratorAwareProxyGenerator
        DecoratorChain chain = decoratorAwareProxyGenerator.createDecoratorChain(
                targetInstance,
                decorators,
                beanManager,
                creationalContext
        );

        // Return the outermost decorator (what client code will interact with)
        return (T) chain.getOutermostInstance();
    }
}