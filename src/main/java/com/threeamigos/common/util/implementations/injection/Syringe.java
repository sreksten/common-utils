package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.builtinbeans.BeanManagerBean;
import com.threeamigos.common.util.implementations.injection.builtinbeans.ConversationBean;
import com.threeamigos.common.util.implementations.injection.builtinbeans.InjectionPointBean;
import com.threeamigos.common.util.implementations.injection.scopes.ContextManager;
import com.threeamigos.common.util.implementations.injection.discovery.*;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.events.ObserverMethodInfo;
import com.threeamigos.common.util.implementations.injection.resolution.BeanAttributesImpl;
import com.threeamigos.common.util.implementations.injection.resolution.BeanImpl;
import com.threeamigos.common.util.implementations.injection.resolution.BeanResolver;
import com.threeamigos.common.util.implementations.injection.resolution.ProducerBean;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.injection.spi.InjectionTargetFactoryImpl;
import com.threeamigos.common.util.implementations.injection.spi.SyntheticBean;
import com.threeamigos.common.util.implementations.injection.spi.SyntheticProducerBeanImpl;
import com.threeamigos.common.util.implementations.injection.spi.spievents.*;
import com.threeamigos.common.util.implementations.messagehandler.ConsoleMessageHandler;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Syringe - CDI 4.1 compliant container implementation.
 *
 * <p>This class implements the complete CDI 4.1 container lifecycle, including:
 * <ul>
 *   <li>Extension loading via ServiceLoader (portable extensions)</li>
 *   <li>Bean discovery (classpath scanning with beans.xml detection)</li>
 *   <li>Extension event firing (BeforeBeanDiscovery, ProcessAnnotatedType, AfterBeanDiscovery, etc.)</li>
 *   <li>Bean validation and registration</li>
 *   <li>Deployment validation</li>
 *   <li>Application lifecycle management</li>
 * </ul>
 *
 * <p><b>CDI 4.1 Container Lifecycle:</b>
 * <pre>
 * 1. Application Initialization
 *    - Load extensions via ServiceLoader
 *    - Create BeanManager
 *
 * 2. Bean Discovery (Type Discovery)
 *    → Fire BeforeBeanDiscovery event
 *    - Scan classpath for classes
 *    - Detect bean archives (explicit/implicit)
 *    → Fire ProcessAnnotatedType for each discovered class
 *    → Fire ProcessInjectionPoint, ProcessInjectionTarget, ProcessBeanAttributes
 *    → Fire ProcessBean, ProcessProducer, ProcessObserverMethod
 *    → Fire AfterBeanDiscovery event
 *
 * 3. Validation
 *    → Fire AfterDeploymentValidation event
 *    - Validate all beans, injection points, decorators, interceptors
 *    - Detect ambiguous dependencies, unsatisfied dependencies
 *    - Check specialization, alternatives, stereotypes
 *
 * 4. Application Running
 *    - Container is ready for use
 *    - Beans can be resolved and created
 *
 * 5. Application Shutdown
 *    → Fire BeforeShutdown event
 *    - Destroy all context instances
 *    - Call @PreDestroy on all beans
 * </pre>
 *
 * @author Stefano Reksten
 */
public class Syringe {

    /**
     * A MessageHandler implementation to log messages
     */
    private final MessageHandler messageHandler;

    /**
     * Package names to scan for beans.
     */
    private final String[] packageNames;

    /**
     * Knowledge base containing all discovered beans, interceptors, decorators, observers.
     */
    private final KnowledgeBase knowledgeBase;

    /**
     * The ContextManager, responsible for handling Scopes
     */
    private final ContextManager contextManager;

    /**
     * Set of extension class names to be loaded.
     * Extensions must implement jakarta.enterprise.inject.spi.Extension.
     */
    private final Set<String> extensionClassNames = new HashSet<>();

    /**
     * Optional forced bean archive mode used during validation/discovery processing.
     * When set, this mode overrides detected archive mode for all classes that are
     * already discovered and present in the KnowledgeBase.
     *
     * <p>Important limitation: this does not alter scanner-time archive detection.
     * If an archive is skipped by the scanner because it is detected as
     * bean-discovery-mode="none", those classes are never added and therefore cannot
     * be affected by this override.
     */
    private BeanArchiveMode forcedBeanArchiveMode;

    /**
     * Loaded extension instances.
     */
    private final List<Extension> extensions = new ArrayList<>();

    /**
     * The BeanManager - central interface for programmatic CDI access.
     */
    private BeanManagerImpl beanManager;

    /**
     * Whether the container has been initialized.
     */
    private boolean initialized = false;

    /**
     * Custom contexts to register programmatically before container initialization.
     * These will be registered during the AfterBeanDiscovery phase.
     * Map key: scope annotation class, Map value: context implementation
     */
    private final Map<Class<? extends Annotation>, Context> customContextsToRegister = new HashMap<>();

    public Syringe() {
        this.messageHandler = new ConsoleMessageHandler();
        this.packageNames = new String[0];
        knowledgeBase = new KnowledgeBase(messageHandler);
        contextManager = new ContextManager(messageHandler);
    }

    public Syringe(String... packageNames) {
        this.messageHandler = new ConsoleMessageHandler();
        this.packageNames = packageNames != null ? packageNames : new String[0];
        knowledgeBase = new KnowledgeBase(messageHandler);
        contextManager = new ContextManager(messageHandler);
    }

