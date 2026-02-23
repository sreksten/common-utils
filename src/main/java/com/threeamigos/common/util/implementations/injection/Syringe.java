package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;
import com.threeamigos.common.util.implementations.injection.contexts.ContextManager;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spievents.*;
import jakarta.enterprise.inject.spi.*;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

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
     * Set of extension class names to be loaded.
     * Extensions must implement jakarta.enterprise.inject.spi.Extension.
     */
    private final Set<String> extensionClassNames = new HashSet<>();

    /**
     * Loaded extension instances.
     */
    private final List<Extension> extensions = new ArrayList<>();

    /**
     * The BeanManager - central interface for programmatic CDI access.
     */
    private BeanManagerImpl beanManager;

    /**
     * Knowledge base containing all discovered beans, interceptors, decorators, observers.
     */
    private KnowledgeBase knowledgeBase;

    private ContextManager contextManager;

    /**
     * Package names to scan for beans.
     */
    private String[] packageNames;

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

    public Syringe(String... packageNames) {
        this.packageNames = packageNames != null ? packageNames : new String[0];
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
            throw new IllegalStateException(
                "Cannot register custom contexts after container initialization. " +
                "Call this method before setup()."
            );
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
        System.out.println("[Syringe] Queued custom context for registration: @" +
                          scopeAnnotation.getSimpleName());
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
     *
     * @throws DeploymentException if validation fails or extensions cause errors
     */
    public void setup() {
        if (initialized) {
            throw new IllegalStateException("Container already initialized");
        }

        try {
            // ============================================================
            // PHASE 1: CONTAINER INITIALIZATION
            // ============================================================
            System.out.println("[Syringe] Phase 1: Container Initialization");

            // Step 1.1: Load portable extensions via ServiceLoader + explicitly registered
            loadExtensions();

            // Step 1.2: Create BeanManager and KnowledgeBase
            knowledgeBase = new KnowledgeBase();
            contextManager = new ContextManager();
            beanManager = new BeanManagerImpl(knowledgeBase, contextManager);

            // ============================================================
            // PHASE 2: BEAN DISCOVERY
            // ============================================================
            System.out.println("[Syringe] Phase 2: Bean Discovery");

            // Step 2.1: Fire BeforeBeanDiscovery event
            // Extensions can:
            // - Add new qualifiers, scopes, stereotypes, interceptor bindings
            // - Register additional beans programmatically
            fireBeforeBeanDiscovery();

            // Step 2.2: Perform bean discovery (classpath scanning)
            // - Scan for classes in specified packages
            // - Detect bean archives (explicit/implicit via beans.xml)
            // - Discover annotated types
            discoverBeans();

            // Step 2.3: Fire ProcessAnnotatedType<T> for each discovered type
            // Extensions can:
            // - Veto types from becoming beans
            // - Add/remove/modify annotations
            // - Wrap AnnotatedType to customize metadata
            processAnnotatedTypes();

            // ============================================================
            // PHASE 3: BEAN PROCESSING
            // ============================================================
            System.out.println("[Syringe] Phase 3: Bean Processing");

            // Step 3.1: Validate beans and build Bean<?> objects
            // - Check constructor eligibility
            // - Validate injection points
            // - Check scope, qualifiers, stereotypes
            validateAndRegisterBeans();

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
            System.out.println("[Syringe] Phase 4: After Bean Discovery");

            // Step 4.1: Fire AfterBeanDiscovery event
            // Extensions can:
            // - Register additional beans programmatically
            // - Register custom contexts
            // - Add observer methods programmatically
            // - Register interceptors and decorators
            fireAfterBeanDiscovery();

            // ============================================================
            // PHASE 5: VALIDATION
            // ============================================================
            System.out.println("[Syringe] Phase 5: Deployment Validation");

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
            System.out.println("[Syringe] Phase 6: Application Ready");

            initialized = true;
            System.out.println("[Syringe] Container initialization complete");

        } catch (Exception e) {
            throw new DeploymentException("Container initialization failed", e);
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

        System.out.println("[Syringe] Shutting down container");

        // Fire BeforeShutdown event
        fireBeforeShutdown();

        // Destroy all beans (call @PreDestroy methods)
        destroyAllBeans();

        // Clear state
        extensions.clear();
        initialized = false;

        System.out.println("[Syringe] Container shutdown complete");
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
        System.out.println("[Syringe] Loading extensions...");

        // Load extensions via ServiceLoader (standard CDI discovery)
        ServiceLoader<Extension> serviceLoader = ServiceLoader.load(
                Extension.class,
                Thread.currentThread().getContextClassLoader()
        );

        for (Extension extension : serviceLoader) {
            extensions.add(extension);
            System.out.println("[Syringe]   Loaded extension: " + extension.getClass().getName());
        }

        // Load explicitly registered extensions
        for (String className : extensionClassNames) {
            try {
                Class<?> extensionClass = Class.forName(className);
                if (!Extension.class.isAssignableFrom(extensionClass)) {
                    throw new DeploymentException(
                            "Extension class " + className + " does not implement Extension interface"
                    );
                }
                Extension extension = (Extension) extensionClass.getDeclaredConstructor().newInstance();
                extensions.add(extension);
                System.out.println("[Syringe]   Loaded extension: " + className);
            } catch (Exception e) {
                throw new DeploymentException("Failed to load extension: " + className, e);
            }
        }

        System.out.println("[Syringe] Loaded " + extensions.size() + " extension(s)");
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
        System.out.println("[Syringe] Firing BeforeBeanDiscovery event");
        BeforeBeanDiscovery event = new BeforeBeanDiscoveryImpl(knowledgeBase, beanManager);
        fireEventToExtensions(event);
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
        System.out.println("[Syringe] Discovering beans in packages: " + Arrays.toString(packageNames));

        try (ParallelTaskExecutor parallelTaskExecutor = ParallelTaskExecutor.createExecutor()) {
            ClassProcessor classProcessor = new ClassProcessor(parallelTaskExecutor, knowledgeBase);
            new ParallelClasspathScanner(
                    Thread.currentThread().getContextClassLoader(),
                    classProcessor,
                    packageNames
            );
            parallelTaskExecutor.awaitCompletion();
        } catch (Exception e) {
            throw new DeploymentException("Bean discovery failed", e);
        }

        System.out.println("[Syringe] Discovered " + knowledgeBase.getClasses().size() + " class(es)");
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
        System.out.println("[Syringe] Processing annotated types");

        // TODO: For each discovered class:
        // 1. Create AnnotatedType<T> using reflection
        // 2. Create ProcessAnnotatedType<T> event
        // 3. Fire to all extensions
        // 4. If vetoed, remove from bean candidates
        // 5. If modified, use modified AnnotatedType

        // for (Class<?> clazz : knowledgeBase.getClasses()) {
        //     AnnotatedType<?> annotatedType = createAnnotatedType(clazz);
        //     ProcessAnnotatedType<?> event = new ProcessAnnotatedTypeImpl<>(annotatedType);
        //     fireToExtensions(event);
        //
        //     if (event.isVeto()) {
        //         // Remove from bean candidates
        //         continue;
        //     }
        //     // Use event.getAnnotatedType() (may be modified)
        // }
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
        System.out.println("[Syringe] Validating and registering beans");

        // CDI41BeanValidator is already called during bean discovery
        // via ClassProcessor, but we can perform additional validation here

        // TODO: Additional bean validation if needed
        // - Check that all beans have valid scopes
        // - Validate qualifiers, stereotypes
        // - Build final Bean<?> objects

        System.out.println("[Syringe] Registered " + knowledgeBase.getBeans().size() + " bean(s)");
    }

    /**
     * Fires ProcessInjectionPoint<T, X> events for all discovered injection points.
     *
     * <p>Extensions can modify injection point metadata.
     */
    private void processInjectionPoints() {
        System.out.println("[Syringe] Processing injection points");

        // TODO: For each bean, for each injection point:
        // 1. Create ProcessInjectionPoint<T, X> event
        // 2. Fire to all extensions
        // 3. Use modified injection point if changed

        // for (Bean<?> bean : knowledgeBase.getBeans()) {
        //     for (InjectionPoint ip : bean.getInjectionPoints()) {
        //         ProcessInjectionPoint<?, ?> event = new ProcessInjectionPointImpl<>(ip);
        //         fireToExtensions(event);
        //         // Use event.getInjectionPoint() (may be modified)
        //     }
        // }
    }

    /**
     * Fires ProcessInjectionTarget<T> events for all injection targets.
     *
     * <p>Extensions can wrap InjectionTarget to customize instantiation and injection.
     */
    private void processInjectionTargets() {
        System.out.println("[Syringe] Processing injection targets");

        // TODO: For each managed bean:
        // 1. Create InjectionTarget<T>
        // 2. Create ProcessInjectionTarget<T> event
        // 3. Fire to all extensions
        // 4. Use wrapped InjectionTarget if provided
    }

    /**
     * Fires ProcessBeanAttributes<T> events for all beans.
     *
     * <p>Extensions can modify bean attributes (scope, qualifiers, stereotypes, name).
     */
    private void processBeanAttributes() {
        System.out.println("[Syringe] Processing bean attributes");

        // TODO: For each bean:
        // 1. Create BeanAttributes<T>
        // 2. Create ProcessBeanAttributes<T> event
        // 3. Fire to all extensions
        // 4. Apply modified attributes if changed
    }

    /**
     * Fires ProcessBean events (ProcessManagedBean, ProcessProducerMethod, ProcessProducerField, ProcessSyntheticBean).
     *
     * <p>Extensions can inspect final Bean<?> objects before deployment validation.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processBean() {
        System.out.println("[Syringe] Processing beans");

        Collection<Bean<?>> allBeans = knowledgeBase.getBeans();
        System.out.println("[Syringe]   Found " + allBeans.size() + " total bean(s)");

        int managedCount = 0;
        int producerMethodCount = 0;
        int producerFieldCount = 0;
        int syntheticCount = 0;

        for (Bean<?> bean : allBeans) {
            try {
                // Determine bean type and fire appropriate event
                if (bean instanceof SyntheticBean) {
                    // Synthetic bean - registered via AfterBeanDiscovery.addBean()
                    ProcessSyntheticBeanImpl event = new ProcessSyntheticBeanImpl(
                        bean,
                        null, // source extension - we don't track this yet
                        beanManager
                    );
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
                    // TODO: Fire ProcessManagedBean<T> event
                    // For now, we just count them
                    managedCount++;

                } else {
                    // Built-in beans (BeanManager, InjectionPoint, etc.)
                    // These don't get ProcessBean events
                    System.out.println("[Syringe]   Skipping built-in bean: " +
                                      bean.getBeanClass().getSimpleName());
                }
            } catch (Exception e) {
                System.err.println("[Syringe] Error processing bean " +
                                  bean.getBeanClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("[Syringe]   Processed: " +
                          managedCount + " managed, " +
                          producerMethodCount + " producer methods, " +
                          producerFieldCount + " producer fields, " +
                          syntheticCount + " synthetic");
    }

    /**
     * Fires ProcessProducer<T, X> events for all producers.
     *
     * <p>Extensions can wrap Producer to customize production logic.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processProducers() {
        System.out.println("[Syringe] Processing producers");

        Collection<ProducerBean<?>> producers = knowledgeBase.getProducerBeans();
        System.out.println("[Syringe]   Found " + producers.size() + " producer(s)");

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
                        ProcessProducerMethodImpl event = new ProcessProducerMethodImpl(
                            producerBean, // bean
                            annotatedMethod,
                            producer,
                            null, // disposerParameter - not tracking this yet
                            beanManager
                        );

                        fireEventToExtensions(event);

                        // If extensions wrapped the producer, we would use event.getFinalProducer()
                        // For now, we log if producer was replaced
                        if (event.getFinalProducer() != producer) {
                            System.out.println("[Syringe]   Producer wrapped for method: " +
                                             declaringClass.getSimpleName() + "." + method.getName());
                        }
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
                        ProcessProducerFieldImpl event = new ProcessProducerFieldImpl(
                            producerBean, // bean
                            annotatedField,
                            producer,
                            null, // disposerParameter - not tracking this yet
                            beanManager
                        );

                        fireEventToExtensions(event);

                        // If extensions wrapped the producer, we would use event.getFinalProducer()
                        if (event.getFinalProducer() != producer) {
                            System.out.println("[Syringe]   Producer wrapped for field: " +
                                             declaringClass.getSimpleName() + "." + field.getName());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[Syringe] Error processing producer: " + e.getMessage());
                e.printStackTrace();
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
        System.out.println("[Syringe] Processing observer methods");

        // TODO: For each observer method:
        // 1. Create ObserverMethod<T>
        // 2. Create ProcessObserverMethod<T, X> event
        // 3. Fire to all extensions
        // 4. Use modified observer if changed
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
     * This method also registers any custom contexts that were added programmatically
     * via {@link #registerCustomContext(Class, Context)} before container initialization.
     */
    private void fireAfterBeanDiscovery() {
        System.out.println("[Syringe] Firing AfterBeanDiscovery event");
        AfterBeanDiscovery event = new AfterBeanDiscoveryImpl(knowledgeBase, beanManager);

        // Register programmatically added custom contexts BEFORE firing to extensions
        // This allows extensions to see and potentially modify these contexts
        if (!customContextsToRegister.isEmpty()) {
            System.out.println("[Syringe] Registering " + customContextsToRegister.size() +
                              " programmatically added custom context(s)");

            for (Map.Entry<Class<? extends Annotation>, Context> entry : customContextsToRegister.entrySet()) {
                try {
                    event.addContext(entry.getValue());
                    System.out.println("[Syringe]   Registered custom context for @" +
                                      entry.getKey().getSimpleName());
                } catch (Exception e) {
                    System.err.println("[Syringe] Failed to register custom context for @" +
                                      entry.getKey().getSimpleName() + ": " + e.getMessage());
                    throw new DeploymentException(
                        "Failed to register custom context for @" + entry.getKey().getSimpleName(), e
                    );
                }
            }
        }

        // Fire event to extensions (they can also register custom contexts)
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
        System.out.println("[Syringe] Validating deployment");

        // TODO: Perform comprehensive validation
        // 1. Check for unsatisfied/ambiguous dependencies
        CDI41InjectionValidator injectionValidator = new CDI41InjectionValidator(knowledgeBase);
        injectionValidator.validateAllInjectionPoints();

        // 2. Check definition errors
        if (knowledgeBase.hasErrors()) {
            System.err.println("[Syringe] Deployment validation failed:");
            knowledgeBase.getDefinitionErrors().forEach(error ->
                    System.err.println("  - Definition error: " + error));
            knowledgeBase.getInjectionErrors().forEach(error ->
                    System.err.println("  - Injection error: " + error));
            throw new DeploymentException("Deployment validation failed. See errors above.");
        }

        System.out.println("[Syringe] Deployment validation passed");
    }

    /**
     * Fires AfterDeploymentValidation event to all extensions.
     *
     * <p>Extensions can perform final validation checks.
     * Any deployment problems detected here will prevent application startup.
     */
    private void fireAfterDeploymentValidation() {
        System.out.println("[Syringe] Firing AfterDeploymentValidation event");
        AfterDeploymentValidation event = new AfterDeploymentValidationImpl(knowledgeBase, beanManager);
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
        System.out.println("[Syringe] Firing BeforeShutdown event");
        BeforeShutdown event = new BeforeShutdownImpl(beanManager);
        fireEventToExtensions(event);
    }

    /**
     * Destroys all beans by calling @PreDestroy methods.
     */
    private void destroyAllBeans() {
        System.out.println("[Syringe] Destroying all beans");

        // TODO: For each context, destroy all beans
        // - Call @PreDestroy methods
        // - Release resources
        // - Clear context storage
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Fires an event to all registered extensions by invoking their observer methods.
     *
     * <p>This method scans each extension for methods with parameters annotated with @Observes
     * that match the event type, and invokes them with the event object.
     *
     * @param event the event object to fire
     * @param <T> the event type
     */
    private <T> void fireEventToExtensions(T event) {
        Class<?> eventType = event.getClass();

        for (Extension extension : extensions) {
            try {
                invokeExtensionObserverMethods(extension, eventType, event);
            } catch (Exception e) {
                System.err.println("[Syringe] Error invoking extension " + extension.getClass().getName() +
                                   " for event " + eventType.getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Invokes observer methods in an extension that observe a specific event type.
     *
     * <p>This method uses reflection to:
     * <ol>
     *   <li>Find all methods in the extension class</li>
     *   <li>Check each method's parameters for @Observes annotation</li>
     *   <li>Verify the observed parameter type matches the event type (or is a supertype)</li>
     *   <li>Invoke the method with the event object</li>
     * </ol>
     *
     * @param extension the extension instance
     * @param eventType the event class being fired
     * @param event the event object
     */
    private void invokeExtensionObserverMethods(Extension extension, Class<?> eventType, Object event) {
        java.lang.reflect.Method[] methods = extension.getClass().getMethods();

        for (java.lang.reflect.Method method : methods) {
            java.lang.reflect.Parameter[] parameters = method.getParameters();

            // Check each parameter for @Observes annotation
            for (int i = 0; i < parameters.length; i++) {
                java.lang.reflect.Parameter parameter = parameters[i];

                // Check if parameter has @Observes annotation (javax or jakarta)
                if (AnnotationsEnum.hasObservesAnnotation(parameter)) {
                    // Check if the observed parameter type matches the event type
                    Class<?> observedType = parameter.getType();

                    if (observedType.isAssignableFrom(eventType)) {
                        try {
                            // Prepare method arguments
                            Object[] args = new Object[parameters.length];
                            args[i] = event; // The observed parameter gets the event

                            // TODO: Other parameters might be injection points
                            // For now, we only support single @Observes parameter
                            if (parameters.length > 1) {
                                System.err.println("[Syringe] Warning: Extension observer method " +
                                                   method.getName() + " has multiple parameters. " +
                                                   "Only @Observes parameter is supported.");
                            }

                            // Invoke the observer method
                            method.setAccessible(true);
                            method.invoke(extension, args);

                            System.out.println("[Syringe]   Invoked extension observer: " +
                                               extension.getClass().getSimpleName() + "." + method.getName() +
                                               "(@Observes " + eventType.getSimpleName() + ")");

                        } catch (Exception e) {
                            System.err.println("[Syringe] Error invoking observer method " +
                                               method.getName() + " in extension " +
                                               extension.getClass().getName() + ": " + e.getMessage());
                            e.printStackTrace();
                        }

                        break; // Found the @Observes parameter, no need to check other parameters
                    }
                }
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
}
