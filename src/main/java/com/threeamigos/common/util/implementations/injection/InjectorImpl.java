package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;
import com.threeamigos.common.util.implementations.injection.contexts.ContextManager;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.literals.DefaultLiteral;
import com.threeamigos.common.util.implementations.injection.scopehandlers.SingletonScopeHandler;
import com.threeamigos.common.util.interfaces.injection.Injector;
import com.threeamigos.common.util.implementations.injection.scopehandlers.ScopeHandler;
import com.threeamigos.common.util.implementations.injection.tx.TransactionServices;
import com.threeamigos.common.util.implementations.injection.tx.TransactionServicesFactory;
import jakarta.annotation.Nonnull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.*;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.lang.annotation.Annotation;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A complete implementation of JSR-330 (Dependency Injection for Java) and JSR-250 (Common Annotations)
 * specifications, providing comprehensive dependency injection capabilities with lifecycle management.
 *
 * <h2>Supported JSR-330 Annotations:</h2>
 * <ul>
 *   <li>{@link jakarta.inject.Inject @Inject} - Marks constructors, fields, and methods for dependency injection</li>
 *   <li>{@link jakarta.inject.Singleton @Singleton} - Marks classes as singleton-scoped (one instance per injector)</li>
 *   <li>{@link jakarta.inject.Qualifier @Qualifier} - Used to create custom qualifier annotations for disambiguation</li>
 *   <li>{@link jakarta.inject.Named @Named} - Qualifier annotation to distinguish multiple implementations by name</li>
 *   <li>{@link jakarta.inject.Provider Provider} - Interface for lazy/dynamic instance provisioning</li>
 * </ul>
 *
 * <h2>Supported JSR-250 Lifecycle Annotations:</h2>
 * <ul>
 *   <li>{@link jakarta.annotation.PostConstruct @PostConstruct} - Invoked after dependency injection is complete</li>
 *   <li>{@link jakarta.annotation.PreDestroy @PreDestroy} - Invoked before instance destruction during scope cleanup</li>
 * </ul>
 *
 * <h2>Supported CDI Annotations (JSR-299/JSR-346):</h2>
 * <ul>
 *   <li>{@link jakarta.enterprise.inject.Instance Instance} - Enhanced Provider with iteration and destruction capabilities</li>
 *   <li>{@link jakarta.enterprise.inject.Any @Any} - Built-in qualifier that matches all beans</li>
 *   <li>{@link jakarta.enterprise.inject.Default @Default} - Default qualifier applied when no other qualifier is present</li>
 *   <li>{@link jakarta.inject.Scope @Scope} - Meta-annotation for creating custom scope annotations</li>
 * </ul>
 *
 * <h2>Injection Points:</h2>
 * <ul>
 *   <li><b>Constructor Injection:</b> Only one constructor may be annotated with @Inject. If none is found,
 *       the no-args constructor is used. Constructor parameters are automatically resolved and injected.</li>
 *   <li><b>Field Injection:</b> Fields annotated with @Inject are set after construction, even if private.
 *       Static field injection is supported and happens once per class.</li>
 *   <li><b>Method Injection:</b> Methods annotated with @Inject are invoked after field injection.
 *       All method parameters are resolved and injected. Static methods are supported.</li>
 * </ul>
 *
 * <h2>Optional Dependencies:</h2>
 * <p>JSR-330's {@code @Inject} annotation does not have a {@code required} attribute (unlike Spring's
 * {@code @Autowired}). To support optional dependencies, wrap the type in {@code java.util.Optional<T>}:</p>
 *
 * <pre>{@code
 * public class MyService {
 *     @Inject
 *     private Optional<CacheService> cache;  // Optional - may be empty
 *
 *     public void doWork() {
 *         cache.ifPresent(c -> c.cache(data));  // Only use if available
 *     }
 * }
 * }</pre>
 *
 * <p>If the dependency cannot be resolved, {@code Optional.empty()} is injected instead of throwing
 * an exception. This works for constructor, field, and method injection.</p>
 *
 * <h2>Lifecycle Management:</h2>
 * <ol>
 *   <li>Constructor is invoked with injected dependencies</li>
 *   <li>Fields are injected (including static fields, once per class)</li>
 *   <li>Methods are invoked with injected parameters (including static methods)</li>
 *   <li>{@code @PostConstruct} methods are invoked (zero parameters required)</li>
 *   <li>Instance is returned to the caller</li>
 *   <li>Upon scope closure or shutdown, {@code @PreDestroy} methods are invoked</li>
 * </ol>
 *
 * <h2>Scope Management:</h2>
 * The injector supports pluggable scope handlers via {@link ScopeHandler}. By default, the following
 * scopes are registered:
 * <ul>
 *   <li>{@link jakarta.inject.Singleton @Singleton} - One instance per injector (JSR-330)</li>
 *   <li>{@link jakarta.enterprise.context.ApplicationScoped @ApplicationScoped} - One instance per injector (CDI)</li>
 * </ul>
 *
 * <p>The application must manually register additional scopes before use. This allows applications
 * to provide context-specific scope handlers that match their runtime environment:</p>
 *
 * <pre>{@code
 * // For web applications - register request, session, and conversation scopes
 * import com.threeamigos.common.util.implementations.injection.scopehandlers.*;
 *
 * InjectorImpl injector = new InjectorImpl("com.myapp");
 *
 * // RequestScoped: One instance per thread (HTTP request)
 * injector.registerScope(RequestScoped.class, new RequestScopeHandler());
 *
 * // SessionScoped: One instance per session ID
 * SessionScopeHandler sessionHandler = new SessionScopeHandler();
 * injector.registerScope(SessionScoped.class, sessionHandler);
 *
 * // ConversationScoped: One instance per conversation ID
 * ConversationScopeHandler conversationHandler = new ConversationScopeHandler();
 * injector.registerScope(ConversationScoped.class, conversationHandler);
 *
 * // In request processing:
 * sessionHandler.setCurrentSession(httpRequest.getSessionId());
 * conversationHandler.beginConversation(conversationId);
 * MyBean bean = injector.inject(MyBean.class);
 * // ... use bean ...
 * conversationHandler.endConversation(conversationId); // Cleanup when conversation ends
 * sessionHandler.close(); // Cleanup when session ends
 * }</pre>
 *
 * <p><b>Why manual registration?</b> Request, session, and conversation scopes are context-dependent
 * and vary by application type (web apps use HTTP sessions, desktop apps might use user sessions, etc.).
 * Manual registration gives applications full control over scope lifecycle and context management.</p>
 *
 * <p>All scopes are automatically closed during JVM shutdown via a registered shutdown hook.</p>
 *
 * <h2>Type Resolution:</h2>
 * When injecting interfaces or abstract classes, the injector searches the configured packages for
 * concrete implementations. If multiple implementations exist, qualifiers must be used to disambiguate.
 * Generic type parameters (e.g., {@code List<String>}) are resolved and matched when possible.
 *
 * <h2>Thread Safety:</h2>
 * This implementation is fully thread-safe. Singleton instances use double-checked locking for
 * efficient concurrent access. Each thread maintains its own injection stack to detect circular
 * dependencies.
 *
 * <h2>Circular Dependency Detection:</h2>
 * The injector maintains a per-thread stack to detect circular dependencies. If class A depends on
 * class B which depends on class A, an {@link InjectionException} is thrown with a detailed
 * dependency chain.
 *
 * <p>Checked and commented with Claude.
 *
 * @author Stefano Reksten
 *
 * @see jakarta.inject.Inject JSR-330: Dependency Injection for Java
 * @see jakarta.annotation.PostConstruct JSR-250: Common Annotations for the Java Platform
 * @see jakarta.enterprise.inject.Instance CDI: Contexts and Dependency Injection
 */
@Deprecated
public class InjectorImpl implements Injector {

    private final KnowledgeBase knowledgeBase;

    /**
     * Thread-local stack for tracking the current dependency resolution chain. Each thread maintains
     * its own stack to enable concurrent injection while detecting circular dependencies within
     * a single resolution path.
     *
     * @see #inject(Type, Stack, Collection)
     */
    private static final ThreadLocal<Stack<Type>> STACK_POOL = ThreadLocal.withInitial(Stack::new);