    public Syringe(MessageHandler messageHandler, Class<?>... classes) {
        this.messageHandler = messageHandler;
        this.packageNames = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            this.packageNames[i] = classes[i].getPackage().getName();
        }
        knowledgeBase = new KnowledgeBase(messageHandler);
        contextManager = new ContextManager(messageHandler);
    }

    /**
     * Manually exclude one or more classes from scanning.
     * This is useful for excluding classes that are known to be problematic (e.g., when running tests) or unnecessary.
     *
     * @param classes the classes to exclude
     */
    public void exclude(Class<?> ... classes) {
        knowledgeBase.exclude(classes);
    }

    /**
     * Registers a portable extension by class name.
     * Extensions will be loaded and initialized during {@link #setup()}.
     *
     * @param extensionClassName fully qualified class name of the extension
     */
    public void addExtension(String extensionClassName) {
        if (initialized) {
            throw new IllegalStateException("Cannot add extensions after container initialization");
        }
        extensionClassNames.add(extensionClassName);
        info("Queued extension: " + extensionClassName);
    }

    /**
     * Programmatically enables an {@code @Alternative} bean class.
     *
     * <p>This is useful in tests and controlled bootstrap scenarios where alternatives
     * must be selected without beans.xml.
     *
     * @param alternativeClass alternative bean class to enable
     */
    public void enableAlternative(Class<?> alternativeClass) {
        if (initialized) {
            throw new IllegalStateException("Cannot enable alternatives after container initialization");
        }

        knowledgeBase.enableAlternative(alternativeClass);
        info("Programmatically enabled alternative: " + alternativeClass.getName());
    }

    /**
     * Forces a bean archive mode for all discovered classes.
     *
     * <p>This is primarily useful for tests that need deterministic discovery behavior
     * regardless of detected beans.xml metadata.
     *
     * <p>Scope of this override:
     * <ul>
     *   <li>It changes the mode used when validating/registering discovered classes.</li>
     *   <li>It does not change scanner-time decisions in {@code ParallelClasspathScanner}.</li>
     *   <li>Archives skipped as {@code BeanArchiveMode.NONE} remain skipped.</li>
     * </ul>
     *
     * @param beanArchiveMode the mode to force (for example {@link BeanArchiveMode#EXPLICIT} or
     *                        {@link BeanArchiveMode#IMPLICIT}); cannot be {@code null}
     */
    public void forceBeanArchiveMode(BeanArchiveMode beanArchiveMode) {
        if (initialized) {
            throw new IllegalStateException("Cannot force bean archive mode after container initialization");
        }
        if (beanArchiveMode == null) {
            throw new IllegalArgumentException("beanArchiveMode cannot be null");
        }

        this.forcedBeanArchiveMode = beanArchiveMode;
        info("Forced bean archive mode: " + beanArchiveMode);
    }

    /**
     * Registers a custom scope and its context programmatically.
     * <p>
     * <b>Non-Standard API:</b> This is a convenience method for direct container configuration
     * and testing. Standard CDI applications should register custom contexts via portable
     * extensions using {@link AfterBeanDiscovery#addContext(Context)}.
     * <p>
     * This method allows you to register custom scopes before calling {@link #setup()}.
     * The contexts will be registered during the AfterBeanDiscovery phase of container
     * initialization.
     * <p>
     * <h3>Example Usage:</h3>
     * <pre>{@code
     * // Create container
     * Syringe syringe = new Syringe("com.myapp");
     *
     * // Register custom scope programmatically
     * syringe.registerCustomContext(MyCustomScope.class, new MyCustomScopeContext());
     *
     * // Initialize container (custom context will be registered during AfterBeanDiscovery)
     * syringe.setup();
     *
     * // Use beans with custom scope
     * BeanManager bm = syringe.getBeanManager();
     * MyBean bean = bm.getReference(...);
     * }</pre>
     * <p>
     * <h3>Requirements:</h3>
     * The scope annotation must be annotated with {@code @NormalScope} or {@code @Scope}
     * from the Jakarta CDI specification. The context must properly implement
     * {@link jakarta.enterprise.context.spi.Context}.
     *
     * @param scopeAnnotation the scope annotation class (must be annotated with @NormalScope or @Scope)
     * @param context the context implementation for this scope
     * @throws IllegalStateException if the container is already initialized
     * @throws IllegalArgumentException if scopeAnnotation or context is null
     */
    public void registerCustomContext(Class<? extends Annotation> scopeAnnotation,
                                       Context context) {
        if (initialized) {
            throw new IllegalStateException("Cannot register custom contexts after container initialization.");
        }

        if (scopeAnnotation == null) {
            throw new IllegalArgumentException("scopeAnnotation cannot be null");
        }

        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }

        // Validate that the context's scope matches the provided scope annotation
        if (!context.getScope().equals(scopeAnnotation)) {
            throw new IllegalArgumentException(
                "Context scope mismatch: context.getScope() returns " +
                context.getScope().getName() + " but scopeAnnotation is " +
                scopeAnnotation.getName()
            );
        }

        customContextsToRegister.put(scopeAnnotation, context);
        info("Queued custom context for registration: @" + scopeAnnotation.getSimpleName());
    }

    /**
     * Initializes the CDI container following the complete CDI 4.1 lifecycle.
     *
     * <p><b>CDI 4.1 Container Lifecycle Steps:</b>
     * <ol>
     *   <li>Load portable extensions</li>
     *   <li>Create BeanManager and KnowledgeBase</li>
     *   <li>Fire BeforeBeanDiscovery event</li>
     *   <li>Perform bean discovery (classpath scanning)</li>
     *   <li>Fire ProcessAnnotatedType for each discovered type</li>
     *   <li>Process and validate beans</li>
     *   <li>Fire ProcessInjectionPoint, ProcessInjectionTarget events</li>
     *   <li>Fire ProcessBean events (ProcessManagedBean, ProcessProducerMethod, etc.)</li>
     *   <li>Fire ProcessObserverMethod events</li>
     *   <li>Fire AfterBeanDiscovery event</li>
     *   <li>Validate deployment (check for errors)</li>
     *   <li>Fire AfterDeploymentValidation event</li>
     * </ol>
     * <p>This method is used for standalone SE mode. It performs automatic bean
     * discovery based on the package names provided in the constructor.
     *
     * @throws DeploymentException if validation fails or extensions cause errors
     */
    public void setup() {
        initialize();
        discoverBeans();
        start();
    }

    /**
     * PHASE 1: CONTAINER INITIALIZATION.
     *
     * <p>Initializes core infrastructure:
     * <ul>
     *   <li>Creates {@link KnowledgeBase} and {@link ContextManager}</li>
     *   <li>Loads portable extensions via ServiceLoader</li>
     *   <li>Creates {@link BeanManagerImpl}</li>
     *   <li>Fires {@code BeforeBeanDiscovery} event</li>
     * </ul>
     *
     * @throws IllegalStateException if the container is already initialized
     */
    public void initialize() {
        if (initialized) {
            throw new IllegalStateException("Container already initialized");
        }

        // ============================================================
        // PHASE 1: CONTAINER INITIALIZATION
        // ============================================================
        info("Phase 1: Container Initialization");

        // Step 1.1: Load portable extensions via ServiceLoader + explicitly registered
        loadExtensions();

        // Step 1.2: Create BeanManager
        beanManager = new BeanManagerImpl(knowledgeBase, contextManager);

        // Register CDI built-in beans before any processing/validation
        registerBuiltInBeans();

        // Step 2.1: Fire BeforeBeanDiscovery event
        // Extensions can:
        // - Add new qualifiers, scopes, stereotypes, interceptor bindings
        // - Register additional beans programmatically
        fireBeforeBeanDiscovery();
    }

    /**
     * Performs bean discovery by scanning the classpath.
     *
     * <p>Steps:
     * <ol>
     *   <li>Use ParallelClasspathScanner to find all classes in specified packages</li>
     *   <li>Use BeanArchiveDetector to determine EXPLICIT/IMPLICIT mode per archive</li>
     *   <li>Collect AnnotatedType<?> for each discovered class</li>
     * </ol>
     */
    private void discoverBeans() {
        // Step 2.2: Perform bean discovery (classpath scanning)
        // - Scan for classes in specified packages
        // - Detect bean archives (explicit/implicit via beans.xml)
        // - Discover annotated types
        // NOTE: If scanner detects BeanArchiveMode.NONE for an archive, that archive is skipped
        // before class registration. forceBeanArchiveMode(...) cannot override this scanner step.
        info("Discovering beans in packages: " + Arrays.toString(packageNames));

        ParallelClasspathScanner scanner;
        try (ParallelTaskExecutor parallelTaskExecutor = ParallelTaskExecutor.createExecutor()) {
            ClassProcessor classProcessor = new ClassProcessor(parallelTaskExecutor, knowledgeBase);
            scanner = new ParallelClasspathScanner(
                    Thread.currentThread().getContextClassLoader(),
                    classProcessor,
                    knowledgeBase,
                    packageNames
            );
            parallelTaskExecutor.awaitCompletion();
        } catch (Exception e) {
            throw new DeploymentException("Bean discovery failed", e);
        }

        info("Discovered " + knowledgeBase.getClasses().size() + " classes");

        // Collect beans.xml configurations from all scanned archives
        for (BeansXml beansXml : scanner.getBeansXmlConfigurations()) {
            knowledgeBase.addBeansXml(beansXml);
        }

        // Process registered AnnotatedTypes (added programmatically via BeforeBeanDiscovery)
        processRegisteredAnnotatedTypes();
    }

    /**
     * Adds a class to the container for bean discovery.
     *
     * <p>This method should be called after {@link #initialize()} and before
     * {@link #start()}. It is intended for managed bootstrap environments
     * (like WildFly) where discovery is performed externally.
     *
     * @param clazz the class to add
     * @throws IllegalStateException if the container is already initialized or not yet initialized
     */
    public void addDiscoveredClass(Class<?> clazz) {
        if (initialized) {
            throw new IllegalStateException("Container already initialized");
        }
        if (knowledgeBase == null) {
            throw new IllegalStateException("Container not yet initialized. Call initialize() first.");
        }
        knowledgeBase.add(clazz, effectiveBeanArchiveMode(BeanArchiveMode.IMPLICIT));
    }

    /**
     * Registers CDI built-in beans required by the spec.
     */
    private void registerBuiltInBeans() {
        knowledgeBase.addBean(new BeanManagerBean(beanManager));
        knowledgeBase.addBean(new InjectionPointBean());
        knowledgeBase.addBean(new ConversationBean());
    }

    /**
     * PHASE 2-6: BEAN PROCESSING AND VALIDATION.
     *
     * <p>Completes the CDI 4.1 lifecycle:
     * <ul>
     *   <li>Fires {@code ProcessAnnotatedType} events</li>
     *   <li>Validates and registers beans</li>
     *   <li>Fires {@code ProcessInjectionPoint}, {@code ProcessInjectionTarget}, etc.</li>
     *   <li>Fires {@code AfterBeanDiscovery} and {@code AfterDeploymentValidation}</li>
     * </ul>
     *
     * @throws DeploymentException if validation fails
     */
    public void start() {
        if (initialized) {
            throw new IllegalStateException("Container already initialized");
        }

        try {
            applyForcedArchiveModeOverride();

            // ============================================================
            // PHASE 2 (CONT): PROCESS DISCOVERED TYPES
            // ============================================================
            info("Phase 2: Processing Discovered Types");

            // Step 2.3: Fire ProcessAnnotatedType<T> for each discovered type
            // Extensions can:
            // - Veto types from becoming beans
            // - Add/remove/modify annotations
            // - Wrap AnnotatedType to customize metadata
            processAnnotatedTypes();

            // ============================================================
            // PHASE 3: BEAN PROCESSING
            // ============================================================
            info("Phase 3: Bean Processing");

            // Step 3.1: Validate beans and build Bean<?> objects
            // - Check constructor eligibility
            // - Validate injection points
            // - Check scope, qualifiers, stereotypes
            validateAndRegisterBeans();
            initializeBeanDependencyResolvers();

            // Step 3.2: Fire ProcessInjectionPoint<T, X> events
            // Extensions can modify injection point metadata
            processInjectionPoints();

            // Step 3.3: Fire ProcessInjectionTarget<T> events
            // Extensions can wrap InjectionTarget to customize instantiation/injection
            processInjectionTargets();

            // Step 3.4: Fire ProcessBeanAttributes<T> events
            // Extensions can modify bean attributes (scope, qualifiers, stereotypes, name)
            processBeanAttributes();

            // Step 3.5: Fire ProcessBean events
            // - ProcessManagedBean<T> for managed beans
            // - ProcessProducerMethod<T, X> for producer methods
            // - ProcessProducerField<T, X> for producer fields
            processBean();

            // Step 3.6: Fire ProcessProducer<T, X> events
            // Extensions can wrap Producer to customize production logic
            processProducers();

            // Step 3.7: Fire ProcessObserverMethod<T, X> events
            // Extensions can modify observer method metadata
            processObserverMethods();

            // ============================================================
            // PHASE 4: AFTER BEAN DISCOVERY
            // ============================================================
            info("Phase 4: After Bean Discovery");

            // Step 4.1: Fire AfterBeanDiscovery event
            // Extensions can:
            // - Register additional beans programmatically
            // - Register custom contexts
            // - Add observer methods programmatically
            // - Register interceptors and decorators
            fireAfterBeanDiscovery();

            // Extensions may add beans programmatically during AfterBeanDiscovery.
            // Re-apply dependency resolver wiring to cover newly registered BeanImpl/ProducerBean instances.
            initializeBeanDependencyResolvers();

            // ============================================================
            // PHASE 5: VALIDATION
            // ============================================================
            info("Phase 5: Deployment Validation");

            // Step 5.1: Perform deployment validation
            // - Check for unsatisfied dependencies
            // - Check for ambiguous dependencies
            // - Validate decorators and interceptors
            // - Validate specialization
            // - Validate alternatives
            validateDeployment();

            // Step 5.2: Fire AfterDeploymentValidation event
            // Extensions can perform final validation checks
            // Any deployment problems detected here will prevent application startup
            fireAfterDeploymentValidation();

            // ============================================================
            // PHASE 6: APPLICATION READY
            // ============================================================
            info("Phase 6: Application Ready");

            initialized = true;
            info("Container initialization complete");

        } catch (DefinitionException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException("Container initialization failed", e);
        }
    }

    /**
     * Wires runtime dependency resolution into beans that perform reflective injection/production.
     *
     * <p>Both managed beans ({@link BeanImpl}) and producer beans ({@link ProducerBean})
     * delegate dependency lookup to {@link BeanResolver} during instance creation.
     */
    private void initializeBeanDependencyResolvers() {
        BeanResolver beanResolver = new BeanResolver(knowledgeBase, contextManager);
        for (Bean<?> bean : knowledgeBase.getBeans()) {
            if (bean instanceof BeanImpl<?>) {
                ((BeanImpl<?>) bean).setDependencyResolver(beanResolver);
            } else if (bean instanceof ProducerBean<?>) {
                ((ProducerBean<?>) bean).setDependencyResolver(beanResolver);
            }
        }
    }

    /**
     * Shuts down the CDI container and destroys all beans.
     *
     * <p><b>CDI 4.1 Shutdown Process:</b>
     * <ol>
     *   <li>Fire BeforeShutdown event to all extensions</li>
     *   <li>Destroy all context instances (call @PreDestroy on all beans)</li>
     *   <li>Clear all caches and references</li>
     * </ol>
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        info("Shutting down container");

        // Fire BeforeShutdown event
        fireBeforeShutdown();

        // Destroy all beans (call @PreDestroy methods)
        destroyAllBeans();

        // Clear state
        extensions.clear();
        initialized = false;

        info("Container shutdown complete");
    }

    // ============================================================
    // PHASE 1: EXTENSION LOADING
    // ============================================================

    /**
     * Loads portable extensions via ServiceLoader and explicitly registered class names.
     *
     * <p>Extensions are discovered through:
     * <ul>
     *   <li>META-INF/services/jakarta.enterprise.inject.spi.Extension (ServiceLoader)</li>
     *   <li>Explicitly registered via {@link #addExtension(String)}</li>
     * </ul>
     */
    private void loadExtensions() {
        info("Loading extensions");

        // Load extensions via ServiceLoader (standard CDI discovery)
        ServiceLoader<Extension> serviceLoader = ServiceLoader.load(
                Extension.class,
                Thread.currentThread().getContextClassLoader()
        );

        for (Extension extension : serviceLoader) {
            extensions.add(extension);
            info("Loaded extension: " + extension.getClass().getName());
        }

        int loadedCount = extensions.size();

        // Load explicitly registered extensions
        for (String className : extensionClassNames) {
            try {
                Class<?> extensionClass = Class.forName(className);
                if (!Extension.class.isAssignableFrom(extensionClass)) {
                    knowledgeBase.addDefinitionError("Extension class " + className + " does not implement the jakarta.enterprise.inject.spi.Extension interface");
                } else {
                    Extension extension = (Extension) extensionClass.getDeclaredConstructor().newInstance();
                    extensions.add(extension);
                    info("Loaded extension: " + className);
                }
                loadedCount++;
            } catch (Exception e) {
                knowledgeBase.addDefinitionError("Failed to load extension: " + className);
                log("Failed to load extension: " + className, e);
            }
        }

        info("Loaded " + loadedCount + " extension(s)");
    }

    // ============================================================
    // PHASE 2: BEAN DISCOVERY EVENTS
    // ============================================================

    /**
     * Fires BeforeBeanDiscovery event to all extensions.
     *
     * <p>Extensions can use this event to:
     * <ul>
     *   <li>Add new qualifiers via addQualifier()</li>
     *   <li>Add new scopes via addScope()</li>
     *   <li>Add new stereotypes via addStereotype()</li>
     *   <li>Add interceptor bindings via addInterceptorBinding()</li>
     *   <li>Add annotated types programmatically via addAnnotatedType()</li>
     * </ul>
     */
    private void fireBeforeBeanDiscovery() {
        info("Firing BeforeBeanDiscovery event");
        BeforeBeanDiscovery event = new BeforeBeanDiscoveryImpl(messageHandler, knowledgeBase, beanManager);
        fireEventToExtensions(event);
    }

    /**
     * Processes AnnotatedTypes that were registered programmatically via BeforeBeanDiscovery.addAnnotatedType().
     *
     * <p>These synthetic types are added to the KnowledgeBase classes collection, so they will be
     * validated and registered as beans during the normal bean processing phase.
     */
    private void processRegisteredAnnotatedTypes() {
        Map<String, AnnotatedType<?>> registeredTypes = knowledgeBase.getRegisteredAnnotatedTypes();

        if (registeredTypes.isEmpty()) {
            info("No registered AnnotatedTypes to process");
            return;
        }

        info("Processing " + registeredTypes.size() + " registered AnnotatedTypes");

        for (Map.Entry<String, AnnotatedType<?>> entry : registeredTypes.entrySet()) {
            String id = entry.getKey();
            AnnotatedType<?> annotatedType = entry.getValue();
            Class<?> clazz = annotatedType.getJavaClass();

            info("Processing registered AnnotatedType: " + clazz.getName() + " (ID: " + id + ")");

            // Add the class to KnowledgeBase so it will be processed as a bean candidate
            knowledgeBase.add(clazz, effectiveBeanArchiveMode(BeanArchiveMode.IMPLICIT));
        }

        info("Total classes after registered types: " + knowledgeBase.getClasses().size());
    }

    /**
     * Fires ProcessAnnotatedType<T> event for each discovered type.
     *
     * <p>Extensions can:
     * <ul>
     *   <li>Veto the type via veto()</li>
     *   <li>Modify the AnnotatedType via setAnnotatedType()</li>
     *   <li>Add/remove/change annotations</li>
     * </ul>
     */
    private void processAnnotatedTypes() {
        info("Processing annotated types");

        // Create exclude filter from all beans.xml configurations
        ExcludeFilter excludeFilter = new ExcludeFilter(knowledgeBase.getBeansXmlConfigurations());

        // For each discovered class:
        // 1. Check if excluded by beans.xml scan filters
        // 2. Create AnnotatedType<T> using BeanManager
        // 3. Create ProcessAnnotatedType<T> event
        // 4. Fire to all extensions
        // 5. If vetoed, mark in KnowledgeBase
        // 6. If modified, use modified AnnotatedType

        int excludedCount = 0;
        for (Class<?> clazz : new ArrayList<>(knowledgeBase.getClasses())) {
            try {
                // Step 1: Check if a class is excluded by beans.xml scan filters
                if (excludeFilter.isExcluded(clazz.getName())) {
                    knowledgeBase.vetoType(clazz);
                    excludedCount++;
                    continue;
                }

                // Step 2: Create AnnotatedType for the class using BeanManager
                AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(clazz);

                // Step 3: Create ProcessAnnotatedType event
                ProcessAnnotatedTypeImpl<?> event = new ProcessAnnotatedTypeImpl<>(messageHandler, annotatedType);

                // Step 4: Fire to all extensions
                fireEventToExtensions(event);

                // Step 5: If vetoed, mark the type in KnowledgeBase
                if (event.isVetoed()) {
                    info("Type vetoed by extension: " + clazz.getName());
                    knowledgeBase.vetoType(clazz);
                }

                // Store AnnotatedType override if modified
                AnnotatedType<?> finalAnnotatedType = event.getAnnotatedType();
                if (finalAnnotatedType != null) {
                    knowledgeBase.setAnnotatedTypeOverride(clazz, finalAnnotatedType);
                }
            } catch (Exception e) {
                log("Error processing annotated type: " + clazz.getName(), e);
            }
        }

        info("Excluded by beans.xml filters: " + excludedCount);
        info("Vetoed types (total): " + knowledgeBase.getVetoedTypes().size());
    }

    // ============================================================
    // PHASE 3: BEAN PROCESSING
    // ============================================================

    /**
     * Validates all discovered beans and registers them in the KnowledgeBase.
     *
     * <p>This uses CDI41BeanValidator to validate:
     * <ul>
     *   <li>Bean class eligibility</li>
     *   <li>Constructor requirements</li>
     *   <li>Injection point validity</li>
     *   <li>Scope correctness</li>
     *   <li>Producer method/field validity</li>
     * </ul>
     */
    private void validateAndRegisterBeans() {
        info("Validating and registering beans");

        CDI41BeanValidator validator = new CDI41BeanValidator(knowledgeBase);
        int validated = 0;

        for (Class<?> clazz : knowledgeBase.getClasses()) {
            try {
                // Effective mode honors forced override when configured; otherwise uses
                // the mode detected during scanning and recorded in KnowledgeBase.
                BeanArchiveMode mode = effectiveBeanArchiveMode(knowledgeBase.getBeanArchiveMode(clazz));
                AnnotatedType<?> override = knowledgeBase.getAnnotatedTypeOverride(clazz);
                validator.validateAndRegisterRaw(clazz, mode, override);
                validated++;
            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                log("Error validating bean class " + clazz.getName(), e);
            }
        }

        info("Validated " + validated + " class(es); registered " + knowledgeBase.getBeans().size() + " bean(s)");
    }

    /**
     * Fires ProcessInjectionPoint<T, X> events for all discovered injection points.
     *
     * <p>Extensions can modify injection point metadata.
     */
    private void processInjectionPoints() {
        info("Processing injection points");

        for (Bean<?> bean : knowledgeBase.getBeans()) {
            try {
                Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();

                for (InjectionPoint ip : injectionPoints) {
                    ProcessInjectionPointImpl<?, ?> event =
                            new ProcessInjectionPointImpl<>(messageHandler, ip, knowledgeBase);

                    fireEventToExtensions(event);

                    InjectionPoint updated = event.getInjectionPoint();
                    if (updated != ip) {
                        updateInjectionPoint(bean, ip, updated);
                    }
                }
            } catch (Exception e) {
                log("Error processing injection points for bean " + bean.getBeanClass().getName(), e);
            }
        }
    }

    /**
     * Fires ProcessInjectionTarget<T> events for all injection targets.
     *
     * <p>Extensions can wrap InjectionTarget to customize instantiation and injection.
     */
    private void processInjectionTargets() {
        info("Processing injection targets");

        for (Bean<?> bean : knowledgeBase.getBeans()) {
            if (bean instanceof BeanImpl<?>) {
                BeanImpl<?> managedBean = (BeanImpl<?>) bean;
                Class<?> beanClass = managedBean.getBeanClass();

                try {
                    AnnotatedType<?> annotatedType = new SimpleAnnotatedType<>(beanClass);
                    InjectionTargetFactory<?> factory = new InjectionTargetFactoryImpl<>(annotatedType, beanManager);

                    @SuppressWarnings("unchecked")
                    InjectionTarget<Object> injectionTarget =
                            (InjectionTarget<Object>) factory.createInjectionTarget((Bean) managedBean);

                    @SuppressWarnings("unchecked")
                    ProcessInjectionTargetImpl<Object> event =
                        new ProcessInjectionTargetImpl<>(messageHandler, knowledgeBase,
                                (AnnotatedType<Object>) annotatedType, injectionTarget);

                    fireEventToExtensions(event);

                    InjectionTarget<?> finalTarget = event.getInjectionTarget();
                    managedBean.setCustomInjectionTarget((InjectionTarget) finalTarget);
                } catch (Exception e) {
                    log("Error processing injection target for " + beanClass.getName(), e);
                }
            }
        }
    }

    /**
     * Fires ProcessBeanAttributes<T> events for all beans.
     *
     * <p>Extensions can modify bean attributes (scope, qualifiers, stereotypes, name).
     */
    private void processBeanAttributes() {
        info("Processing bean attributes");

        List<Bean<?>> vetoed = new ArrayList<>();

        for (Bean<?> bean : knowledgeBase.getBeans()) {
            try {
                BeanAttributes<?> attrs = new BeanAttributesImpl<>(bean.getName(), bean.getQualifiers(),
                    bean.getScope(), bean.getStereotypes(), bean.getTypes(), bean.isAlternative());

                Annotated annotated = new SimpleAnnotatedType<>(bean.getBeanClass());
                ProcessBeanAttributesImpl<?> event =
                    new ProcessBeanAttributesImpl<>(messageHandler, annotated, attrs, beanManager, knowledgeBase);

                fireEventToExtensions(event);

                if (event.isVetoed()) {
                    vetoed.add(bean);
                    continue;
                }

                if (event.isIgnoreFinalMethods()) {
                    knowledgeBase.addWarning("ProcessBeanAttributes ignoreFinalMethods requested for " +
                                             bean.getBeanClass().getName());
                }

                BeanAttributes<?> finalAttrs = event.getBeanAttributes();
                applyBeanAttributes(bean, finalAttrs);

            } catch (Exception e) {
                log("Error processing bean attributes for bean " + bean.getBeanClass().getName(), e);
            }
        }

        // Remove vetoed beans
        if (!vetoed.isEmpty()) {
            knowledgeBase.getBeans().removeAll(vetoed);
            info("Vetoed " + vetoed.size() + " bean(s) via ProcessBeanAttributes");
        }
    }

    /**
     * Fires ProcessBean events (ProcessManagedBean, ProcessProducerMethod, ProcessProducerField, ProcessSyntheticBean).
     *
     * <p>Extensions can inspect final Bean<?> objects before deployment validation.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processBean() {
        info("Processing beans");

        Collection<Bean<?>> allBeans = knowledgeBase.getBeans();
        info("Found " + allBeans.size() + " total bean(s)");

        int managedCount = 0;
        int producerMethodCount = 0;
        int producerFieldCount = 0;
        int syntheticCount = 0;

        for (Bean<?> bean : allBeans) {
            try {
                // Determine the bean type and fire an appropriate event
                if (bean instanceof SyntheticBean) {
                    // Synthetic bean - registered via AfterBeanDiscovery.addBean()
                    ProcessSyntheticBeanImpl event = new ProcessSyntheticBeanImpl(messageHandler, knowledgeBase, bean,
                            null);
                    fireEventToExtensions(event);
                    syntheticCount++;

                } else if (bean instanceof ProducerBean) {
                    // Producer beans are already handled by processProducers()
                    // which fires ProcessProducerMethod/ProcessProducerField
                    ProducerBean<?> producerBean = (ProducerBean<?>) bean;
                    if (producerBean.isMethod()) {
                        producerMethodCount++;
                    } else if (producerBean.isField()) {
                        producerFieldCount++;
                    }

                } else if (bean instanceof BeanImpl) {
                    // Managed bean - discovered via classpath scanning
                    BeanImpl<?> managedBean = (BeanImpl<?>) bean;
                    AnnotatedType<?> annotatedType = knowledgeBase.getAnnotatedTypeOverride(managedBean.getBeanClass());
                    if (annotatedType == null) {
                        annotatedType = beanManager.createAnnotatedType(managedBean.getBeanClass());
                    }

                    @SuppressWarnings({"rawtypes", "unchecked"})
                    ProcessManagedBeanImpl<?> event = new ProcessManagedBeanImpl(messageHandler, knowledgeBase,
                         managedBean, annotatedType);
                    fireEventToExtensions(event);
                    managedCount++;

                } else {
                    // Built-in beans (BeanManager, InjectionPoint, etc.)
                    // These don't get ProcessBean events
                    info("Skipping built-in bean: " + bean.getBeanClass().getSimpleName());
                }
            } catch (Exception e) {
                log("Error processing bean " + bean.getBeanClass().getName(), e);
            }
        }

        info("Processed: " + managedCount + " managed, " + producerMethodCount + " producer methods, " +
                producerFieldCount + " producer fields, " + syntheticCount + " synthetic");
    }

    /**
     * Fires ProcessProducer<T, X> events for all producers.
     *
     * <p>Extensions can wrap Producer to customize production logic.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processProducers() {
        info("Processing producers");

        Collection<ProducerBean<?>> producers = knowledgeBase.getProducerBeans();
        info("Found " + producers.size() + " producers");

        for (ProducerBean<?> producerBean : producers) {
            try {
                // Get the declaring class
                Class<?> declaringClass = producerBean.getDeclaringClass();

                // Create AnnotatedType for the declaring class
                AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(declaringClass);

                if (producerBean.isMethod()) {
                    // Process producer method
                    Method method = producerBean.getProducerMethod();

                    // Find the matching AnnotatedMethod
                    AnnotatedMethod annotatedMethod = findAnnotatedMethod(annotatedType, method);

                    if (annotatedMethod != null) {
                        // Create a Producer wrapper for the ProducerBean
                        Producer producer = new ProducerBeanAdapter(producerBean);

                        // Create and fire ProcessProducerMethod event
                        ProcessProducerMethodImpl event = new ProcessProducerMethodImpl(messageHandler,
                                knowledgeBase, producerBean, annotatedMethod, producer, null);

                        fireEventToExtensions(event);

                        Producer finalProducer = event.getFinalProducer();
                        if (finalProducer != producer) {
                            info("Producer wrapped for method: " + declaringClass.getSimpleName() + "." +
                                    method.getName());
                        }
                        replaceProducerBean(producerBean, finalProducer);
                    }
                } else if (producerBean.isField()) {
                    // Process producer field
                    Field field = producerBean.getProducerField();

                    // Find the matching AnnotatedField
                    AnnotatedField annotatedField = findAnnotatedField(annotatedType, field);

                    if (annotatedField != null) {
                        // Create a Producer wrapper for the ProducerBean
                        Producer producer = new ProducerBeanAdapter(producerBean);

                        // Create and fire ProcessProducerField event
                        ProcessProducerFieldImpl event = new ProcessProducerFieldImpl(messageHandler, knowledgeBase,
                            producerBean, annotatedField, producer, null);

                        fireEventToExtensions(event);

                        Producer finalProducer = event.getFinalProducer();
                        if (finalProducer != producer) {
                            info("Producer wrapped for field: " + declaringClass.getSimpleName() + "." +
                                    field.getName());
                        }
                        replaceProducerBean(producerBean, finalProducer);
                    }
                }
            } catch (Exception e) {
                log("Error processing producer", e);
            }
        }
    }

    /**
     * Finds the AnnotatedMethod matching the given Method.
     */
    private AnnotatedMethod<?> findAnnotatedMethod(AnnotatedType<?> annotatedType, Method method) {
        for (AnnotatedMethod<?> am : annotatedType.getMethods()) {
            if (am.getJavaMember().equals(method)) {
                return am;
            }
        }
        return null;
    }

    /**
     * Finds the AnnotatedField matching the given Field.
     */
    private AnnotatedField<?> findAnnotatedField(AnnotatedType<?> annotatedType, Field field) {
        for (AnnotatedField<?> af : annotatedType.getFields()) {
            if (af.getJavaMember().equals(field)) {
                return af;
            }
        }
        return null;
    }

    /**
     * Replaces a discovered ProducerBean with a synthetic bean that delegates to the
     * final Producer selected by extensions via ProcessProducer events.
     * This ensures the container uses the wrapped/replaced Producer for lifecycle operations.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void replaceProducerBean(ProducerBean<?> original, Producer<?> finalProducer) {
        if (original == null || finalProducer == null) {
            return;
        }

        // Remove the original bean from the resolvable set
        knowledgeBase.getBeans().remove(original);

        // Create a synthetic bean that delegates create/destroy/injection points to the final producer
        Bean synthetic = new SyntheticProducerBeanImpl(original, original.getBeanClass(), finalProducer);
        knowledgeBase.addBean(synthetic);
    }

    /**
     * Simple Producer adapter that wraps a ProducerBean.
     * This allows extensions to observe and wrap producer logic.
     */
    private static class ProducerBeanAdapter<T> implements Producer<T> {
        private final ProducerBean<T> producerBean;

        ProducerBeanAdapter(ProducerBean<T> producerBean) {
            this.producerBean = producerBean;
        }

        @Override
        public T produce(CreationalContext<T> ctx) {
            return producerBean.create(ctx);
        }

        @Override
        public void dispose(T instance) {
            producerBean.destroy(instance, null);
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return producerBean.getInjectionPoints();
        }
    }

    /**
     * Fires ProcessObserverMethod<T, X> events for all observer methods.
     *
     * <p>Extensions can modify observer method metadata.
     */
    private void processObserverMethods() {
        info("Processing observer methods");

        Collection<ObserverMethodInfo> existing = new ArrayList<>(knowledgeBase.getObserverMethodInfos());
        List<ObserverMethodInfo> updated = new ArrayList<>();

        for (ObserverMethodInfo info : existing) {
            try {
                ObserverMethod<?> observer;
                AnnotatedMethod<?> annotatedMethod = null;

                if (info.isSynthetic()) {
                    observer = info.getSyntheticObserver();
                } else {
                    Method method = info.getObserverMethod();
                    AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(method.getDeclaringClass());
                    annotatedMethod = findAnnotatedMethod(annotatedType, method);
                    observer = new ReflectiveObserverMethodAdapter<>(info,
                            new BeanResolver(knowledgeBase, contextManager),
                            contextManager);
                }

                ProcessObserverMethodImpl<?, ?> event =
                        new ProcessObserverMethodImpl(messageHandler, knowledgeBase, observer, annotatedMethod);

                fireEventToExtensions(event);

                if (event.isVetoed()) {
                    continue; // remove this observer
                }

                ObserverMethod<?> finalObserver = event.getObserverMethod();
                updated.add(toObserverMethodInfo(finalObserver, info.getDeclaringBean()));

            } catch (Exception e) {
                log("Error processing observer method", e);
            }
        }

        knowledgeBase.getObserverMethodInfos().clear();
        knowledgeBase.getObserverMethodInfos().addAll(updated);
    }

    /**
     * Applies updated BeanAttributes back to the underlying bean implementation.
     */
    private void applyBeanAttributes(Bean<?> bean, BeanAttributes<?> attrs) {
        if (bean instanceof BeanImpl<?>) {
            BeanImpl<?> b = (BeanImpl<?>) bean;
            b.setName(attrs.getName());
            b.setQualifiers(attrs.getQualifiers());
            b.setScope(attrs.getScope());
            b.setStereotypes(attrs.getStereotypes());
            b.setTypes(attrs.getTypes());
        } else if (bean instanceof ProducerBean<?>) {
            ProducerBean<?> b = (ProducerBean<?>) bean;
            b.setName(attrs.getName());
            b.setQualifiers(attrs.getQualifiers());
            b.setScope(attrs.getScope());
            b.setStereotypes(attrs.getStereotypes());
            b.setTypes(attrs.getTypes());
            // the alternative flag is final; cannot be changed post-creation
        } else {
            // Synthetic or built-in beans: no-op
        }
    }

    /**
     * Replaces an injection point inside a bean with an updated instance.
     */
    private void updateInjectionPoint(Bean<?> bean, InjectionPoint original, InjectionPoint updated) {
        if (bean instanceof BeanImpl<?>) {
            ((BeanImpl<?>) bean).replaceInjectionPoint(original, updated);
        } else if (bean instanceof ProducerBean<?>) {
            ProducerBean<?> pb = (ProducerBean<?>) bean;
            pb.replaceInjectionPoint(original, updated);
        } else if (bean instanceof SyntheticBean<?>) {
            try {
                SyntheticBean<?> sb = (SyntheticBean<?>) bean;
                Set<InjectionPoint> ips = new HashSet<>(sb.getInjectionPoints());
                if (original != null) {
                    ips.remove(original);
                }
                if (updated != null) {
                    ips.add(updated);
                }

                @SuppressWarnings("unchecked")
                Function<CreationalContext<Object>, Object> createCb =
                        (Function<CreationalContext<Object>, Object>) getPrivateField(sb, "createCallback");
                @SuppressWarnings("unchecked")
                BiConsumer<Object, CreationalContext<Object>> destroyCb =
                        (BiConsumer<Object, CreationalContext<Object>>) getPrivateField(sb, "destroyCallback");
                Integer priority = (Integer) getPrivateField(sb, "priority");

                SyntheticBean<?> replacement = new SyntheticBean<>(
                        sb.getBeanClass(),
                        sb.getTypes(),
                        sb.getQualifiers(),
                        sb.getScope(),
                        sb.getName(),
                        sb.getStereotypes(),
                        sb.isAlternative(),
                        priority,
                        createCb,
                        destroyCb,
                        ips
                );
                knowledgeBase.getBeans().remove(sb);
                knowledgeBase.addBean(replacement);
            } catch (Exception e) {
                log("Error updating injection point for synthetic bean", e);
            }
        } else if (bean instanceof SyntheticProducerBeanImpl<?>) {
            try {
                SyntheticProducerBeanImpl<?> sp = (SyntheticProducerBeanImpl<?>) bean;
                Set<InjectionPoint> ips = new HashSet<>(sp.getInjectionPoints());
                if (original != null) {
                    ips.remove(original);
                }
                if (updated != null) {
                    ips.add(updated);
                }

                @SuppressWarnings("unchecked")
                Producer<Object> originalProducer = (Producer<Object>) getPrivateField(sp, "producer");
                @SuppressWarnings("unchecked")
                BeanAttributes<Object> attributes = (BeanAttributes<Object>) getPrivateField(sp, "attributes");
                Class<?> beanClass = (Class<?>) getPrivateField(sp, "beanClass");

                Producer<Object> wrapper = new Producer<Object>() {
                    @Override
                    public Object produce(CreationalContext<Object> ctx) {
                        return originalProducer.produce(ctx);
                    }

                    @Override
                    public void dispose(Object instance) {
                        originalProducer.dispose(instance);
                    }

                    @Override
                    public Set<InjectionPoint> getInjectionPoints() {
                        return ips;
                    }
                };

                SyntheticProducerBeanImpl<Object> replacement =
                        new SyntheticProducerBeanImpl<>(attributes, beanClass, wrapper);
                knowledgeBase.getBeans().remove(sp);
                knowledgeBase.addBean(replacement);
            } catch (Exception e) {
                log("Error updating injection point for synthetic producer bean", e);
            }
        }
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    private ObserverMethodInfo toObserverMethodInfo(ObserverMethod<?> observer, Bean<?> declaringBean) {
        return new ObserverMethodInfo(
                observer.getObservedType(),
                observer.getObservedQualifiers(),
                observer.getReception(),
                observer.getTransactionPhase(),
                observer.getPriority(),
                observer.isAsync(),
                declaringBean,
                observer
        );
    }

    /**
     * ObserverMethod adapter that invokes the original reflective observer method.
     * Used so ProcessObserverMethod can replace/keep reflective observers while allowing
     * extensions to wrap or veto them.
     */
    private static class ReflectiveObserverMethodAdapter<T> implements ObserverMethod<T> {
        private final ObserverMethodInfo info;
        private final BeanResolver beanResolver;
        private final ContextManager contextManager;

        ReflectiveObserverMethodAdapter(ObserverMethodInfo info,
                                        BeanResolver beanResolver,
                                        ContextManager contextManager) {
            this.info = info;
            this.beanResolver = beanResolver;
            this.contextManager = contextManager;
        }

        @Override
        public Class<?> getBeanClass() {
            return info.getDeclaringBean() != null
                    ? info.getDeclaringBean().getBeanClass()
                    : info.getObserverMethod().getDeclaringClass();
        }

        @Override
        public Type getObservedType() {
            return info.getEventType();
        }

        @Override
        public Set<Annotation> getObservedQualifiers() {
            return info.getQualifiers();
        }

        @Override
        public Reception getReception() {
            return info.getReception();
        }

        @Override
        public TransactionPhase getTransactionPhase() {
            return info.getTransactionPhase();
        }

        @Override
        public void notify(T event) {
            try {
                // Honor IF_EXISTS reception
                if (info.getReception() == Reception.IF_EXISTS && info.getDeclaringBean() != null) {
                    Class<? extends Annotation> scope = info.getDeclaringBean().getScope();
                    try {
                        com.threeamigos.common.util.implementations.injection.scopes.ScopeContext ctx =
                                contextManager.getContext(scope);
                        Object existing = ctx.getIfExists(info.getDeclaringBean());
                        if (existing == null) {
                            return; // skip notification
                        }
                    } catch (IllegalArgumentException ignored) {
                        return;
                    }
                }

                Method method = info.getObserverMethod();
                Object beanInstance = info.getDeclaringBean() != null
                        ? beanResolver.resolveDeclaringBeanInstance(info.getDeclaringBean().getBeanClass())
                        : beanResolver.resolveDeclaringBeanInstance(method.getDeclaringClass());

                java.lang.reflect.Parameter[] params = method.getParameters();
                Object[] args = new Object[params.length];
                for (int i = 0; i < params.length; i++) {
                    java.lang.reflect.Parameter p = params[i];
                    if (AnnotationsEnum.hasObservesAnnotation(p) || AnnotationsEnum.hasObservesAsyncAnnotation(p)) {
                        args[i] = event;
                    } else {
                        args[i] = beanResolver.resolve(p.getParameterizedType(), p.getAnnotations());
                    }
                }

                method.setAccessible(true);
                method.invoke(beanInstance, args);
            } catch (Exception e) {
                throw new RuntimeException("Failed to notify observer " +
                        info.getObserverMethod().getName() + ": " + e.getMessage(), e);
            }
        }

        @Override
        public boolean isAsync() {
            return info.isAsync();
        }

        @Override
        public int getPriority() {
            return info.getPriority();
        }
    }

    // ============================================================
    // PHASE 4: AFTER BEAN DISCOVERY
    // ============================================================

    /**
     * Fires AfterBeanDiscovery event to all extensions.
     *
     * <p>Extensions can:
     * <ul>
     *   <li>Register additional beans via addBean()</li>
     *   <li>Register custom contexts via addContext()</li>
     *   <li>Add observer methods via addObserverMethod()</li>
     *   <li>Add interceptors/decorators</li>
     * </ul>
     * <p>
     * This method also registers any custom contexts added programmatically
     * via {@link #registerCustomContext(Class, Context)} before container initialization.
     */
    private void fireAfterBeanDiscovery() {
        info("Firing AfterBeanDiscovery event");
        AfterBeanDiscovery event = new AfterBeanDiscoveryImpl(messageHandler, knowledgeBase, beanManager);

        // Register programmatically added custom contexts BEFORE firing to extensions
        // This allows extensions to see and potentially modify these contexts
        if (!customContextsToRegister.isEmpty()) {
            info("Registering " + customContextsToRegister.size() + " programmatically added custom contexts");

            for (Map.Entry<Class<? extends Annotation>, Context> entry : customContextsToRegister.entrySet()) {
                try {
                    event.addContext(entry.getValue());
                    info("Registered custom context for @" +entry.getKey().getSimpleName());
                } catch (Exception e) {
                    log("Failed to register custom context for @" + entry.getKey().getSimpleName(), e);
                    throw new DeploymentException("Failed to register custom context for @" + entry.getKey().getSimpleName(), e);
                }
            }
        }

        fireEventToExtensions(event);
    }

    // ============================================================
    // PHASE 5: VALIDATION
    // ============================================================

    /**
     * Performs deployment validation.
     *
     * <p>Checks for:
     * <ul>
     *   <li>Unsatisfied dependencies</li>
     *   <li>Ambiguous dependencies</li>
     *   <li>Invalid decorator/interceptor configurations</li>
     *   <li>Specialization errors</li>
     *   <li>Alternative priority conflicts</li>
     * </ul>
     *
     * @throws DeploymentException if validation fails
     */
    private void validateDeployment() {
        info("Validating deployment");

        Collection<Class<?>> excludedClasses = knowledgeBase.getExcludedClasses();
        if (!excludedClasses.isEmpty()) {
            info("Manually excluded classes:");
            for (Class<?> excludedClass : excludedClasses) {
                info("  - " + excludedClass.getName());
            }
        }

        // 1. Check for unsatisfied/ambiguous dependencies
        CDI41InjectionValidator injectionValidator = new CDI41InjectionValidator(knowledgeBase);
        injectionValidator.validateAllInjectionPoints();

        // 2. Check definition errors
        if (knowledgeBase.hasErrors()) {
            error("Deployment validation failed:");
            knowledgeBase.getDefinitionErrors().forEach(error ->
                    error("  - Definition error: " + error));
            knowledgeBase.getInjectionErrors().forEach(error ->
                    error("  - Injection error: " + error));
            knowledgeBase.getIllegalProductErrors().forEach(error ->
                    error("  - Illegal product error: " + error));
            knowledgeBase.getErrors().forEach(error ->
                    error("  - Generic Error: " + error));


            if (!knowledgeBase.getDefinitionErrors().isEmpty()) {
                throw new DefinitionException("Deployment validation failed. See log for details.");
            } else if (!knowledgeBase.getIllegalProductErrors().isEmpty()) {
                throw new IllegalProductException("Deployment validation failed. See log for details.");
            } else {
                throw new DeploymentException("Deployment validation failed. See log for details.");
            }
        }

        info("Deployment validation passed");
    }

    /**
     * Fires AfterDeploymentValidation event to all extensions.
     *
     * <p>Extensions can perform final validation checks.
     * Any deployment problems detected here will prevent application startup.
     */
    private void fireAfterDeploymentValidation() {
        info("Firing AfterDeploymentValidation event");
        AfterDeploymentValidation event = new AfterDeploymentValidationImpl(knowledgeBase);
        fireEventToExtensions(event);
    }

    // ============================================================
    // SHUTDOWN
    // ============================================================

    /**
     * Fires BeforeShutdown event to all extensions.
     *
     * <p>Extensions can perform cleanup before the container shuts down.
     */
    private void fireBeforeShutdown() {
        info("Firing BeforeShutdown event");
        BeforeShutdown event = new BeforeShutdownImpl();
        fireEventToExtensions(event);
    }

    /**
     * Destroys all beans by calling @PreDestroy methods.
     */
    private void destroyAllBeans() {
        info("Destroying all beans");

        if (contextManager != null) {
            try {
                contextManager.destroyAll();
            } catch (Exception e) {
                log("Error destroying contexts", e);
            }
        }

    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Fires an event to all registered extensions by invoking their observer methods.
     *
     * <p>This method scans each extension for methods with parameters annotated with @Observes
     * that match the event type and invokes them with the event object.
     *
     * @param event event object to fire
     * @param <T> the event type
     */
    private <T> void fireEventToExtensions(T event) {
        Class<?> eventType = event.getClass();
        List<ExtensionObserverInvocation> invocations = new ArrayList<>();

        // Collect all matching observer methods across all extensions, with priority
        for (Extension extension : extensions) {
            collectExtensionObserverMethods(extension, eventType, invocations);
        }

        // Sort by @Priority (ascending). Methods without @Priority run last.
        invocations.sort(Comparator.comparingInt(inv -> inv.priority));

        for (ExtensionObserverInvocation invocation : invocations) {
            try {
                invocation.invoke(event);
            } catch (Exception e) {
                log("Error invoking extension " + invocation.extension.getClass().getName() +
                        " for event " + eventType.getSimpleName(), e);
            }
        }
    }

    private void collectExtensionObserverMethods(Extension extension,
                                                 Class<?> eventType,
                                                 List<ExtensionObserverInvocation> sink) {
        for (Method method : extension.getClass().getMethods()) {
            Parameter[] parameters = method.getParameters();

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                if (AnnotationsEnum.hasObservesAnnotation(parameter)) {
                    Class<?> observedType = parameter.getType();
                    if (observedType.isAssignableFrom(eventType)) {
                        int priority = resolvePriority(method);
                        sink.add(new ExtensionObserverInvocation(extension, method, i, priority, beanManager,
                                knowledgeBase, messageHandler));
                    }
                }
            }
        }
    }

    private int resolvePriority(Method method) {
        Priority priorityAnn = method.getAnnotation(Priority.class);
        if (priorityAnn != null) {
            return priorityAnn.value();
        }
        // javax.annotation.Priority fallback
        javax.annotation.Priority legacy = method.getAnnotation(javax.annotation.Priority.class);
        if (legacy != null) {
            return legacy.value();
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Lightweight wrapper capturing an extension observer method invocation.
     * Sorting happens on the priority value.
     */
    private static class ExtensionObserverInvocation {
        private final Extension extension;
        private final Method method;
        private final int observesIndex;
        private final int priority;
        private final BeanManager beanManager;
        private final KnowledgeBase knowledgeBase;
        private final MessageHandler messageHandler;

        ExtensionObserverInvocation(Extension extension,
                                    Method method,
                                    int observesIndex,
                                    int priority,
                                    BeanManager beanManager,
                                    KnowledgeBase knowledgeBase,
                                    MessageHandler messageHandler) {
            this.extension = extension;
            this.method = method;
            this.observesIndex = observesIndex;
            this.priority = priority;
            this.beanManager = beanManager;
            this.knowledgeBase = knowledgeBase;
            this.messageHandler = messageHandler;
        }

        void invoke(Object event) throws Exception {
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];
            args[observesIndex] = event;

            for (int i = 0; i < parameters.length; i++) {
                if (i == observesIndex) continue;
                args[i] = resolveExtensionParameter(parameters[i]);
            }

            method.setAccessible(true);
            method.invoke(extension, args);

            messageHandler.handleInfoMessage("[Syringe] Invoked extension observer: " +
                    extension.getClass().getSimpleName() + "." + method.getName() +
                    "(@Observes " + event.getClass().getSimpleName() +
                    ", priority=" + priority + ")");
        }

        private Object resolveExtensionParameter(java.lang.reflect.Parameter parameter) {
            Class<?> pType = parameter.getType();
            if (BeanManager.class.isAssignableFrom(pType)) {
                return beanManager;
            }

            // Collect qualifier annotations on the parameter
            List<Annotation> qualifiers = new ArrayList<>();
            for (Annotation ann : parameter.getAnnotations()) {
                if (AnnotationsEnum.hasQualifierAnnotation(ann.annotationType())) {
                    qualifiers.add(ann);
                }
            }

            try {
                Set<Bean<?>> beans = beanManager.getBeans(parameter.getParameterizedType(),
                        qualifiers.toArray(new java.lang.annotation.Annotation[0]));
                Bean<?> resolved = beanManager.resolve(beans);
                if (resolved == null) {
                    throw new IllegalStateException("No bean resolved for " + parameter.getParameterizedType());
                }
                CreationalContext<?> ctx = beanManager.createCreationalContext(resolved);
                return beanManager.getReference(resolved, parameter.getParameterizedType(), ctx);
            } catch (Exception e) {
                String msg = "[Syringe] Extension observer parameter " +
                        parameter.getName() + " (" + parameter.getType().getName() +
                        ") could not be injected: " + e.getMessage();
                knowledgeBase.addDefinitionError(msg);
                throw new RuntimeException(msg, e);
            }
        }
    }

    /**
     * Returns the BeanManager for programmatic CDI access.
     *
     * @return the BeanManager
     */
    public BeanManager getBeanManager() {
        if (!initialized) {
            throw new IllegalStateException("Container not initialized. Call setup() first.");
        }
        return beanManager;
    }

    /**
     * Programmatically get an injected instance of the given bean class.
     * Uses default qualifiers unless explicit qualifiers are provided.
     */
    public <T> T inject(Class<T> beanClass, Annotation... qualifiers) {
        if (!initialized) {
            throw new IllegalStateException("Container not initialized. Call setup() first.");
        }
        BeanManager bm = getBeanManager();
        Set<Bean<?>> beans = (qualifiers != null && qualifiers.length > 0)
                ? bm.getBeans(beanClass, qualifiers)
                : bm.getBeans(beanClass);
        if (beans == null || beans.isEmpty()) {
            throw new UnsatisfiedResolutionException("No bean found for type " + beanClass.getName());
        }
        Bean<?> bean = bm.resolve(beans); // may throw AmbiguousResolutionException
        CreationalContext<?> ctx = bm.createCreationalContext(bean);
        @SuppressWarnings("unchecked")
        T instance = (T) bm.getReference(bean, beanClass, ctx);
        return instance;
    }

    /**
     * Returns the CDI instance for the container.
     *
     * <p>This enables static container access via {@code CDI.current()} when registered
     * with {@link com.threeamigos.common.util.implementations.injection.spi.SyringeCDIProvider}.
     *
     * @return the CDI instance
     */
    public jakarta.enterprise.inject.spi.CDI<Object> getCDI() {
        if (!initialized) {
            throw new IllegalStateException("Container not initialized. Call setup() first.");
        }
        return new com.threeamigos.common.util.implementations.injection.spi.CDIImpl(beanManager);
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    private BeanArchiveMode effectiveBeanArchiveMode(BeanArchiveMode discoveredMode) {
        if (forcedBeanArchiveMode != null) {
            return forcedBeanArchiveMode;
        }
        return discoveredMode != null ? discoveredMode : BeanArchiveMode.IMPLICIT;
    }

    private void applyForcedArchiveModeOverride() {
        if (forcedBeanArchiveMode == null) {
            return;
        }

        // Rewrites class-level archive mode entries for already discovered classes.
        // This does not discover additional classes; it only changes how existing
        // entries will be validated/registered.
        int updated = 0;
        for (Class<?> clazz : knowledgeBase.getClasses()) {
            knowledgeBase.add(clazz, forcedBeanArchiveMode);
            updated++;
        }
        info("Applied forced bean archive mode " + forcedBeanArchiveMode + " to " + updated + " class(es)");
    }

    private void info(String message) {
        messageHandler.handleInfoMessage("[Syringe] " + message);
    }

    private void error(String message) {
        messageHandler.handleErrorMessage("[Syringe] " + message);
    }

    private void log(String error, Exception t) {
        messageHandler.handleException("[Syringe] " + error, t);
    }
}
