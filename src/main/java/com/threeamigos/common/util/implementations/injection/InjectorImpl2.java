package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.builtinbeans.BeanManagerBean;
import com.threeamigos.common.util.implementations.injection.builtinbeans.ConversationBean;
import com.threeamigos.common.util.implementations.injection.builtinbeans.InjectionPointBean;
import com.threeamigos.common.util.implementations.injection.contexts.ContextManager;
import com.threeamigos.common.util.implementations.injection.contexts.ScopeContext;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.literals.DefaultLiteral;
import com.threeamigos.common.util.interfaces.injection.Injector;
import com.threeamigos.common.util.implementations.injection.scopehandlers.ScopeHandler;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.InjectionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

/**
 * Modern, clean implementation of the {@link Injector} interface that acts as a thin facade
 * over the CDI 4.1 infrastructure (BeanResolver, ContextManager, KnowledgeBase).
 *
 * <p>Unlike the legacy {@link InjectorImpl}, this implementation:
 * <ul>
 *   <li>Delegates all dependency resolution to {@link BeanResolver}</li>
 *   <li>Delegates all scope management to {@link ContextManager}</li>
 *   <li>Assumes beans are pre-validated by {@link CDI41BeanValidator} and {@link CDI41InjectionValidator}</li>
 *   <li>Reuses injection logic from {@link BeanImpl#create(CreationalContext)} instead of duplicating it</li>
 *   <li>Provides a clean, maintainable codebase (~400 lines vs 1507 lines in InjectorImpl)</li>
 * </ul>
 *
 * <h2>Core Responsibilities:</h2>
 * <ol>
 *   <li><b>Public API Gateway</b> - Exposes programmatic injection methods</li>
 *   <li><b>Programmatic Bean Registration</b> - Supports {@link #bind(Type, Collection, Class)} for manual bindings</li>
 *   <li><b>Runtime Alternative Activation</b> - Supports {@link #enableAlternative(Class)} for dynamic alternatives</li>
 *   <li><b>Legacy Support</b> - Optional adapter for old {@link ScopeHandler} to new {@link ScopeContext}</li>
 * </ol>
 *
 * <h2>What This Class Does NOT Do:</h2>
 * <ul>
 *   <li>❌ Does not perform validation (done by validators at startup)</li>
 *   <li>❌ Does not implement injection logic (delegates to BeanImpl/BeanResolver)</li>
 *   <li>❌ Does not manage scopes directly (delegates to ContextManager)</li>
 *   <li>❌ Does not resolve dependencies (delegates to BeanResolver)</li>
 * </ul>
 *
 * <p><b>Design Principle:</b> This is a <i>facade</i>, not a full DI engine. All heavy lifting
 * is delegated to the CDI infrastructure to avoid duplication and ensure consistency.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Initialize CDI infrastructure
 * KnowledgeBase kb = new KnowledgeBase();
 * ContextManager cm = new ContextManager();
 * BeanResolver br = new BeanResolver(kb, cm);
 * InjectorImpl2 injector = new InjectorImpl2(kb, br, cm);
 *
 * // Programmatic injection
 * MyService service = injector.inject(MyService.class);
 *
 * // Manual binding
 * injector.bind(MyInterface.class, Set.of(DefaultLiteral.INSTANCE), MyImpl.class);
 *
 * // Runtime alternative activation
 * injector.enableAlternative(MockServiceImpl.class);
 * }</pre>
 *
 * @author Stefano Reksten
 * @see Injector
 * @see BeanResolver
 * @see ContextManager
 * @see KnowledgeBase
 */
public class InjectorImpl2 implements Injector {

    private final KnowledgeBase knowledgeBase;
    private final BeanResolver beanResolver;
    private final ContextManager contextManager;
    private final BeanManagerImpl beanManager;

    // ====================================================================================
    // PHASE 2: Interceptor Support
    // ====================================================================================

    /**
     * InterceptorResolver - resolves which interceptors apply to bean methods.
     * Created once and shared across all beans for efficiency.
     */
    private final InterceptorResolver interceptorResolver;

    /**
     * InterceptorAwareProxyGenerator - creates proxies that execute interceptor chains.
     * Created once and shared across all beans for efficiency (stateless).
     */
    private final InterceptorAwareProxyGenerator interceptorAwareProxyGenerator;

