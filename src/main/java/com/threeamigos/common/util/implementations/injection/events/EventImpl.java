package com.threeamigos.common.util.implementations.injection.events;

import com.threeamigos.common.util.implementations.injection.AnnotationsEnum;
import com.threeamigos.common.util.implementations.injection.scopes.ContextManager;
import com.threeamigos.common.util.implementations.injection.scopes.ScopeContext;
import com.threeamigos.common.util.implementations.injection.scopes.RequestScopedContext;
import com.threeamigos.common.util.implementations.injection.scopes.ConversationScopedContext;
import com.threeamigos.common.util.implementations.injection.scopes.SessionScopedContext;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.resolution.BeanImpl;
import com.threeamigos.common.util.implementations.injection.resolution.BeanResolver;
import com.threeamigos.common.util.implementations.injection.resolution.TypeChecker;
import com.threeamigos.common.util.implementations.injection.scopes.InjectionPointImpl;
import com.threeamigos.common.util.implementations.injection.util.LifecycleMethodHelper;
import com.threeamigos.common.util.implementations.injection.util.QualifiersHelper;
import com.threeamigos.common.util.implementations.injection.util.tx.TransactionServices;
import com.threeamigos.common.util.implementations.injection.util.tx.NoOpTransactionServices;
import com.threeamigos.common.util.implementations.injection.util.tx.TransactionSynchronizationCallbacks;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.event.ObserverException;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CDI 4.1 Event implementation for firing synchronous and asynchronous events.
 *
 * <p>This class implements the {@link Event} interface from CDI 4.1 specification,
 * providing mechanisms to fire events to registered observer methods annotated with
 * {@link jakarta.enterprise.event.Observes @Observes} or
 * {@link jakarta.enterprise.event.ObservesAsync @ObservesAsync}.
 *
 * <p><b>CDI 4.1 Event Features:</b>
 * <ul>
 *   <li>Synchronous event firing via {@link #fire(Object)}</li>
 *   <li>Asynchronous event firing via {@link #fireAsync(Object)} and {@link #fireAsync(Object, NotificationOptions)}</li>
 *   <li>Observer method matching based on the event type and qualifiers</li>
 *   <li>Priority-based observer ordering (lower priority = earlier execution)</li>
 *   <li>Reception condition handling (IF_EXISTS vs. ALWAYS)</li>
 *   <li>Transaction phase support for synchronous observers</li>
 *   <li>Dynamic qualifier selection via {@link #select(Annotation...)}</li>
 * </ul>
 *
 * <p><b>Observer Matching Rules:</b>
 * <ul>
 *   <li>Observer event type must be assignable from the fired event type</li>
 *   <li>All observer qualifiers must be present in the fired event qualifiers</li>
 *   <li>Synchronous observers (@Observes) are notified during fire()</li>
 *   <li>Asynchronous observers (@ObservesAsync) are notified during fireAsync()</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. Event firing can be performed
 * concurrently from multiple threads. Each observer method invocation is isolated.
 *
 * @param <T> the type of events this Event instance can fire
 * @author Stefano Reksten
 * @see jakarta.enterprise.event.Event
 * @see jakarta.enterprise.event.Observes
 * @see jakarta.enterprise.event.ObservesAsync
 * @see ObserverMethodInfo
 */
public class EventImpl<T> implements Event<T> {

    private final Type eventType;
    private final Set<Annotation> qualifiers;
    private final KnowledgeBase knowledgeBase;
    private final BeanResolver beanResolver;
    private final ContextManager contextManager;
    private final TypeChecker typeChecker;
    private final TransactionServices transactionServices;
    private final ContextTokenProvider tokenProvider;
    private final InjectionPoint firingInjectionPoint;
    private final boolean allowStartupEventDispatch;
    private static final AtomicBoolean TRANSACTION_DOWNGRADE_WARNED = new AtomicBoolean(false);
    private static final ConcurrentHashMap<Class<? extends Annotation>, AtomicBoolean> INACTIVE_SCOPE_WARNED =
        new ConcurrentHashMap<>();
    private static final Executor DEFAULT_ASYNC_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "syringe-event-async-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    });

    /**
     * Creates an Event instance for firing events of a specific type with qualifiers.
     *
     * @param eventType the type of events to fire
     * @param qualifiers the qualifiers for event filtering
     * @param knowledgeBase the knowledge base containing registered observers
     * @param beanResolver the resolver for obtaining observer bean instances
     * @param contextManager the context manager for checking bean existence in scopes
     */
    public EventImpl(Type eventType, Set<Annotation> qualifiers, KnowledgeBase knowledgeBase,
                    BeanResolver beanResolver, ContextManager contextManager, TransactionServices transactionServices) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        validateEventType(this.eventType);
        this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers cannot be null");
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.beanResolver = Objects.requireNonNull(beanResolver, "beanResolver cannot be null");
        this.contextManager = Objects.requireNonNull(contextManager, "contextManager cannot be null");
        this.typeChecker = new TypeChecker();
        this.transactionServices = Objects.requireNonNull(transactionServices, "transactionServices cannot be null");
        this.tokenProvider = new NoopContextTokenProvider();
        this.firingInjectionPoint = null;
        this.allowStartupEventDispatch = false;
    }

    public EventImpl(Type eventType, Set<Annotation> qualifiers, KnowledgeBase knowledgeBase,
                     BeanResolver beanResolver, ContextManager contextManager,
                     TransactionServices transactionServices, ContextTokenProvider tokenProvider) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        validateEventType(this.eventType);
        this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers cannot be null");
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.beanResolver = Objects.requireNonNull(beanResolver, "beanResolver cannot be null");
        this.contextManager = Objects.requireNonNull(contextManager, "contextManager cannot be null");
        this.typeChecker = new TypeChecker();
        this.transactionServices = Objects.requireNonNull(transactionServices, "transactionServices cannot be null");
        this.tokenProvider = tokenProvider == null ? new NoopContextTokenProvider() : tokenProvider;
        this.firingInjectionPoint = null;
        this.allowStartupEventDispatch = false;
    }

    public EventImpl(Type eventType, Set<Annotation> qualifiers, KnowledgeBase knowledgeBase,
                     BeanResolver beanResolver, ContextManager contextManager,
                     TransactionServices transactionServices, ContextTokenProvider tokenProvider,
                     InjectionPoint firingInjectionPoint) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        validateEventType(this.eventType);
        this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers cannot be null");
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.beanResolver = Objects.requireNonNull(beanResolver, "beanResolver cannot be null");
        this.contextManager = Objects.requireNonNull(contextManager, "contextManager cannot be null");
        this.typeChecker = new TypeChecker();
        this.transactionServices = Objects.requireNonNull(transactionServices, "transactionServices cannot be null");
        this.tokenProvider = tokenProvider == null ? new NoopContextTokenProvider() : tokenProvider;
        this.firingInjectionPoint = firingInjectionPoint;
        this.allowStartupEventDispatch = false;
    }

    public EventImpl(Type eventType, Set<Annotation> qualifiers, KnowledgeBase knowledgeBase,
                     BeanResolver beanResolver, ContextManager contextManager,
                     TransactionServices transactionServices, ContextTokenProvider tokenProvider,
                     InjectionPoint firingInjectionPoint, boolean allowStartupEventDispatch) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        validateEventType(this.eventType);
        this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers cannot be null");
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.beanResolver = Objects.requireNonNull(beanResolver, "beanResolver cannot be null");
        this.contextManager = Objects.requireNonNull(contextManager, "contextManager cannot be null");
        this.typeChecker = new TypeChecker();
        this.transactionServices = Objects.requireNonNull(transactionServices, "transactionServices cannot be null");
        this.tokenProvider = tokenProvider == null ? new NoopContextTokenProvider() : tokenProvider;
        this.firingInjectionPoint = firingInjectionPoint;
        this.allowStartupEventDispatch = allowStartupEventDispatch;
    }

    /**
     * Fires a synchronous event, notifying all matching observer methods annotated with @Observes.
     *
     * <p>This method:
     * <ol>
     *   <li>Finds all synchronous observers matching the event type and qualifiers</li>
     *   <li>Sorts observers by priority (lower = earlier)</li>
     *   <li>Resolves observer bean instances</li>
     *   <li>Invokes each observer method with the event payload</li>
     *   <li>Handles any exceptions thrown by observers</li>
     * </ol>
     *
     * <p>Per CDI 4.1 specification, synchronous observers are invoked in the calling thread
     * during the fire() method execution. Transaction phase handling is currently limited to
     * IN_PROGRESS phase.
     *
     * @param event the event payload to fire (must not be null)
     * @throws IllegalArgumentException if the event is null
     * @throws RuntimeException if observer invocation fails
     */
    @Override
    public void fire(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        validateEventObject(event);

        // Find all matching synchronous observers
        List<ObserverMethodInfo> matchingObservers = findMatchingObservers(event, false);

        // Sort by priority (lower = earlier)
        matchingObservers.sort(Comparator.comparingInt(ObserverMethodInfo::getPriority));

        boolean txActive = transactionServices.isTransactionActive();
        List<ObserverMethodInfo> beforeCompletion = new ArrayList<>();
        List<ObserverMethodInfo> afterSuccess = new ArrayList<>();
        List<ObserverMethodInfo> afterFailure = new ArrayList<>();
        List<ObserverMethodInfo> afterCompletion = new ArrayList<>();

        // Invoke each observer
        for (ObserverMethodInfo observerInfo : matchingObservers) {
            // Check reception condition per CDI 4.1 specification (Section 10.5.2):
            // - Reception.IF_EXISTS: Only notify if bean instance already exists in scope
            // - Reception.ALWAYS (default): Always notify, creating bean instance if needed
            if (observerInfo.getReception() == Reception.IF_EXISTS) {
                // Only notify if a bean instance already exists in scope
                Bean<?> declaringBean = observerInfo.getDeclaringBean();
                if (declaringBean != null) {
                    Class<? extends Annotation> scopeType = declaringBean.getScope();
                    try {
                        ScopeContext context = contextManager.getContext(scopeType);
                        Object existingInstance = context.getIfExists(declaringBean);

                        // Skip this observer if the bean doesn't exist yet
                        if (existingInstance == null) {
                            continue;
                        }
                    } catch (IllegalArgumentException e) {
                        // Scope not registered - skip this observer
                        continue;
                    }
                }
            }
            // If Reception.ALWAYS: invokeObserver() will create the bean if needed via beanResolver

            TransactionPhase phase = observerInfo.getTransactionPhase();

            if (phase == TransactionPhase.IN_PROGRESS) {
                invokeObserverWithContextCheck(observerInfo, event);
            } else if (!txActive) {
                // Spec: if no transaction is active, transactional observers fire immediately
                maybeWarnTransactionalDowngrade(observerInfo);
                invokeObserverWithContextCheck(observerInfo, event);
            } else {
                switch (phase) {
                    case BEFORE_COMPLETION:
                        beforeCompletion.add(observerInfo);
                        break;
                    case AFTER_SUCCESS:
                        afterSuccess.add(observerInfo);
                        break;
                    case AFTER_FAILURE:
                        afterFailure.add(observerInfo);
                        break;
                    case AFTER_COMPLETION:
                        afterCompletion.add(observerInfo);
                        break;
                    default:
                        // fallback
                        invokeObserverWithContextCheck(observerInfo, event);
                }
            }
        }

        if (txActive && (!beforeCompletion.isEmpty() || !afterSuccess.isEmpty() ||
            !afterFailure.isEmpty() || !afterCompletion.isEmpty())) {
            try {
                transactionServices.registerSynchronization(new TransactionSynchronizationCallbacks() {
                    @Override
                    public void beforeCompletion() {
                        invokeObserverList(beforeCompletion, event);
                    }

                    @Override
                    public void afterCompletion(boolean committed) {
                        if (committed) {
                            invokeObserverList(afterSuccess, event);
                        } else {
                            invokeObserverList(afterFailure, event);
                        }
                        invokeObserverList(afterCompletion, event);
                    }
                });
            } catch (RuntimeException registrationFailure) {
                // CDI 4.1: If synchronization callbacks cannot be registered, notify BEFORE_COMPLETION,
                // AFTER_COMPLETION and AFTER_FAILURE immediately, and skip AFTER_SUCCESS observers.
                invokeObserverList(beforeCompletion, event);
                invokeObserverList(afterFailure, event);
                invokeObserverList(afterCompletion, event);
            }
        }
    }

    private void maybeWarnTransactionalDowngrade(ObserverMethodInfo observerInfo) {
        if (transactionServices instanceof NoOpTransactionServices) {
            if (TRANSACTION_DOWNGRADE_WARNED.compareAndSet(false, true)) {
                String method = observerInfo.getObserverMethod() != null
                    ? observerInfo.getObserverMethod().toGenericString()
                    : "synthetic observer";
                System.out.println("[Event] Transactional observer downgraded to immediate (no transaction services). First occurrence: " + method);
            }
        }
    }

    /**
     * Fires an asynchronous event, notifying all matching observer methods annotated with @ObservesAsync.
     *
     * <p>This method:
     * <ol>
     *   <li>Finds all asynchronous observers matching the event type and qualifiers</li>
     *   <li>Sorts observers by priority (lower = earlier)</li>
     *   <li>Creates a CompletionStage that invokes observers asynchronously</li>
     *   <li>Uses the ForkJoinPool.commonPool() as the default executor</li>
     *   <li>Returns immediately, with observers executing in background</li>
     * </ol>
     *
     * <p>Per CDI 4.1 specification, asynchronous observers are executed in a separate thread.
     * The returned CompletionStage completes when all observers have finished executing.
     *
     * @param <U> the subtype of T
     * @param event the event payload to fire (must not be null)
     * @return CompletionStage that completes when all async observers finish
     * @throws IllegalArgumentException if event is null
     */
    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event) {
        return fireAsync(event, NotificationOptions.ofExecutor(DEFAULT_ASYNC_EXECUTOR));
    }

    /**
     * Fires an asynchronous event with custom notification options.
     *
     * <p>This method allows customization of async event delivery through {@link NotificationOptions}:
     * <ul>
     *   <li>Custom executor for observer execution</li>
     *   <li>Timeout settings</li>
     *   <li>Other implementation-specific options</li>
     * </ul>
     *
     * <p>Observers are invoked sequentially in priority order within the async execution context.
     * If any observer throws an exception, the CompletionStage completes exceptionally.
     *
     * @param <U> the subtype of T
     * @param event the event payload to fire (must not be null)
     * @param options notification options including executor (must not be null)
     * @return CompletionStage that completes when all async observers finish
     * @throws IllegalArgumentException if event or options is null
     */
    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        validateEventObject(event);
        if (options == null) {
            throw new IllegalArgumentException("NotificationOptions cannot be null");
        }

        // Find all matching asynchronous observers
        List<ObserverMethodInfo> matchingObservers = findMatchingObservers(event, true);

        // Sort by priority (lower = earlier)
        matchingObservers.sort(Comparator.comparingInt(ObserverMethodInfo::getPriority));

        // Get executor from options or use default
        Executor executor = options.getExecutor();
        if (executor == null) {
            executor = DEFAULT_ASYNC_EXECUTOR;
        }

        // Create an async task
        CompletableFuture<U> future = CompletableFuture.supplyAsync(() -> {
            ScopeContext requestScopeContext = contextManager.getContext(RequestScoped.class);
            boolean activatedRequestContext = false;
            if (!requestScopeContext.isActive() && requestScopeContext instanceof RequestScopedContext) {
                ((RequestScopedContext) requestScopeContext).activateRequest();
                activatedRequestContext = true;
            }
            try {
                List<Throwable> observerFailures = new ArrayList<>();
                for (ObserverMethodInfo observerInfo : matchingObservers) {
                    // Check reception condition per CDI 4.1 specification (Section 10.5.2):
                    // - Reception.IF_EXISTS: Only notify if bean instance already exists in scope
                    // - Reception.ALWAYS (default): Always notify, creating bean instance if needed
                    if (observerInfo.getReception() == Reception.IF_EXISTS) {
                        // Only notify if a bean instance already exists in scope
                        Bean<?> declaringBean = observerInfo.getDeclaringBean();
                        if (declaringBean != null) {
                            Class<? extends Annotation> scopeType = declaringBean.getScope();
                            try {
                                ScopeContext context = contextManager.getContext(scopeType);
                                Object existingInstance = context.getIfExists(declaringBean);

                                // Skip this observer if the bean doesn't exist yet
                                if (existingInstance == null) {
                                    continue;
                                }
                            } catch (IllegalArgumentException e) {
                                // Scope not registered - skip this observer
                                continue;
                            }
                        }
                    }
                    // If Reception.ALWAYS: invokeObserver() will create the bean if needed via beanResolver

                    ContextActivation activation = ensureObserverContext(observerInfo);
                    if (activation.isSkip()) {
                        continue;
                    }
                    try {
                        invokeObserver(observerInfo, event);
                    } catch (RuntimeException e) {
                        // Async observer failure aborts that observer, not whole event delivery.
                        observerFailures.add(e);
                    } finally {
                        activation.close();
                    }
                }
                if (!observerFailures.isEmpty()) {
                    Throwable primary = observerFailures.get(0);
                    CompletionException completionException =
                        new CompletionException("Asynchronous observer notification failed", primary);
                    for (Throwable failure : observerFailures) {
                        completionException.addSuppressed(failure);
                    }
                    throw completionException;
                }
            } finally {
                if (activatedRequestContext) {
                    ((RequestScopedContext) requestScopeContext).deactivateRequest();
                }
            }
            return event;
        }, executor);

        return future;
    }

    /**
     * Creates a refined Event instance with additional qualifiers for more specific observer selection.
     *
     * <p>This method allows dynamic narrowing of the observer set at runtime by adding qualifiers.
     * The new Event instance will only notify observers that match ALL qualifiers (original + new).
     *
     * <p>Example:
     * <pre>{@code
     * @Inject Event<String> event;
     *
     * // Fire to all observers
     * event.fire("message");
     *
     * // Fire only to @Important observers
     * event.select(new ImportantLiteral()).fire("urgent message");
     * }</pre>
     *
     * @param qualifiers additional qualifiers to filter observers
     * @return new Event instance with combined qualifiers
     * @throws IllegalArgumentException if any qualifier is null
     */
    @Override
    public Event<T> select(Annotation... qualifiers) {
        validateAdditionalQualifiers(qualifiers);
        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        for (Annotation qualifier : qualifiers) {
            if (qualifier == null) {
                throw new IllegalArgumentException("Qualifier cannot be null");
            }
            newQualifiers.add(qualifier);
        }
        return new EventImpl<>(eventType, newQualifiers, knowledgeBase, beanResolver, contextManager,
                transactionServices, tokenProvider, firingInjectionPoint, allowStartupEventDispatch);
    }

    /**
     * Creates a refined Event instance for a subtype with additional qualifiers.
     *
     * <p>This method allows both type narrowing and qualifier refinement. The subtype must
     * be assignable from the current event type.
     *
     * <p>Example:
     * <pre>{@code
     * @Inject Event<Object> event;
     *
     * // Fire specific type with qualifier
     * event.select(String.class, new ImportantLiteral()).fire("message");
     * }</pre>
     *
     * @param <U> the subtype of T
     * @param subtype the class of the subtype
     * @param qualifiers additional qualifiers to filter observers
     * @return new Event instance for the subtype with combined qualifiers
     * @throws IllegalArgumentException if subtype or any qualifier is null
     */
    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
        if (subtype == null) {
            throw new IllegalArgumentException("Subtype cannot be null");
        }
        validateEventType(subtype);
        validateAdditionalQualifiers(qualifiers);

        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        for (Annotation qualifier : qualifiers) {
            if (qualifier == null) {
                throw new IllegalArgumentException("Qualifier cannot be null");
            }
            newQualifiers.add(qualifier);
        }

        // Create raw EventImpl and cast - this is safe because EventImpl<U> implements Event<U>
        return (Event<U>) new EventImpl<U>(subtype, newQualifiers, knowledgeBase, beanResolver, contextManager,
                transactionServices, tokenProvider, firingInjectionPoint, allowStartupEventDispatch);
    }

    /**
     * Creates a refined Event instance for a type literal with additional qualifiers.
     *
     * <p>This method is used for generic type selection where type parameters need to be preserved.
     *
     * <p>Example:
     * <pre>{@code
     * @Inject Event<Object> event;
     *
     * // Fire generic type with qualifier
     * event.select(new TypeLiteral<List<String>>(){}, new ImportantLiteral()).fire(list);
     * }</pre>
     *
     * @param <U> the subtype of T
     * @param subtype the type literal of the subtype
     * @param qualifiers additional qualifiers to filter observers
     * @return new Event instance for the subtype with combined qualifiers
     * @throws IllegalArgumentException if subtype or any qualifier is null
     */
    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        if (subtype == null) {
            throw new IllegalArgumentException("Subtype cannot be null");
        }
        validateEventType(subtype.getType());
        validateAdditionalQualifiers(qualifiers);

        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        for (Annotation qualifier : qualifiers) {
            if (qualifier == null) {
                throw new IllegalArgumentException("Qualifier cannot be null");
            }
            newQualifiers.add(qualifier);
        }

        // Create raw EventImpl and cast - this is safe because EventImpl<U> implements Event<U>
        return (Event<U>) new EventImpl<U>(subtype.getType(), newQualifiers, knowledgeBase, beanResolver, contextManager,
                transactionServices, tokenProvider, firingInjectionPoint, allowStartupEventDispatch);
    }

    private void validateEventType(Type type) {
        if (containsUnresolvableTypeVariable(type)) {
            throw new IllegalArgumentException(
                    "Event type may not contain an unresolvable type variable: " + type.getTypeName());
        }
    }

    private void validateAdditionalQualifiers(Annotation[] qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            return;
        }

        Set<Class<? extends Annotation>> seenNonRepeatableQualifiers = new HashSet<>();
        for (Annotation qualifier : qualifiers) {
            if (qualifier == null) {
                throw new IllegalArgumentException("Qualifier cannot be null");
            }

            Class<? extends Annotation> qualifierType = qualifier.annotationType();
            if (!AnnotationsEnum.hasQualifierAnnotation(qualifierType)) {
                throw new IllegalArgumentException(
                        "Annotation is not a qualifier type: " + qualifierType.getName());
            }

            if (!isRepeatableQualifier(qualifierType) && !seenNonRepeatableQualifiers.add(qualifierType)) {
                throw new IllegalArgumentException(
                        "Duplicate non-repeatable qualifier type passed to select(): " + qualifierType.getName());
            }
        }
    }

    private boolean isRepeatableQualifier(Class<? extends Annotation> qualifierType) {
        return qualifierType.isAnnotationPresent(Repeatable.class);
    }

    private void validateEventObject(Object event) {
        Class<?> runtimeType = event.getClass();
        if (runtimeType.getTypeParameters().length > 0) {
            throw new IllegalArgumentException(
                    "Runtime type of event object contains unresolvable type variable(s): " +
                            runtimeType.getName());
        }

        if (("jakarta.enterprise.event.Startup".equals(runtimeType.getName()) ||
             "jakarta.enterprise.event.Shutdown".equals(runtimeType.getName())) && !allowStartupEventDispatch) {
            throw new IllegalArgumentException(
                    "Application must not manually fire events with payload type " + runtimeType.getName());
        }

        if (isContainerLifecycleEventType(runtimeType)) {
            throw new IllegalArgumentException(
                    "Runtime type of event object is assignable to a container lifecycle event type: " +
                            runtimeType.getName());
        }
    }

    private boolean isContainerLifecycleEventType(Class<?> type) {
        Set<Class<?>> allTypes = new HashSet<>();
        collectTypeClosure(type, allTypes);
        for (Class<?> candidate : allTypes) {
            String name = candidate.getName();
            if (CONTAINER_LIFECYCLE_EVENT_TYPES.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private void collectTypeClosure(Class<?> type, Set<Class<?>> acc) {
        if (type == null || !acc.add(type)) {
            return;
        }
        collectTypeClosure(type.getSuperclass(), acc);
        for (Class<?> itf : type.getInterfaces()) {
            collectTypeClosure(itf, acc);
        }
    }

    private static final Set<String> CONTAINER_LIFECYCLE_EVENT_TYPES = new HashSet<String>(Arrays.asList(
            "jakarta.enterprise.inject.spi.BeforeBeanDiscovery",
            "jakarta.enterprise.inject.spi.AfterTypeDiscovery",
            "jakarta.enterprise.inject.spi.AfterBeanDiscovery",
            "jakarta.enterprise.inject.spi.AfterDeploymentValidation",
            "jakarta.enterprise.inject.spi.BeforeShutdown",
            "jakarta.enterprise.inject.spi.ProcessAnnotatedType",
            "jakarta.enterprise.inject.spi.ProcessSyntheticAnnotatedType",
            "jakarta.enterprise.inject.spi.ProcessInjectionTarget",
            "jakarta.enterprise.inject.spi.ProcessInjectionPoint",
            "jakarta.enterprise.inject.spi.ProcessBeanAttributes",
            "jakarta.enterprise.inject.spi.ProcessBean",
            "jakarta.enterprise.inject.spi.ProcessManagedBean",
            "jakarta.enterprise.inject.spi.ProcessSessionBean",
            "jakarta.enterprise.inject.spi.ProcessSyntheticBean",
            "jakarta.enterprise.inject.spi.ProcessProducer",
            "jakarta.enterprise.inject.spi.ProcessProducerMethod",
            "jakarta.enterprise.inject.spi.ProcessProducerField",
            "jakarta.enterprise.inject.spi.ProcessObserverMethod",
            "jakarta.enterprise.inject.spi.ProcessSyntheticObserverMethod",
            "javax.enterprise.inject.spi.BeforeBeanDiscovery",
            "javax.enterprise.inject.spi.AfterTypeDiscovery",
            "javax.enterprise.inject.spi.AfterBeanDiscovery",
            "javax.enterprise.inject.spi.AfterDeploymentValidation",
            "javax.enterprise.inject.spi.BeforeShutdown",
            "javax.enterprise.inject.spi.ProcessAnnotatedType",
            "javax.enterprise.inject.spi.ProcessInjectionTarget",
            "javax.enterprise.inject.spi.ProcessInjectionPoint",
            "javax.enterprise.inject.spi.ProcessBeanAttributes",
            "javax.enterprise.inject.spi.ProcessBean",
            "javax.enterprise.inject.spi.ProcessManagedBean",
            "javax.enterprise.inject.spi.ProcessSessionBean",
            "javax.enterprise.inject.spi.ProcessProducer",
            "javax.enterprise.inject.spi.ProcessProducerMethod",
            "javax.enterprise.inject.spi.ProcessProducerField",
            "javax.enterprise.inject.spi.ProcessObserverMethod"
    ));

    private boolean containsUnresolvableTypeVariable(Type type) {
        if (type instanceof TypeVariable) {
            return true;
        }

        if (type instanceof ParameterizedType) {
            for (Type argument : ((ParameterizedType) type).getActualTypeArguments()) {
                if (containsUnresolvableTypeVariable(argument)) {
                    return true;
                }
            }
            return false;
        }

        if (type instanceof GenericArrayType) {
            return containsUnresolvableTypeVariable(((GenericArrayType) type).getGenericComponentType());
        }

        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            for (Type lowerBound : wildcardType.getLowerBounds()) {
                if (containsUnresolvableTypeVariable(lowerBound)) {
                    return true;
                }
            }
            for (Type upperBound : wildcardType.getUpperBounds()) {
                if (containsUnresolvableTypeVariable(upperBound)) {
                    return true;
                }
            }
            return false;
        }

        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return containsUnresolvableTypeVariable(clazz.getComponentType());
            }
        }

        return false;
    }

    /**
     * Finds all observer methods that match the given event and synchronization mode.
     *
     * <p>An observer matches if:
     * <ul>
     *   <li>The observer's async flag matches the requested mode</li>
     *   <li>The observer's event type is assignable from the event's type</li>
     *   <li>All observer qualifiers are present in this Event's qualifiers</li>
     * </ul>
     *
     * @param event the event to match
     * @param async true for @ObservesAsync observers, false for @Observes observers
     * @return list of matching observer method metadata
     */
    private List<ObserverMethodInfo> findMatchingObservers(Object event, boolean async) {
        List<ObserverMethodInfo> matching = new ArrayList<>();
        Type eventRuntimeType = event.getClass();

        // Find matching reflection-based observers (from @Observes/@ObservesAsync methods)
        for (ObserverMethodInfo observerInfo : knowledgeBase.getObserverMethodInfos()) {
            // Match async/sync mode
            if (observerInfo.isAsync() != async) {
                continue;
            }

            // Check if observer event type is assignable from the runtime type
            if (!typeChecker.isEventTypeAssignable(observerInfo.getEventType(), eventRuntimeType)) {
                continue;
            }

            // CDI qualifier matching must honor annotation members and @Nonbinding.
            if (!QualifiersHelper.qualifiersMatch(observerInfo.getQualifiers(), qualifiers)) {
                continue;
            }

            matching.add(observerInfo);
        }

        // Also find matching synthetic observers (registered via AfterBeanDiscovery.addObserverMethod())
        for (jakarta.enterprise.inject.spi.ObserverMethod<?> syntheticObserver : knowledgeBase.getSyntheticObserverMethods()) {
            // Match async/sync mode
            if (syntheticObserver.isAsync() != async) {
                continue;
            }

            // Check if observer event type is assignable from the runtime type
            if (!typeChecker.isEventTypeAssignable(syntheticObserver.getObservedType(), eventRuntimeType)) {
                continue;
            }

            // CDI qualifier matching must honor annotation members and @Nonbinding.
            if (!QualifiersHelper.qualifiersMatch(syntheticObserver.getObservedQualifiers(), qualifiers)) {
                continue;
            }

            // Wrap the synthetic ObserverMethod as an ObserverMethodInfo
            // Note: Synthetic observers don't have a Method, so we create a minimal wrapper
            matching.add(createSyntheticObserverInfo(syntheticObserver));
        }

        return matching;
    }

    /**
     * Creates an ObserverMethodInfo wrapper for a synthetic ObserverMethod.
     *
     * <p>Synthetic observers registered via AfterBeanDiscovery.addObserverMethod()
     * don't have an underlying java.lang.reflect.Method, so we create a minimal
     * ObserverMethodInfo that delegates to the ObserverMethod's notify() method.
     */
    private ObserverMethodInfo createSyntheticObserverInfo(jakarta.enterprise.inject.spi.ObserverMethod<?> syntheticObserver) {
        return new ObserverMethodInfo(
            syntheticObserver.getObservedType(),
            syntheticObserver.getObservedQualifiers(),
            syntheticObserver.getReception(),
            syntheticObserver.getTransactionPhase(),
            syntheticObserver.getPriority(),
            syntheticObserver.isAsync(),
            null, // No declaring bean for synthetic observers
            syntheticObserver // Store the synthetic observer for invocation
        );
    }

    /**
     * Invokes a single observer method with the event payload.
     *
     * <p>This method:
     * <ol>
     *   <li>Resolves the observer bean instance from the declaring bean</li>
     *   <li>Resolves any additional injection point parameters</li>
     *   <li>Invokes the observer method with event + parameters</li>
     *   <li>Handles reflection accessibility</li>
     * </ol>
     *
     * <p><b>CDI 4.1 Bean Creation Behavior (Section 10.5.2):</b>
     * <ul>
     *   <li>If the observer has {@link Reception#ALWAYS}, the bean instance is created if it
     *       doesn't already exist in its scope. This is the default behavior.</li>
     *   <li>The {@code beanResolver.resolveDeclaringBeanInstance()} call handles this by:
     *       <ul>
     *         <li>Checking if an instance exists in the appropriate scope context</li>
     *         <li>Creating and storing a new instance if none exists</li>
     *         <li>Storing the instance in the scope (ApplicationScoped, RequestScoped, etc.)</li>
     *       </ul>
     *   </li>
     *   <li>This ensures lazy initialization of observer beans - they're only created when
     *       a matching event is fired.</li>
     * </ul>
     *
     * @param observerInfo the observer method metadata
     * @param event the event payload
     * @throws RuntimeException if observer invocation fails
     */
    @SuppressWarnings("unchecked")
    private void invokeObserver(ObserverMethodInfo observerInfo, Object event) {
        try {
            // Check if this is a synthetic observer (registered via AfterBeanDiscovery.addObserverMethod())
            if (observerInfo.isSynthetic()) {
                // Invoke the synthetic observer's notify() method
                jakarta.enterprise.inject.spi.ObserverMethod syntheticObserver = observerInfo.getSyntheticObserver();
                syntheticObserver.notify(event);
                return;
            }

            // Handle reflection-based observers (from @Observes/@ObservesAsync methods)
            Method method = observerInfo.getObserverMethod();
            Bean<?> declaringBean = observerInfo.getDeclaringBean();

            // Resolve method parameters
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];

                // The @Observes or @ObservesAsync parameter gets the event
                if (AnnotationsEnum.hasObservesAnnotation(param) ||
                    AnnotationsEnum.hasObservesAsyncAnnotation(param)) {
                    args[i] = event;
                } else if (EventMetadata.class.equals(param.getType())) {
                    args[i] = new EventMetadataImpl(qualifiers, firingInjectionPoint, event.getClass());
                } else {
                    // Other parameters are injection points - resolve them
                    Type paramType = param.getParameterizedType();
                    Annotation[] paramAnnotations = param.getAnnotations();
                    args[i] = resolveObserverParameterWithContext(param, paramType, paramAnnotations, declaringBean);
                }
            }

            // For static observer methods, invocation must not require declaring bean instance creation.
            Object beanInstance = null;
            if (!Modifier.isStatic(method.getModifiers())) {
                // Get the bean instance that declares this observer method
                // For Reception.ALWAYS (default): This will create the bean if it doesn't exist
                // For Reception.IF_EXISTS: This is only called after checking bean existence
                if (declaringBean != null) {
                    beanInstance = beanResolver.resolveDeclaringBeanInstance(declaringBean.getBeanClass());
                } else {
                    // If no bean metadata, try to resolve by method's declaring class
                    beanInstance = beanResolver.resolveDeclaringBeanInstance(method.getDeclaringClass());
                }
            }

            try {
                // Invoke the observer method
                if (shouldUseRuntimeMethodDispatch(declaringBean, method, beanInstance)) {
                    invokeOnRuntimeMethod(beanInstance, method, args);
                } else {
                    method.setAccessible(true);
                    method.invoke(beanInstance, args);
                }
            } finally {
                destroyDependentInvocationParameters(parameters, args);
                destroyDependentObserverReceiver(beanInstance, method);
            }

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new ObserverException(cause);
        } catch (ObserverException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ObserverException(e);
        }
    }

    private void invokeObserverWithContextCheck(ObserverMethodInfo observerInfo, Object event) {
        ContextActivation activation = ensureObserverContext(observerInfo);
        if (activation.isSkip()) {
            return;
        }
        try {
            invokeObserver(observerInfo, event);
        } finally {
            activation.close();
        }
    }

    private void destroyDependentInvocationParameters(Parameter[] parameters, Object[] args) throws Exception {
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            if (AnnotationsEnum.hasObservesAnnotation(parameter) ||
                AnnotationsEnum.hasObservesAsyncAnnotation(parameter)) {
                continue;
            }
            if (!isDependentParameter(parameter)) {
                continue;
            }
            LifecycleMethodHelper.invokeLifecycleMethod(arg, PreDestroy.class);
        }
    }

    private Object invokeOnRuntimeMethod(Object targetInstance, Method method, Object[] args) throws Exception {
        Method invocable = method;
        if (targetInstance != null && !Modifier.isStatic(method.getModifiers())) {
            Method resolved = findMethodInHierarchy(targetInstance.getClass(), method.getName(), method.getParameterTypes());
            if (resolved != null) {
                invocable = resolved;
            }
        }
        invocable.setAccessible(true);
        return invocable.invoke(targetInstance, args);
    }

    private Method findMethodInHierarchy(Class<?> type, String methodName, Class<?>[] parameterTypes) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private boolean shouldUseRuntimeMethodDispatch(Bean<?> declaringBean, Method method, Object beanInstance) {
        if (beanInstance == null || method == null || Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        if (!(declaringBean instanceof BeanImpl<?>)) {
            return false;
        }
        return ((BeanImpl<?>) declaringBean).hasInterceptors();
    }

    private void destroyDependentObserverReceiver(Object beanInstance, Method method) throws Exception {
        if (beanInstance == null || method == null || Modifier.isStatic(method.getModifiers())) {
            return;
        }

        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(Dependent.class)) {
            LifecycleMethodHelper.invokeLifecycleMethod(beanInstance, PreDestroy.class);
            return;
        }
        for (Annotation annotation : declaringClass.getAnnotations()) {
            if ("javax.enterprise.context.Dependent".equals(annotation.annotationType().getName())) {
                LifecycleMethodHelper.invokeLifecycleMethod(beanInstance, PreDestroy.class);
                return;
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object resolveObserverParameterWithContext(Parameter parameter,
                                                       Type parameterType,
                                                       Annotation[] parameterAnnotations,
                                                       Bean<?> declaringBean) {
        InjectionPoint injectionPoint = new InjectionPointImpl(parameter, (Bean) declaringBean);
        beanResolver.setCurrentInjectionPoint(injectionPoint);
        try {
            return beanResolver.resolve(parameterType, parameterAnnotations);
        } finally {
            beanResolver.clearCurrentInjectionPoint();
        }
    }

    private boolean isDependentParameter(Parameter parameter) {
        Class<?> parameterType = parameter.getType();
        if (parameterType == null) {
            return false;
        }
        if (parameterType.isAnnotationPresent(Dependent.class)) {
            return true;
        }
        for (Annotation annotation : parameterType.getAnnotations()) {
            if ("javax.enterprise.context.Dependent".equals(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private void invokeObserverList(List<ObserverMethodInfo> observers, Object event) {
        for (ObserverMethodInfo observer : observers) {
            ContextActivation activation = ensureObserverContext(observer);
            if (activation.isSkip()) {
                continue;
            }
            try {
                invokeObserver(observer, event);
            } catch (Exception e) {
                // Per spec, observer exceptions must not affect transaction outcome
                System.err.println("Transactional observer error (" + observer + "): " + e.getMessage());
            } finally {
                activation.close();
            }
        }
    }

    /**
     * Guards transactional observer callbacks by ensuring required normal scopes are active.
     * If the declaring bean uses an inactive normal scope (e.g., @RequestScoped after request end),
     * the invocation is skipped and a warning is logged once per scope type.
     */
    private ContextActivation ensureObserverContext(ObserverMethodInfo observer) {
        Bean<?> declaringBean = observer.getDeclaringBean();
        if (declaringBean == null) {
            return ContextActivation.NOOP; // Synthetic or unknown bean; proceed
        }
        Class<? extends Annotation> scope = declaringBean.getScope();
        // Dependent/Application are always safe
        if (scope == jakarta.enterprise.context.Dependent.class ||
            scope == jakarta.enterprise.context.ApplicationScoped.class) {
            return ContextActivation.NOOP;
        }

        try {
            ScopeContext ctx = contextManager.getContext(scope);
            if (ctx.isActive()) {
                return ContextActivation.NOOP;
            }

            // Per CDI 4.1 (9.5), observers are not called when declaring scope context is inactive.
            warnOnce(scope, declaringBean);
            return ContextActivation.SKIP;
        } catch (IllegalArgumentException e) {
            // Unknown scope; proceed to avoid hiding functionality
            return ContextActivation.NOOP;
        }
    }

    private void warnOnce(Class<? extends Annotation> scope, Bean<?> bean) {
        AtomicBoolean flag = INACTIVE_SCOPE_WARNED.computeIfAbsent(scope, k -> new AtomicBoolean(false));
        if (flag.compareAndSet(false, true)) {
            System.out.println("[Event] Skipping observer for @" +
                scope.getSimpleName() + " (" + bean.getBeanClass().getName() + ") because scope inactive");
        }
    }

    private ContextActivation tryProviderRestore(Class<? extends Annotation> scope, ContextSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        if (scope == ConversationScoped.class && snapshot.conversationId != null) {
            ScopeContext ctx = contextManager.getContext(ConversationScoped.class);
            if (ctx instanceof ConversationScopedContext) {
                ((ConversationScopedContext) ctx).beginConversation(snapshot.conversationId);
                return new ContextActivation((ConversationScopedContext) ctx, snapshot.conversationId, true);
            }
        }

        if (scope == SessionScoped.class && snapshot.sessionId != null) {
            ScopeContext ctx = contextManager.getContext(SessionScoped.class);
            if (ctx instanceof SessionScopedContext && snapshot.sessionData != null) {
                ((SessionScopedContext) ctx).activateSession(snapshot.sessionId, snapshot.sessionData);
                return new ContextActivation((SessionScopedContext) ctx, snapshot.sessionId, true);
            }
        }

        return null;
    }

    /**
     * Tracks temporary context activations (currently RequestScoped only).
     */
    private static class ContextActivation implements AutoCloseable {
        static final ContextActivation NOOP = new ContextActivation();
        static final ContextActivation SKIP = new ContextActivation(true);

        private final boolean skip;
        private final RequestScopedContext requestCtx;
        private final ConversationScopedContext conversationCtx;
        private final SessionScopedContext sessionCtx;
        private final String conversationId;
        private final String sessionId;
        private final boolean deactivateRequest;
        private final boolean endConversation;
        private final boolean deactivateSession;

        private ContextActivation() {
            this.skip = false;
            this.requestCtx = null;
            this.conversationCtx = null;
            this.sessionCtx = null;
            this.conversationId = null;
            this.sessionId = null;
            this.deactivateRequest = false;
            this.endConversation = false;
            this.deactivateSession = false;
        }

        private ContextActivation(boolean skip) {
            this.skip = skip;
            this.requestCtx = null;
            this.conversationCtx = null;
            this.sessionCtx = null;
            this.conversationId = null;
            this.sessionId = null;
            this.deactivateRequest = false;
            this.endConversation = false;
            this.deactivateSession = false;
        }

        private ContextActivation(RequestScopedContext requestCtx) {
            this.skip = false;
            this.requestCtx = requestCtx;
            this.conversationCtx = null;
            this.sessionCtx = null;
            this.conversationId = null;
            this.sessionId = null;
            this.deactivateRequest = true;
            this.endConversation = false;
            this.deactivateSession = false;
        }

        private ContextActivation(ConversationScopedContext conversationCtx, String conversationId, boolean endConversation) {
            this.skip = false;
            this.requestCtx = null;
            this.conversationCtx = conversationCtx;
            this.sessionCtx = null;
            this.conversationId = conversationId;
            this.sessionId = null;
            this.deactivateRequest = false;
            this.endConversation = endConversation;
            this.deactivateSession = false;
        }

        private ContextActivation(SessionScopedContext sessionCtx, String sessionId, boolean deactivateSession) {
            this.skip = false;
            this.requestCtx = null;
            this.conversationCtx = null;
            this.sessionCtx = sessionCtx;
            this.conversationId = null;
            this.sessionId = sessionId;
            this.deactivateRequest = false;
            this.endConversation = false;
            this.deactivateSession = deactivateSession;
        }

        boolean isSkip() {
            return skip;
        }

        @Override
        public void close() {
            if (deactivateRequest && requestCtx != null) {
                requestCtx.deactivateRequest();
            }
            if (endConversation && conversationCtx != null && conversationId != null) {
                conversationCtx.endConversation(conversationId);
            }
            if (deactivateSession && sessionCtx != null && sessionId != null) {
                sessionCtx.deactivateSession();
            }
        }
    }

    /**
     * Snapshot of contextual identifiers used for provider-assisted restoration.
     */
    public static class ContextSnapshot {
        public final String requestId; // unused placeholder
        public final String conversationId;
        public final String sessionId;
        public final byte[] sessionData;

        public ContextSnapshot(String requestId, String conversationId, String sessionId, byte[] sessionData) {
            this.requestId = requestId;
            this.conversationId = conversationId;
            this.sessionId = sessionId;
            this.sessionData = sessionData;
        }
    }

    public interface ContextTokenProvider {
        ContextSnapshot capture();
    }

    private static class NoopContextTokenProvider implements ContextTokenProvider {
        @Override
        public ContextSnapshot capture() {
            return null;
        }
    }

    private static final class EventMetadataImpl implements EventMetadata {
        private final Set<Annotation> qualifiers;
        private final InjectionPoint injectionPoint;
        private final Type type;

        private EventMetadataImpl(Set<Annotation> qualifiers, InjectionPoint injectionPoint, Type type) {
            this.qualifiers = Collections.unmodifiableSet(new HashSet<>(qualifiers));
            this.injectionPoint = injectionPoint;
            this.type = type;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        @Override
        public InjectionPoint getInjectionPoint() {
            return injectionPoint;
        }

        @Override
        public Type getType() {
            return type;
        }
    }

    @Override
    public String toString() {
        return "EventImpl{" +
                "eventType=" + eventType.getTypeName() +
                ", qualifiers=" + qualifiers +
                '}';
    }
}