    /**
     * Registry mapping scope annotations to their handlers. Handlers control instance lifecycle
     * and cardinality (e.g., singleton vs. prototype). This map is thread-safe and supports
     * runtime registration of custom scopes per JSR-330 extensibility requirements.
     *
     * @see jakarta.inject.Scope
     * @see #registerScope(Class, ScopeHandler)
     */
    private final Map<Class<? extends Annotation>, ScopeHandler> scopeRegistry = new ConcurrentHashMap<>();

    /**
     * Resolves concrete implementations for interfaces and abstract classes by scanning the
     * configured package(s). Maintains internal caches for performance optimization during
     * repeated lookups.
     *
     * @see ClassResolver
     */
    private final ClassResolver classResolver;

    /**
     * Set of classes that have already undergone static member injection. Static fields and methods
     * are injected only once per class per injector instance, regardless of how many times the class
     * is instantiated. Thread-safe to support concurrent injection.
     *
     * @see #injectFields(Object, Type, Stack, Class, Class, boolean)
     * @see #injectMethods(Object, Type, Stack, Class, Class, boolean)
     */
    private final Set<Class<?>> injectedStaticClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Shared CDI infrastructure used by Event and Instance wrappers to ensure contextual consistency.
     * Keeping these singletons avoids creating isolated containers per injection point.
     */
    private final ContextManager contextManager;
    private final TransactionServices transactionServices;
    private final BeanResolver beanResolver;