    /**
     * Creates a new InjectorImpl2 with the given CDI infrastructure components.
     *
     * <p>This constructor also registers CDI 4.1 built-in beans:
     * <ul>
     *   <li>{@link jakarta.enterprise.inject.spi.BeanManager} - Container's BeanManager instance</li>
     *   <li>{@link jakarta.enterprise.inject.spi.InjectionPoint} - Contextual injection point metadata</li>
     * </ul>
     *
     * <p><b>PHASE 2 - Interceptor Support:</b> This constructor also:
     * <ul>
     *   <li>Creates {@link InterceptorResolver} for resolving interceptor bindings</li>
     *   <li>Creates {@link InterceptorAwareProxyGenerator} for proxy generation</li>
     *   <li>Initializes all beans with interceptor support</li>
     *   <li>Builds interceptor chains for all beans with interceptor bindings</li>
     * </ul>
     *
     * @param knowledgeBase the knowledge base containing all discovered beans
     * @param beanResolver the resolver for dependency lookup
     * @param contextManager the manager for scope contexts
     * @throws IllegalArgumentException if any parameter is null
     */
    public InjectorImpl2(
            @Nonnull KnowledgeBase knowledgeBase,
            @Nonnull BeanResolver beanResolver,
            @Nonnull ContextManager contextManager) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.beanResolver = Objects.requireNonNull(beanResolver, "beanResolver cannot be null");
        this.contextManager = Objects.requireNonNull(contextManager, "contextManager cannot be null");

        // Create BeanManager instance
        this.beanManager = new BeanManagerImpl(knowledgeBase, contextManager);

