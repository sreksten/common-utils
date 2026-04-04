package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;
import com.threeamigos.common.util.implementations.injection.bce.BceSupportedPhase;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.Alternatives;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.bce.BuildCompatibleExtensionRunner;
import com.threeamigos.common.util.implementations.injection.bce.BceInvokerRegistry;
import com.threeamigos.common.util.implementations.injection.builtinbeans.BeanManagerBean;
import com.threeamigos.common.util.implementations.injection.builtinbeans.ConversationBean;
import com.threeamigos.common.util.implementations.injection.builtinbeans.InjectionPointBean;
import com.threeamigos.common.util.implementations.injection.builtinbeans.InterceptionFactoryBean;
import com.threeamigos.common.util.implementations.injection.builtinbeans.RequestContextControllerBean;
import com.threeamigos.common.util.implementations.injection.builtinbeans.ActivateRequestContextInterceptor;
import com.threeamigos.common.util.implementations.injection.scopes.ContextManager;
import com.threeamigos.common.util.implementations.injection.discovery.*;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.knowledgebase.DecoratorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.InterceptorInfo;
import com.threeamigos.common.util.implementations.injection.events.EventImpl;
import com.threeamigos.common.util.implementations.injection.events.ObserverMethodInfo;
import com.threeamigos.common.util.implementations.injection.events.propagation.ConversationPropagationRegistry;
import com.threeamigos.common.util.implementations.injection.interceptors.InterceptorAwareProxyGenerator;
import com.threeamigos.common.util.implementations.injection.interceptors.InterceptorResolver;
import com.threeamigos.common.util.implementations.injection.decorators.DecoratorAwareProxyGenerator;
import com.threeamigos.common.util.implementations.injection.decorators.DecoratorResolver;
import com.threeamigos.common.util.implementations.injection.resolution.BeanAttributesImpl;
import com.threeamigos.common.util.implementations.injection.resolution.BeanImpl;
import com.threeamigos.common.util.implementations.injection.resolution.BeanResolver;
import com.threeamigos.common.util.implementations.injection.resolution.ProducerBean;
import com.threeamigos.common.util.implementations.injection.resolution.DestroyedInstanceTracker;
import com.threeamigos.common.util.implementations.injection.scopes.ClientProxyGenerator;
import com.threeamigos.common.util.implementations.injection.scopes.ConversationImpl;
import com.threeamigos.common.util.implementations.injection.scopes.InjectionPointImpl;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.injection.spi.InjectionTargetFactoryImpl;
import com.threeamigos.common.util.implementations.injection.spi.SyringeCDIProvider;
import com.threeamigos.common.util.implementations.injection.spi.SyntheticBean;
import com.threeamigos.common.util.implementations.injection.spi.SyntheticProducerBeanImpl;
import com.threeamigos.common.util.implementations.injection.spi.spievents.*;
import com.threeamigos.common.util.implementations.injection.util.AnnotatedMetadataHelper;
import com.threeamigos.common.util.implementations.injection.util.GenericTypeResolver;
import com.threeamigos.common.util.implementations.messagehandler.ConsoleMessageHandler;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.event.Shutdown;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.spi.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;

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
     * If true, keep only classes declared directly in the requested packages.
     * This is used by class-based convenience bootstrap to avoid accidental
     * pickup from sibling subpackages that contain unrelated test fixtures.
     */
    private final boolean exactPackageMatchOnly;

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
     * Explicitly registered extension instances.
     */
    private final Set<Extension> extensionInstances = new HashSet<Extension>();
    /**
     * Set of build compatible extension class names to be loaded.
     */
    private final Set<String> buildCompatibleExtensionClassNames = new HashSet<>();

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
     * Loaded build compatible extension instances.
     */
    private final List<BuildCompatibleExtension> buildCompatibleExtensions = new ArrayList<>();

    /**
     * BCE phase runner.
     */
    private BuildCompatibleExtensionRunner buildCompatibleExtensionRunner;
    private final BceInvokerRegistry bceInvokerRegistry = new BceInvokerRegistry();

    /**
     * The BeanManager - central interface for programmatic CDI access.
     */
    private BeanManagerImpl beanManager;

    /**
     * Whether the container has been initialized.
     */
    private boolean initialized = false;
    /**
     * Whether container shutdown has started.
     */
    private boolean shutdownStarted = false;
    /**
     * ClassLoader used when retaining dynamic BCE metadata for this container lifecycle.
     */
    private ClassLoader dynamicAnnotationClassLoader;
    /**
     * Whether this container lifecycle retained dynamic BCE metadata.
     */
    private boolean dynamicAnnotationsRetained = false;
    /**
     * Whether BeforeBeanDiscovery has already been fired for this container lifecycle.
     */
    private boolean beforeBeanDiscoveryFired = false;
    /**
     * Whether BeforeShutdown has already been fired for this container lifecycle.
     */
    private boolean beforeShutdownFired = false;
    /**
     * If true, expose CDI Lite behavior for CDI#getBeanManager() where only BeanContainer
     * methods are portable.
     */
    private boolean cdiLiteMode = false;
    private boolean cdiFullLegacyInterceptionEnabled = true;
    private boolean legacyCdi10NewEnabled = false;
    private boolean allowNonPortableAsyncObserverEventParameterPriority = false;

    /**
     * Custom contexts to register programmatically before container initialization.
     * These will be registered during the AfterBeanDiscovery phase.
     * Map key: scope annotation class, Map value: context implementation
     */
    private final Map<Class<? extends Annotation>, Context> customContextsToRegister = new HashMap<>();
    private final Set<String> processedSyntheticAnnotatedTypeIds = new HashSet<String>();
    private final Set<Class<?>> syntheticAnnotatedTypeClasses = new HashSet<Class<?>>();
    private InterceptorAwareProxyGenerator runtimeInterceptorAwareProxyGenerator;
    private DecoratorAwareProxyGenerator runtimeDecoratorAwareProxyGenerator;

    public Syringe() {
        this.messageHandler = new ConsoleMessageHandler();
        this.packageNames = new String[0];
        this.exactPackageMatchOnly = false;
        knowledgeBase = new KnowledgeBase(messageHandler);
        contextManager = new ContextManager(messageHandler);
    }

    public Syringe(String... packageNames) {
        this.messageHandler = new ConsoleMessageHandler();
        this.packageNames = packageNames != null ? packageNames : new String[0];
        this.exactPackageMatchOnly = false;
        knowledgeBase = new KnowledgeBase(messageHandler);
        contextManager = new ContextManager(messageHandler);
    }

    public Syringe(MessageHandler messageHandler, Class<?>... classes) {
        this.messageHandler = messageHandler;
        this.packageNames = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            this.packageNames[i] = classes[i].getPackage().getName();
        }
        this.exactPackageMatchOnly = true;
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
     * Registers a portable extension instance directly.
     *
     * @param extension extension instance
     */
    public void addExtension(Extension extension) {
        if (initialized) {
            throw new IllegalStateException("Cannot add extensions after container initialization");
        }
        if (extension == null) {
            throw new IllegalArgumentException("extension cannot be null");
        }
        extensionInstances.add(extension);
        info("Queued extension instance: " + extension.getClass().getName());
    }

    /**
     * Registers a build compatible extension by class name.
     * Build compatible extensions are loaded and invoked during {@link #setup()} BCE checkpoints.
     *
     * @param extensionClassName fully qualified class name of the build compatible extension
     */
    public void addBuildCompatibleExtension(String extensionClassName) {
        if (initialized) {
            throw new IllegalStateException("Cannot add build compatible extensions after container initialization");
        }
        buildCompatibleExtensionClassNames.add(extensionClassName);
        info("Queued build compatible extension: " + extensionClassName);
    }

    /**
     * Forces CDI Lite mode semantics for CDI#getBeanManager() view.
     * In this mode, invoking BeanManager methods not inherited from BeanContainer
     * is treated as non-portable behavior.
     *
     * @param cdiLiteMode true to enable CDI Lite BeanManager surface restrictions
     */
    public void forceCdiLiteMode(boolean cdiLiteMode) {
        if (initialized) {
            throw new IllegalStateException("Cannot change CDI mode after container initialization");
        }
        this.cdiLiteMode = cdiLiteMode;
        // Keep interception behavior aligned with selected CDI mode by default:
        // CDI Lite -> strict non-portable checks; CDI Full -> allow legacy forms.
        this.cdiFullLegacyInterceptionEnabled = !cdiLiteMode;
    }

    /**
     * Enables CDI Full legacy interception forms such as {@code @jakarta.interceptor.Interceptors}.
     *
     * <p>When disabled (default), these forms are treated as non-portable behavior for CDI Lite compatibility.
     *
     * @param enabled true to enable legacy interception forms
     */
    public void enableCdiFullLegacyInterception(boolean enabled) {
        if (initialized) {
            throw new IllegalStateException("Cannot change CDI interception mode after container initialization");
        }
        this.cdiFullLegacyInterceptionEnabled = enabled;
    }

    /**
     * Enables legacy CDI 1.0 {@code @javax.enterprise.inject.New} compatibility.
     *
     * <p>When disabled (default), {@code @New} injection points remain unsatisfied.
     * When enabled, {@code @New} resolves to a dependent-style contextual instance of the
     * selected bean class.
     *
     * @param enabled true to enable legacy {@code @New} compatibility
     */
    public void enableLegacyCdi10New(boolean enabled) {
        if (initialized) {
            throw new IllegalStateException("Cannot change legacy @New mode after container initialization");
        }
        this.legacyCdi10NewEnabled = enabled;
    }

    /**
     * Allows non-portable behavior where an asynchronous observer event parameter is annotated with {@code @Priority}.
     *
     * <p>By default this remains disabled and such observer methods cause
     * {@link com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException}
     * during deployment validation.
     *
     * <p><b>CDI 4.1 note:</b> This switch should stay disabled for spec-conformant behavior.
     * It exists only for legacy compatibility tests that intentionally exercise older,
     * non-portable observer declarations.
     *
     * @param enabled true to allow this non-portable observer declaration
     */
    public void allowNonPortableAsyncObserverEventParameterPriority(boolean enabled) {
        if (initialized) {
            throw new IllegalStateException(
                    "Cannot change async observer @Priority non-portable mode after container initialization");
        }
        this.allowNonPortableAsyncObserverEventParameterPriority = enabled;
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
        discover();
        start();
    }

    /**
     * Performs classpath bean discovery between {@link #initialize()} and {@link #start()}.
     */
    public void discover() {
        discoverBeans();
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
        shutdownStarted = false;
        beforeBeanDiscoveryFired = false;
        beforeShutdownFired = false;
        dynamicAnnotationClassLoader = null;
        dynamicAnnotationsRetained = false;
        processedSyntheticAnnotatedTypeIds.clear();
        syntheticAnnotatedTypeClasses.clear();

        // ============================================================
        // PHASE 1: CONTAINER INITIALIZATION
        // ============================================================
        info("Phase 1: Container Initialization");

        // Step 1.1: Load portable extensions via ServiceLoader + explicitly registered
        loadExtensions();
        loadBuildCompatibleExtensions();

        // Step 1.2: Create BeanManager
        beanManager = new BeanManagerImpl(knowledgeBase, contextManager);
        dynamicAnnotationClassLoader = beanManager.getRegistrationClassLoader();
        AnnotationsEnum.retainDynamicAnnotationsForClassLoader(dynamicAnnotationClassLoader);
        dynamicAnnotationsRetained = true;
        beanManager.setLegacyCdi10NewEnabled(legacyCdi10NewEnabled);
        beanManager.registerExtensions(extensions);
        buildCompatibleExtensionRunner = new BuildCompatibleExtensionRunner(
            messageHandler, knowledgeBase, beanManager, bceInvokerRegistry);

        // Register CDI built-in beans before any processing/validation
        registerBuiltInBeans();

        // Step 2.1: Fire BeforeBeanDiscovery event
        // Extensions can:
        // - Add new qualifiers, scopes, stereotypes, interceptor bindings
        // - Register additional beans programmatically
        fireBeforeBeanDiscovery();
        beforeBeanDiscoveryFired = true;
        fireBuildCompatibleExtensionPhase(BceSupportedPhase.DISCOVERY);
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
        Set<Class<?>> preexistingDiscoveredClasses = new HashSet<Class<?>>(knowledgeBase.getClasses());
        try (ParallelTaskExecutor parallelTaskExecutor = ParallelTaskExecutor.createExecutor()) {
            ClassProcessor classProcessor = new ClassProcessor(parallelTaskExecutor, knowledgeBase);
            scanner = new ParallelClasspathScanner(
                    Thread.currentThread().getContextClassLoader(),
                    classProcessor,
                    knowledgeBase,
                    packageNames
            );
            parallelTaskExecutor.awaitCompletion();
            filterDiscoveredClassesToRequestedPackages(preexistingDiscoveredClasses);
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

    private void filterDiscoveredClassesToRequestedPackages(Set<Class<?>> preexistingDiscoveredClasses) {
        if (!exactPackageMatchOnly || packageNames == null || packageNames.length == 0) {
            return;
        }
        Set<String> allowedPackages = new HashSet<String>();
        for (String packageName : packageNames) {
            if (packageName != null && !packageName.isEmpty()) {
                allowedPackages.add(packageName);
            }
        }
        if (allowedPackages.isEmpty()) {
            return;
        }
        for (Class<?> discovered : new ArrayList<Class<?>>(knowledgeBase.getClasses())) {
            if (preexistingDiscoveredClasses != null && preexistingDiscoveredClasses.contains(discovered)) {
                continue;
            }
            Package discoveredPackage = discovered.getPackage();
            String discoveredPackageName = discoveredPackage != null ? discoveredPackage.getName() : "";
            if (!allowedPackages.contains(discoveredPackageName)) {
                knowledgeBase.removeDiscoveredClass(discovered);
            }
        }
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
        addDiscoveredClass(clazz, BeanArchiveMode.IMPLICIT);
    }

    public void addDiscoveredClass(Class<?> clazz, BeanArchiveMode beanArchiveMode) {
        if (initialized) {
            throw new IllegalStateException("Container already initialized");
        }
        if (knowledgeBase == null) {
            throw new IllegalStateException("Container not yet initialized. Call initialize() first.");
        }
        if (knowledgeBase.getClasses().contains(clazz)) {
            throw new NonPortableBehaviourException(
                    "Non-portable behavior: bean class " + clazz.getName() +
                            " was already discovered in another bean archive");
        }
        BeanArchiveMode mode = beanArchiveMode != null ? beanArchiveMode : BeanArchiveMode.IMPLICIT;
        knowledgeBase.add(clazz, effectiveBeanArchiveMode(mode));
    }

    /**
     * Registers a parsed beans.xml configuration for managed bootstrap environments.
     *
     * <p>This is used when class discovery is delegated to an application server and
     * beans.xml parsing is performed externally.
     *
     * @param beansXml parsed beans.xml configuration
     */
    public void addBeansXmlConfiguration(BeansXml beansXml) {
        if (initialized) {
            throw new IllegalStateException("Container already initialized");
        }
        if (knowledgeBase == null) {
            throw new IllegalStateException("Container not yet initialized. Call initialize() first.");
        }
        knowledgeBase.addBeansXml(beansXml);
    }

    /**
     * Registers CDI built-in beans required by the spec.
     */
    private void registerBuiltInBeans() {
        knowledgeBase.addBean(new BeanManagerBean(beanManager));
        knowledgeBase.addBean(new InjectionPointBean());
        knowledgeBase.addBean(new InterceptionFactoryBean());
        knowledgeBase.addBean(new ConversationBean());
        knowledgeBase.addBean(new RequestContextControllerBean(contextManager));
        knowledgeBase.add(ActivateRequestContextInterceptor.class, BeanArchiveMode.IMPLICIT);
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
            fireBuildCompatibleExtensionPhase(BceSupportedPhase.ENHANCEMENT);
            fireAfterTypeDiscovery();

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
            fireBuildCompatibleExtensionPhase(BceSupportedPhase.REGISTRATION);

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
            fireBuildCompatibleExtensionPhase(BceSupportedPhase.SYNTHESIS);

            // Extensions may add beans programmatically during AfterBeanDiscovery.
            // Re-apply dependency resolver wiring to cover newly registered BeanImpl/ProducerBean instances.
            initializeBeanDependencyResolvers();
            // CDI 4.1 §13.5.2: after synthesis, run registration callbacks for newly registered synthetic components.
            fireBuildCompatibleExtensionPhase(BceSupportedPhase.REGISTRATION);

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
            fireBuildCompatibleExtensionPhase(BceSupportedPhase.VALIDATION);
            // Re-validate after BCE @Validation; calls to Messages.error(...) must become deployment problems.
            validateDeployment();

            // Step 5.2: Fire AfterDeploymentValidation event
            // Extensions can perform final validation checks
            // Any deployment problems detected here will prevent application startup
            fireAfterDeploymentValidation();

            // Fire built-in application context initialized event after deployment is fully validated
            // so all observers are discovered and ready to receive it.
            contextManager.fireApplicationContextInitialized();

            // CDI 4.1 9.6.1: fire Startup after application context initialization.
            Set<Annotation> startupQualifiers = new HashSet<Annotation>();
            startupQualifiers.add(Any.Literal.INSTANCE);
            new EventImpl<Startup>(Startup.class, startupQualifiers, knowledgeBase, beanManager.getBeanResolver(),
                    contextManager, beanManager.getBeanResolver().getTransactionServices(),
                    null, null, true).fire(new Startup());

            // ============================================================
            // PHASE 6: APPLICATION READY
            // ============================================================
            info("Phase 6: Application Ready");

            // Ensure CDI.current() can resolve this container in managed bootstrap paths
            // (e.g. WildFly integration) where no explicit global provider registration occurs.
            SyringeCDIProvider.ensureProviderConfigured();
            SyringeCDIProvider.registerGlobalCDI(getCDI());
            ClientProxyGenerator.registerContainer(beanManager.getRegistrationClassLoader(), beanManager, contextManager);

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
        BeanResolver beanResolver = beanManager.getBeanResolver();
        InterceptorResolver interceptorResolver = new InterceptorResolver(knowledgeBase);
        InterceptorAwareProxyGenerator interceptorAwareProxyGenerator = new InterceptorAwareProxyGenerator();
        DecoratorResolver decoratorResolver = new DecoratorResolver(knowledgeBase);
        DecoratorAwareProxyGenerator decoratorAwareProxyGenerator = new DecoratorAwareProxyGenerator();
        runtimeInterceptorAwareProxyGenerator = interceptorAwareProxyGenerator;
        runtimeDecoratorAwareProxyGenerator = decoratorAwareProxyGenerator;

        for (Bean<?> bean : knowledgeBase.getBeans()) {
            if (bean instanceof BeanImpl<?>) {
                BeanImpl<?> beanImpl = (BeanImpl<?>) bean;
                beanImpl.setDependencyResolver(beanResolver);
                beanImpl.setInterceptorResolver(interceptorResolver);
                beanImpl.setKnowledgeBase(knowledgeBase);
                beanImpl.setInterceptorAwareProxyGenerator(interceptorAwareProxyGenerator);
                beanImpl.setDecoratorResolver(decoratorResolver);
                beanImpl.setDecoratorAwareProxyGenerator(decoratorAwareProxyGenerator);
                beanImpl.setBeanManager(beanManager);
                beanImpl.buildMethodInterceptorChains();
            } else if (bean instanceof ProducerBean<?>) {
                ((ProducerBean<?>) bean).setDependencyResolver(beanResolver);
            }
        }

        contextManager.setRequestContextLifecycleListener(new ContextManager.RequestContextLifecycleListener() {
            @Override
            public void onInitialized() {
                beanManager.getEvent()
                        .select(Object.class, Initialized.Literal.of(RequestScoped.class), Any.Literal.INSTANCE)
                        .fire(new Object());
            }

            @Override
            public void onBeforeDestroyed() {
                beanManager.getEvent()
                        .select(Object.class, BeforeDestroyed.Literal.of(RequestScoped.class), Any.Literal.INSTANCE)
                        .fire(new Object());
            }

            @Override
            public void onDestroyed() {
                beanManager.getEvent()
                        .select(Object.class, Destroyed.Literal.of(RequestScoped.class), Any.Literal.INSTANCE)
                        .fire(new Object());
            }
        });

        contextManager.setApplicationContextLifecycleListener(new ContextManager.ApplicationContextLifecycleListener() {
            @Override
            public void onInitialized() {
                beanManager.getEvent()
                        .select(Object.class, Initialized.Literal.of(ApplicationScoped.class), Any.Literal.INSTANCE)
                        .fire(new Object());
            }

            @Override
            public void onBeforeDestroyed() {
                beanManager.getEvent()
                        .select(Object.class, BeforeDestroyed.Literal.of(ApplicationScoped.class), Any.Literal.INSTANCE)
                        .fire(new Object());
            }

            @Override
            public void onDestroyed() {
                beanManager.getEvent()
                        .select(Object.class, Destroyed.Literal.of(ApplicationScoped.class), Any.Literal.INSTANCE)
                        .fire(new Object());
            }
        });
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
        if (shutdownStarted) {
            return;
        }
        shutdownStarted = true;
        try {
            if (!initialized) {
                return;
            }

            info("Shutting down container");

            // CDI 4.1 9.6.2: fire Shutdown during container shutdown and not later than
            // @BeforeDestroyed(ApplicationScoped.class).
            Set<Annotation> shutdownQualifiers = new HashSet<Annotation>();
            shutdownQualifiers.add(Any.Literal.INSTANCE);
            new EventImpl<Shutdown>(Shutdown.class, shutdownQualifiers, knowledgeBase, beanManager.getBeanResolver(),
                    contextManager, beanManager.getBeanResolver().getTransactionServices(),
                    null, null, true).fire(new Shutdown());

            // Destroy all beans (call @PreDestroy methods)
            destroyAllBeans();

            // Fire BeforeShutdown as the final lifecycle event after contexts are destroyed.
            fireBeforeShutdown();
        } finally {
            cleanupStaticState();
            cleanupInstanceState();
            // Clear state
            extensions.clear();
            buildCompatibleExtensions.clear();
            initialized = false;
            info("Container shutdown complete");
        }
    }

    private void cleanupStaticState() {
        ClassLoader classLoader = null;
        if (beanManager != null) {
            classLoader = beanManager.getRegistrationClassLoader();
            beanManager.unregisterFromGlobalRegistries();
        }
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = Syringe.class.getClassLoader();
        }

        ClientProxyGenerator.unregisterContainer(classLoader);
        InterceptorAwareProxyGenerator.clearTargetAroundInvokeCache();
        InterceptorAwareProxyGenerator.clearTargetAroundInvokeCacheForClassLoader(classLoader);
        BeansXmlParser.clearJaxbContextCacheForClassLoader(classLoader);
        if (dynamicAnnotationsRetained && dynamicAnnotationClassLoader != null) {
            AnnotationsEnum.releaseDynamicAnnotationsForClassLoader(dynamicAnnotationClassLoader);
        }
        ConversationImpl.clearAllGlobalState();
        ConversationPropagationRegistry.clear();
        DestroyedInstanceTracker.clear();
        EventImpl.clearStaticState();
        SyringeCDIProvider.unregisterThreadLocalCDI();
        SyringeCDIProvider.unregisterGlobalCDI();
    }

    private void cleanupInstanceState() {
        // Release instance registries and BCE handles.
        bceInvokerRegistry.clear();
        extensionClassNames.clear();
        extensionInstances.clear();
        buildCompatibleExtensionClassNames.clear();
        customContextsToRegister.clear();
        processedSyntheticAnnotatedTypeIds.clear();
        syntheticAnnotatedTypeClasses.clear();
        buildCompatibleExtensionRunner = null;

        // Drop runtime metadata references eagerly.
        knowledgeBase.clearAllState();

        if (beanManager != null) {
            try {
                beanManager.clearRuntimeState();
            } catch (Exception ignored) {
                // Best-effort cleanup.
            }
        }
        if (runtimeInterceptorAwareProxyGenerator != null) {
            runtimeInterceptorAwareProxyGenerator.clearCache();
            runtimeInterceptorAwareProxyGenerator = null;
        }
        if (runtimeDecoratorAwareProxyGenerator != null) {
            runtimeDecoratorAwareProxyGenerator.clearCache();
            runtimeDecoratorAwareProxyGenerator = null;
        }
        dynamicAnnotationsRetained = false;
        dynamicAnnotationClassLoader = null;
        beanManager = null;
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
        Set<String> loadedExtensionClassNames = new HashSet<String>();

        for (Extension extension : extensionInstances) {
            String className = extension.getClass().getName();
            if (loadedExtensionClassNames.add(className)) {
                extensions.add(extension);
                info("Loaded extension instance: " + className);
            }
        }

        // Load extensions via ServiceLoader (standard CDI discovery)
        ServiceLoader<Extension> serviceLoader = ServiceLoader.load(
                Extension.class,
                Thread.currentThread().getContextClassLoader()
        );

        for (Extension extension : serviceLoader) {
            String className = extension.getClass().getName();
            if (loadedExtensionClassNames.add(className)) {
                extensions.add(extension);
                info("Loaded extension: " + className);
            }
        }

        int loadedCount = extensions.size();

        // Load explicitly registered extensions
        for (String className : extensionClassNames) {
            try {
                Class<?> extensionClass = Class.forName(className);
                if (!Extension.class.isAssignableFrom(extensionClass)) {
                    knowledgeBase.addDefinitionError("Extension class " + className + " does not implement the jakarta.enterprise.inject.spi.Extension interface");
                } else {
                    if (loadedExtensionClassNames.add(className)) {
                        Extension extension = (Extension) extensionClass.getDeclaredConstructor().newInstance();
                        extensions.add(extension);
                        info("Loaded extension: " + className);
                    } else {
                        info("Skipped duplicate extension registration: " + className);
                    }
                }
                loadedCount++;
            } catch (Exception e) {
                knowledgeBase.addDefinitionError("Failed to load extension: " + className);
                log("Failed to load extension: " + className, e);
            }
        }

        info("Loaded " + loadedCount + " extension(s)");
    }

    /**
     * Loads build compatible extensions via ServiceLoader and explicitly registered class names.
     */
    private void loadBuildCompatibleExtensions() {
        info("Loading build compatible extensions");
        Set<String> loadedBceClassNames = new HashSet<String>();
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        ServiceLoader<BuildCompatibleExtension> serviceLoader = ServiceLoader.load(
            BuildCompatibleExtension.class,
            tccl
        );

        for (BuildCompatibleExtension extension : serviceLoader) {
            String className = extension.getClass().getName();
            if (loadedBceClassNames.add(className)) {
                if (shouldSkipBuildCompatibleExtension(extension.getClass())) {
                    info("Skipped build compatible extension due to @SkipIfPortableExtensionPresent: " + className);
                    continue;
                }
                buildCompatibleExtensions.add(extension);
                info("Loaded build compatible extension: " + className);
            }
        }

        discoverBuildCompatibleExtensionsFromServiceResources(loadedBceClassNames, tccl);

        int loadedCount = buildCompatibleExtensions.size();

        for (String className : buildCompatibleExtensionClassNames) {
            try {
                Class<?> extensionClass = loadClassWithTcclFallback(className, tccl);
                if (!BuildCompatibleExtension.class.isAssignableFrom(extensionClass)) {
                    knowledgeBase.addDefinitionError("Build compatible extension class " + className +
                        " does not implement jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension");
                } else {
                    if (loadedBceClassNames.add(className)) {
                        if (shouldSkipBuildCompatibleExtension(extensionClass)) {
                            info("Skipped build compatible extension due to @SkipIfPortableExtensionPresent: " + className);
                            continue;
                        }
                        BuildCompatibleExtension extension =
                            (BuildCompatibleExtension) extensionClass.getDeclaredConstructor().newInstance();
                        buildCompatibleExtensions.add(extension);
                        info("Loaded build compatible extension: " + className);
                    } else {
                        info("Skipped duplicate build compatible extension registration: " + className);
                    }
                }
                loadedCount++;
            } catch (Exception e) {
                knowledgeBase.addDefinitionError("Failed to load build compatible extension: " + className);
                log("Failed to load build compatible extension: " + className, e);
            }
        }

        info("Loaded " + loadedCount + " build compatible extension(s)");
    }

    private void discoverBuildCompatibleExtensionsFromServiceResources(Set<String> loadedBceClassNames,
                                                                       ClassLoader tccl) {
        final String servicePath = "META-INF/services/jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension";
        try {
            Enumeration<URL> resources = (tccl != null)
                ? tccl.getResources(servicePath)
                : Syringe.class.getClassLoader().getResources(servicePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                List<String> providerClassNames = readServiceProviderClassNames(url);
                for (String providerClassName : providerClassNames) {
                    if (!loadedBceClassNames.add(providerClassName)) {
                        continue;
                    }
                    try {
                        Class<?> providerClass = loadClassWithTcclFallback(providerClassName, tccl);
                        if (!BuildCompatibleExtension.class.isAssignableFrom(providerClass)) {
                            knowledgeBase.addDefinitionError("Build compatible extension class " + providerClassName +
                                " from " + servicePath + " does not implement " +
                                "jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension");
                            continue;
                        }
                        if (shouldSkipBuildCompatibleExtension(providerClass)) {
                            info("Skipped build compatible extension due to @SkipIfPortableExtensionPresent: " + providerClassName);
                            continue;
                        }
                        BuildCompatibleExtension extension =
                            (BuildCompatibleExtension) providerClass.getDeclaredConstructor().newInstance();
                        buildCompatibleExtensions.add(extension);
                        info("Loaded build compatible extension from service resource: " + providerClassName);
                    } catch (Exception e) {
                        knowledgeBase.addDefinitionError("Failed to load build compatible extension from service resource: " +
                            providerClassName);
                        log("Failed to load build compatible extension from service resource: " + providerClassName, e);
                    }
                }
            }
        } catch (Exception e) {
            log("Failed to scan build compatible extension service resources", e);
        }
    }

    private List<String> readServiceProviderClassNames(URL url) {
        List<String> classNames = new ArrayList<String>();
        InputStream stream = null;
        BufferedReader reader = null;
        try {
            stream = url.openStream();
            reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int commentIdx = trimmed.indexOf('#');
                if (commentIdx >= 0) {
                    trimmed = trimmed.substring(0, commentIdx).trim();
                }
                if (!trimmed.isEmpty()) {
                    classNames.add(trimmed);
                }
            }
        } catch (Exception e) {
            log("Failed to read build compatible extension service resource: " + url, e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                } else if (stream != null) {
                    stream.close();
                }
            } catch (Exception ignored) {
                // Best effort.
            }
        }
        return classNames;
    }

    private Class<?> loadClassWithTcclFallback(String className, ClassLoader tccl) throws ClassNotFoundException {
        if (tccl != null) {
            try {
                return Class.forName(className, true, tccl);
            } catch (ClassNotFoundException ignored) {
                // Fallback below.
            }
        }
        return Class.forName(className);
    }

    private boolean shouldSkipBuildCompatibleExtension(Class<?> extensionClass) {
        if (!hasSkipIfPortableExtensionPresentAnnotation(extensionClass)) {
            return false;
        }
        jakarta.enterprise.inject.build.compatible.spi.SkipIfPortableExtensionPresent skipAnnotation =
                getSkipIfPortableExtensionPresentAnnotation(extensionClass);
        if (skipAnnotation == null) {
            return false;
        }
        Class<? extends Extension> portableExtensionType = skipAnnotation.value();
        if (portableExtensionType == null) {
            return false;
        }
        for (Extension extension : extensions) {
            if (portableExtensionType.isInstance(extension)) {
                return true;
            }
        }
        for (String extensionClassName : extensionClassNames) {
            try {
                Class<?> configuredExtensionType = Class.forName(extensionClassName);
                if (portableExtensionType.isAssignableFrom(configuredExtensionType)) {
                    return true;
                }
            } catch (ClassNotFoundException ignored) {
                // Definition error is handled in loadExtensions(); skip evaluation remains best-effort.
            }
        }
        return false;
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
            if (processedSyntheticAnnotatedTypeIds.contains(id)) {
                continue;
            }
            AnnotatedType<?> annotatedType = entry.getValue();
            Class<?> clazz = annotatedType.getJavaClass();

            info("Processing registered AnnotatedType: " + clazz.getName() + " (ID: " + id + ")");

            if (shouldSkipProcessAnnotatedTypeEvent(clazz)) {
                knowledgeBase.vetoType(clazz);
                processedSyntheticAnnotatedTypeIds.add(id);
                continue;
            }

            @SuppressWarnings({"rawtypes", "unchecked"})
            ProcessSyntheticAnnotatedTypeImpl<?> event = new ProcessSyntheticAnnotatedTypeImpl(
                    messageHandler,
                    annotatedType,
                    knowledgeBase.getRegisteredAnnotatedTypeSource(id));
            fireEventToExtensions(event);

            if (event.isVetoed()) {
                knowledgeBase.vetoType(clazz);
                processedSyntheticAnnotatedTypeIds.add(id);
                continue;
            }

            AnnotatedType<?> finalAnnotatedType = event.getAnnotatedTypeInternal();
            if (finalAnnotatedType == null) {
                finalAnnotatedType = annotatedType;
            }

            // Add the class to KnowledgeBase so it will be processed as a bean candidate.
            knowledgeBase.add(clazz, effectiveBeanArchiveMode(BeanArchiveMode.IMPLICIT));
            syntheticAnnotatedTypeClasses.add(clazz);
            knowledgeBase.setAnnotatedTypeOverride(clazz, finalAnnotatedType);
            processedSyntheticAnnotatedTypeIds.add(id);
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
                if (syntheticAnnotatedTypeClasses.contains(clazz)) {
                    continue;
                }
                // Step 1: Check if a class is excluded by beans.xml scan filters
                if (excludeFilter.isExcluded(clazz.getName())) {
                    knowledgeBase.vetoType(clazz);
                    excludedCount++;
                    continue;
                }
                if (shouldSkipProcessAnnotatedTypeEvent(clazz)) {
                    knowledgeBase.vetoType(clazz);
                    continue;
                }
                if (!shouldIncludeTypeInDiscoveryForArchiveMode(clazz)) {
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

                // Store AnnotatedType override only when extensions replace/configure metadata.
                // Keeping default BeanManager.createAnnotatedType() output as an override can
                // unintentionally change baseline annotation inheritance behavior during validation.
                AnnotatedType<?> finalAnnotatedType = event.getAnnotatedTypeInternal();
                if (finalAnnotatedType != null && finalAnnotatedType != annotatedType) {
                    knowledgeBase.setAnnotatedTypeOverride(clazz, finalAnnotatedType);
                }
            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new DefinitionException("Error processing annotated type: " + clazz.getName(), e);
            }
        }

        info("Excluded by beans.xml filters: " + excludedCount);
        info("Vetoed types (total): " + knowledgeBase.getVetoedTypes().size());
    }

    private boolean shouldSkipProcessAnnotatedTypeEvent(Class<?> clazz) {
        if (clazz == null) {
            return true;
        }
        if (clazz.isAnnotation()) {
            return true;
        }
        if (hasVetoedAnnotation(clazz)) {
            return true;
        }
        return isPackageOrParentPackageVetoed(clazz.getPackage());
    }

    private boolean shouldIncludeTypeInDiscoveryForArchiveMode(Class<?> clazz) {
        BeanArchiveMode mode = knowledgeBase.getBeanArchiveMode(clazz);
        if (mode == null) {
            mode = BeanArchiveMode.IMPLICIT;
        }
        if (BeanArchiveMode.NONE.equals(mode)) {
            return false;
        }
        if (BeanArchiveMode.EXPLICIT.equals(mode)) {
            return true;
        }
        // IMPLICIT/TRIMMED discovery only includes Java classes with bean defining annotations.
        if (clazz.isInterface() || clazz.isEnum() || clazz.isAnnotation()) {
            return false;
        }
        return hasBeanDefiningAnnotation(clazz);
    }

    private boolean hasBeanDefiningAnnotation(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        if (hasInterceptorAnnotation(clazz) || hasDecoratorAnnotation(clazz)) {
            return true;
        }
        for (Annotation annotation : clazz.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (isScopeOrNormalScope(type) || isStereotype(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isScopeOrNormalScope(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return false;
        }
        return hasScopeAnnotation(annotationType)
                || hasNormalScopeAnnotation(annotationType)
                || hasDependentAnnotation(annotationType);
    }

    private boolean isStereotype(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return false;
        }
        return hasStereotypeAnnotation(annotationType);
    }

    private boolean isPackageOrParentPackageVetoed(Package pkg) {
        if (pkg == null) {
            return false;
        }
        if (hasVetoedAnnotation(pkg)) {
            return true;
        }
        String packageName = pkg.getName();
        while (packageName.contains(".")) {
            packageName = packageName.substring(0, packageName.lastIndexOf('.'));
            try {
                Class<?> packageInfo = Class.forName(packageName + ".package-info");
                Package parent = packageInfo.getPackage();
                if (hasVetoedAnnotation(parent)) {
                    return true;
                }
            } catch (ClassNotFoundException ignored) {
                // No package-info, continue with next parent package.
            }
        }
        return false;
    }

    private void fireAfterTypeDiscovery() {
        info("Firing AfterTypeDiscovery event");

        List<Class<?>> alternatives = collectPriorityEnabledAlternatives();
        List<Class<?>> interceptors = collectPriorityEnabledInterceptors();
        List<Class<?>> decorators = collectPriorityEnabledDecorators();
        List<Class<?>> initialAlternatives = new ArrayList<>(alternatives);
        List<Class<?>> initialInterceptors = new ArrayList<>(interceptors);
        List<Class<?>> initialDecorators = new ArrayList<>(decorators);

        AfterTypeDiscovery event = new AfterTypeDiscoveryImpl(
                messageHandler, knowledgeBase, beanManager, alternatives, interceptors, decorators);
        fireEventToExtensions(event);
        processRegisteredAnnotatedTypes();

        knowledgeBase.setApplicationAlternativeOrder(alternatives);
        knowledgeBase.setApplicationInterceptorOrder(interceptors);
        knowledgeBase.setApplicationDecoratorOrder(decorators);
        knowledgeBase.setAfterTypeDiscoveryAlternativesCustomized(!initialAlternatives.equals(alternatives));
        knowledgeBase.setAfterTypeDiscoveryInterceptorsCustomized(!initialInterceptors.equals(interceptors));
        knowledgeBase.setAfterTypeDiscoveryDecoratorsCustomized(!initialDecorators.equals(decorators));
    }

    private List<Class<?>> collectPriorityEnabledAlternatives() {
        List<Class<?>> enabled = new ArrayList<Class<?>>();
        for (Class<?> candidate : knowledgeBase.getClasses()) {
            if (!hasAlternativeAnnotation(candidate)) {
                continue;
            }
            Integer priority = getEffectivePriority(candidate);
            if (priority == null) {
                continue;
            }
            if (!enabled.contains(candidate)) {
                enabled.add(candidate);
            }
        }
        enabled.sort(Comparator
                .comparingInt((Class<?> clazz) -> {
                    Integer priority = getEffectivePriority(clazz);
                    return priority != null ? priority : Integer.MAX_VALUE;
                })
                .thenComparing(Class::getName));
        return enabled;
    }

    private List<Class<?>> collectPriorityEnabledInterceptors() {
        List<Class<?>> enabled = new ArrayList<Class<?>>();
        for (Class<?> candidate : knowledgeBase.getClasses()) {
            if (!hasInterceptorAnnotation(candidate)) {
                continue;
            }
            if (ActivateRequestContextInterceptor.class.equals(candidate)) {
                continue;
            }
            Integer priority = getEffectivePriority(candidate);
            if (priority == null) {
                continue;
            }
            if (!enabled.contains(candidate)) {
                enabled.add(candidate);
            }
        }
        enabled.sort(Comparator
                .comparingInt((Class<?> clazz) -> {
                    Integer priority = getEffectivePriority(clazz);
                    return priority != null ? priority : Integer.MAX_VALUE;
                })
                .thenComparing(Class::getName));
        return enabled;
    }

    private List<Class<?>> collectPriorityEnabledDecorators() {
        List<Class<?>> enabled = new ArrayList<>();
        for (Class<?> candidate : knowledgeBase.getClasses()) {
            if (!hasDecoratorAnnotation(candidate)) {
                continue;
            }
            Integer priority = getEffectivePriority(candidate);
            if (priority == null) {
                continue;
            }
            if (!enabled.contains(candidate)) {
                enabled.add(candidate);
            }
        }
        enabled.sort(Comparator
                .comparingInt((Class<?> clazz) -> {
                    Integer priority = getEffectivePriority(clazz);
                    return priority != null ? priority : Integer.MAX_VALUE;
                })
                .thenComparing(Class::getName));
        return enabled;
    }

    private Integer getEffectivePriority(Class<?> candidate) {
        if (candidate == null) {
            return null;
        }

        Integer directPriority = getPriorityValue(candidate);
        if (directPriority != null) {
            return directPriority;
        }

        AnnotatedType<?> override = knowledgeBase.getAnnotatedTypeOverride(candidate);
        if (override == null) {
            return null;
        }

        for (Annotation annotation : override.getAnnotations()) {
            Integer value = extractPriorityValue(annotation);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Integer extractPriorityValue(Annotation annotation) {
        if (annotation == null || !hasPriorityAnnotation(annotation.annotationType())) {
            return null;
        }
        try {
            Object value = annotation.annotationType().getMethod("value").invoke(annotation);
            return value instanceof Integer ? (Integer) value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void addClassByName(List<Class<?>> target, String className) {
        if (className == null || className.trim().isEmpty()) {
            return;
        }
        try {
            Class<?> loaded = Class.forName(className);
            if (!target.contains(loaded)) {
                target.add(loaded);
            }
        } catch (ClassNotFoundException ignored) {
            // Validation phase reports unresolved beans.xml class names.
        }
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

        CDI41BeanValidator validator = new CDI41BeanValidator(
                knowledgeBase,
                cdiFullLegacyInterceptionEnabled
        );
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
                processInjectionPointsForBean(bean, bean.getInjectionPoints());
                processLifecycleMethodInjectionPoints(bean);
            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new DefinitionException("Error processing injection points for bean " +
                        bean.getBeanClass().getName(), e);
            }
        }

        processInterceptorAndDecoratorInjectionPoints();
    }

    private void processInjectionPointsForBean(Bean<?> bean, Set<InjectionPoint> injectionPoints) {
        List<InjectionPoint> snapshot = new ArrayList<>(injectionPoints);
        for (InjectionPoint ip : snapshot) {
            ProcessInjectionPointImpl<?, ?> event =
                    new ProcessInjectionPointImpl<>(messageHandler, ip, knowledgeBase);

            fireEventToExtensions(event);

            if (bean != null) {
                InjectionPoint updated = event.getInjectionPointInternal();
                if (updated != ip) {
                    updateInjectionPoint(bean, ip, updated);
                }
            }
        }
    }

    private void processLifecycleMethodInjectionPoints(Bean<?> bean) {
        if (bean == null || bean.getBeanClass() == null) {
            return;
        }

        Map<String, Method> methodsBySignature = new LinkedHashMap<>();
        Class<?> current = bean.getBeanClass();
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                methodsBySignature.putIfAbsent(signature, method);
            }
            current = current.getSuperclass();
        }

        for (Method method : methodsBySignature.values()) {
            boolean disposerMethod = false;
            boolean observerMethod = false;
            for (Parameter parameter : method.getParameters()) {
                if (hasDisposesAnnotation(parameter)) {
                    disposerMethod = true;
                }
                if (hasObservesAnnotation(parameter) || hasObservesAsyncAnnotation(parameter)) {
                    observerMethod = true;
                }
            }
            if (!disposerMethod && !observerMethod) {
                continue;
            }

            for (Parameter parameter : method.getParameters()) {
                if (hasDisposesAnnotation(parameter)
                        || hasObservesAnnotation(parameter)
                        || hasObservesAsyncAnnotation(parameter)) {
                    continue;
                }
                InjectionPoint ip = new InjectionPointImpl(parameter, bean);
                ProcessInjectionPointImpl<?, ?> event =
                        new ProcessInjectionPointImpl<>(messageHandler, ip, knowledgeBase);
                fireEventToExtensions(event);
            }
        }
    }

    private void processInterceptorAndDecoratorInjectionPoints() {
        Set<Class<?>> lifecycleTypes = new LinkedHashSet<>();
        for (InterceptorInfo interceptorInfo : knowledgeBase.getInterceptorInfos()) {
            if (interceptorInfo != null && interceptorInfo.getInterceptorClass() != null) {
                lifecycleTypes.add(interceptorInfo.getInterceptorClass());
            }
        }
        for (DecoratorInfo decoratorInfo : knowledgeBase.getDecoratorInfos()) {
            if (decoratorInfo != null && decoratorInfo.getDecoratorClass() != null) {
                lifecycleTypes.add(decoratorInfo.getDecoratorClass());
            }
        }

        for (Class<?> lifecycleType : lifecycleTypes) {
            try {
                AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(lifecycleType);
                InjectionTargetFactory<?> factory = new InjectionTargetFactoryImpl<>(annotatedType, beanManager);
                InjectionTarget<?> injectionTarget = factory.createInjectionTarget(null);
                processInjectionPointsForBean(null, injectionTarget.getInjectionPoints());
            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new DefinitionException("Error processing injection points for lifecycle type " +
                        lifecycleType.getName(), e);
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
                    AnnotatedType<?> annotatedType = knowledgeBase.getAnnotatedTypeOverride(beanClass);
                    if (annotatedType == null) {
                        annotatedType = new SimpleAnnotatedType<>(beanClass);
                    }
                    InjectionTargetFactory<?> factory = new InjectionTargetFactoryImpl<>(annotatedType, beanManager);

                    @SuppressWarnings("unchecked")
                    InjectionTarget<Object> injectionTarget =
                            (InjectionTarget<Object>) factory.createInjectionTarget((Bean) managedBean);

                    @SuppressWarnings("unchecked")
                    ProcessInjectionTargetImpl<Object> event =
                        new ProcessInjectionTargetImpl<>(messageHandler, knowledgeBase,
                                (AnnotatedType<Object>) annotatedType, injectionTarget);

                    fireEventToExtensions(event);

                    InjectionTarget<?> finalTarget = event.getInjectionTargetInternal();
                    if (finalTarget != null && finalTarget != injectionTarget) {
                        managedBean.setCustomInjectionTarget((InjectionTarget) finalTarget);
                    } else {
                        managedBean.setCustomInjectionTarget(null);
                    }
                } catch (DefinitionException e) {
                    throw e;
                } catch (Exception e) {
                    throw new DefinitionException("Error processing injection target for " +
                            beanClass.getName(), e);
                }
            }
        }

        processInterceptorAndDecoratorInjectionTargets();
    }

    private void processInterceptorAndDecoratorInjectionTargets() {
        Set<Class<?>> lifecycleTypes = new LinkedHashSet<Class<?>>();
        for (InterceptorInfo interceptorInfo : knowledgeBase.getInterceptorInfos()) {
            if (interceptorInfo != null && interceptorInfo.getInterceptorClass() != null) {
                lifecycleTypes.add(interceptorInfo.getInterceptorClass());
            }
        }
        for (DecoratorInfo decoratorInfo : knowledgeBase.getDecoratorInfos()) {
            if (decoratorInfo != null && decoratorInfo.getDecoratorClass() != null) {
                lifecycleTypes.add(decoratorInfo.getDecoratorClass());
            }
        }

        for (Class<?> lifecycleType : lifecycleTypes) {
            try {
                AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(lifecycleType);
                InjectionTargetFactory<?> factory = new InjectionTargetFactoryImpl<>(annotatedType, beanManager);
                InjectionTarget<?> injectionTarget = factory.createInjectionTarget(null);
                @SuppressWarnings({"rawtypes", "unchecked"})
                ProcessInjectionTargetImpl<?> event = new ProcessInjectionTargetImpl(
                        messageHandler, knowledgeBase, (AnnotatedType) annotatedType, (InjectionTarget) injectionTarget);
                fireEventToExtensions(event);
            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new DefinitionException("Error processing injection target for lifecycle type " +
                        lifecycleType.getName(), e);
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
        Set<Class<?>> processedBeanClasses = new HashSet<Class<?>>();

        for (Bean<?> bean : knowledgeBase.getBeans()) {
            if (!isProcessBeanAttributesCandidate(bean)) {
                continue;
            }
            if (bean.getBeanClass() != null) {
                processedBeanClasses.add(bean.getBeanClass());
            }
            try {
                BeanAttributes<?> attrs = new BeanAttributesImpl<>(bean.getName(), bean.getQualifiers(),
                    bean.getScope(), bean.getStereotypes(), bean.getTypes(), bean.isAlternative());

                Annotated annotated = resolveProcessBeanAttributesAnnotated(bean);
                ProcessBeanAttributesImpl<?> event =
                    new ProcessBeanAttributesImpl<>(messageHandler, annotated, attrs, knowledgeBase);

                fireEventToExtensions(event);

                if (event.isVetoed()) {
                    vetoed.add(bean);
                    continue;
                }

                if (event.isIgnoreFinalMethods()) {
                    knowledgeBase.addWarning("ProcessBeanAttributes ignoreFinalMethods requested for " +
                                             bean.getBeanClass().getName());
                    knowledgeBase.markIgnoreFinalMethods(bean);
                    if (bean instanceof BeanImpl<?>) {
                        ((BeanImpl<?>) bean).setIgnoreFinalMethods(true);
                    }
                }

                BeanAttributes<?> finalAttrs = event.getBeanAttributesInternal();
                applyBeanAttributes(bean, finalAttrs);

            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new DefinitionException("Error processing bean attributes for bean " +
                        bean.getBeanClass().getName(), e);
            }
        }

        processInterceptorAndDecoratorBeanAttributes(processedBeanClasses);

        // Remove vetoed beans
        if (!vetoed.isEmpty()) {
            knowledgeBase.getBeans().removeAll(vetoed);
            info("Vetoed " + vetoed.size() + " bean(s) via ProcessBeanAttributes");
        }
    }

    private void processInterceptorAndDecoratorBeanAttributes(Set<Class<?>> processedBeanClasses) {
        Set<Class<?>> lifecycleTypes = new LinkedHashSet<Class<?>>();
        for (InterceptorInfo interceptorInfo : knowledgeBase.getInterceptorInfos()) {
            if (interceptorInfo != null && interceptorInfo.getInterceptorClass() != null) {
                lifecycleTypes.add(interceptorInfo.getInterceptorClass());
            }
        }
        for (DecoratorInfo decoratorInfo : knowledgeBase.getDecoratorInfos()) {
            if (decoratorInfo != null && decoratorInfo.getDecoratorClass() != null) {
                lifecycleTypes.add(decoratorInfo.getDecoratorClass());
            }
        }

        for (Class<?> lifecycleType : lifecycleTypes) {
            if (lifecycleType == null || processedBeanClasses.contains(lifecycleType)) {
                continue;
            }
            try {
                AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(lifecycleType);
                BeanAttributes<?> attrs = beanManager.createBeanAttributes((AnnotatedType) annotatedType);
                ProcessBeanAttributesImpl<?> event =
                        new ProcessBeanAttributesImpl<>(messageHandler, annotatedType, attrs, knowledgeBase);
                fireEventToExtensions(event);
            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new DefinitionException("Error processing bean attributes for lifecycle type " +
                        lifecycleType.getName(), e);
            }
        }
    }

    private boolean isProcessBeanAttributesCandidate(Bean<?> bean) {
        return bean instanceof BeanImpl<?> || bean instanceof ProducerBean<?>;
    }

    private Annotated resolveProcessBeanAttributesAnnotated(Bean<?> bean) {
        if (bean instanceof ProducerBean<?>) {
            ProducerBean<?> producerBean = (ProducerBean<?>) bean;
            Class<?> declaringClass = producerBean.getDeclaringClass();
            AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(declaringClass);
            if (producerBean.isMethod()) {
                Method producerMethod = producerBean.getProducerMethod();
                AnnotatedMethod<?> annotatedMethod = findAnnotatedMethod(annotatedType, producerMethod);
                return annotatedMethod != null ? annotatedMethod : annotatedType;
            }
            if (producerBean.isField()) {
                Field producerField = producerBean.getProducerField();
                AnnotatedField<?> annotatedField = findAnnotatedField(annotatedType, producerField);
                return annotatedField != null ? annotatedField : annotatedType;
            }
            return annotatedType;
        }

        if (bean instanceof BeanImpl<?>) {
            BeanImpl<?> managedBean = (BeanImpl<?>) bean;
            AnnotatedType<?> annotatedType = knowledgeBase.getAnnotatedTypeOverride(managedBean.getBeanClass());
            if (annotatedType != null) {
                return annotatedType;
            }
        }

        return new SimpleAnnotatedType<>(bean.getBeanClass());
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

        Set<Class<?>> processedBeanClasses = new HashSet<Class<?>>();
        int managedCount = 0;
        int producerMethodCount = 0;
        int producerFieldCount = 0;
        int syntheticCount = 0;

        for (Bean<?> bean : allBeans) {
            try {
                if (bean.getBeanClass() != null) {
                    processedBeanClasses.add(bean.getBeanClass());
                }
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
                         managedBean, annotatedType, beanManager);
                    fireEventToExtensions(event);
                    managedCount++;

                } else {
                    // Built-in beans (BeanManager, InjectionPoint, etc.)
                    // These don't get ProcessBean events
                    info("Skipping built-in bean: " + bean.getBeanClass().getSimpleName());
                }
            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new DefinitionException("Error processing bean " + bean.getBeanClass().getName(), e);
            }
        }

        managedCount += processInterceptorAndDecoratorBeans(processedBeanClasses);

        info("Processed: " + managedCount + " managed, " + producerMethodCount + " producer methods, " +
                producerFieldCount + " producer fields, " + syntheticCount + " synthetic");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int processInterceptorAndDecoratorBeans(Set<Class<?>> processedBeanClasses) {
        int managedCount = 0;
        Set<Class<?>> lifecycleTypes = new LinkedHashSet<Class<?>>();
        for (InterceptorInfo interceptorInfo : knowledgeBase.getInterceptorInfos()) {
            if (interceptorInfo != null && interceptorInfo.getInterceptorClass() != null) {
                lifecycleTypes.add(interceptorInfo.getInterceptorClass());
            }
        }
        for (DecoratorInfo decoratorInfo : knowledgeBase.getDecoratorInfos()) {
            if (decoratorInfo != null && decoratorInfo.getDecoratorClass() != null) {
                lifecycleTypes.add(decoratorInfo.getDecoratorClass());
            }
        }

        for (Class<?> lifecycleType : lifecycleTypes) {
            if (lifecycleType == null || processedBeanClasses.contains(lifecycleType)) {
                continue;
            }
            try {
                AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(lifecycleType);
                BeanAttributes<?> attrs = beanManager.createBeanAttributes((AnnotatedType) annotatedType);
                BeanImpl<?> syntheticManagedBean = new BeanImpl(lifecycleType, attrs.isAlternative());
                applyBeanAttributes(syntheticManagedBean, attrs);
                ProcessManagedBeanImpl<?> event = new ProcessManagedBeanImpl(
                        messageHandler, knowledgeBase, syntheticManagedBean, (AnnotatedType) annotatedType, beanManager);
                fireEventToExtensions(event);
                managedCount++;
            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new DefinitionException(
                        "Error processing interceptor/decorator bean " + lifecycleType.getName(), e);
            }
        }
        return managedCount;
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
            if (producerBean == null || producerBean.isVetoed() || producerBean.hasValidationErrors()) {
                continue;
            }
            if (!knowledgeBase.getBeans().contains(producerBean)) {
                // Skip producer beans previously removed from resolvable beans (e.g. PBA veto).
                continue;
            }
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
            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new DefinitionException("Error processing producer", e);
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
        if (existing.isEmpty()) {
            // Fallback discovery so ProcessObserverMethod can still be delivered at lifecycle time
            // even when runtime observer registration is deferred to deployment validation.
            existing = discoverObserverMethodsForLifecycleDispatch();
        }
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

                if (info.isSynthetic()) {
                    ProcessSyntheticObserverMethodImpl<?, ?> event =
                            new ProcessSyntheticObserverMethodImpl(messageHandler, knowledgeBase, observer, null);
                    fireEventToExtensions(event);
                    if (event.isVetoed()) {
                        continue; // remove this observer
                    }
                    ObserverMethod<?> finalObserver = event.getFinalObserverMethod();
                    if (finalObserver == observer) {
                        updated.add(info);
                    } else {
                        updated.add(toObserverMethodInfo(finalObserver, info.getDeclaringBean()));
                    }
                } else {
                    ProcessObserverMethodImpl<?, ?> event =
                            new ProcessObserverMethodImpl(messageHandler, knowledgeBase, observer, annotatedMethod);
                    fireEventToExtensions(event);
                    if (event.isVetoed()) {
                        continue; // remove this observer
                    }
                    ObserverMethod<?> finalObserver = event.getFinalObserverMethod();
                    if (finalObserver == observer) {
                        updated.add(info);
                    } else {
                        updated.add(toObserverMethodInfo(finalObserver, info.getDeclaringBean()));
                    }
                }

            } catch (DefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new DefinitionException("Error processing observer method", e);
            }
        }

        knowledgeBase.getObserverMethodInfos().clear();
        List<ObserverMethodInfo> deduped = new ArrayList<ObserverMethodInfo>();
        Set<String> seen = new HashSet<String>();
        for (ObserverMethodInfo info : updated) {
            String key = observerInfoKey(info);
            if (seen.add(key)) {
                deduped.add(info);
            }
        }
        knowledgeBase.getObserverMethodInfos().addAll(deduped);
    }

    private Collection<ObserverMethodInfo> discoverObserverMethodsForLifecycleDispatch() {
        List<ObserverMethodInfo> out = new ArrayList<ObserverMethodInfo>();
        for (Bean<?> bean : filterObserverDeclaringBeansForLifecycleDispatch()) {
            Class<?> beanClass = bean.getBeanClass();
            for (Method method : collectObserverCandidateMethods(beanClass)) {
                ObserverMethodInfo info = toObserverInfoForLifecycleDispatch(method, bean);
                if (info != null) {
                    out.add(info);
                }
            }
        }
        return out;
    }

    private Set<Bean<?>> filterObserverDeclaringBeansForLifecycleDispatch() {
        Set<Class<?>> discoveredClasses = new HashSet<Class<?>>(knowledgeBase.getClasses());
        Set<Bean<?>> candidates = new LinkedHashSet<Bean<?>>();

        for (Bean<?> bean : knowledgeBase.getBeans()) {
            if (!(bean instanceof BeanImpl<?>)) {
                continue;
            }
            Class<?> beanClass = bean.getBeanClass();
            if (beanClass == null || !discoveredClasses.contains(beanClass)) {
                continue;
            }
            if (beanClass.getName().startsWith(Syringe.class.getName() + "$")) {
                continue;
            }
            if (!isBeanEnabledForObserverLifecycle(bean)) {
                continue;
            }
            candidates.add(bean);
        }

        return applyObserverSpecializationFiltering(candidates);
    }

    private boolean isBeanEnabledForObserverLifecycle(Bean<?> bean) {
        if (bean == null) {
            return false;
        }
        if (!bean.isAlternative()) {
            return true;
        }
        if (bean instanceof BeanImpl<?>) {
            return ((BeanImpl<?>) bean).isAlternativeEnabled();
        }
        return true;
    }

    private Set<Bean<?>> applyObserverSpecializationFiltering(Set<Bean<?>> candidates) {
        if (candidates == null || candidates.size() < 2) {
            return candidates;
        }

        Set<Class<?>> specializedSuperclasses = new HashSet<Class<?>>();
        for (Bean<?> candidate : candidates) {
            Class<?> beanClass = candidate.getBeanClass();
            if (hasSpecializesAnnotation(beanClass)) {
                specializedSuperclasses.addAll(collectSpecializedSuperclasses(beanClass));
            }
        }

        if (specializedSuperclasses.isEmpty()) {
            return candidates;
        }

        Set<Bean<?>> filtered = new LinkedHashSet<Bean<?>>();
        for (Bean<?> candidate : candidates) {
            if (!specializedSuperclasses.contains(candidate.getBeanClass())) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    private Set<Class<?>> collectSpecializedSuperclasses(Class<?> beanClass) {
        Set<Class<?>> out = new HashSet<Class<?>>();
        if (beanClass == null || !hasSpecializesAnnotation(beanClass)) {
            return out;
        }
        Class<?> current = beanClass.getSuperclass();
        while (current != null && !Object.class.equals(current)) {
            out.add(current);
            if (!hasSpecializesAnnotation(current)) {
                break;
            }
            current = current.getSuperclass();
        }
        return out;
    }

    private List<Method> collectObserverCandidateMethods(Class<?> beanClass) {
        List<Class<?>> hierarchy = new ArrayList<Class<?>>();
        Class<?> current = beanClass;
        while (current != null && !Object.class.equals(current)) {
            hierarchy.add(0, current);
            current = current.getSuperclass();
        }

        Map<String, Method> bySignature = new LinkedHashMap<String, Method>();
        for (Class<?> type : hierarchy) {
            for (Method method : type.getDeclaredMethods()) {
                String signature = observerMethodSignature(method);
                bySignature.put(signature, method);
            }
        }
        return new ArrayList<Method>(bySignature.values());
    }

    private String observerMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(parameterTypes[i].getName());
        }
        sb.append(')');
        return sb.toString();
    }

    private ObserverMethodInfo toObserverInfoForLifecycleDispatch(Method method, Bean<?> declaringBean) {
        int observesCount = 0;
        int observesAsyncCount = 0;
        Parameter observedParameter = null;
        Annotation[] observedParameterAnnotations = null;
        Type observedParameterBaseType = null;
        int observedParameterPosition = -1;
        AnnotatedMethod<?> annotatedMethod = null;
        AnnotatedType<?> override = declaringBean != null
                ? knowledgeBase.getAnnotatedTypeOverride(declaringBean.getBeanClass())
                : null;
        if (override != null) {
            annotatedMethod = AnnotatedMetadataHelper.findAnnotatedMethod(override, method);
        }

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            AnnotatedParameter<?> annotatedParameter = annotatedMethod != null
                    ? findAnnotatedParameter(annotatedMethod, i)
                    : null;
            Annotation[] parameterAnnotations = annotatedParameter != null
                    ? annotatedParameter.getAnnotations().toArray(new Annotation[0])
                    : parameter.getAnnotations();
            Type parameterBaseType = annotatedParameter != null
                    ? annotatedParameter.getBaseType()
                    : parameter.getParameterizedType();

            if (hasObservesAnnotationIn(parameterAnnotations)) {
                observesCount++;
                observedParameter = parameter;
                observedParameterAnnotations = parameterAnnotations;
                observedParameterBaseType = parameterBaseType;
                observedParameterPosition = i;
            }
            if (hasObservesAsyncAnnotationIn(parameterAnnotations)) {
                observesAsyncCount++;
                observedParameter = parameter;
                observedParameterAnnotations = parameterAnnotations;
                observedParameterBaseType = parameterBaseType;
                observedParameterPosition = i;
            }
        }

        if (observesCount == 0 && observesAsyncCount == 0) {
            return null;
        }
        if (observesCount + observesAsyncCount != 1 || observedParameter == null) {
            return null;
        }

        boolean async = observesAsyncCount > 0;
        Type eventType = GenericTypeResolver.resolve(
                observedParameterBaseType != null ? observedParameterBaseType : observedParameter.getParameterizedType(),
                declaringBean.getBeanClass(),
                method.getDeclaringClass()
        );
        Set<Annotation> qualifiers = extractObserverQualifiers(
                observedParameterAnnotations != null ? observedParameterAnnotations : observedParameter.getAnnotations());
        Reception reception = Reception.ALWAYS;
        TransactionPhase transactionPhase = TransactionPhase.IN_PROGRESS;
        int priority = jakarta.interceptor.Interceptor.Priority.APPLICATION + 500;

        if (async) {
            jakarta.enterprise.event.ObservesAsync observesAsync = getObservesAsyncAnnotationFrom(
                    observedParameterAnnotations != null ? observedParameterAnnotations : observedParameter.getAnnotations());
            if (observesAsync != null) {
                reception = observesAsync.notifyObserver();
            }
        } else {
            jakarta.enterprise.event.Observes observes = getObservesAnnotationFrom(
                    observedParameterAnnotations != null ? observedParameterAnnotations : observedParameter.getAnnotations());
            if (observes != null) {
                reception = observes.notifyObserver();
                transactionPhase = observes.during();
            }
            Integer paramPriority = getPriorityValueFromAnnotations(
                    observedParameterAnnotations != null ? observedParameterAnnotations : observedParameter.getAnnotations());
            if (paramPriority != null) {
                priority = paramPriority;
            } else {
                Integer methodPriority = getPriorityValue(method);
                if (methodPriority != null) {
                    priority = methodPriority;
                }
            }
        }

        return new ObserverMethodInfo(
                method,
                eventType,
                qualifiers,
                reception,
                transactionPhase,
                async,
                declaringBean,
                priority,
                observedParameterPosition
        );
    }

    private Set<Annotation> extractObserverQualifiers(Annotation[] observedParameterAnnotations) {
        if (observedParameterAnnotations == null) {
            return new HashSet<Annotation>();
        }
        return new HashSet<Annotation>(
                com.threeamigos.common.util.implementations.injection.util.QualifiersHelper
                        .extractQualifierAnnotations(observedParameterAnnotations));
    }

    private AnnotatedParameter<?> findAnnotatedParameter(AnnotatedMethod<?> annotatedMethod, int position) {
        if (annotatedMethod == null) {
            return null;
        }
        for (AnnotatedParameter<?> parameter : annotatedMethod.getParameters()) {
            if (parameter.getPosition() == position) {
                return parameter;
            }
        }
        return null;
    }

    private boolean hasObservesAnnotationIn(Annotation[] annotations) {
        return getObservesAnnotationFrom(annotations) != null;
    }

    private boolean hasObservesAsyncAnnotationIn(Annotation[] annotations) {
        return getObservesAsyncAnnotationFrom(annotations) != null;
    }

    private jakarta.enterprise.event.Observes getObservesAnnotationFrom(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof jakarta.enterprise.event.Observes) {
                return (jakarta.enterprise.event.Observes) annotation;
            }
        }
        return null;
    }

    private jakarta.enterprise.event.ObservesAsync getObservesAsyncAnnotationFrom(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof jakarta.enterprise.event.ObservesAsync) {
                return (jakarta.enterprise.event.ObservesAsync) annotation;
            }
        }
        return null;
    }

    private Integer getPriorityValueFromAnnotations(Annotation[] annotations) {
        if (annotations == null) {
            return null;
        }
        for (Annotation annotation : annotations) {
            if (annotation == null) {
                continue;
            }
            String annotationTypeName = annotation.annotationType().getName();
            if (Priority.class.getName().equals(annotationTypeName) ||
                    "javax.annotation.Priority".equals(annotationTypeName)) {
                try {
                    Method valueMethod = annotation.annotationType().getMethod("value");
                    Object value = valueMethod.invoke(annotation);
                    if (value instanceof Integer) {
                        return (Integer) value;
                    }
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String observerInfoKey(ObserverMethodInfo info) {
        if (info == null) {
            return "";
        }
        String methodKey = "";
        if (info.getObserverMethod() != null) {
            Method m = info.getObserverMethod();
            methodKey = m.getDeclaringClass().getName() + "#" + m.getName() + "/" + m.getParameterCount();
        } else if (info.getSyntheticObserver() != null) {
            methodKey = "synthetic:" + info.getSyntheticObserver().getClass().getName();
        }
        String eventType = String.valueOf(info.getEventType());
        String qualifiers = String.valueOf(info.getQualifiers());
        String declaringBeanClass = info.getDeclaringBean() != null && info.getDeclaringBean().getBeanClass() != null
                ? info.getDeclaringBean().getBeanClass().getName()
                : "";
        return methodKey + "|" + declaringBeanClass + "|" + eventType + "|" + qualifiers + "|" + info.isAsync();
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
                        sb.getId(),
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
                    if (hasObservesAnnotation(p) || hasObservesAsyncAnnotation(p)) {
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
        beanManager.markAfterBeanDiscoveryFired();
        AfterBeanDiscovery event = new AfterBeanDiscoveryImpl(messageHandler, knowledgeBase, beanManager, this::fireEventToExtensions);

        // Register programmatically added custom contexts BEFORE firing to extensions
        // This allows extensions to see and potentially modify these contexts
        if (!customContextsToRegister.isEmpty()) {
            info("Registering " + customContextsToRegister.size() + " programmatically added custom contexts");

            try {
                if (event instanceof ObserverInvocationLifecycle) {
                    ((ObserverInvocationLifecycle) event).beginObserverInvocation();
                }
                for (Map.Entry<Class<? extends Annotation>, Context> entry : customContextsToRegister.entrySet()) {
                    try {
                        event.addContext(entry.getValue());
                        info("Registered custom context for @" +entry.getKey().getSimpleName());
                    } catch (Exception e) {
                        log("Failed to register custom context for @" + entry.getKey().getSimpleName(), e);
                        throw new DeploymentException("Failed to register custom context for @" + entry.getKey().getSimpleName(), e);
                    }
                }
            } finally {
                if (event instanceof ObserverInvocationLifecycle) {
                    ((ObserverInvocationLifecycle) event).endObserverInvocation();
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
        CDI41InjectionValidator injectionValidator =
                new CDI41InjectionValidator(
                        knowledgeBase,
                        legacyCdi10NewEnabled,
                        allowNonPortableAsyncObserverEventParameterPriority);
        injectionValidator.validateAllInjectionPoints();

        // 1.1 Validate beans.xml alternatives declarations (CDI Full modularity rules)
        validateBeansXmlAlternativesConfiguration();
        // 1.2 Validate beans.xml interceptor declarations
        validateBeansXmlInterceptorsConfiguration();
        // 1.3 Validate beans.xml decorator declarations
        validateBeansXmlDecoratorsConfiguration();

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

    private void validateBeansXmlAlternativesConfiguration() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = Syringe.class.getClassLoader();
        }

        for (BeansXml beansXml : knowledgeBase.getBeansXmlConfigurations()) {
            if (beansXml == null) {
                continue;
            }
            Alternatives alternatives = beansXml.getAlternatives();
            if (alternatives == null) {
                continue;
            }

            List<String> classes = alternatives.getClasses() != null ? alternatives.getClasses() : Collections.<String>emptyList();
            List<String> stereotypes = alternatives.getStereotypes() != null ? alternatives.getStereotypes() : Collections.<String>emptyList();

            validateNoDuplicateEntries(classes, "beans.xml <alternatives><class>");
            validateNoDuplicateEntries(stereotypes, "beans.xml <alternatives><stereotype>");

            for (String className : classes) {
                validateAlternativeClassEntry(className, classLoader);
            }
            for (String stereotypeName : stereotypes) {
                validateAlternativeStereotypeEntry(stereotypeName, classLoader);
            }
        }
    }

    private void validateBeansXmlInterceptorsConfiguration() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = Syringe.class.getClassLoader();
        }

        for (BeansXml beansXml : knowledgeBase.getBeansXmlConfigurations()) {
            if (beansXml == null) {
                continue;
            }

            com.threeamigos.common.util.implementations.injection.beansxml.Interceptors interceptors =
                    beansXml.getInterceptors();
            if (interceptors == null) {
                continue;
            }

            List<String> classes = interceptors.getClasses() != null
                    ? interceptors.getClasses()
                    : Collections.<String>emptyList();

            validateNoDuplicateEntries(classes, "beans.xml <interceptors><class>");

            for (String className : classes) {
                validateInterceptorClassEntry(className, classLoader);
            }
        }
    }

    private void validateBeansXmlDecoratorsConfiguration() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = Syringe.class.getClassLoader();
        }

        for (BeansXml beansXml : knowledgeBase.getBeansXmlConfigurations()) {
            if (beansXml == null) {
                continue;
            }

            com.threeamigos.common.util.implementations.injection.beansxml.Decorators decorators =
                    beansXml.getDecorators();
            if (decorators == null) {
                continue;
            }

            List<String> classes = decorators.getClasses() != null
                    ? decorators.getClasses()
                    : Collections.<String>emptyList();

            validateNoDuplicateEntries(classes, "beans.xml <decorators><class>");

            for (String className : classes) {
                validateDecoratorClassEntry(className, classLoader);
            }
        }
    }

    private void validateNoDuplicateEntries(List<String> entries, String location) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<String>();
        Set<String> duplicates = new LinkedHashSet<String>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            if (!seen.add(entry)) {
                duplicates.add(entry);
            }
        }
        if (!duplicates.isEmpty()) {
            knowledgeBase.addDefinitionError(location + " contains duplicate entries: " + duplicates);
        }
    }

    private void validateAlternativeClassEntry(String className, ClassLoader classLoader) {
        if (className == null || className.trim().isEmpty()) {
            knowledgeBase.addDefinitionError("beans.xml <alternatives><class> must not be empty");
            return;
        }

        Class<?> clazz;
        try {
            clazz = Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            knowledgeBase.addDefinitionError("beans.xml alternative class not found: " + className);
            return;
        }

        if (isAlternativeDeclaration(clazz) ||
                declaresAlternativeProducerMember(clazz) ||
                hasAlternativeBeanWithBeanClassName(className)) {
            return;
        }

        knowledgeBase.addDefinitionError(
                "beans.xml alternative class '" + className + "' is invalid: " +
                        "not an @Alternative bean, not an alternative producer holder, and no matching alternative bean exists");
    }

    private void validateAlternativeStereotypeEntry(String stereotypeName, ClassLoader classLoader) {
        if (stereotypeName == null || stereotypeName.trim().isEmpty()) {
            knowledgeBase.addDefinitionError("beans.xml <alternatives><stereotype> must not be empty");
            return;
        }

        Class<?> loaded;
        try {
            loaded = Class.forName(stereotypeName, false, classLoader);
        } catch (ClassNotFoundException e) {
            knowledgeBase.addDefinitionError("beans.xml alternative stereotype not found: " + stereotypeName);
            return;
        }

        if (!loaded.isAnnotation()) {
            knowledgeBase.addDefinitionError(
                    "beans.xml alternative stereotype '" + stereotypeName + "' is not an annotation type");
            return;
        }

        @SuppressWarnings("unchecked")
        Class<? extends Annotation> annotationType = (Class<? extends Annotation>) loaded;
        if (!hasStereotypeAnnotation(annotationType) ||
                !declaresAlternativeViaStereotype(annotationType, new HashSet<Class<? extends Annotation>>())) {
            knowledgeBase.addDefinitionError(
                    "beans.xml alternative stereotype '" + stereotypeName + "' is not an @Alternative stereotype");
        }
    }

    private void validateInterceptorClassEntry(String className, ClassLoader classLoader) {
        if (className == null || className.trim().isEmpty()) {
            knowledgeBase.addDefinitionError("beans.xml <interceptors><class> must not be empty");
            return;
        }

        Class<?> clazz;
        try {
            clazz = Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            knowledgeBase.addDefinitionError("beans.xml interceptor class not found: " + className);
            return;
        }

        if (hasInterceptorAnnotation(clazz) ||
                jakarta.enterprise.inject.spi.Interceptor.class.isAssignableFrom(clazz)) {
            return;
        }

        knowledgeBase.addDefinitionError(
                "beans.xml interceptor class '" + className + "' is not an interceptor class");
    }

    private void validateDecoratorClassEntry(String className, ClassLoader classLoader) {
        if (className == null || className.trim().isEmpty()) {
            knowledgeBase.addDefinitionError("beans.xml <decorators><class> must not be empty");
            return;
        }

        Class<?> clazz;
        try {
            clazz = Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            knowledgeBase.addDefinitionError("beans.xml decorator class not found: " + className);
            return;
        }

        if (hasDecoratorAnnotation(clazz) ||
                jakarta.enterprise.inject.spi.Decorator.class.isAssignableFrom(clazz)) {
            return;
        }

        knowledgeBase.addDefinitionError(
                "beans.xml decorator class '" + className + "' is not a decorator class");
    }

    private boolean hasAlternativeBeanWithBeanClassName(String className) {
        for (Bean<?> bean : knowledgeBase.getBeans()) {
            Class<?> beanClass = bean.getBeanClass();
            if (beanClass != null &&
                    className.equals(beanClass.getName()) &&
                    bean.isAlternative()) {
                return true;
            }
        }
        return false;
    }

    private boolean declaresAlternativeProducerMember(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (hasProducesAnnotation(method) && isAlternativeDeclaration(method)) {
                return true;
            }
        }
        for (Field field : clazz.getDeclaredFields()) {
            if (hasProducesAnnotation(field) && isAlternativeDeclaration(field)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAlternativeDeclaration(AnnotatedElement element) {
        if (element == null) {
            return false;
        }
        if (hasAlternativeAnnotation(element)) {
            return true;
        }
        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (hasStereotypeAnnotation(type) &&
                    declaresAlternativeViaStereotype(type, new HashSet<Class<? extends Annotation>>())) {
                return true;
            }
        }
        return false;
    }

    private boolean declaresAlternativeViaStereotype(Class<? extends Annotation> stereotypeType,
                                                     Set<Class<? extends Annotation>> visited) {
        if (stereotypeType == null || !visited.add(stereotypeType)) {
            return false;
        }
        if (hasAlternativeAnnotation(stereotypeType)) {
            return true;
        }
        for (Annotation meta : stereotypeType.getAnnotations()) {
            Class<? extends Annotation> metaType = meta.annotationType();
            if (hasStereotypeAnnotation(metaType) &&
                    declaresAlternativeViaStereotype(metaType, visited)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fires AfterDeploymentValidation event to all extensions.
     *
     * <p>Extensions can perform final validation checks.
     * Any deployment problems detected here will prevent application startup.
     */
    private void fireAfterDeploymentValidation() {
        info("Firing AfterDeploymentValidation event");
        beanManager.markAfterDeploymentValidationFired();
        int deploymentProblemsBefore = knowledgeBase.getErrors().size();
        AfterDeploymentValidation event = new AfterDeploymentValidationImpl(knowledgeBase);
        fireEventToExtensions(event);
        int deploymentProblemsAfter = knowledgeBase.getErrors().size();
        if (deploymentProblemsAfter > deploymentProblemsBefore) {
            throw new DeploymentException("Deployment validation failed due to AfterDeploymentValidation problems.");
        }
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
        beforeShutdownFired = true;
        BeforeShutdown event = new BeforeShutdownImpl();
        fireEventToExtensions(event);
    }

    private void fireBuildCompatibleExtensionPhase(BceSupportedPhase phase) {
        if (buildCompatibleExtensionRunner == null || buildCompatibleExtensions.isEmpty()) {
            return;
        }
        info("Firing BCE phase: " + phase);
        buildCompatibleExtensionRunner.runPhase(phase, buildCompatibleExtensions);
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
        Class<?> eventType = event != null ? event.getClass() : Object.class;
        boolean afterDeploymentValidationEvent = isAfterDeploymentValidationLifecycleEvent(eventType);
        boolean beforeShutdownEvent = isBeforeShutdownLifecycleEvent(eventType);
        List<ExtensionObserverInvocation> invocations = new ArrayList<>();

        // Collect all matching observer methods across all extensions, with priority
        for (Extension extension : extensions) {
            collectExtensionObserverMethods(extension, event, invocations);
        }

        // Sort by @Priority (ascending). Methods without @Priority run last.
        invocations.sort(Comparator.comparingInt(inv -> inv.priority));

        for (ExtensionObserverInvocation invocation : invocations) {
            boolean lifecycleInvocationStarted = false;
            boolean extensionAwareInvocationStarted = false;
            try {
                if (event instanceof ObserverInvocationLifecycle) {
                    ((ObserverInvocationLifecycle) event).beginObserverInvocation();
                    lifecycleInvocationStarted = true;
                }
                if (event instanceof ExtensionAwareObserverInvocation) {
                    ((ExtensionAwareObserverInvocation) event).enterObserverInvocation(invocation.extension);
                    extensionAwareInvocationStarted = true;
                }
                invocation.invoke(event);
            } catch (Exception e) {
                Throwable cause = e;
                if (e instanceof InvocationTargetException &&
                    ((InvocationTargetException) e).getTargetException() != null) {
                    cause = ((InvocationTargetException) e).getTargetException();
                }
                if (cause instanceof DefinitionException) {
                    throw (DefinitionException) cause;
                }
                if (cause instanceof NonPortableBehaviourException) {
                    throw (NonPortableBehaviourException) cause;
                }
                if (isDefinitionErrorLifecycleEvent(eventType)) {
                    throw new DefinitionException("Error invoking extension " +
                            invocation.extension.getClass().getName() + " for event " +
                            eventType.getSimpleName(), cause);
                }
                if (afterDeploymentValidationEvent) {
                    String causeMessage = cause.getMessage();
                    if (causeMessage == null || causeMessage.isEmpty()) {
                        causeMessage = cause.getClass().getName();
                    }
                    knowledgeBase.addError("[AfterDeploymentValidation] Observer exception from extension " +
                            invocation.extension.getClass().getName() + ": " + causeMessage);
                    continue;
                }
                if (beforeShutdownEvent) {
                    log("Ignoring BeforeShutdown observer exception from extension " +
                            invocation.extension.getClass().getName(), cause instanceof Exception ? (Exception) cause : null);
                    continue;
                }
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new DefinitionException("Error invoking extension " +
                    invocation.extension.getClass().getName() + " for event " +
                    eventType.getSimpleName(), cause);
            } finally {
                if (extensionAwareInvocationStarted && event instanceof ExtensionAwareObserverInvocation) {
                    ((ExtensionAwareObserverInvocation) event).exitObserverInvocation();
                }
                if (lifecycleInvocationStarted && event instanceof ObserverInvocationLifecycle) {
                    ((ObserverInvocationLifecycle) event).endObserverInvocation();
                }
            }
        }
    }

    private boolean isDefinitionErrorLifecycleEvent(Class<?> eventType) {
        if (eventType == null) {
            return false;
        }
        Set<String> typeNames = new HashSet<String>();
        Class<?> current = eventType;
        while (current != null) {
            typeNames.add(current.getName());
            for (Class<?> iface : current.getInterfaces()) {
                typeNames.add(iface.getName());
            }
            current = current.getSuperclass();
        }
        return typeNames.contains("jakarta.enterprise.inject.spi.BeforeBeanDiscovery") ||
                typeNames.contains("jakarta.enterprise.inject.spi.AfterTypeDiscovery") ||
                typeNames.contains("jakarta.enterprise.inject.spi.AfterBeanDiscovery");
    }

    private boolean isAfterDeploymentValidationLifecycleEvent(Class<?> eventType) {
        if (eventType == null) {
            return false;
        }
        Set<String> typeNames = new HashSet<String>();
        Class<?> current = eventType;
        while (current != null) {
            typeNames.add(current.getName());
            for (Class<?> iface : current.getInterfaces()) {
                typeNames.add(iface.getName());
            }
            current = current.getSuperclass();
        }
        return typeNames.contains("jakarta.enterprise.inject.spi.AfterDeploymentValidation");
    }

    private boolean isBeforeShutdownLifecycleEvent(Class<?> eventType) {
        if (eventType == null) {
            return false;
        }
        Set<String> typeNames = new HashSet<String>();
        Class<?> current = eventType;
        while (current != null) {
            typeNames.add(current.getName());
            for (Class<?> iface : current.getInterfaces()) {
                typeNames.add(iface.getName());
            }
            current = current.getSuperclass();
        }
        return typeNames.contains("jakarta.enterprise.inject.spi.BeforeShutdown");
    }

    private void collectExtensionObserverMethods(Extension extension,
                                                 Object event,
                                                 List<ExtensionObserverInvocation> sink) {
        Class<?> eventType = event != null ? event.getClass() : Object.class;
        for (Method method : getExtensionObserverCandidateMethods(extension.getClass())) {
            Parameter[] parameters = method.getParameters();
            int observesParameterIndex = -1;

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                if (hasObservesAnnotation(parameter)) {
                    observesParameterIndex = i;
                    break;
                }
            }

            if (observesParameterIndex >= 0) {
                validateWithAnnotationsUsage(method, observesParameterIndex, parameters);
            }

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                if (hasObservesAnnotation(parameter)) {
                    Class<?> observedType = parameter.getType();
                    validateExtensionObserverStaticMethod(method, parameter, observedType);
                    if (observedType.isAssignableFrom(eventType) &&
                            matchesObservedGenericEventType(parameter, event)) {
                        int priority = resolvePriority(method, parameter);
                        Set<Class<? extends Annotation>> withAnnotationsFilter =
                                resolveWithAnnotationsFilter(parameter);
                        if (withAnnotationsFilter != null &&
                                !ProcessAnnotatedType.class.isAssignableFrom(observedType)) {
                            throw new DefinitionException("@WithAnnotations is only valid on ProcessAnnotatedType observer parameters: " +
                                    method.getDeclaringClass().getName() + "." + method.getName());
                        }
                        sink.add(new ExtensionObserverInvocation(extension, method, i, priority, beanManager,
                                knowledgeBase, messageHandler, withAnnotationsFilter));
                    }
                }
            }
        }
    }

    private Collection<Method> getExtensionObserverCandidateMethods(Class<?> extensionClass) {
        Map<String, Method> methodsBySignature = new LinkedHashMap<String, Method>();
        Class<?> current = extensionClass;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                methodsBySignature.putIfAbsent(signature, method);
            }
            current = current.getSuperclass();
        }
        return methodsBySignature.values();
    }

    private boolean matchesObservedGenericEventType(Parameter observerParameter, Object event) {
        if (event == null) {
            return true;
        }

        Type parameterizedObservedType = observerParameter.getParameterizedType();
        if (!(parameterizedObservedType instanceof ParameterizedType)) {
            return true;
        }

        ParameterizedType observedParameterizedType = (ParameterizedType) parameterizedObservedType;
        Type rawType = observedParameterizedType.getRawType();
        if (!(rawType instanceof Class)) {
            return true;
        }

        Type[] observedTypeArguments = observedParameterizedType.getActualTypeArguments();
        if (observedTypeArguments.length != 1) {
            return true;
        }

        Class<?> discoveredType = extractObservedTypeForGenericEventMatch((Class<?>) rawType, event);
        if (discoveredType == null) {
            return true;
        }

        return matchesObservedTypeArgument(observedTypeArguments[0], discoveredType);
    }

    private Class<?> extractObservedTypeForGenericEventMatch(Class<?> observedRawType, Object event) {
        if (ProcessAnnotatedType.class.isAssignableFrom(observedRawType) &&
                event instanceof ProcessAnnotatedType<?>) {
            AnnotatedType<?> annotatedType = extractAnnotatedTypeFromPatEvent(event);
            return annotatedType != null ? annotatedType.getJavaClass() : null;
        }

        if (ProcessBeanAttributes.class.isAssignableFrom(observedRawType) &&
                event instanceof ProcessBeanAttributes<?>) {
            Annotated annotated = extractAnnotatedFromPbaEvent(event);
            if (annotated instanceof AnnotatedType<?>) {
                return ((AnnotatedType<?>) annotated).getJavaClass();
            }
            if (annotated instanceof AnnotatedMember<?>) {
                AnnotatedType<?> declaringType = ((AnnotatedMember<?>) annotated).getDeclaringType();
                return declaringType != null ? declaringType.getJavaClass() : null;
            }
        }

        return null;
    }

    private boolean matchesObservedTypeArgument(Type observedTypeArgument, Class<?> discoveredType) {
        if (observedTypeArgument instanceof Class<?>) {
            Class<?> observedClass = (Class<?>) observedTypeArgument;
            return observedClass.isAssignableFrom(discoveredType);
        }

        if (observedTypeArgument instanceof ParameterizedType) {
            Type rawObservedType = ((ParameterizedType) observedTypeArgument).getRawType();
            return rawObservedType instanceof Class<?> &&
                    ((Class<?>) rawObservedType).isAssignableFrom(discoveredType);
        }

        if (observedTypeArgument instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) observedTypeArgument;

            Type[] upperBounds = wildcardType.getUpperBounds();
            for (Type upperBound : upperBounds) {
                if (upperBound instanceof Class<?> &&
                        !((Class<?>) upperBound).isAssignableFrom(discoveredType)) {
                    return false;
                }
            }

            Type[] lowerBounds = wildcardType.getLowerBounds();
            for (Type lowerBound : lowerBounds) {
                if (lowerBound instanceof Class<?> &&
                        !discoveredType.isAssignableFrom((Class<?>) lowerBound)) {
                    return false;
                }
            }

            return true;
        }

        // TypeVariable and other reflective forms are treated as unrestricted for PAT matching.
        return true;
    }

    private AnnotatedType<?> extractAnnotatedTypeFromPatEvent(Object event) {
        if (event instanceof ProcessAnnotatedTypeImpl<?>) {
            return ((ProcessAnnotatedTypeImpl<?>) event).getAnnotatedTypeInternal();
        }
        try {
            return ((ProcessAnnotatedType<?>) event).getAnnotatedType();
        } catch (IllegalStateException ignored) {
            // Guarded lifecycle event implementations may reject access outside invocation.
            return null;
        }
    }

    private Annotated extractAnnotatedFromPbaEvent(Object event) {
        if (event instanceof ProcessBeanAttributesImpl<?>) {
            return ((ProcessBeanAttributesImpl<?>) event).getAnnotatedInternal();
        }
        try {
            return ((ProcessBeanAttributes<?>) event).getAnnotated();
        } catch (IllegalStateException ignored) {
            // Guarded lifecycle event implementations may reject access outside invocation.
            return null;
        }
    }

    private void validateWithAnnotationsUsage(Method method,
                                              int observesParameterIndex,
                                              Parameter[] parameters) {
        for (int i = 0; i < parameters.length; i++) {
            if (i == observesParameterIndex) {
                continue;
            }
            if (resolveWithAnnotationsFilter(parameters[i]) != null) {
                throw new DefinitionException("@WithAnnotations may only be declared on the @Observes event parameter: " +
                        method.getDeclaringClass().getName() + "." + method.getName());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Class<? extends Annotation>> resolveWithAnnotationsFilter(Parameter parameter) {
        if (!hasWithAnnotationsAnnotation(parameter)) {
            return null;
        }
        Annotation[] annotations = parameter.getAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (!WITH_ANNOTATIONS.matches(annotationType)) {
                continue;
            }
            try {
                Method valueMethod = annotationType.getMethod("value");
                Object value = valueMethod.invoke(annotation);
                if (!(value instanceof Class[])) {
                    return Collections.emptySet();
                }
                Class<?>[] rawValues = (Class<?>[]) value;
                Set<Class<? extends Annotation>> filter = new LinkedHashSet<Class<? extends Annotation>>();
                for (Class<?> rawValue : rawValues) {
                    if (rawValue != null && Annotation.class.isAssignableFrom(rawValue)) {
                        filter.add((Class<? extends Annotation>) rawValue);
                    }
                }
                return filter;
            } catch (Exception e) {
                throw new DefinitionException("Unable to read @WithAnnotations value on parameter " + parameter, e);
            }
        }
        return null;
    }

    private void validateExtensionObserverStaticMethod(Method method,
                                                       Parameter observedParameter,
                                                       Class<?> observedType) {
        if (!Modifier.isStatic(method.getModifiers())) {
            return;
        }

        boolean lifecycleObservedType = isContainerLifecycleObservedType(observedType);
        boolean objectObservedWithNoOrAnyQualifier =
                Object.class.equals(observedType) && hasNoQualifierOrOnlyAnyQualifier(observedParameter);

        if (lifecycleObservedType || objectObservedWithNoOrAnyQualifier) {
            throw new NonPortableBehaviourException("Static extension observer method " +
                    method.getDeclaringClass().getName() + "." + method.getName() +
                    " is non-portable for observed type " + observedType.getName());
        }
    }

    private boolean isContainerLifecycleObservedType(Class<?> observedType) {
        String name = observedType.getName();
        return name.startsWith("jakarta.enterprise.inject.spi.Process") ||
                "jakarta.enterprise.inject.spi.BeforeBeanDiscovery".equals(name) ||
                "jakarta.enterprise.inject.spi.AfterTypeDiscovery".equals(name) ||
                "jakarta.enterprise.inject.spi.AfterBeanDiscovery".equals(name) ||
                "jakarta.enterprise.inject.spi.AfterDeploymentValidation".equals(name) ||
                "jakarta.enterprise.inject.spi.BeforeShutdown".equals(name);
    }

    private boolean hasNoQualifierOrOnlyAnyQualifier(Parameter observedParameter) {
        List<Annotation> qualifierAnnotations = new ArrayList<>();
        for (Annotation annotation : observedParameter.getAnnotations()) {
            if (hasQualifierAnnotation(annotation.annotationType())) {
                qualifierAnnotations.add(annotation);
            }
        }

        if (qualifierAnnotations.isEmpty()) {
            return true;
        }

        return qualifierAnnotations.size() == 1 &&
                Any.class.equals(qualifierAnnotations.get(0).annotationType());
    }

    private int resolvePriority(Method method, Parameter observedParameter) {
        Integer parameterPriority = getPriorityValue(observedParameter);
        if (parameterPriority != null) {
            return parameterPriority;
        }
        Integer methodPriority = getPriorityValue(method);
        if (methodPriority != null) {
            return methodPriority;
        }
        return Integer.MAX_VALUE;
    }

    @SuppressWarnings("unchecked")
    private <T> T wrapEventForObserverInvocationGuard(T event) {
        if (event == null) {
            return null;
        }
        if (event instanceof ObserverInvocationControlled) {
            return event;
        }
        if (!shouldGuardEventInvocation(event)) {
            return event;
        }

        Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
        Class<?> current = event.getClass();
        while (current != null) {
            for (Class<?> iface : current.getInterfaces()) {
                interfaces.add(iface);
            }
            current = current.getSuperclass();
        }

        if (interfaces.isEmpty()) {
            return event;
        }

        interfaces.add(ObserverInvocationControlled.class);
        InvocationGuardHandler handler = new InvocationGuardHandler(event);
        return (T) Proxy.newProxyInstance(
                event.getClass().getClassLoader(),
                interfaces.toArray(new Class<?>[0]),
                handler);
    }

    private boolean shouldGuardEventInvocation(Object event) {
        Set<String> interfaceNames = new HashSet<String>();
        Class<?> current = event.getClass();
        while (current != null) {
            for (Class<?> iface : current.getInterfaces()) {
                interfaceNames.add(iface.getName());
            }
            current = current.getSuperclass();
        }

        return interfaceNames.contains("jakarta.enterprise.inject.spi.BeforeBeanDiscovery") ||
                interfaceNames.contains("jakarta.enterprise.inject.spi.AfterTypeDiscovery") ||
                interfaceNames.contains("jakarta.enterprise.inject.spi.AfterBeanDiscovery") ||
                interfaceNames.contains("jakarta.enterprise.inject.spi.AfterDeploymentValidation") ||
                interfaceNames.contains("jakarta.enterprise.inject.spi.BeforeShutdown");
    }

    private interface ObserverInvocationControlled {
        void enterObserverInvocation();

        void exitObserverInvocation();
    }

    private static class InvocationGuardHandler implements InvocationHandler {
        private final Object delegate;
        private final ThreadLocal<Boolean> observerInvocationActive = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return Boolean.FALSE;
            }
        };

        InvocationGuardHandler(Object delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Class<?> declaringClass = method.getDeclaringClass();
            if (Object.class.equals(declaringClass)) {
                return method.invoke(delegate, args);
            }
            if (ObserverInvocationControlled.class.equals(declaringClass)) {
                if ("enterObserverInvocation".equals(method.getName())) {
                    observerInvocationActive.set(Boolean.TRUE);
                    if (delegate instanceof ObserverInvocationLifecycle) {
                        ((ObserverInvocationLifecycle) delegate).beginObserverInvocation();
                    }
                    return null;
                }
                if ("exitObserverInvocation".equals(method.getName())) {
                    if (delegate instanceof ObserverInvocationLifecycle) {
                        ((ObserverInvocationLifecycle) delegate).endObserverInvocation();
                    }
                    observerInvocationActive.set(Boolean.FALSE);
                    return null;
                }
            }

            if (!observerInvocationActive.get()) {
                throw new IllegalStateException("Container lifecycle event method " + method.getName() +
                        " may only be called during observer method invocation");
            }

            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

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
        private final Set<Class<? extends Annotation>> withAnnotationsFilter;

        ExtensionObserverInvocation(Extension extension,
                                    Method method,
                                    int observesIndex,
                                    int priority,
                                    BeanManager beanManager,
                                    KnowledgeBase knowledgeBase,
                                    MessageHandler messageHandler,
                                    Set<Class<? extends Annotation>> withAnnotationsFilter) {
            this.extension = extension;
            this.method = method;
            this.observesIndex = observesIndex;
            this.priority = priority;
            this.beanManager = beanManager;
            this.knowledgeBase = knowledgeBase;
            this.messageHandler = messageHandler;
            this.withAnnotationsFilter = withAnnotationsFilter;
        }

        void invoke(Object event) throws Exception {
            if (!matchesWithAnnotationsFilter(event)) {
                return;
            }
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

        private boolean matchesWithAnnotationsFilter(Object event) {
            if (withAnnotationsFilter == null) {
                return true;
            }
            if (!(event instanceof ProcessAnnotatedType<?>)) {
                return true;
            }
            AnnotatedType<?> annotatedType = ((ProcessAnnotatedType<?>) event).getAnnotatedType();
            if (annotatedType == null) {
                return false;
            }
            return hasAnyConfiguredAnnotation(annotatedType);
        }

        private boolean hasAnyConfiguredAnnotation(AnnotatedType<?> annotatedType) {
            if (matchesAnyAnnotation(annotatedType.getAnnotations())) {
                return true;
            }
            for (AnnotatedField<?> field : annotatedType.getFields()) {
                if (matchesAnyAnnotation(field.getAnnotations())) {
                    return true;
                }
            }
            for (AnnotatedMethod<?> method : annotatedType.getMethods()) {
                if (matchesAnyAnnotation(method.getAnnotations())) {
                    return true;
                }
                for (AnnotatedParameter<?> parameter : method.getParameters()) {
                    if (matchesAnyAnnotation(parameter.getAnnotations())) {
                        return true;
                    }
                }
            }
            for (AnnotatedConstructor<?> constructor : annotatedType.getConstructors()) {
                if (matchesAnyAnnotation(constructor.getAnnotations())) {
                    return true;
                }
                for (AnnotatedParameter<?> parameter : constructor.getParameters()) {
                    if (matchesAnyAnnotation(parameter.getAnnotations())) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean matchesAnyAnnotation(Set<Annotation> annotations) {
            for (Annotation annotation : annotations) {
                Class<? extends Annotation> presentType = annotation.annotationType();
                if (withAnnotationsFilter.contains(presentType)) {
                    return true;
                }
                for (Class<? extends Annotation> filter : withAnnotationsFilter) {
                    if (presentType.isAnnotationPresent(filter)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private Object resolveExtensionParameter(java.lang.reflect.Parameter parameter) {
            Class<?> pType = parameter.getType();
            if (BeanManager.class.isAssignableFrom(pType)) {
                return beanManager;
            }
            throw new NonPortableBehaviourException("Injecting " + pType.getName() +
                    " into extension observer method parameter is non-portable; only BeanManager is supported");
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
     * Activates request context if currently inactive.
     *
     * @return true when this call activated the request context, false if it was already active
     */
    public boolean activateRequestContextIfNeeded() {
        if (!initialized) {
            throw new IllegalStateException("Container not initialized. Call setup() first.");
        }
        if (contextManager.getContext(RequestScoped.class).isActive()) {
            return false;
        }
        contextManager.activateRequest();
        return true;
    }

    /**
     * Deactivates request context if currently active.
     */
    public void deactivateRequestContextIfActive() {
        if (!initialized) {
            throw new IllegalStateException("Container not initialized. Call setup() first.");
        }
        if (contextManager.getContext(RequestScoped.class).isActive()) {
            contextManager.deactivateRequest();
        }
    }

    /**
     * Activates a synthetic session context when no session is currently associated
     * with the current thread.
     *
     * @return synthetic session id if activated by this call, otherwise null
     */
    public String activateSyntheticSessionContextIfNeeded() {
        if (!initialized) {
            throw new IllegalStateException("Container not initialized. Call setup() first.");
        }
        String currentSessionId = contextManager.getCurrentSessionId();
        if (currentSessionId != null) {
            return null;
        }
        String syntheticSessionId = "syringe-auto-session-" + java.util.UUID.randomUUID();
        contextManager.activateSession(syntheticSessionId);
        return syntheticSessionId;
    }

    /**
     * Deactivates session context for the current thread, when active.
     */
    public void deactivateSessionContextIfActive() {
        if (!initialized) {
            throw new IllegalStateException("Container not initialized. Call setup() first.");
        }
        if (contextManager.getCurrentSessionId() != null) {
            contextManager.deactivateSession();
        }
    }

    /**
     * Invalidates and destroys a specific session context.
     *
     * @param sessionId id of the session to invalidate
     */
    public void invalidateSessionContext(String sessionId) {
        if (!initialized) {
            throw new IllegalStateException("Container not initialized. Call setup() first.");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }
        contextManager.invalidateSession(sessionId);
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
            String message = "No bean found for type " + beanClass.getName();
            knowledgeBase.addInjectionError("Programmatic lookup unsatisfied dependency: " + message);
            throw new UnsatisfiedResolutionException(message);
        }
        Bean<?> bean;
        try {
            bean = bm.resolve(beans);
        } catch (AmbiguousResolutionException e) {
            knowledgeBase.addInjectionError("Programmatic lookup ambiguous dependency for type " +
                    beanClass.getName() + " with qualifiers " + formatQualifiers(qualifiers) + ": " + e.getMessage());
            throw e;
        }
        if (bean == null) {
            String candidates = beans.stream()
                    .map(candidate -> candidate.getBeanClass().getName())
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(", "));
            String message = "Ambiguous dependency for type " + beanClass.getName() +
                    " with qualifiers " + formatQualifiers(qualifiers) +
                    ". Matching beans: [" + candidates + "]";
            knowledgeBase.addInjectionError("Programmatic lookup " + message);
            throw new AmbiguousResolutionException(message);
        }
        CreationalContext<?> ctx = bm.createCreationalContext(bean);
        @SuppressWarnings("unchecked")
        T instance = (T) bm.getReference(bean, beanClass, ctx);
        return instance;
    }

    private String formatQualifiers(Annotation... qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            return "[@Default]";
        }
        return Arrays.stream(qualifiers)
                .filter(Objects::nonNull)
                .map(annotation -> "@" + annotation.annotationType().getSimpleName())
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
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
        if (beanManager == null) {
            if (isInvokedThroughCdiCurrentLookup()) {
                BeanManagerImpl provisionalBeanManager = new BeanManagerImpl(knowledgeBase, contextManager);
                return new com.threeamigos.common.util.implementations.injection.spi.CDIImpl(
                        provisionalBeanManager,
                        cdiLiteMode,
                        this::isCdiPortableAccessWindow);
            }
            throw new NonPortableBehaviourException(
                    "CDI.current() access is non-portable before BeforeBeanDiscovery is fired");
        }
        return new com.threeamigos.common.util.implementations.injection.spi.CDIImpl(
                beanManager,
                cdiLiteMode,
                this::isCdiPortableAccessWindow);
    }

    private boolean isCdiPortableAccessWindow() {
        return beforeBeanDiscoveryFired && !beforeShutdownFired;
    }

    private boolean isInvokedThroughCdiCurrentLookup() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            if ("jakarta.enterprise.inject.spi.CDI".equals(element.getClassName())) {
                String method = element.getMethodName();
                if ("current".equals(method) || "getCDIProvider".equals(method)) {
                    return true;
                }
            }
        }
        return false;
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
            knowledgeBase.setBeanArchiveMode(clazz, forcedBeanArchiveMode);
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