    /**
     * Creates a new injector that scans the specified package(s) for injectable components.
     * If no packages are specified, the injector will scan all available packages, which may
     * impact startup performance.
     *
     * <p>The injector automatically registers the default {@link Singleton} scope and sets up
     * a JVM shutdown hook to invoke {@link PreDestroy} methods on all managed instances.
     *
     * @param packageNames optional package names to restrict classpath scanning. All interfaces
     *                     and implementations must reside within these packages or their subpackages
     * @throws IllegalArgumentException if package scanning fails
     */
    public InjectorImpl(final String ... packageNames) {

        knowledgeBase = new KnowledgeBase();
        try (ParallelTaskExecutor parallelTaskExecutor = ParallelTaskExecutor.createExecutor()) {
            // ClassProcessor now receives BeanArchiveMode per-class from ParallelClasspathScanner
            // which detects the mode for each JAR/directory by examining META-INF/beans.xml
            ClassProcessor classProcessor = new ClassProcessor(parallelTaskExecutor, knowledgeBase);
            new ParallelClasspathScanner(
                    Thread.currentThread().getContextClassLoader(), classProcessor, packageNames);
            parallelTaskExecutor.awaitCompletion();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Validate and register beans AFTER ProcessAnnotatedType should run in modern path.
        // InjectorImpl is legacy; we perform validation now using recorded archive modes.
        CDI41BeanValidator validator = new CDI41BeanValidator(knowledgeBase);
        for (Class<?> clazz : knowledgeBase.getClasses()) {
            validator.validateAndRegisterRaw(clazz,
                    knowledgeBase.getBeanArchiveMode(clazz),
                    knowledgeBase.getAnnotatedTypeOverride(clazz));
        }

        // After scanning completes, validate all injection points
        CDI41InjectionValidator injectionValidator = new CDI41InjectionValidator(knowledgeBase);
        injectionValidator.validateAllInjectionPoints();

        // Check if there are any errors that would prevent application startup
        if (knowledgeBase.hasErrors()) {
            StringBuilder errorReport = new StringBuilder();
            errorReport.append("Application cannot start due to validation/injection errors:\n\n");

            if (!knowledgeBase.getDefinitionErrors().isEmpty()) {
                errorReport.append("=== Definition Errors ===\n");
                knowledgeBase.getDefinitionErrors().forEach(e -> errorReport.append("  - ").append(e).append("\n"));
                errorReport.append("\n");
            }

            if (!knowledgeBase.getInjectionErrors().isEmpty()) {
                errorReport.append("=== Injection Errors ===\n");
                knowledgeBase.getInjectionErrors().forEach(e -> errorReport.append("  - ").append(e).append("\n"));
                errorReport.append("\n");
            }

            if (!knowledgeBase.getErrors().isEmpty()) {
                errorReport.append("=== General Errors ===\n");
                knowledgeBase.getErrors().forEach(e -> errorReport.append("  - ").append(e).append("\n"));
                errorReport.append("\n");
            }

            throw new RuntimeException(errorReport.toString());
        }

        this.classResolver = new ClassResolver(knowledgeBase);
        this.contextManager = new ContextManager();
        this.transactionServices = TransactionServicesFactory.create();
        this.beanResolver = new BeanResolver(knowledgeBase, contextManager, transactionServices);
        registerDefaultScopes();
        addShutdownHook();
    }

    /**
     * Package-private constructor for testing purposes. Allows injection of a custom
     * {@link ClassResolver} instance to control type resolution behavior.
     *
     * @param classResolver the class resolver to use for finding implementations
     * @throws IllegalArgumentException if classResolver is null
     */
    InjectorImpl(final @Nonnull ClassResolver classResolver) {
        knowledgeBase = new KnowledgeBase();
        if (classResolver == null) {
            throw new IllegalArgumentException("Class resolver cannot be null");
        }
        this.classResolver = classResolver;
        this.contextManager = new ContextManager();
        this.transactionServices = TransactionServicesFactory.create();
        this.beanResolver = new BeanResolver(knowledgeBase, contextManager, transactionServices);
        registerDefaultScopes();
        addShutdownHook();
    }

    /**
     * Registers the default {@link Singleton} scope handler as required by JSR-330.
     * This handler uses thread-safe double-checked locking to ensure only one instance
     * per class exists within this injector. The handler also manages {@link PreDestroy}
     * lifecycle callbacks when the scope is closed.
     *
     * @see jakarta.inject.Singleton
     */
    private void registerDefaultScopes() {
        // Standard Singleton scope implementation - shared handler for Singleton and ApplicationScoped
        SingletonScopeHandler singletonHandler = new SingletonScopeHandler();
        scopeRegistry.put(Singleton.class, singletonHandler);
        // ApplicationScoped behaves like Singleton in CDI - uses the same handler instance
        scopeRegistry.put(ApplicationScoped.class, singletonHandler);
        // RequestScoped and SessionScoped must be registered manually by the user
        // with appropriate scope handlers (BasicScopeHandler or SessionScopeHandler)
    }

    /**
     * Registers a JVM shutdown hook that invokes {@link PreDestroy} callbacks on all scoped
     * instances when the application terminates. This ensures proper resource cleanup per
     * JSR-250 requirements.
     *
     * @see jakarta.annotation.PreDestroy
     * @see #shutdown()
     */
    void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**
     * Registers a custom scope annotation with its corresponding handler. The scope annotation
     * must be meta-annotated with {@link jakarta.inject.Scope @Scope} per JSR-330. Once registered,
     * classes annotated with this scope will have their lifecycle managed by the provided handler.
     *
     * <p>Example custom scope registration:
     * <pre>{@code
     * import com.threeamigos.common.util.implementations.injection.scopehandlers.RequestScopeHandler;
     *
     * @Scope
     * @Retention(RUNTIME)
     * public @interface RequestScoped {}
     *
     * injector.registerScope(RequestScoped.class, new RequestScopeHandler());
     * }</pre>
     *
     * @param scopeAnnotation the scope annotation class (must be meta-annotated with @Scope)
     * @param handler the handler that manages instances of this scope
     * @throws IllegalArgumentException if either parameter is null or if the scope is already registered
     * @see jakarta.inject.Scope
     * @see ScopeHandler
     */
    @Override
    public void registerScope(@Nonnull Class<? extends Annotation> scopeAnnotation, @Nonnull ScopeHandler handler) {
        if (scopeAnnotation == null) {
            throw new IllegalArgumentException("Scope annotation cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Scope handler cannot be null");
        }
        if (scopeRegistry.containsKey(scopeAnnotation)) {
            throw new IllegalArgumentException("Scope handler for annotation " + scopeAnnotation.getName() + " is already registered");
        }
        scopeRegistry.put(scopeAnnotation, handler);
    }

    /**
     * Returns an immutable set of all currently registered scope annotations.
     * The default {@link Singleton} scope is always present.
     *
     * @return unmodifiable set of registered scope annotation classes
     * @see jakarta.inject.Singleton
     */
    public Set<Class<? extends Annotation>> getRegisteredScopes() {
        return Collections.unmodifiableSet(scopeRegistry.keySet());
    }

    /**
     * Checks whether a specific scope annotation has been registered with this injector.
     *
     * @param scopeAnnotation the scope annotation to check
     * @return true if the scope is registered, false otherwise
     */
    public boolean isScopeRegistered(Class<? extends Annotation> scopeAnnotation) {
        return scopeRegistry.containsKey(scopeAnnotation);
    }

    /**
     * Unregisters a scope handler, preventing further use of that scope. Existing instances
     * managed by the scope are not affected, but new injections will fail if they require
     * this scope.
     *
     * <p><b>Warning:</b> Unregistering a scope does not invoke cleanup on existing instances.
     * Call the scope handler's {@link ScopeHandler#close()} method explicitly if cleanup is needed.
     *
     * @param scopeAnnotation the scope annotation to unregister
     * @throws IllegalArgumentException if scopeAnnotation is null
     */
    public void unregisterScope(@Nonnull Class<? extends Annotation> scopeAnnotation) {
        if (scopeAnnotation == null) {
            throw new IllegalArgumentException("Scope annotation cannot be null");
        }
        scopeRegistry.remove(scopeAnnotation);
    }

    /**
     * Enables an alternative implementation class for dependency resolution. Alternative classes
     * take precedence over standard implementations when resolving interfaces or abstract types.
     * This is useful for testing or swapping implementations at runtime.
     *
     * <p>Per CDI specification (JSR-299/346), alternatives must be explicitly enabled to be
     * considered during resolution.
     *
     * @param alternativeClass the alternative implementation to enable
     * @throws IllegalArgumentException if alternativeClass is null
     * @see jakarta.enterprise.inject.Alternative
     */
    @Override
    public void enableAlternative(@Nonnull Class<?> alternativeClass) {
        // Non-null check done by classResolver
        classResolver.enableAlternative(alternativeClass);
    }

    /**
     * Manually binds a type to a specific implementation with optional qualifiers. This overrides
     * automatic resolution and is useful for programmatic configuration or when implementations
     * cannot be discovered via classpath scanning.
     *
     * <p>Example binding:
     * <pre>{@code
     * Collection<Annotation> qualifiers = Arrays.asList(new NamedLiteral("database"));
     * injector.bind(DataSource.class, qualifiers, PostgresDataSource.class);
     * }</pre>
     *
     * @param type the interface or abstract type to bind
     * @param qualifiers qualifier annotations to distinguish this binding (can be empty)
     * @param implementation the concrete implementation class
     * @throws IllegalArgumentException if any parameter is null
     * @see jakarta.inject.Qualifier
     */
    @Override
    public void bind(@Nonnull Type type, @Nonnull Collection<Annotation> qualifiers, @Nonnull Class<?> implementation) {
        // Non-null checks done by classResolver
        classResolver.bind(type, qualifiers, implementation);
    }

    /**
     * Creates and injects an instance of the specified class. This is the main entry point for
     * dependency injection. The injector will:
     * <ol>
     *   <li>Resolve concrete implementation if the type is abstract</li>
     *   <li>Find or use the injectable constructor</li>
     *   <li>Recursively inject all constructor parameters</li>
     *   <li>Inject fields annotated with {@link Inject @Inject}</li>
     *   <li>Invoke methods annotated with {@link Inject @Inject}</li>
     *   <li>Call {@link PostConstruct @PostConstruct} lifecycle methods</li>
     * </ol>
     *
     * <p>The instance lifecycle is managed according to its scope annotation. {@link Singleton}
     * instances are cached and reused.
     *
     * @param <T> the type to inject
     * @param classToInject the class to instantiate and inject
     * @return fully injected instance of the specified class
     * @throws IllegalArgumentException if classToInject is null
     * @throws InjectionException if injection fails (e.g., circular dependency, no suitable constructor)
     * @see jakarta.inject.Inject
     * @see jakarta.annotation.PostConstruct
     */
    @Override
    public <T> T inject(@Nonnull Class<T> classToInject) {
        if (classToInject == null) {
            throw new IllegalArgumentException("Class to inject cannot be null");
        }
        Stack<Type> stack = STACK_POOL.get();
        try {
            stack.clear();
            return inject(classToInject, stack, null);
        } finally {
            stack.clear();
        }
    }

    /**
     * Creates and injects an instance using a {@link TypeLiteral} to preserve generic type
     * information at runtime. This is necessary when injecting parameterized types like
     * {@code List<String>} where the type parameter matters.
     *
     * <p>Example usage:
     * <pre>{@code
     * TypeLiteral<List<String>> type = new TypeLiteral<List<String>>() {};
     * List<String> list = injector.inject(type);
     * }</pre>
     *
     * @param <T> the generic type to inject
     * @param typeLiteral type literal capturing the generic type information
     * @return fully injected instance matching the generic type
     * @throws IllegalArgumentException if typeLiteral is null
     * @throws InjectionException if injection fails
     * @see jakarta.enterprise.util.TypeLiteral
     */
    @Override
    public <T> T inject(@Nonnull TypeLiteral<T> typeLiteral) {
        if (typeLiteral == null) {
            throw new IllegalArgumentException("Type literal cannot be null");
        }
        Stack<Type> stack = STACK_POOL.get();
        try {
            stack.clear();
            return inject(typeLiteral.getType(), new Stack<>(), null);
        } finally {
            stack.clear();
        }
    }

    /**
     * Internal injection method that handles the core dependency resolution logic.
     * Validates the type, detects circular dependencies, resolves the implementation,
     * applies scope handling, and performs the actual injection.
     *
     * <p><b>Optional Dependency Support:</b> If the type is {@code Optional<T>}, this method
     * will attempt to inject {@code T}. If {@code T} cannot be resolved, an empty {@code Optional}
     * is returned instead of throwing an exception. This provides JSR-330 compliant optional
     * dependency injection without requiring a {@code required} attribute on {@code @Inject}.
     *
     * @param <T> the type to inject
     * @param typeToInject the type to resolve and inject (can be generic, including Optional&lt;T&gt;)
     * @param stack dependency resolution stack for circular dependency detection
     * @param qualifiers optional qualifiers to disambiguate implementations
     * @return fully injected instance, or {@code Optional.empty()} if type is Optional and dependency not found
     * @throws InjectionException if injection fails for any reason (except for Optional types)
     * @see #checkClassValidity(Type)
     * @see #performInjection(Type, Class, Stack)
     * @see java.util.Optional
     */
    private <T> T inject(@Nonnull Type typeToInject, Stack<Type> stack, Collection<Annotation> qualifiers) {
        // Handle Optional<T> injection - JSR-330 optional dependency pattern
        if (typeToInject instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) typeToInject;
            if (Optional.class.equals(pt.getRawType())) {
                Type innerType = pt.getActualTypeArguments()[0];
                try {
                    // Try to inject the inner type
                    T instance = inject(innerType, stack, qualifiers);
                    @SuppressWarnings("unchecked")
                    T result = (T) Optional.of(instance);
                    return result;
                } catch (InjectionException e) {
                    // Dependency not found - return empty Optional (this is expected behavior)
                    @SuppressWarnings("unchecked")
                    T result = (T) Optional.empty();
                    return result;
                }
            }
        }

        if (stack.contains(typeToInject)) {
            stack.add(typeToInject);
            throw new InjectionException("Circular dependency detected for class " +
                    typeToInject.getClass().getName() + ": " +
                    stack.stream().map(Type::getTypeName).collect(Collectors.joining(" -> ")));
        }
        stack.push(typeToInject);
        try {
            checkClassValidity(typeToInject);
            Class<? extends T> resolvedClass = classResolver.resolveImplementation(typeToInject, qualifiers);

            // Find the scope annotation on the resolved class
            Class<? extends Annotation> scopeType = getScopeType(resolvedClass);

            if (scopeType != null && scopeRegistry.containsKey(scopeType)) {
                ScopeHandler handler = scopeRegistry.get(scopeType);
                // A helper method to handle the wildcard capture safely
                T t = handleScopedInjection(handler, typeToInject, resolvedClass, stack);
                stack.pop();
                return t;
            }

            T t = performInjection(typeToInject, resolvedClass, stack);
            stack.pop();
            return t;
        } catch (InjectionException e) {
            throw e;
        } catch (Exception e) {
            String injectionPath = stack.stream()
                    .map(Type::getTypeName)
                    .collect(Collectors.joining(" -> "));
            throw new InjectionException("Failed to inject " + RawTypeExtractor.getRawType(typeToInject).getName() +
                    "\nInjection path: " + injectionPath +
                    "\nCause: " + e.getMessage(), e);
        }
    }