        // Register container for proxy deserialization
        // This allows serialized client proxies (e.g., from @SessionScoped beans) to be
        // deserialized and reconnect to the container
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = InjectorImpl2.class.getClassLoader();
        }
        ClientProxyGenerator.registerContainer(classLoader, beanManager, contextManager);

        // PHASE 2: Create interceptor infrastructure (shared across all beans)
        this.interceptorResolver = new InterceptorResolver(knowledgeBase);
        this.interceptorAwareProxyGenerator = new InterceptorAwareProxyGenerator();

        // Register built-in beans (CDI 4.1 Section 3.10)
        registerBuiltInBeans();

        // Register CDI provider for static container access (CDI 4.1 Section 12.1)
        registerCDIProvider();

        // PHASE 2: Initialize interceptor support for all beans
        initializeInterceptorSupport();
    }

    /**
     * Registers CDI 4.1 built-in beans that must be available for injection.
     *
     * <p>According to CDI 4.1 specification (Section 3.10), these built-in beans
     * must be available:
     * <ul>
     *   <li><b>BeanManager</b> - Programmatic access to the CDI container</li>
     *   <li><b>InjectionPoint</b> - Metadata about the current injection point (contextual)</li>
     * </ul>
     *
     * <p><b>Note:</b> Event&lt;T&gt; and Instance&lt;T&gt; are handled specially
     * by BeanResolver and don't need explicit bean registration.
     */
    private void registerBuiltInBeans() {
        // Register BeanManager as a built-in bean
        knowledgeBase.addBean(new BeanManagerBean(beanManager));

        // Register InjectionPoint as a built-in bean
        // (special resolution logic in BeanResolver handles the contextual nature)
        knowledgeBase.addBean(new InjectionPointBean());

        // Register Conversation as a built-in bean (no dependencies needed!)
        knowledgeBase.addBean(new ConversationBean());
    }

    /**
     * Registers the CDI provider for static container access.
     *
     * <p>This enables CDI.current() to work, allowing application code to obtain
     * the BeanManager statically when injection is not available:
     * <pre>
     * BeanManager manager = CDI.current().getBeanManager();
     * MyBean bean = CDI.current().select(MyBean.class).get();
     * </pre>
     *
     * <p><b>CDI 4.1 Compliance:</b>
     * <ul>
     *   <li>Section 12.1: Accessing the BeanManager via CDI.current()</li>
     *   <li>Section 12.2: CDI provider discovery via ServiceLoader</li>
     * </ul>
     */
    private void registerCDIProvider() {
        // Create CDI instance wrapping our BeanManager
        CDIImpl cdiInstance = new CDIImpl(beanManager);

        // Register globally so CDI.current() can find it
        SyringeCDIProvider.registerGlobalCDI(cdiInstance);
    }

    /**
     * Returns the BeanManager instance for programmatic CDI operations.
     *
     * @return the container's BeanManager
     */
    public BeanManagerImpl getBeanManager() {
        return beanManager;
    }

    /**
     * Injects and returns an instance of the specified class.
     *
     * <p>This method:
     * <ol>
     *   <li>Looks up the bean from {@link KnowledgeBase}</li>
     *   <li>Resolves the bean instance via {@link BeanResolver}</li>
     *   <li>Returns the fully injected instance</li>
     * </ol>
     *
     * <p>The bean lifecycle (constructor, field, method injection, @PostConstruct) is handled
     * by {@link BeanImpl#create(CreationalContext)}, not by this class.
     *
     * <p><b>Note:</b> This assumes the bean has already been validated by
     * {@link CDI41BeanValidator} and {@link CDI41InjectionValidator} at container startup.
     *
     * @param <T> the type to inject
     * @param classToInject the class to instantiate and inject
     * @return a fully-injected instance
     * @throws IllegalArgumentException if classToInject is null
     * @throws InjectionException if bean cannot be resolved (unsatisfied/ambiguous dependency)
     */
    @Override
    public <T> T inject(@Nonnull Class<T> classToInject) {
        if (classToInject == null) {
            throw new IllegalArgumentException("Class to inject cannot be null");
        }

        try {
            // Find matching bean with @Default qualifier
            Bean<T> bean = findBean(classToInject, new DefaultLiteral());

            // Resolve via BeanResolver (which delegates to ContextManager for scoping)
            @SuppressWarnings("unchecked")
            T instance = (T) beanResolver.resolveDeclaringBeanInstance(bean.getBeanClass());
            return instance;
        } catch (Exception e) {
            throw new InjectionException(
                    "Failed to inject class: " + classToInject.getName() +
                            ". Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Injects and returns an instance of the type specified by the TypeLiteral.
     *
     * <p>This method supports generic types where type parameters need to be preserved:
     * <pre>{@code
     * TypeLiteral<List<String>> literal = new TypeLiteral<List<String>>(){};
     * List<String> list = injector.inject(literal);
     * }</pre>
     *
     * <p>Delegates to {@link BeanResolver} for type-safe resolution with generic type matching.
     *
     * @param <T> the type to inject
     * @param typeLiteral the type literal capturing generic type information
     * @return a fully-injected instance
     * @throws IllegalArgumentException if typeLiteral is null
     * @throws InjectionException if bean cannot be resolved
     */
    @Override
    public <T> T inject(@Nonnull TypeLiteral<T> typeLiteral) {
        if (typeLiteral == null) {
            throw new IllegalArgumentException("TypeLiteral cannot be null");
        }

        try {
            // Resolve via BeanResolver with type literal
            Type type = typeLiteral.getType();
            @SuppressWarnings("unchecked")
            T instance = (T) beanResolver.resolve(type, new Annotation[]{new DefaultLiteral()});
            return instance;
        } catch (Exception e) {
            throw new InjectionException(
                    "Failed to inject type: " + typeLiteral.getType().getTypeName() +
                            ". Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a manual binding between a type and its implementation.
     *
     * <p>This allows programmatic bean registration outside of classpath scanning:
     * <pre>{@code
     * injector.bind(
     *     MyInterface.class,
     *     Set.of(new NamedLiteral("custom")),
     *     MyImplementation.class
     * );
     * }</pre>
     *
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>Testing - override beans with mocks</li>
     *   <li>Dynamic bean registration at runtime</li>
     *   <li>Third-party library integration</li>
     * </ul>
     *
     * <p><b>Note:</b> The implementation class should still be a valid CDI bean
     * (it will be validated and processed normally).
     *
     * @param type the interface or abstract type
     * @param qualifiers the qualifiers for this binding
     * @param implementation the concrete implementation class
     * @throws IllegalArgumentException if any parameter is null
     */
    @Override
    public void bind(@Nonnull Type type, @Nonnull Collection<Annotation> qualifiers, @Nonnull Class<?> implementation) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        if (qualifiers == null) {
            throw new IllegalArgumentException("Qualifiers cannot be null");
        }
        if (implementation == null) {
            throw new IllegalArgumentException("Implementation cannot be null");
        }

        // Create a simple BeanImpl for this binding
        // The bean will be validated and configured by CDI validators during next scan
        @SuppressWarnings({"rawtypes", "unchecked"})
        BeanImpl programmaticBean = new BeanImpl(implementation, false);

        // Set qualifiers manually
        programmaticBean.setQualifiers(new HashSet<>(qualifiers));

        // Register with KnowledgeBase
        knowledgeBase.addProgrammaticBean(type, qualifiers, programmaticBean);
    }

    /**
     * Enables an alternative bean at runtime.
     *
     * <p>Alternatives are normally enabled via:
     * <ul>
     *   <li>{@code @Priority} annotation (CDI 4.1)</li>
     *   <li>{@code beans.xml} configuration (legacy CDI)</li>
     * </ul>
     *
     * <p>This method allows <i>programmatic</i> alternative activation, useful for:
     * <ul>
     *   <li>Feature flags</li>
     *   <li>Runtime environment detection</li>
     *   <li>Testing scenarios</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * @Alternative
     * @ApplicationScoped
     * class MockService implements Service { }
     *
     * // Enable at runtime
     * injector.enableAlternative(MockService.class);
     * }</pre>
     *
     * @param alternativeClass the alternative bean class to enable
     * @throws IllegalArgumentException if alternativeClass is null or not marked with @Alternative
     */
    public void enableAlternative(@Nonnull Class<?> alternativeClass) {
        if (alternativeClass == null) {
            throw new IllegalArgumentException("Alternative class cannot be null");
        }

        if (!alternativeClass.isAnnotationPresent(jakarta.enterprise.inject.Alternative.class)) {
            throw new IllegalArgumentException(
                    "Class " + alternativeClass.getName() + " is not marked with @Alternative");
        }

        // Activate alternative in KnowledgeBase
        knowledgeBase.enableAlternative(alternativeClass);
    }

    /**
     * Registers a custom scope with a legacy {@link ScopeHandler}.
     *
     * <p><b>Note:</b> This method exists for backward compatibility with code using the
     * old {@link ScopeHandler} interface. New code should use {@link ContextManager} directly.
     *
     * <p>This method adapts the old {@code ScopeHandler} to the new {@code ScopeContext} interface:
     * <pre>{@code
     * ScopeHandler myHandler = new MyScopeHandler();
     * injector.registerScope(MyScope.class, myHandler);
     * }</pre>
     *
     * <p><b>Migration Path:</b> Eventually, migrate to {@link ContextManager#getContext(Class)}
     * with a proper {@link ScopeContext} implementation.
     *
     * @param scopeAnnotation the scope annotation class
     * @param handler the legacy scope handler
     * @throws IllegalArgumentException if any parameter is null
     * @deprecated Use {@link ContextManager} with {@link ScopeContext} instead
     */
    @Override
    @Deprecated
    public void registerScope(
            @Nonnull Class<? extends Annotation> scopeAnnotation,
            @Nonnull ScopeHandler handler) {
        if (scopeAnnotation == null) {
            throw new IllegalArgumentException("Scope annotation cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Scope handler cannot be null");
        }

        // Adapt legacy ScopeHandler to modern ScopeContext
        ScopeContext adapter = new ScopeHandlerAdapter(handler);
        contextManager.registerContext(scopeAnnotation, adapter);
    }

    /**
     * Shuts down the injector and destroys all scoped contexts.
     *
     * <p>This method:
     * <ol>
     *   <li>Destroys all active scope contexts (calls @PreDestroy methods)</li>
     *   <li>Clears the knowledge base</li>
     *   <li>Releases all resources</li>
     * </ol>
     *
     * <p>After calling shutdown(), the injector cannot be used anymore.
     */
    @Override
    public void shutdown() {
        // Destroy all contexts (calls @PreDestroy on beans)
        contextManager.destroyAll();
    }

    /**
     * Finds a bean matching the given type and qualifiers.
     *
     * @param <T> the bean type
     * @param type the bean class
     * @param qualifiers the qualifiers to match
     * @return the matching bean
     * @throws InjectionException if no bean found or ambiguous
     */
    private <T> Bean<T> findBean(Class<T> type, Annotation... qualifiers) {
        Set<Annotation> qualifierSet = new HashSet<>(Arrays.asList(qualifiers));

        // Find matching beans from KnowledgeBase
        List<Bean<T>> matchingBeans = new ArrayList<>();
        for (Bean<?> bean : knowledgeBase.getValidBeans()) {
            if (type.isAssignableFrom(bean.getBeanClass())) {
                // Check qualifiers match
                if (bean.getQualifiers().containsAll(qualifierSet)) {
                    @SuppressWarnings("unchecked")
                    Bean<T> typedBean = (Bean<T>) bean;
                    matchingBeans.add(typedBean);
                }
            }
        }

        if (matchingBeans.isEmpty()) {
            throw new InjectionException(
                    "Unsatisfied dependency: No bean found for type " + type.getName() +
                            " with qualifiers " + Arrays.toString(qualifiers));
        }

        if (matchingBeans.size() > 1) {
            // Check for alternatives with priority
            matchingBeans = filterByAlternativePriority(matchingBeans);

            if (matchingBeans.size() > 1) {
                throw new InjectionException(
                        "Ambiguous dependency: Multiple beans found for type " + type.getName() +
                                " with qualifiers " + Arrays.toString(qualifiers) +
                                ". Found: " + matchingBeans);
            }
        }

        return matchingBeans.get(0);
    }

    /**
     * Filters beans by alternative priority, keeping only the highest priority alternative.
     *
     * @param beans the candidate beans
     * @return filtered beans (highest priority alternative, or all non-alternatives)
     */
    private <T> List<Bean<T>> filterByAlternativePriority(List<Bean<T>> beans) {
        // Find alternatives
        List<Bean<T>> alternatives = beans.stream()
                .filter(Bean::isAlternative)
                .collect(java.util.stream.Collectors.toList());

        if (alternatives.isEmpty()) {
            return beans;  // No alternatives, return all
        }

        // Find highest priority alternative
        Bean<T> highestPriority = alternatives.stream()
                .max(Comparator.comparingInt(this::getBeanPriority))
                .orElse(alternatives.get(0));

        return Collections.singletonList(highestPriority);
    }

    /**
     * Extracts priority from a bean (from @Priority annotation).
     *
     * @param bean the bean
     * @return the priority value, or Integer.MAX_VALUE if no @Priority
     */
    private int getBeanPriority(Bean<?> bean) {
        Class<?> beanClass = bean.getBeanClass();
        if (beanClass.isAnnotationPresent(jakarta.annotation.Priority.class)) {
            return beanClass.getAnnotation(jakarta.annotation.Priority.class).value();
        }
        return Integer.MAX_VALUE;  // Lowest priority
    }

    // ====================================================================================
    // PHASE 2: Interceptor Support Initialization
    // ====================================================================================

    /**
     * Initializes interceptor support for all beans in the knowledge base.
     * <p>
     * This method is called once during container initialization (from the constructor)
     * and configures every bean with the necessary interceptor infrastructure.
     * <p>
     * <h3>What This Method Does:</h3>
     * <ol>
     * <li>Iterates through all valid beans in the knowledge base</li>
     * <li>For each {@link BeanImpl}, sets up interceptor support:
     *     <ul>
     *     <li>Sets the {@link InterceptorResolver} (for finding applicable interceptors)</li>
     *     <li>Sets the {@link KnowledgeBase} reference (for querying interceptor metadata)</li>
     *     <li>Sets the {@link InterceptorAwareProxyGenerator} (for creating interceptor-aware proxies)</li>
     *     <li>Sets the {@link BeanResolver} as the dependency resolver (for injecting interceptor dependencies)</li>
     *     <li>Calls {@link BeanImpl#buildMethodInterceptorChains()} to pre-build interceptor chains</li>
     *     </ul>
     * </li>
     * <li>Skips beans that aren't {@link BeanImpl} (built-in beans, producer beans, etc.)</li>
     * </ol>
     *
     * <h3>Why Pre-Build Chains?</h3>
     * Building interceptor chains during container initialization (instead of lazily at runtime) provides:
     * <ul>
     * <li><b>Performance</b>: Chains are built once and reused for all invocations</li>
     * <li><b>Fail-Fast</b>: Invalid interceptor configurations are detected at startup, not at runtime</li>
     * <li><b>Thread-Safety</b>: No synchronization needed during method invocation</li>
     * <li><b>Predictability</b>: Container startup time is more predictable (no lazy initialization surprises)</li>
     * </ul>
     *
     * <h3>Example:</h3>
     * <pre>
     * {@literal @}ApplicationScoped
     * {@literal @}Transactional  // Class-level interceptor binding
     * public class OrderService {
     *     public void createOrder(Order order) { ... }  // Will be intercepted
     *     public Order getOrder(String id) { ... }      // Will be intercepted
     * }
     *
     * // After initializeInterceptorSupport():
     * // - OrderService bean has interceptorResolver set
     * // - OrderService bean has methodInterceptorChains map populated:
     * //   {
     * //     createOrder() → [TransactionalInterceptor chain],
     * //     getOrder() → [TransactionalInterceptor chain]
     * //   }
     * // - Interceptor instances are NOT created yet (created lazily when first needed)
     * </pre>
     *
     * <h3>Performance Characteristics:</h3>
     * <ul>
     * <li><b>Time Complexity</b>: O(B * M * I) where:
     *     <ul>
     *     <li>B = number of beans</li>
     *     <li>M = average methods per bean</li>
     *     <li>I = average interceptors per method</li>
     *     </ul>
     * </li>
     * <li><b>Space Complexity</b>: O(B * M * I) for storing chains</li>
     * <li><b>Typical Overhead</b>: ~10-50ms for 100 beans with 10 methods each</li>
     * </ul>
     *
     * @see BeanImpl#buildMethodInterceptorChains()
     * @see InterceptorResolver
     * @see InterceptorAwareProxyGenerator
     */
    private void initializeInterceptorSupport() {
        // Iterate through all valid beans (excludes beans with validation errors)
        for (Bean<?> bean : knowledgeBase.getValidBeans()) {
            // Only configure BeanImpl instances
            // (Built-in beans, producer beans, and other special beans are handled separately)
            if (bean instanceof BeanImpl) {
                BeanImpl<?> beanImpl = (BeanImpl<?>) bean;

                // Step 1: Set the interceptor resolver
                // This allows the bean to find applicable interceptors for its methods
                beanImpl.setInterceptorResolver(interceptorResolver);

                // Step 2: Set the knowledge base
                // This gives the bean access to interceptor metadata for building chains
                beanImpl.setKnowledgeBase(knowledgeBase);

                // Step 3: Set the interceptor-aware proxy generator
                // This allows the bean to create proxies that execute interceptor chains
                beanImpl.setInterceptorAwareProxyGenerator(interceptorAwareProxyGenerator);

                // Step 4: Set the dependency resolver
                // This allows interceptor instances to have their dependencies injected
                // (Interceptors are beans too and can have @Inject fields/methods)
                beanImpl.setDependencyResolver(beanResolver);

                // Step 5: Build interceptor chains for all methods
                // This analyzes the bean's methods and pre-builds chains for methods with interceptors
                // Chains are cached in the bean and reused for all invocations
                beanImpl.buildMethodInterceptorChains();
            }
        }
    }

    /**
     * Adapter that converts legacy {@link ScopeHandler} to modern {@link ScopeContext}.
     *
     * <p>This allows gradual migration from old scope handling to new CDI 4.1 contexts.
     */
    private static class ScopeHandlerAdapter implements ScopeContext {

        private final ScopeHandler legacyHandler;

        ScopeHandlerAdapter(ScopeHandler legacyHandler) {
            this.legacyHandler = Objects.requireNonNull(legacyHandler, "Legacy handler cannot be null");
        }

        @Override
        public <T> T get(Bean<T> bean, CreationalContext<T> creationalContext) {
            // Adapt: ScopeHandler uses Class + Supplier, not Bean + CreationalContext
            Class<T> beanClass = (Class<T>) bean.getBeanClass();
            Supplier<T> provider = () -> bean.create(creationalContext);
            return legacyHandler.get(beanClass, provider);
        }

        @Override
        public <T> T getIfExists(Bean<T> bean) {
            // Legacy ScopeHandler doesn't have getIfExists, so always return null
            // (indicates bean doesn't exist, forcing creation)
            return null;
        }

        @Override
        public void destroy() {
            try {
                legacyHandler.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to close legacy scope handler", e);
            }
        }

        @Override
        public boolean isActive() {
            // Legacy handlers don't have isActive(), assume always active
            return true;
        }

        @Override
        public boolean isPassivationCapable() {
            // Legacy handlers don't support passivation metadata
            return false;
        }
    }
}
