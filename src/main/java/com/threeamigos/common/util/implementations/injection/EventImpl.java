package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.contexts.ContextManager;
import com.threeamigos.common.util.implementations.injection.contexts.ScopeContext;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.knowledgebase.ObserverMethodInfo;
import com.threeamigos.common.util.implementations.injection.tx.TransactionServices;
import com.threeamigos.common.util.implementations.injection.tx.TransactionSynchronizationCallbacks;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

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
        this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers cannot be null");
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.beanResolver = Objects.requireNonNull(beanResolver, "beanResolver cannot be null");
        this.contextManager = Objects.requireNonNull(contextManager, "contextManager cannot be null");
        this.typeChecker = new TypeChecker();
        this.transactionServices = Objects.requireNonNull(transactionServices, "transactionServices cannot be null");
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
                invokeObserver(observerInfo, event);
            } else if (!txActive) {
                // Spec: if no transaction is active, transactional observers fire immediately
                invokeObserver(observerInfo, event);
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
                        invokeObserver(observerInfo, event);
                }
            }
        }

        if (txActive && (!beforeCompletion.isEmpty() || !afterSuccess.isEmpty() ||
            !afterFailure.isEmpty() || !afterCompletion.isEmpty())) {
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
        return fireAsync(event, NotificationOptions.ofExecutor(ForkJoinPool.commonPool()));
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
            executor = ForkJoinPool.commonPool();
        }

        // Create an async task
        CompletableFuture<U> future = CompletableFuture.supplyAsync(() -> {
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

                invokeObserver(observerInfo, event);
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
        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        for (Annotation qualifier : qualifiers) {
            if (qualifier == null) {
                throw new IllegalArgumentException("Qualifier cannot be null");
            }
            newQualifiers.add(qualifier);
        }
        return new EventImpl<>(eventType, newQualifiers, knowledgeBase, beanResolver, contextManager, transactionServices);
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

        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        for (Annotation qualifier : qualifiers) {
            if (qualifier == null) {
                throw new IllegalArgumentException("Qualifier cannot be null");
            }
            newQualifiers.add(qualifier);
        }

        // Create raw EventImpl and cast - this is safe because EventImpl<U> implements Event<U>
        return (Event<U>) new EventImpl<U>(subtype, newQualifiers, knowledgeBase, beanResolver, contextManager, transactionServices);
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

        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        for (Annotation qualifier : qualifiers) {
            if (qualifier == null) {
                throw new IllegalArgumentException("Qualifier cannot be null");
            }
            newQualifiers.add(qualifier);
        }

        // Create raw EventImpl and cast - this is safe because EventImpl<U> implements Event<U>
        return (Event<U>) new EventImpl<U>(subtype.getType(), newQualifiers, knowledgeBase, beanResolver, contextManager, transactionServices);
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
            if (!typeChecker.isAssignable(observerInfo.getEventType(), eventRuntimeType)) {
                continue;
            }

            // Check if all observer qualifiers are present in event qualifiers
            if (!qualifiers.containsAll(observerInfo.getQualifiers())) {
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
            if (!typeChecker.isAssignable(syntheticObserver.getObservedType(), eventRuntimeType)) {
                continue;
            }

            // Check if all observer qualifiers are present in event qualifiers
            if (!qualifiers.containsAll(syntheticObserver.getObservedQualifiers())) {
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

            // Get the bean instance that declares this observer method
            // For Reception.ALWAYS (default): This will create the bean if it doesn't exist
            // For Reception.IF_EXISTS: This is only called after checking bean existence
            Object beanInstance = null;
            if (declaringBean != null) {
                beanInstance = beanResolver.resolveDeclaringBeanInstance(declaringBean.getBeanClass());
            } else {
                // If no bean metadata, try to resolve by method's declaring class
                beanInstance = beanResolver.resolveDeclaringBeanInstance(method.getDeclaringClass());
            }

            // Resolve method parameters
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];

                // The @Observes or @ObservesAsync parameter gets the event
                if (AnnotationsEnum.hasObservesAnnotation(param) ||
                    AnnotationsEnum.hasObservesAsyncAnnotation(param)) {
                    args[i] = event;
                } else {
                    // Other parameters are injection points - resolve them
                    Type paramType = param.getParameterizedType();
                    Annotation[] paramAnnotations = param.getAnnotations();
                    args[i] = beanResolver.resolve(paramType, paramAnnotations);
                }
            }

            // Invoke the observer method
            method.setAccessible(true);
            method.invoke(beanInstance, args);

        } catch (Exception e) {
            String observerName = observerInfo.isSynthetic() ?
                "synthetic observer for " + observerInfo.getEventType() :
                observerInfo.getObserverMethod().getName() + " in " + observerInfo.getObserverMethod().getDeclaringClass().getName();

            throw new RuntimeException(
                "Failed to invoke observer: " + observerName,
                e
            );
        }
    }

    private void invokeObserverList(List<ObserverMethodInfo> observers, Object event) {
        for (ObserverMethodInfo observer : observers) {
            try {
                invokeObserver(observer, event);
            } catch (Exception e) {
                // Per spec, observer exceptions must not affect transaction outcome
                System.err.println("Transactional observer error (" + observer + "): " + e.getMessage());
            }
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