    /**
     * Determines the scope annotation present on a class. Per JSR-330, a class may have at most
     * one scope annotation. This method searches for any annotation that is itself annotated with
     * {@link jakarta.inject.Scope @Scope}, or for the built-in {@link Singleton} annotation.
     *
     * <p>If no scope annotation is found, returns null, indicating the class uses a dependent scope
     * (new instance per injection point).
     *
     * @param clazz the class to inspect for scope annotations
     * @return the scope annotation type, or null if no scope is defined
     * @see jakarta.inject.Scope
     * @see jakarta.inject.Singleton
     */
    Class<? extends Annotation> getScopeType(Class<?> clazz) {
        Class<? extends Annotation> scopeAnnotation = Arrays.stream(clazz.getAnnotations())
                .map(Annotation::annotationType)
                .filter(at -> hasScopeAnnotation(at) // For custom scopes meta-annotated with @Scope
                        || AnnotationsEnum.SINGLETON.matches(at)
                        || AnnotationsEnum.APPLICATION_SCOPED.matches(at)
                        || AnnotationsEnum.REQUEST_SCOPED.matches(at)
                        || AnnotationsEnum.SESSION_SCOPED.matches(at)
                        || AnnotationsEnum.CONVERSATION_SCOPED.matches(at)
                        || AnnotationsEnum.DEPENDENT.matches(at))
                .findFirst()
                .orElse(null);

        // Normalize javax annotations to jakarta equivalents for scope registry lookup
        if (scopeAnnotation != null) {
            if (scopeAnnotation.equals(javax.inject.Singleton.class)) {
                return Singleton.class;
            } else if (scopeAnnotation.equals(javax.enterprise.context.ApplicationScoped.class)) {
                return ApplicationScoped.class;
            } else if (scopeAnnotation.equals(javax.enterprise.context.RequestScoped.class)) {
                return RequestScoped.class;
            } else if (scopeAnnotation.equals(javax.enterprise.context.SessionScoped.class)) {
                return jakarta.enterprise.context.SessionScoped.class;
            } else if (scopeAnnotation.equals(javax.enterprise.context.ConversationScoped.class)) {
                return jakarta.enterprise.context.ConversationScoped.class;
            } else if (scopeAnnotation.equals(javax.enterprise.context.Dependent.class)) {
                return jakarta.enterprise.context.Dependent.class;
            }
        }
        return scopeAnnotation;
    }

    /**
     * Delegates instance creation to the appropriate scope handler. This method bridges the
     * generic type safety gap between {@code Class<? extends T>} and {@code ScopeHandler}.
     * The handler determines whether to create a new instance or return an existing one based
     * on the scope's lifecycle rules.
     *
     * @param <T> the type being injected
     * @param handler the scope handler managing instances of this scope
     * @param typeContext the type context for generic type resolution
     * @param clazz the concrete class to instantiate
     * @param stack the dependency stack for circular dependency detection
     * @return instance managed by the scope handler
     * @see ScopeHandler#get(Class, Supplier)
     */
    @SuppressWarnings("unchecked")
    <T> T handleScopedInjection(ScopeHandler handler, Type typeContext, Class<? extends T> clazz, Stack<Type> stack) {
        return (T) handler.get((Class<Object>) clazz, () -> performInjection(typeContext, clazz, stack));
    }

    /**
     * Performs the complete injection lifecycle for a class instance:
     * <ol>
     *   <li>Finds the appropriate constructor (with {@link Inject @Inject} or no-args)</li>
     *   <li>Resolves and injects constructor parameter dependencies</li>
     *   <li>Invokes the constructor to create the instance</li>
     *   <li>Injects static members (once per class)</li>
     *   <li>Injects instance fields annotated with {@link Inject @Inject}</li>
     *   <li>Invokes methods annotated with {@link Inject @Inject} with resolved parameters</li>
     *   <li>Calls {@link PostConstruct @PostConstruct} lifecycle methods</li>
     * </ol>
     *
     * @param <T> the type being injected
     * @param typeContext the type context for resolving generic parameters
     * @param resolvedClass the concrete class to instantiate and inject
     * @param stack the dependency stack for circular dependency detection
     * @return fully initialized and injected instance
     * @throws InjectionException if any step of the injection process fails
     * @see jakarta.inject.Inject
     * @see jakarta.annotation.PostConstruct
     * @see #getConstructor(Class)
     * @see #injectFields(Object, Type, Stack, Class, Class, boolean)
     * @see #injectMethods(Object, Type, Stack, Class, Class, boolean)
     * @see #invokePostConstruct(Object)
     */
    @SuppressWarnings("unchecked")
    <T> T performInjection(Type typeContext, Class<? extends T> resolvedClass, Stack<Type> stack) {
        try {
            if (resolvedClass.isArray()) {
                return (T) Array.newInstance(resolvedClass.getComponentType(), 0);
            }
            Constructor<? extends T> constructor = getConstructor(resolvedClass);
            Parameter[] parameters = constructor.getParameters();
            validateConstructorParameters(parameters, resolvedClass);
            Object[] args = resolveParameters(typeContext, parameters, stack);
            T t = buildInstance(constructor, args);

            // Collect all classes in the hierarchy, from top to bottom
            List<Class<?>> hierarchy = LifecycleMethodHelper.buildHierarchy(t);

            for (Class<?> clazz : hierarchy) {
                injectFields(t, typeContext, stack, clazz, resolvedClass, true);
            }
            for (Class<?> clazz : hierarchy) {
                injectMethods(t, typeContext, stack, clazz, resolvedClass, true);
                // After processing fields and methods for this specific class in the hierarchy,
                // mark it as static-injected.
                injectedStaticClasses.add(clazz);
            }
            for (Class<?> clazz : hierarchy) {
                injectFields(t, typeContext, stack, clazz, resolvedClass, false);
                injectMethods(t, typeContext, stack, clazz, resolvedClass, false);
            }

            // Call @PostConstruct methods
            invokePostConstruct(t);

            return t;
        } catch (Exception e) {
            throw new InjectionException("Injection failed for " + resolvedClass.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Finds the constructor to use for dependency injection per JSR-330 rules.
     * <ol>
     *   <li>If exactly one constructor is annotated with {@link Inject @Inject}, use it</li>
     *   <li>If no constructor is annotated, use the no-argument constructor</li>
     *   <li>If multiple constructors are annotated or no suitable constructor exists, throw exception</li>
     * </ol>
     *
     * @param <T> the type of the class
     * @param clazz the class whose constructor to find
     * @return the constructor to use for injection
     * @throws InjectionException if multiple @Inject constructors exist or no suitable constructor is found
     * @see jakarta.inject.Inject
     */
    @SuppressWarnings("unchecked")
    <T> Constructor<T> getConstructor(@Nonnull Class<T> clazz) {
        Constructor<T>[] allConstructors = (Constructor<T>[])clazz.getDeclaredConstructors();

        // First, look for @Inject annotated constructors
        List<Constructor<T>> injectConstructors = Arrays.stream(allConstructors)
                .filter(c -> hasInjectAnnotation(c))
                .collect(Collectors.toList());

        if (injectConstructors.size() > 1) {
            throw new InjectionException("More than one constructor annotated with @Inject in class " + clazz.getName());
        } else if (injectConstructors.size() == 1) {
            return injectConstructors.get(0);
        }

        // No @Inject constructor found - JSR-330 rule:
        // Use the no-argument constructor (if it exists)
        List<Constructor<T>> noArgConstructors = Arrays.stream(allConstructors)
                .filter(c -> c.getParameterCount() == 0)
                .collect(Collectors.toList());

        if (noArgConstructors.isEmpty()) {
            throw new InjectionException("No empty constructor or a constructor annotated with @Inject in class " + clazz.getName());
        }

        return noArgConstructors.get(0);
    }

    /**
     * Validates that all constructor parameters are valid injectable types per JSR-330.
     * Each parameter is checked using {@link #checkClassValidity(Type)}.
     *
     * @param parameters the constructor parameters to validate
     * @param clazz the class containing the constructor (for error messages)
     * @throws InjectionException if any parameter is not a valid injectable type
     * @see #checkClassValidity(Type)
     */
    void validateConstructorParameters(Parameter[] parameters, Class<?> clazz) {
        for (Parameter param : parameters) {
            try {
                checkClassValidity(param.getType());
            } catch (IllegalArgumentException e) {
                throw new InjectionException("Cannot inject into constructor parameter " + param.getName() + " of class " +
                        clazz.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Validates that all method parameters are valid injectable types per JSR-330.
     * Each parameter is checked using {@link #checkClassValidity(Type)}.
     *
     * @param parameters the method parameters to validate
     * @param methodName the name of the method (for error messages)
     * @param clazz the class containing the method (for error messages)
     * @throws InjectionException if any parameter is not a valid injectable type
     * @see #checkClassValidity(Type)
     */
    void validateMethodParameters(Parameter[] parameters, String methodName, Class<?> clazz) {
        for (Parameter param : parameters) {
            try {
                checkClassValidity(param.getType());
            } catch (IllegalArgumentException e) {
                throw new InjectionException("Cannot inject into parameter " + param.getName() + " of method " +
                        clazz.getName() + "::" + methodName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Resolves and injects all parameters for a constructor or method. For each parameter:
     * <ul>
     *   <li>If the parameter is a {@link Provider} or {@link Instance}, creates a wrapper</li>
     *   <li>Otherwise, recursively injects the parameter type</li>
     *   <li>Uses qualifiers from the parameter to disambiguate multiple implementations</li>
     * </ul>
     *
     * @param typeContext the type context for resolving generic type parameters
     * @param parameters the parameters to resolve
     * @param stack the dependency stack for circular dependency detection
     * @return array of resolved parameter values ready for invocation
     * @see #resolveType(Type, Type)
     * @see #getQualifiers(Parameter)
     */
    Object[] resolveParameters(Type typeContext, Parameter[] parameters, Stack<Type> stack) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (Provider.class.isAssignableFrom(param.getType())) {
                args[i] = createInstanceWrapper(param);
            } else if (jakarta.enterprise.event.Event.class.isAssignableFrom(param.getType())) {
                args[i] = createEventWrapper(param);
            } else {
                Type paramType = resolveType(param.getParameterizedType(), typeContext);
                checkClassValidity(paramType);
                Collection<Annotation> paramQualifiers = getQualifiers(param);
                args[i] = inject(paramType, stack, paramQualifiers);
            }
        }
        return args;
    }

    /**
     * Resolves a {@link TypeVariable} to its concrete type within a given context. This is essential
     * for supporting generic injection where the actual type parameter needs to be determined.
     *
     * <p>For example, when injecting into {@code class MyList extends ArrayList<String>}, this method
     * resolves the type variable {@code E} to {@code String} when the context is
     * {@code ArrayList<String>}.
     *
     * <p>If the type to resolve is not a TypeVariable, it is returned as-is. If the context is not
     * a ParameterizedType or the TypeVariable cannot be resolved, the original TypeVariable is returned.
     *
     * @param toResolve the type to resolve (can be a TypeVariable or concrete type)
     * @param context the context type containing actual type arguments (typically a ParameterizedType)
     * @return the resolved concrete type, or the original type if resolution is not possible
     * @see java.lang.reflect.TypeVariable
     * @see java.lang.reflect.ParameterizedType
     */
    Type resolveType(Type toResolve, Type context) {
        if (!(toResolve instanceof TypeVariable)) {
            return toResolve;
        }
        TypeVariable<?> tv = (TypeVariable<?>) toResolve;
        if (context instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) context;
            Class<?> raw = (Class<?>) pt.getRawType();
            TypeVariable<?>[] vars = raw.getTypeParameters();
            for (int i = 0; i < vars.length; i++) {
                if (vars[i].getName().equals(tv.getName())) {
                    return pt.getActualTypeArguments()[i];
                }
            }
        }
        return toResolve;
    }

    /**
     * Invokes a constructor with the given arguments to create an instance. Sets the constructor
     * accessible if it is not public, allowing injection into private and package-private constructors.
     *
     * @param <T> the type being instantiated
     * @param constructor the constructor to invoke
     * @param args the arguments to pass to the constructor
     * @return the newly created instance
     * @throws Exception if instantiation fails for any reason
     */
    <T> T buildInstance(Constructor<? extends T> constructor, Object... args) throws Exception {
        if (!Modifier.isPublic(constructor.getModifiers())) {
            constructor.setAccessible(true);
        }
        return constructor.newInstance(args);
    }

    /**
     * Injects dependencies into fields annotated with {@link Inject @Inject} per JSR-330.
     * Handles both static and instance fields, with special logic to ensure static fields are
     * injected only once per class.
     *
     * <p><b>Injection Rules:</b>
     * <ul>
     *   <li>Final fields cannot be injected (throws exception)</li>
     *   <li>Static fields are injected once per class across all instances</li>
     *   <li>Private fields are made accessible before injection</li>
     *   <li>{@link Provider} and {@link Instance} fields receive wrappers instead of direct instances</li>
     *   <li>Qualifiers on fields are used for disambiguation</li>
     * </ul>
     *
     * @param <T> the type of the instance
     * @param t the instance to inject into (null for static fields)
     * @param typeContext the type context for resolving generic parameters
     * @param stack the dependency stack for circular dependency detection
     * @param clazz the class whose fields to process
     * @param resolvedClass the concrete resolved class (for error messages)
     * @param onlyStatic true to inject only static fields, false to inject only instance fields
     * @throws Exception if field injection fails
     * @see jakarta.inject.Inject
     * @see #getQualifiers(Field)
     */
    <T> void injectFields(T t, Type typeContext, Stack<Type> stack, Class<?> clazz, Class<?> resolvedClass, boolean onlyStatic) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        boolean isStaticAlreadyInjected = injectedStaticClasses.contains(clazz);

        for (Field field : fields) {
            if (hasInjectAnnotation(field)) {
                boolean isStatic = Modifier.isStatic(field.getModifiers());

                if (onlyStatic != isStatic) {
                    continue;
                }

                if (isStatic && isStaticAlreadyInjected) {
                    continue;
                }

                if (Modifier.isFinal(field.getModifiers())) {
                    throw new IllegalStateException("Cannot inject into final field " + field.getName() + " of class " +
                            resolvedClass.getName());
                }
                field.setAccessible(true);
                if (Provider.class.isAssignableFrom(field.getType())) {
                    field.set(t, createInstanceWrapper(field));
                } else if (jakarta.enterprise.event.Event.class.isAssignableFrom(field.getType())) {
                    field.set(t, createEventWrapper(field));
                } else {
                    Type fieldType = resolveType(field.getGenericType(), typeContext);
                    checkClassValidity(fieldType);
                    Collection<Annotation> fieldQualifiers = getQualifiers(field);
                    field.set(t, inject(fieldType, stack, fieldQualifiers));
                }
            }
        }
    }

    /**
     * Invokes methods annotated with {@link Inject @Inject} per JSR-330, injecting their parameters.
     * Handles both static and instance methods, with special logic to ensure static methods are
     * invoked only once per class. Properly handles method overriding per JSR-330 rules.
     *
     * <p><b>Injection Rules:</b>
     * <ul>
     *   <li>Abstract methods cannot be injected (throws exception)</li>
     *   <li>Generic methods (with type parameters) cannot be injected (throws exception)</li>
     *   <li>Static methods are invoked once per class across all instances</li>
     *   <li>Private methods are made accessible before invocation</li>
     *   <li>Overridden methods are not re-injected in subclasses per JSR-330</li>
     *   <li>Method parameters are resolved and injected recursively</li>
     * </ul>
     *
     * @param <T> the type of the instance
     * @param t the instance to inject into (null for static methods)
     * @param typeContext the type context for resolving generic parameters
     * @param stack the dependency stack for circular dependency detection
     * @param clazz the class whose methods to process
     * @param resolvedClass the concrete resolved class (for error messages and override detection)
     * @param onlyStatic true to invoke only static methods, false to invoke only instance methods
     * @throws Exception if method injection fails
     * @see jakarta.inject.Inject
     * @see #isOverridden(Method, Class)
     */
    <T> void injectMethods(T t, Type typeContext, Stack<Type> stack, Class<?> clazz, Class<?> resolvedClass, boolean onlyStatic) throws Exception {
        Method[] methods = clazz.getDeclaredMethods();
        boolean isStaticAlreadyInjected = injectedStaticClasses.contains(clazz);

        for (Method method : methods) {
            if (hasInjectAnnotation(method)) {
                boolean isStatic = Modifier.isStatic(method.getModifiers());

                if (onlyStatic != isStatic) {
                    continue;
                }

                if (isStatic && isStaticAlreadyInjected) {
                    continue;
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    throw new InjectionException("Cannot inject into abstract method " + method.getName() + " of class " +
                            resolvedClass.getName());
                }

                if (method.getTypeParameters().length > 0) {
                    throw new InjectionException("Cannot inject into generic method " + method.getName() + " of class " +
                            resolvedClass.getName());
                }

                // JSR-330: Only inject the most specific override.
                // If this method is overridden by a subclass, skip it here.
                // It will be handled when the loop reaches that subclass.
                if (isOverridden(method, t.getClass())) {
                    continue;
                }

                method.setAccessible(true);
                Parameter[] parameters = method.getParameters();
                validateMethodParameters(parameters, method.getName(), resolvedClass);
                Object[] params = resolveParameters(typeContext, parameters, stack);
                method.invoke(t, params);
            }
        }
    }

    /**
     * Invokes all {@link PostConstruct @PostConstruct} annotated methods on the given instance.
     * Per JSR-250 specification, {@code @PostConstruct} methods:
     * <ul>
     *   <li>Must have no parameters</li>
     *   <li>May have any access modifier (public, protected, package-private, or private)</li>
     *   <li>Must not throw checked exceptions (except checked exceptions allowed by the platform)</li>
     *   <li>May be defined on the class itself or inherited from superclasses</li>
     *   <li>Are invoked after all dependency injection is complete</li>
     *   <li>Are invoked in order from superclass to subclass (parent first)</li>
     * </ul>
     *
     * <p>This method is called automatically as the final step in {@link #performInjection(Type, Class, Stack)}
     * after constructor injection, field injection, and method injection are complete.
     *
     * @param instance the fully injected instance on which to invoke @PostConstruct methods
     * @throws InvocationTargetException if a @PostConstruct method throws an exception
     * @throws IllegalAccessException if a @PostConstruct method cannot be accessed
     * @see jakarta.annotation.PostConstruct
     * @see LifecycleMethodHelper#invokeLifecycleMethod(Object, Class)
     */
    private void invokePostConstruct(Object instance) throws InvocationTargetException, IllegalAccessException {
        LifecycleMethodHelper.invokeLifecycleMethod(instance, PostConstruct.class);
    }

    /**
     * Invokes all {@link PreDestroy @PreDestroy} annotated methods on the given instance.
     * Per JSR-250 specification, {@code @PreDestroy} methods:
     * <ul>
     *   <li>Must have no parameters</li>
     *   <li>May have any access modifier (public, protected, package-private, or private)</li>
     *   <li>Must not throw checked exceptions (except checked exceptions allowed by the platform)</li>
     *   <li>May be defined on the class itself or inherited from superclasses</li>
     *   <li>Are invoked during scope cleanup or application shutdown</li>
     *   <li>Are invoked in order from subclass to superclass (child first)</li>
     * </ul>
     *
     * <p>This method is called automatically when:
     * <ul>
     *   <li>A scope is closed via {@link ScopeHandler#close()}</li>
     *   <li>The application shuts down (via {@link #shutdown()})</li>
     *   <li>An instance is explicitly destroyed via {@link Instance#destroy(Object)}</li>
     * </ul>
     *
     * @param instance the instance on which to invoke @PreDestroy methods
     * @throws InvocationTargetException if a @PreDestroy method throws an exception
     * @throws IllegalAccessException if a @PreDestroy method cannot be accessed
     * @see jakarta.annotation.PreDestroy
     * @see LifecycleMethodHelper#invokeLifecycleMethod(Object, Class)
     * @see #shutdown()
     */
   private void invokePreDestroy(Object instance) throws InvocationTargetException, IllegalAccessException {
       LifecycleMethodHelper.invokeLifecycleMethod(instance, PreDestroy.class);
    }

    /**
     * Determines whether a method has been overridden in a subclass according to JSR-330 rules.
     * This is critical for correctly processing {@link Inject @Inject} methods in class hierarchies,
     * as JSR-330 §5.2 specifies that overridden methods should not be injected multiple times.
     *
     * <p><b>JSR-330 Method Override Rules:</b>
     * <ul>
     *   <li><b>Private methods:</b> Never considered overridden (private methods are not inherited)</li>
     *   <li><b>Public methods:</b> Can be overridden across package boundaries</li>
     *   <li><b>Protected methods:</b> Can be overridden across package boundaries</li>
     *   <li><b>Package-private methods:</b> Can only be overridden within the same package</li>
     * </ul>
     *
     * <p>A method is considered overridden if:
     * <ol>
     *   <li>It is not private</li>
     *   <li>The leaf class is different from the method's declaring class</li>
     *   <li>A method with the same signature exists in the leaf class or its hierarchy</li>
     *   <li>If the method is package-private, both declaring and overriding classes are in the same package</li>
     * </ol>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * class Parent {
     *     @Inject void injectMethod() { } // Will be called
     * }
     * class Child extends Parent {
     *     @Override @Inject void injectMethod() { } // Will NOT be called (overridden)
     * }
     * }</pre>
     *
     * @param superMethod the method in the superclass to check for override
     * @param leafClass the most derived class in the hierarchy to check
     * @return true if the method is overridden in leafClass according to JSR-330 rules, false otherwise
     * @see jakarta.inject.Inject
     * @see #findMethod(Class, String, Class[])
     * @see #getPackageName(Class)
     */
    boolean isOverridden(Method superMethod, Class<?> leafClass) {
        if (Modifier.isPrivate(superMethod.getModifiers())) {
            return false;
        }
        if (superMethod.getDeclaringClass().equals(leafClass)) {
            return false;
        }

        Method subMethod = findMethod(leafClass, superMethod.getName(), superMethod.getParameterTypes());
        if (subMethod == null || subMethod.equals(superMethod)) {
            return false;
        }

        // Check JSR-330 package-private rules:
        // Package-private methods can only override if they're in the same package
        boolean isSuperPackagePrivate = !Modifier.isPublic(superMethod.getModifiers()) &&
                !Modifier.isProtected(superMethod.getModifiers()) &&
                !Modifier.isPrivate(superMethod.getModifiers());

        if (isSuperPackagePrivate) {
            // Package-private method is only overridden if a subclass method is in the same package
            return getPackageName(superMethod.getDeclaringClass())
                    .equals(getPackageName(subMethod.getDeclaringClass()));
        }

        return true;
    }

    /**
     * Extracts the package name from a class's fully qualified name. This is used in conjunction
     * with {@link #isOverridden(Method, Class)} to determine whether package-private methods can
     * be overridden according to JSR-330 rules.
     *
     * <p><b>Examples:</b>
     * <ul>
     *   <li>{@code getPackageName(java.lang.String.class)} returns {@code "java.lang"}</li>
     *   <li>{@code getPackageName(int.class)} returns {@code ""} (primitives have no package)</li>
     *   <li>{@code getPackageName(int[].class)} returns {@code ""} (arrays have no package)</li>
     * </ul>
     *
     * <p>For classes in the default package (which is rare in modern Java and typically only
     * includes primitives and arrays), this method returns an empty string.
     *
     * @param clazz the class whose package name to extract
     * @return the fully qualified package name or empty string if the class has no package
     * @see #isOverridden(Method, Class)
     * @see Class#getPackage()
     */
    String getPackageName(Class<?> clazz) {
        String name = clazz.getName();
        int lastDot = name.lastIndexOf('.');
        return (lastDot == -1) ? "" : name.substring(0, lastDot);
    }

    /**
     * Searches for a method with the given signature in the class hierarchy, traversing from
     * the specified class up through its superclasses until the method is found or {@link Object}
     * is reached. This method is essential for finding methods that may be private or package-private,
     * which {@link Class#getMethod(String, Class[])} cannot locate.
     *
     * <p><b>Search Behavior:</b>
     * <ul>
     *   <li>Starts with the given class and searches each superclass in order</li>
     *   <li>Uses {@link Class#getDeclaredMethod(String, Class[])} to find private and package-private methods</li>
     *   <li>Stops searching when {@link Object} is reached (Object methods are excluded)</li>
     *   <li>Returns null if no matching method is found before reaching Object</li>
     * </ul>
     *
     * <p>The search stops at the Object class because methods declared in Object (toString, equals,
     * hashCode, etc.) are not relevant for JSR-330 method override detection with {@code @Inject}.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * class Parent {
     *     private void privateMethod() { }
     *     void packageMethod() { }
     * }
     * class Child extends Parent { }
     *
     * // Can find privateMethod in Parent even though it's private
     * Method m = findMethod(Child.class, "privateMethod", new Class[0]);
     * assert m != null;
     * assert m.getDeclaringClass() == Parent.class;
     * }</pre>
     *
     * @param clazz the class to start searching from
     * @param name the name of the method to find
     * @param parameterTypes the parameter types of the method signature
     * @return the Method if found in the hierarchy (excluding Object), or null if not found
     * @see Class#getDeclaredMethod(String, Class[])
     * @see #isOverridden(Method, Class)
     */
    Method findMethod(Class<?> clazz, String name, Class<?>[] parameterTypes) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Validates that a type is suitable for dependency injection according to JSR-330 specifications
     * and Java language constraints. This method enforces type safety and prevents injection of
     * problematic types that cannot be properly instantiated or managed by the dependency injection
     * container.
     *
     * <p><b>Validation Rules (JSR-330 and Java Constraints):</b>
     * <ol>
     *   <li><b>Enums:</b> Cannot be injected because enums have predefined instances and
     *       cannot be instantiated via constructors. Use {@code @Inject} on fields within
     *       the enum instead.</li>
     *   <li><b>Primitives:</b> Cannot be injected because primitives have no constructors and
     *       cannot be null. Use wrapper classes (Integer, Boolean, etc.) instead.</li>
     *   <li><b>Synthetic Classes:</b> Cannot be injected because these are compiler-generated
     *       internal classes (e.g., lambda implementations, bridges) not intended for direct use.</li>
     *   <li><b>Local Classes:</b> Cannot be injected because local classes (defined within methods)
     *       may capture local variables and have complex scoping rules.</li>
     *   <li><b>Anonymous Classes:</b> Cannot be injected because anonymous classes have no
     *       constructors that can be annotated with {@code @Inject} and are typically
     *       single-use instances.</li>
     *   <li><b>Non-static Inner Classes:</b> Cannot be injected because non-static inner classes
     *       require an enclosing instance, which the injector cannot provide. Use static
     *       nested classes instead.</li>
     * </ol>
     *
     * <p><b>Recursive Validation:</b>
     * For parameterized types (generics), this method recursively validates all type arguments.
     * For example, {@code List<MyEnum>} will be rejected because {@code MyEnum} is not injectable,
     * even though {@code List} itself would be valid. This ensures type safety throughout the
     * entire type hierarchy.
     *
     * <p><b>Examples of Invalid Types:</b>
     * <pre>{@code
     * // Enums - INVALID
     * @Inject MyEnum myEnum; // Rejected
     *
     * // Primitives - INVALID
     * @Inject int count; // Rejected (use Integer instead)
     *
     * // Non-static inner classes - INVALID
     * class Outer {
     *     class Inner { } // Rejected (make it static)
     * }
     *
     * // Parameterized with invalid type argument - INVALID
     * @Inject List<MyEnum> enums; // Rejected (MyEnum is not injectable)
     * }</pre>
     *
     * @param type the type to validate (can be a Class or ParameterizedType)
     * @throws IllegalArgumentException if the type violates any injection constraints
     * @see RawTypeExtractor#getRawType(Type)
     * @see jakarta.inject.Inject
     */
    void checkClassValidity(Type type) {
        Class<?> clazz = RawTypeExtractor.getRawType(type);

        // Basic JSR-330 and Java constraints
        if (clazz.isEnum()) {
            throw new IllegalArgumentException("Cannot inject an enum");
        }
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("Cannot inject a primitive");
        }
        if (clazz.isSynthetic()) {
            throw new IllegalArgumentException("Cannot inject a synthetic class");
        }
        if (clazz.isLocalClass()) {
            throw new IllegalArgumentException("Cannot inject a local class");
        }
        if (clazz.isAnonymousClass()) {
            throw new IllegalArgumentException("Cannot inject an anonymous class");
        }
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            throw new IllegalArgumentException("Cannot inject a non-static inner class");
        }

        // Recursive validation for Parameterized Types (Generics)
        // This ensures Holder<MyEnum> is rejected if MyEnum is not injectable
        if (type instanceof ParameterizedType) {
            for (Type arg : ((ParameterizedType) type).getActualTypeArguments()) {
                // We only validate classes/types that the injector would actually try to resolve
                if (arg instanceof Class<?> || arg instanceof ParameterizedType) {
                    checkClassValidity(arg);
                }
            }
        }
    }

    /**
     * Extracts qualifier annotations from a parameter per JSR-330. Qualifiers are annotations
     * that are themselves annotated with {@link jakarta.inject.Qualifier @Qualifier}. If no
     * qualifiers are present, the {@link Default @Default} qualifier is added automatically.
     *
     * @param param the parameter to extract qualifiers from
     * @return collection of qualifier annotations (at least {@link Default @Default})
     * @see jakarta.inject.Qualifier
     * @see jakarta.enterprise.inject.Default
     */
    Collection<Annotation> getQualifiers(Parameter param) {
        Collection<Annotation> qualifiers = Arrays.stream(param.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .collect(Collectors.toList());
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
        return qualifiers;
    }

    /**
     * Extracts qualifier annotations from a field per JSR-330. Qualifiers are annotations
     * that are themselves annotated with {@link jakarta.inject.Qualifier @Qualifier}. If no
     * qualifiers are present, the {@link Default @Default} qualifier is added automatically.
     *
     * @param field the field to extract qualifiers from
     * @return collection of qualifier annotations (at least {@link Default @Default})
     * @see jakarta.inject.Qualifier
     * @see jakarta.enterprise.inject.Default
     */
    Collection<Annotation> getQualifiers(Field field) {
        Collection<Annotation> qualifiers = Arrays.stream(field.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .collect(Collectors.toList());
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
        return qualifiers;
    }

    /**
     * Creates an {@link Instance} wrapper for a field injection point. Extracts the generic
     * type argument (e.g., {@code String} from {@code Instance<String>}) and the field's
     * qualifiers to create the appropriate Instance implementation.
     *
     * @param field the field requiring an Instance wrapper
     * @return Instance implementation that can lazily resolve and provide instances
     * @see jakarta.enterprise.inject.Instance
     */
    Instance<?> createInstanceWrapper(Field field) {
        ParameterizedType type = (ParameterizedType) field.getGenericType();
        Class<?> genericType = (Class<?>) type.getActualTypeArguments()[0];
        Collection<Annotation> qualifiers = getQualifiers(field);
        return createInstance(genericType, qualifiers);
    }

    /**
     * Creates an {@link Instance} wrapper for a parameter injection point. Extracts the generic
     * type argument (e.g., {@code String} from {@code Instance<String>}) and the parameter's
     * qualifiers to create the appropriate Instance implementation.
     *
     * @param param the parameter requiring an Instance wrapper
     * @return Instance implementation that can lazily resolve and provide instances
     * @see jakarta.enterprise.inject.Instance
     */
    Instance<?> createInstanceWrapper(Parameter param) {
        ParameterizedType type = (ParameterizedType) param.getParameterizedType();
        Class<?> genericType = (Class<?>) type.getActualTypeArguments()[0];
        Collection<Annotation> qualifiers = getQualifiers(param);
        return createInstance(genericType, qualifiers);
    }

    /**
     * Creates an {@link Instance} implementation per CDI specification (JSR-299/346).
     * Instance provides lazy and programmatic access to bean instances, allowing iteration
     * over multiple implementations and explicit destruction via {@link Instance#destroy(Object)}.
     *
     * <p>The returned Instance supports:
     * <ul>
     *   <li>{@link Instance#get()} - Lazily retrieves an instance</li>
     *   <li>{@link Instance#select(Annotation...)} - Refines selection with additional qualifiers</li>
     *   <li>{@link Instance#isAmbiguous()} - Checks if multiple implementations exist</li>
     *   <li>{@link Instance#isUnsatisfied()} - Checks if no implementations exist</li>
     *   <li>{@link Instance#iterator()} - Iterates over all matching implementations</li>
     *   <li>{@link Instance#destroy(Object)} - Explicitly invokes {@link PreDestroy} on an instance</li>
     * </ul>
     *
     * @param <T> the type of instances this Instance provides
     * @param type the class of instances to provide
     * @param qualifiers the qualifiers to use for instance resolution
     * @return Instance implementation for the specified type and qualifiers
     * @see jakarta.enterprise.inject.Instance
     * @see jakarta.annotation.PreDestroy
     */
    <T> Instance<T> createInstance(Class<T> type, Collection<Annotation> qualifiers) {
        // Create a resolution strategy that delegates to InjectorImpl's methods
        InstanceImpl.ResolutionStrategy<T> strategy = new InstanceImpl.ResolutionStrategy<T>() {
            @Override
            public T resolveInstance(Class<T> typeToResolve, Collection<Annotation> quals) throws Exception {
                return inject(typeToResolve, new Stack<>(), quals);
            }

            @Override
            public Collection<Class<? extends T>> resolveImplementations(Class<T> typeToResolve, Collection<Annotation> quals) throws Exception {
                return classResolver.<T>resolveImplementations(typeToResolve, quals);
            }

            @Override
            public void invokePreDestroy(T instance) throws InvocationTargetException, IllegalAccessException {
                InjectorImpl.this.invokePreDestroy(instance);
            }
        };

        // Look up the Bean metadata from KnowledgeBase so Handle#getBean can return it
        java.util.function.Function<Class<? extends T>, jakarta.enterprise.inject.spi.Bean<? extends T>> beanLookup = beanClass -> {
            for (jakarta.enterprise.inject.spi.Bean<?> bean : knowledgeBase.getValidBeans()) {
                if (bean.getBeanClass().equals(beanClass)) {
                    @SuppressWarnings("unchecked")
                    jakarta.enterprise.inject.spi.Bean<? extends T> cast = (jakarta.enterprise.inject.spi.Bean<? extends T>) bean;
                    return cast;
                }
            }
            return null;
        };

        return new InstanceImpl<>(type, qualifiers, strategy, beanLookup);
    }

    /**
     * Creates an {@link jakarta.enterprise.event.Event} wrapper for a field injection point.
     * Extracts the generic type argument (e.g., {@code String} from {@code Event<String>})
     * and the field's qualifiers to create the appropriate Event implementation.
     *
     * @param field the field requiring an Event wrapper
     * @return Event implementation that can fire events
     * @see jakarta.enterprise.event.Event
     */
    jakarta.enterprise.event.Event<?> createEventWrapper(Field field) {
        ParameterizedType type = (ParameterizedType) field.getGenericType();
        Type genericType = type.getActualTypeArguments()[0];
        Collection<Annotation> qualifiers = getQualifiers(field);
        return createEvent(genericType, qualifiers);
    }

    /**
     * Creates an {@link jakarta.enterprise.event.Event} wrapper for a parameter injection point.
     * Extracts the generic type argument (e.g., {@code String} from {@code Event<String>})
     * and the parameter's qualifiers to create the appropriate Event implementation.
     *
     * @param param the parameter requiring an Event wrapper
     * @return Event implementation that can fire events
     * @see jakarta.enterprise.event.Event
     */
    jakarta.enterprise.event.Event<?> createEventWrapper(Parameter param) {
        ParameterizedType type = (ParameterizedType) param.getParameterizedType();
        Type genericType = type.getActualTypeArguments()[0];
        Collection<Annotation> qualifiers = getQualifiers(param);
        return createEvent(genericType, qualifiers);
    }

    /**
     * Creates an {@link jakarta.enterprise.event.Event} implementation per CDI specification (JSR-299/346).
     * Event provides synchronous and asynchronous event firing to registered observer methods.
     *
     * <p>The returned Event supports:
     * <ul>
     *   <li>{@link jakarta.enterprise.event.Event#fire(Object)} - Fires synchronous event to @Observes methods</li>
     *   <li>{@link jakarta.enterprise.event.Event#fireAsync(Object)} - Fires async event to @ObservesAsync methods</li>
     *   <li>{@link jakarta.enterprise.event.Event#select(Annotation...)} - Refines selection with additional qualifiers</li>
     * </ul>
     *
     * @param <T> the type of events this Event instance can fire
     * @param eventType the type of events to fire
     * @param qualifiers the qualifiers to use for observer matching
     * @return Event implementation for the specified type and qualifiers
     * @see jakarta.enterprise.event.Event
     * @see jakarta.enterprise.event.Observes
     * @see jakarta.enterprise.event.ObservesAsync
     */
    <T> jakarta.enterprise.event.Event<T> createEvent(Type eventType, Collection<Annotation> qualifiers) {
        Set<Annotation> qualifierSet = new HashSet<>(qualifiers);
        return new EventImpl<>(eventType, qualifierSet, knowledgeBase, beanResolver, contextManager, transactionServices);
    }

    /**
     * Clears all internal state, including custom scope handlers and static injection tracking.
     * After clearing, the default {@link Singleton} scope is re-registered. This method is
     * primarily used for testing purposes.
     *
     * <p><b>Warning:</b> This does not destroy existing scoped instances. It only clears the
     * injector's internal state. Existing singleton instances will become unreachable but not
     * explicitly destroyed.
     *
     * @see #registerDefaultScopes()
     */
    void clearState() {
        scopeRegistry.clear();
        registerDefaultScopes();
        injectedStaticClasses.clear();
    }

    /**
     * Initiates graceful shutdown of all registered scopes, invoking {@link PreDestroy @PreDestroy}
     * methods on all managed instances per JSR-250. This method is automatically called during JVM
     * shutdown via the registered shutdown hook.
     *
     * <p>Each {@link ScopeHandler} is closed in turn. If a handler throws an exception during
     * closure, it is caught and ignored to allow other handlers to complete their cleanup.
     *
     * @see jakarta.annotation.PreDestroy
     * @see ScopeHandler#close()
     * @see #addShutdownHook()
     */
    @Override
    public void shutdown() {
        // Notify custom scopes
        for (ScopeHandler handler : scopeRegistry.values()) {
            try {
                handler.close();
            } catch (Exception e) {
                // Log but continue
            }
        }
        classResolver.clearCaches();
    }
}
