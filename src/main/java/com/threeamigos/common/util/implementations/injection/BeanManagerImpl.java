package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.contexts.ContextManager;
import com.threeamigos.common.util.implementations.injection.knowledgebase.DecoratorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.InterceptorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.knowledgebase.ObserverMethodInfo;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;

/**
 * Implementation of the CDI 4.1 BeanManager interface.
 *
 * <p>This class provides access to the CDI container's core functionality including:
 * <ul>
 *   <li>Bean discovery and resolution</li>
 *   <li>Dependency injection</li>
 *   <li>Event firing and observer resolution</li>
 *   <li>Context management</li>
 *   <li>Interceptor and decorator resolution</li>
 * </ul>
 *
 * <p>The implementation delegates to existing components:
 * <ul>
 *   <li>{@link KnowledgeBase} - stores discovered beans and metadata</li>
 *   <li>{@link BeanResolver} - resolves dependencies and creates instances</li>
 *   <li>{@link ContextManager} - manages scoped contexts</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Create BeanManager
 * KnowledgeBase kb = new KnowledgeBase();
 * ContextManager cm = new ContextManager();
 * BeanManager bm = new BeanManagerImpl(kb, cm);
 *
 * // Find beans by type
 * Set<Bean<?>> beans = bm.getBeans(MyService.class);
 * Bean<?> bean = bm.resolve(beans);
 *
 * // Get bean instance
 * CreationalContext<?> ctx = bm.createCreationalContext(bean);
 * MyService service = (MyService) bm.getReference(bean, MyService.class, ctx);
 *
 * // Fire events
 * Event<Object> event = bm.getEvent();
 * event.select(MyEvent.class).fire(new MyEvent());
 * }</pre>
 *
 * @author Stefano Reksten
 * @see jakarta.enterprise.inject.spi.BeanManager
 */
public class BeanManagerImpl implements BeanManager {

    private final KnowledgeBase knowledgeBase;
    private final BeanResolver beanResolver;
    private final ContextManager contextManager;
    private final TypeChecker typeChecker;

    /**
     * Creates a new BeanManager implementation.
     *
     * @param knowledgeBase the knowledge base containing all discovered beans
     * @param contextManager the context manager for scope handling
     */
    public BeanManagerImpl(KnowledgeBase knowledgeBase, ContextManager contextManager) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.contextManager = Objects.requireNonNull(contextManager, "contextManager cannot be null");
        this.beanResolver = new BeanResolver(knowledgeBase, contextManager);
        this.typeChecker = new TypeChecker();
    }

    // ==================== BeanContainer Methods ====================

    /**
     * Obtains a contextual reference for a bean.
     *
     * <p>For normal scopes (@ApplicationScoped, @RequestScoped, etc.), this returns a client proxy
     * that delegates to the current contextual instance. For pseudo-scopes (@Dependent), this
     * returns the actual bean instance.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Set<Bean<?>> beans = beanManager.getBeans(UserService.class);
     * Bean<?> bean = beanManager.resolve(beans);
     * CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
     *
     * // Returns a proxy for @ApplicationScoped beans
     * UserService service = (UserService) beanManager.getReference(bean, UserService.class, ctx);
     * }</pre>
     *
     * @param bean the bean to obtain a reference for
     * @param beanType the type of the bean (must be in bean.getTypes())
     * @param ctx the creational context
     * @return a contextual reference (proxy for normal scopes, instance for pseudo-scopes)
     * @throws IllegalArgumentException if bean or beanType is null
     */
    @Override
    public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> ctx) {
        if (bean == null) {
            throw new IllegalArgumentException("bean cannot be null");
        }
        if (beanType == null) {
            throw new IllegalArgumentException("beanType cannot be null");
        }

        // For normal scopes, return a client proxy
        if (contextManager.isNormalScope(bean.getScope())) {
            return contextManager.createClientProxy(bean);
        }

        // For pseudo-scopes (e.g., @Dependent), get from context
        Context context = getContext(bean.getScope());
        @SuppressWarnings("unchecked")
        Object instance = context.get((Contextual) bean, ctx);
        return instance;
    }

    /**
     * Creates a new creational context for managing dependent objects.
     *
     * <p>The creational context tracks dependent objects created during bean instantiation
     * so they can be destroyed when the parent bean is destroyed.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Bean<MyService> bean = ...;
     * CreationalContext<MyService> ctx = beanManager.createCreationalContext(bean);
     * MyService instance = bean.create(ctx);
     *
     * // Later, destroy the bean and its dependents
     * bean.destroy(instance, ctx);
     * ctx.release();
     * }</pre>
     *
     * @param contextual the contextual type (usually a Bean)
     * @param <T> the type of the contextual instance
     * @return a new creational context
     */
    @Override
    public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual) {
        return new CreationalContextImpl<>();
    }

    /**
     * Returns all beans matching the given type and qualifiers.
     *
     * <p>This method performs type-safe resolution by checking:
     * <ul>
     *   <li>Type assignability (using CDI type rules)</li>
     *   <li>Qualifier matching (all required qualifiers must be present)</li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Find all PaymentProcessor beans
     * Set<Bean<?>> beans = beanManager.getBeans(PaymentProcessor.class);
     *
     * // Find PaymentProcessor with @CreditCard qualifier
     * Set<Bean<?>> creditCardProcessors = beanManager.getBeans(
     *     PaymentProcessor.class,
     *     new CreditCardLiteral()
     * );
     *
     * // Find by name
     * Set<Bean<?>> namedBeans = beanManager.getBeans(
     *     PaymentProcessor.class,
     *     new NamedLiteral("stripe")
     * );
     * }</pre>
     *
     * @param beanType the required type
     * @param qualifiers the required qualifiers (empty = @Default)
     * @return set of matching beans (may be empty, never null)
     * @throws IllegalArgumentException if beanType is null
     */
    @Override
    public Set<Bean<?>> getBeans(Type beanType, Annotation... qualifiers) {
        if (beanType == null) {
            throw new IllegalArgumentException("beanType cannot be null");
        }

        Set<Annotation> requiredQualifiers = extractQualifiers(qualifiers);
        Set<Bean<?>> matchingBeans = new HashSet<>();

        for (Bean<?> bean : knowledgeBase.getValidBeans()) {
            // Check type compatibility
            boolean typeMatches = false;
            for (Type type : bean.getTypes()) {
                if (typeChecker.isAssignable(beanType, type)) {
                    typeMatches = true;
                    break;
                }
            }

            if (!typeMatches) {
                continue;
            }

            // Check qualifier match
            if (qualifiersMatch(requiredQualifiers, bean.getQualifiers())) {
                matchingBeans.add(bean);
            }
        }

        return matchingBeans;
    }

    /**
     * Returns all beans with the given EL name.
     *
     * <p>Beans can be named using {@code @Named} annotation:
     * <pre>{@code
     * @Named("userService")
     * @ApplicationScoped
     * public class UserService { ... }
     * }</pre>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Find bean by name
     * Set<Bean<?>> beans = beanManager.getBeans("userService");
     * if (!beans.isEmpty()) {
     *     Bean<?> bean = beanManager.resolve(beans);
     *     // Use bean...
     * }
     * }</pre>
     *
     * @param name the EL name
     * @return set of beans with the given name (may be empty, never null)
     * @throws IllegalArgumentException if name is null or empty
     */
    @Override
    public Set<Bean<?>> getBeans(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }

        Set<Bean<?>> namedBeans = new HashSet<>();

        for (Bean<?> bean : knowledgeBase.getValidBeans()) {
            if (name.equals(bean.getName())) {
                namedBeans.add(bean);
            }
        }

        return namedBeans;
    }

    /**
     * Resolves ambiguous bean dependencies using CDI 4.1 resolution rules.
     *
     * <p>Resolution algorithm:
     * <ol>
     *   <li>If only one bean, return it</li>
     *   <li>If multiple alternatives exist, return highest priority alternative</li>
     *   <li>If multiple alternatives with same priority, return null (ambiguous)</li>
     *   <li>If multiple non-alternatives exist, return null (ambiguous)</li>
     * </ol>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Multiple implementations exist
     * Set<Bean<?>> beans = beanManager.getBeans(PaymentProcessor.class);
     *
     * // Resolve using priority
     * Bean<?> bean = beanManager.resolve(beans);
     * if (bean == null) {
     *     throw new AmbiguousResolutionException("Cannot resolve PaymentProcessor");
     * }
     *
     * // Alternative beans with @Priority
     * @Alternative
     * @Priority(100)
     * public class StripePaymentProcessor implements PaymentProcessor { ... }
     *
     * @Alternative
     * @Priority(200)  // Higher priority wins
     * public class PayPalPaymentProcessor implements PaymentProcessor { ... }
     * }</pre>
     *
     * @param beans the set of beans to resolve
     * @param <X> the bean type
     * @return the resolved bean, or null if ambiguous or empty
     */
    @Override
    public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans) {
        if (beans == null || beans.isEmpty()) {
            return null;
        }

        if (beans.size() == 1) {
            return beans.iterator().next();
        }

        // Apply CDI 4.1 resolution rules:
        // 1. Filter enabled beans (non-alternatives or enabled alternatives)
        // 2. Select highest priority alternative if any exist
        // 3. If multiple alternatives with same priority, return null (ambiguous)

        List<Bean<? extends X>> alternatives = new ArrayList<>();
        List<Bean<? extends X>> nonAlternatives = new ArrayList<>();

        for (Bean<? extends X> bean : beans) {
            if (bean.isAlternative()) {
                alternatives.add(bean);
            } else {
                nonAlternatives.add(bean);
            }
        }

        // If alternatives exist, use highest priority alternative
        if (!alternatives.isEmpty()) {
            // Sort by priority (higher priority value = higher precedence)
            alternatives.sort((b1, b2) -> {
                int p1 = getPriority(b1);
                int p2 = getPriority(b2);
                return Integer.compare(p2, p1); // Descending order
            });

            Bean<? extends X> highest = alternatives.get(0);

            // Check for ambiguity (multiple alternatives with same priority)
            if (alternatives.size() > 1) {
                int highestPriority = getPriority(highest);
                if (getPriority(alternatives.get(1)) == highestPriority) {
                    return null; // Ambiguous
                }
            }

            return highest;
        }

        // No alternatives, check non-alternatives
        if (nonAlternatives.size() == 1) {
            return nonAlternatives.get(0);
        }

        // Multiple non-alternatives = ambiguous
        return null;
    }

    /**
     * Finds all synchronous observer methods matching the event and qualifiers.
     *
     * <p>Observer methods are matched by:
     * <ul>
     *   <li>Event type assignability</li>
     *   <li>Qualifier matching (all observer qualifiers must be in event qualifiers)</li>
     *   <li>Synchronous only (@Observes, not @ObservesAsync)</li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Observer method
     * public class OrderService {
     *     void onOrderCreated(@Observes @Created Order order) {
     *         // Process order...
     *     }
     * }
     *
     * // Find matching observers
     * Order order = new Order();
     * Set<ObserverMethod<? super Order>> observers =
     *     beanManager.resolveObserverMethods(order, new CreatedLiteral());
     *
     * // Notify observers
     * for (ObserverMethod<? super Order> observer : observers) {
     *     observer.notify(order);
     * }
     * }</pre>
     *
     * @param event the event object
     * @param qualifiers the event qualifiers
     * @param <T> the event type
     * @return set of matching observer methods (may be empty, never null)
     * @throws IllegalArgumentException if event is null
     */
    @Override
    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, Annotation... qualifiers) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }

        Type eventType = event.getClass();
        Set<Annotation> eventQualifiers = new HashSet<>(Arrays.asList(qualifiers));

        Set<ObserverMethod<? super T>> observerMethods = new HashSet<>();

        for (ObserverMethodInfo observerInfo : knowledgeBase.getObserverMethodInfos()) {
            // Check if the observer is synchronous (@Observes, not @ObservesAsync)
            if (observerInfo.isAsync()) {
                continue;
            }

            // Check type compatibility
            if (!typeChecker.isAssignable(eventType, observerInfo.getEventType())) {
                continue;
            }

            // Check qualifier match
            if (observerQualifiersMatch(eventQualifiers, observerInfo.getQualifiers())) {
                // Create ObserverMethod wrapper
                observerMethods.add(createObserverMethod(observerInfo));
            }
        }

        return observerMethods;
    }

    /**
     * Resolves interceptors for the given interception type and bindings.
     *
     * <p>Interceptors are matched by:
     * <ul>
     *   <li>Interception type support (AROUND_INVOKE, POST_CONSTRUCT, etc.)</li>
     *   <li>Interceptor binding annotations</li>
     *   <li>Sorted by priority (lower = earlier execution)</li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Interceptor binding
     * @InterceptorBinding
     * @Target({TYPE, METHOD})
     * @Retention(RUNTIME)
     * public @interface Transactional { }
     *
     * // Interceptor
     * @Transactional
     * @Interceptor
     * @Priority(100)
     * public class TransactionInterceptor {
     *     @AroundInvoke
     *     public Object intercept(InvocationContext ctx) throws Exception {
     *         // Begin transaction...
     *         return ctx.proceed();
     *     }
     * }
     *
     * // Resolve interceptors
     * List<Interceptor<?>> interceptors = beanManager.resolveInterceptors(
     *     InterceptionType.AROUND_INVOKE,
     *     new TransactionalLiteral()
     * );
     * }</pre>
     *
     * @param type the interception type
     * @param interceptorBindings the required interceptor bindings
     * @return list of interceptors sorted by priority (can be empty, never null)
     * @throws IllegalArgumentException if the type is null
     */
    @Override
    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        // Convert varargs to Set for query
        Set<Annotation> requiredBindings = interceptorBindings.length > 0
                ? new HashSet<>(Arrays.asList(interceptorBindings))
                : Collections.emptySet();

        // Use KnowledgeBase query method - already filters by type and bindings, and sorts by priority
        List<InterceptorInfo> matchingInfos = requiredBindings.isEmpty()
                ? knowledgeBase.getInterceptorsByType(type)
                : knowledgeBase.getInterceptorsByBindingsAndType(type, requiredBindings);

        // Convert InterceptorInfo to Interceptor<?> beans
        return matchingInfos.stream()
                .map(this::createInterceptor)
                .collect(Collectors.toList());
    }

    /**
     * Checks if an annotation is a scope annotation.
     *
     * <p>Recognizes both {@code @Scope} (pseudo-scopes) and {@code @NormalScope} annotations.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * beanManager.isScope(ApplicationScoped.class);  // true
     * beanManager.isScope(Dependent.class);         // true (pseudo-scope)
     * beanManager.isScope(RequestScoped.class);     // true
     * beanManager.isScope(Inject.class);            // false
     * }</pre>
     *
     * @param annotationType the annotation type to check
     * @return true if it's a scope annotation
     */
    @Override
    public boolean isScope(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return false;
        }
        return hasScopeAnnotation(annotationType) || hasNormalScopeAnnotation(annotationType);
    }

    /**
     * Checks if an annotation is a normal scope.
     *
     * <p>Normal scopes require client proxies:
     * <ul>
     *   <li>@ApplicationScoped</li>
     *   <li>@RequestScoped</li>
     *   <li>@SessionScoped</li>
     *   <li>@ConversationScoped</li>
     * </ul>
     *
     * <p>Pseudo-scopes like @Dependent do not require proxies.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * beanManager.isNormalScope(ApplicationScoped.class);  // true
     * beanManager.isNormalScope(Dependent.class);         // false (pseudo-scope)
     * }</pre>
     *
     * @param annotationType the annotation type to check
     * @return true if it's a normal scope requiring proxies
     */
    @Override
    public boolean isNormalScope(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return false;
        }
        return contextManager.isNormalScope(annotationType);
    }

    /**
     * Checks if an annotation is a qualifier.
     *
     * <p>Qualifiers are meta-annotated with {@code @Qualifier}.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @Qualifier
     * @Retention(RUNTIME)
     * @Target({FIELD, METHOD, PARAMETER, TYPE})
     * public @interface CreditCard { }
     *
     * beanManager.isQualifier(CreditCard.class);  // true
     * beanManager.isQualifier(Default.class);     // true
     * beanManager.isQualifier(Inject.class);      // false
     * }</pre>
     *
     * @param annotationType the annotation type to check
     * @return true if it's a qualifier
     */
    @Override
    public boolean isQualifier(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return false;
        }
        return hasQualifierAnnotation(annotationType);
    }

    /**
     * Checks if an annotation is a stereotype.
     *
     * <p>Stereotypes bundle multiple annotations together and are meta-annotated
     * with {@code @Stereotype}.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @Stereotype
     * @ApplicationScoped
     * @Transactional
     * @Retention(RUNTIME)
     * @Target(TYPE)
     * public @interface Service { }
     *
     * beanManager.isStereotype(Service.class);  // true
     * }</pre>
     *
     * @param annotationType the annotation type to check
     * @return true if it's a stereotype
     */
    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return false;
        }
        // Check both @Stereotype annotation and programmatically registered stereotypes
        return hasStereotypeAnnotation(annotationType) ||
               knowledgeBase.isRegisteredStereotype(annotationType);
    }

    /**
     * Checks if an annotation is an interceptor binding.
     *
     * <p>Interceptor bindings are meta-annotated with {@code @InterceptorBinding}.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @InterceptorBinding
     * @Retention(RUNTIME)
     * @Target({TYPE, METHOD})
     * public @interface Transactional { }
     *
     * beanManager.isInterceptorBinding(Transactional.class);  // true
     * }</pre>
     *
     * @param annotationType the annotation type to check
     * @return true if it's an interceptor binding
     */
    @Override
    public boolean isInterceptorBinding(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return false;
        }
        return annotationType.isAnnotationPresent(jakarta.interceptor.InterceptorBinding.class);
    }

    /**
     * Returns the context for the given scope type.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Get application scope context
     * Context appContext = beanManager.getContext(ApplicationScoped.class);
     * if (appContext.isActive()) {
     *     // Context is active
     * }
     *
     * // Get request scope context
     * try {
     *     Context reqContext = beanManager.getContext(RequestScoped.class);
     * } catch (ContextNotActiveException e) {
     *     // Request scope not active
     * }
     * }</pre>
     *
     * @param scopeType the scope annotation class
     * @return the context for the scope
     * @throws IllegalArgumentException if scopeType is null
     * @throws jakarta.enterprise.context.ContextNotActiveException if context not active
     */
    @Override
    public Context getContext(Class<? extends Annotation> scopeType) {
        if (scopeType == null) {
            throw new IllegalArgumentException("scopeType cannot be null");
        }

        // Delegate to ContextManager - wrap ScopeContext as Context
        try {
            com.threeamigos.common.util.implementations.injection.contexts.ScopeContext scopeContext =
                contextManager.getContext(scopeType);
            return new ScopeContextAdapter(scopeContext, scopeType);
        } catch (IllegalArgumentException e) {
            throw new jakarta.enterprise.context.ContextNotActiveException(
                "Context not active for scope: " + scopeType.getName());
        }
    }

    /**
     * Returns all contexts for the given scope type.
     *
     * <p>CDI allows multiple contexts per scope (e.g., for propagation).
     * This implementation returns a singleton collection.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Collection<Context> contexts = beanManager.getContexts(RequestScoped.class);
     * for (Context ctx : contexts) {
     *     if (ctx.isActive()) {
     *         // Use context...
     *     }
     * }
     * }</pre>
     *
     * @param scopeType the scope annotation class
     * @return collection of contexts (may be empty, never null)
     * @throws IllegalArgumentException if scopeType is null
     */
    @Override
    public Collection<Context> getContexts(Class<? extends Annotation> scopeType) {
        if (scopeType == null) {
            throw new IllegalArgumentException("scopeType cannot be null");
        }

        // For now, return single context (can be extended for custom scopes)
        try {
            Context context = getContext(scopeType);
            return Collections.singleton(context);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns an Event object for firing events.
     *
     * <p>The returned Event has type Object with @Any qualifier,
     * allowing any event to be fired after narrowing with select().
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Get event object
     * Event<Object> event = beanManager.getEvent();
     *
     * // Fire specific event type
     * event.select(UserCreatedEvent.class)
     *      .fire(new UserCreatedEvent(user));
     *
     * // Fire with qualifier
     * event.select(OrderEvent.class, new CompletedLiteral())
     *      .fire(new OrderEvent(order));
     * }</pre>
     *
     * @return an Event<Object> instance for firing events
     */
    @Override
    public Event<Object> getEvent() {
        // Create an Event<Object> with @Any qualifier
        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(new com.threeamigos.common.util.implementations.injection.literals.AnyLiteral());

        return new EventImpl<>(Object.class, qualifiers, knowledgeBase, beanResolver, contextManager);
    }

    /**
     * Creates a programmatic Instance for dynamic bean lookup.
     *
     * <p>Instance allows runtime lookup of beans without injection.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Get instance handle
     * Instance<Object> instance = beanManager.createInstance();
     *
     * // Lookup specific bean type
     * Instance<UserService> userServiceInstance = instance.select(UserService.class);
     * if (userServiceInstance.isResolvable()) {
     *     UserService service = userServiceInstance.get();
     *     // Use service...
     * }
     *
     * // Lookup with qualifier
     * Instance<PaymentProcessor> creditCardProcessor = instance.select(
     *     PaymentProcessor.class,
     *     new CreditCardLiteral()
     * );
     * }</pre>
     *
     * @return an Instance<Object> for programmatic bean lookup
     */
    @Override
    public Instance<Object> createInstance() {
        // Create an Instance<Object> with @Any qualifier
        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(new com.threeamigos.common.util.implementations.injection.literals.AnyLiteral());

        // Create resolution strategy
        InstanceImpl.ResolutionStrategy<Object> strategy = new InstanceImpl.ResolutionStrategy<Object>() {
            @Override
            public Object resolveInstance(Class<Object> type, Collection<Annotation> quals) throws Exception {
                Annotation[] qualArray = quals.toArray(new Annotation[0]);
                return beanResolver.resolve(type, qualArray);
            }

            @Override
            public Collection<Class<?>> resolveImplementations(Class<Object> type, Collection<Annotation> quals) throws Exception {
                Annotation[] qualArray = quals.toArray(new Annotation[0]);
                Set<Bean<?>> beans = getBeans(type, qualArray);
                return beans.stream()
                    .map(bean -> (Class<?>) bean.getBeanClass())
                    .collect(Collectors.toList());
            }

            @Override
            public void invokePreDestroy(Object instance) throws java.lang.reflect.InvocationTargetException, IllegalAccessException {
                LifecycleMethodHelper.invokeLifecycleMethod(instance, jakarta.annotation.PreDestroy.class);
            }
        };

        java.util.function.Function<Class<?>, Bean<?>> beanLookup = beanClass -> {
            for (Bean<?> bean : knowledgeBase.getValidBeans()) {
                if (bean.getBeanClass().equals(beanClass)) {
                    return (Bean<?>) bean;
                }
            }
            return null;
        };

        return new InstanceImpl<>(Object.class, qualifiers, strategy, beanLookup);
    }

    /**
     * Checks if a bean matches an injection point.
     *
     * <p>Performs type and qualifier matching without actually resolving the bean.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Bean<?> bean = ...;
     * Set<Type> beanTypes = bean.getTypes();
     * Set<Annotation> beanQualifiers = bean.getQualifiers();
     *
     * boolean matches = beanManager.isMatchingBean(
     *     beanTypes,
     *     beanQualifiers,
     *     UserService.class,
     *     Set.of(new DefaultLiteral())
     * );
     * }</pre>
     *
     * @param beanTypes the bean's types
     * @param beanQualifiers the bean's qualifiers
     * @param requiredType the required injection type
     * @param requiredQualifiers the required qualifiers
     * @return true if the bean matches
     */
    @Override
    public boolean isMatchingBean(Set<Type> beanTypes, Set<Annotation> beanQualifiers,
                                   Type requiredType, Set<Annotation> requiredQualifiers) {
        // Check type compatibility
        boolean typeMatches = false;
        for (Type beanType : beanTypes) {
            if (typeChecker.isAssignable(requiredType, beanType)) {
                typeMatches = true;
                break;
            }
        }

        if (!typeMatches) {
            return false;
        }

        // Check qualifier match
        return qualifiersMatch(requiredQualifiers, beanQualifiers);
    }

    /**
     * Checks if an event matches an observer method.
     *
     * <p>Performs type and qualifier matching for event-observer compatibility.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * boolean matches = beanManager.isMatchingEvent(
     *     UserCreatedEvent.class,
     *     Set.of(new CreatedLiteral()),
     *     Object.class,
     *     Set.of()
     * );
     * }</pre>
     *
     * @param eventType the event type
     * @param eventQualifiers the event qualifiers
     * @param observedType the observer's observed type
     * @param observedQualifiers the observer's qualifiers
     * @return true if the event matches the observer
     */
    @Override
    public boolean isMatchingEvent(Type eventType, Set<Annotation> eventQualifiers,
                                    Type observedType, Set<Annotation> observedQualifiers) {
        // Check type compatibility
        if (!typeChecker.isAssignable(eventType, observedType)) {
            return false;
        }

        // Check qualifier match
        return observerQualifiersMatch(eventQualifiers, observedQualifiers);
    }

    // ==================== BeanManager Methods ====================

    /**
     * Obtains an injectable reference for an injection point.
     *
     * <p>Performs full resolution including type and qualifier matching.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * InjectionPoint injectionPoint = ...; // from @Inject field
     * CreationalContext<?> ctx = beanManager.createCreationalContext(null);
     *
     * Object reference = beanManager.getInjectableReference(injectionPoint, ctx);
     * }</pre>
     *
     * @param injectionPoint the injection point
     * @param ctx the creational context
     * @return the injectable reference (may be a proxy)
     * @throws IllegalArgumentException if injectionPoint is null
     * @throws jakarta.enterprise.inject.UnsatisfiedResolutionException if no bean found
     */
    @Override
    public Object getInjectableReference(InjectionPoint injectionPoint, CreationalContext<?> ctx) {
        if (injectionPoint == null) {
            throw new IllegalArgumentException("injectionPoint cannot be null");
        }

        Type requiredType = injectionPoint.getType();
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();

        Set<Bean<?>> beans = getBeans(requiredType, qualifiers.toArray(new Annotation[0]));
        Bean<?> bean = resolve(beans);

        if (bean == null) {
            throw new jakarta.enterprise.inject.UnsatisfiedResolutionException(
                "No bean found for injection point: " + injectionPoint);
        }

        return getReference(bean, requiredType, ctx);
    }

    /**
     * Obtains a passivation-capable bean by its ID.
     *
     * <p>Passivation-capable beans can be serialized/deserialized.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // During serialization
     * if (bean instanceof PassivationCapable) {
     *     String id = ((PassivationCapable) bean).getId();
     *     // Store ID...
     * }
     *
     * // During deserialization
     * Bean<?> bean = beanManager.getPassivationCapableBean(id);
     * }</pre>
     *
     * @param id the passivation ID
     * @return the bean with the given ID, or null if not found
     * @throws IllegalArgumentException if id is null
     */
    @Override
    public Bean<?> getPassivationCapableBean(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        // Search through all beans for one with matching ID
        for (Bean<?> bean : knowledgeBase.getValidBeans()) {
            if (bean instanceof jakarta.enterprise.inject.spi.PassivationCapable) {
                jakarta.enterprise.inject.spi.PassivationCapable pc =
                    (jakarta.enterprise.inject.spi.PassivationCapable) bean;
                if (id.equals(pc.getId())) {
                    return bean;
                }
            }
        }

        return null;
    }

    /**
     * Validates an injection point at deployment time.
     *
     * <p>Checks that the injection point can be satisfied and is not ambiguous.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * InjectionPoint injectionPoint = ...;
     *
     * try {
     *     beanManager.validate(injectionPoint);
     *     // Valid injection point
     * } catch (UnsatisfiedResolutionException e) {
     *     // No matching bean
     * } catch (AmbiguousResolutionException e) {
     *     // Multiple matching beans
     * }
     * }</pre>
     *
     * @param injectionPoint the injection point to validate
     * @throws IllegalArgumentException if injectionPoint is null
     * @throws jakarta.enterprise.inject.UnsatisfiedResolutionException if no bean found
     * @throws jakarta.enterprise.inject.AmbiguousResolutionException if ambiguous
     */
    @Override
    public void validate(InjectionPoint injectionPoint) {
        if (injectionPoint == null) {
            throw new IllegalArgumentException("injectionPoint cannot be null");
        }

        Type requiredType = injectionPoint.getType();
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();

        Set<Bean<?>> beans = getBeans(requiredType, qualifiers.toArray(new Annotation[0]));

        if (beans.isEmpty()) {
            throw new jakarta.enterprise.inject.UnsatisfiedResolutionException(
                "No bean found for injection point: " + injectionPoint);
        }

        Bean<?> resolved = resolve(beans);
        if (resolved == null && beans.size() > 1) {
            throw new jakarta.enterprise.inject.AmbiguousResolutionException(
                "Ambiguous dependency at injection point: " + injectionPoint +
                ". Matching beans: " + beans.stream()
                    .map(b -> b.getBeanClass().getName())
                    .collect(Collectors.joining(", ")));
        }
    }

    /**
     * Resolves decorators for a set of types.
     *
     * <p>Decorators intercept method calls on beans by implementing the same interface.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Decorator
     * @Decorator
     * @Priority(100)
     * public class CachingPaymentProcessor implements PaymentProcessor {
     *     @Inject @Delegate PaymentProcessor delegate;
     *
     *     public void process(Payment payment) {
     *         // Check cache...
     *         delegate.process(payment);
     *     }
     * }
     *
     * // Resolve decorators
     * Set<Type> types = Set.of(PaymentProcessor.class);
     * List<Decorator<?>> decorators = beanManager.resolveDecorators(types);
     * }</pre>
     *
     * @param types the decorated types
     * @param qualifiers the qualifiers (usually empty for decorators)
     * @return list of decorators sorted by priority (can be empty, never null)
     * @throws IllegalArgumentException if the types collection is null or empty
     */
    @Override
    public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... qualifiers) {
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("types cannot be null or empty");
        }

        Set<Annotation> requiredQualifiers = extractQualifiers(qualifiers);
        List<Decorator<?>> matchingDecorators = new ArrayList<>();

        for (DecoratorInfo decoratorInfo : knowledgeBase.getDecoratorInfos()) {
            // Check if the decorator applies to any of the types
            boolean typeMatches = false;
            for (Type decoratedType : decoratorInfo.getDecoratedTypes()) {
                for (Type requestedType : types) {
                    if (typeChecker.isAssignable(requestedType, decoratedType)) {
                        typeMatches = true;
                        break;
                    }
                }
                if (typeMatches) break;
            }

            if (!typeMatches) {
                continue;
            }

            // Check qualifier match (decorators don't use qualifiers, they use delegate injection point)
            matchingDecorators.add(createDecorator(decoratorInfo));
        }

        // Sort by priority (lower priority value = outer decorator)
        matchingDecorators.sort(Comparator.comparingInt(this::getPriority));

        return matchingDecorators;
    }

    /**
     * Checks if a scope is passivating.
     *
     * <p>Passivating scopes (@SessionScoped, @ConversationScoped) require
     * beans to be Serializable.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * beanManager.isPassivatingScope(SessionScoped.class);       // true
     * beanManager.isPassivatingScope(ConversationScoped.class);  // true
     * beanManager.isPassivatingScope(ApplicationScoped.class);   // false
     * beanManager.isPassivatingScope(RequestScoped.class);       // false
     * }</pre>
     *
     * @param annotationType the scope annotation
     * @return true if the scope is passivating
     */
    @Override
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return false;
        }

        // Check if the annotation has @NormalScope(passivating=true)
        if (hasNormalScopeAnnotation(annotationType)) {
            jakarta.enterprise.context.NormalScope normalScope =
                annotationType.getAnnotation(jakarta.enterprise.context.NormalScope.class);
            return normalScope != null && normalScope.passivating();
        }

        return false;
    }

    /**
     * Returns the full definition of an interceptor binding.
     *
     * <p>Includes all meta-annotations (for transitive bindings).
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @InterceptorBinding
     * @Inherited
     * @Retention(RUNTIME)
     * @Target({TYPE, METHOD})
     * public @interface Transactional {
     *     TransactionType value() default TransactionType.REQUIRED;
     * }
     *
     * Set<Annotation> definition = beanManager.getInterceptorBindingDefinition(Transactional.class);
     * // Returns: @InterceptorBinding, @Inherited, @Retention, @Target
     * }</pre>
     *
     * @param bindingType the interceptor binding annotation
     * @return set of annotations comprising the binding definition
     * @throws IllegalArgumentException if not an interceptor binding
     */
    @Override
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> bindingType) {
        if (bindingType == null || !isInterceptorBinding(bindingType)) {
            throw new IllegalArgumentException("Not an interceptor binding: " + bindingType);
        }

        // Return all annotations on the interceptor binding, including transitives
        Set<Annotation> bindings = new HashSet<>();
        bindings.addAll(Arrays.asList(bindingType.getAnnotations()));

        return bindings;
    }

    /**
     * Returns the full definition of a stereotype.
     *
     * <p>Includes all annotations bundled by the stereotype.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @Stereotype
     * @ApplicationScoped
     * @Transactional
     * @Named
     * @Retention(RUNTIME)
     * @Target(TYPE)
     * public @interface Service { }
     *
     * Set<Annotation> definition = beanManager.getStereotypeDefinition(Service.class);
     * // Returns: @Stereotype, @ApplicationScoped, @Transactional, @Named, @Retention, @Target
     * }</pre>
     *
     * @param stereotype the stereotype annotation
     * @return set of annotations comprising the stereotype definition
     * @throws IllegalArgumentException if not a stereotype
     */
    @Override
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype) {
        if (stereotype == null || !isStereotype(stereotype)) {
            throw new IllegalArgumentException("Not a stereotype: " + stereotype);
        }

        // Check if this is a programmatically registered stereotype
        Set<Annotation> registeredDef = knowledgeBase.getStereotypeDefinition(stereotype);
        if (registeredDef != null) {
            // Return the programmatically registered definition
            return new HashSet<>(registeredDef);
        }

        // Otherwise, return all annotations on the stereotype (for @Stereotype-annotated classes)
        Set<Annotation> annotations = new HashSet<>();
        annotations.addAll(Arrays.asList(stereotype.getAnnotations()));

        return annotations;
    }

    /**
     * Checks if two qualifiers are equivalent.
     *
     * <p>Qualifiers are equivalent if they have the same type and member values.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Annotation q1 = new NamedLiteral("myBean");
     * Annotation q2 = new NamedLiteral("myBean");
     * Annotation q3 = new NamedLiteral("otherBean");
     *
     * beanManager.areQualifiersEquivalent(q1, q2);  // true
     * beanManager.areQualifiersEquivalent(q1, q3);  // false
     * }</pre>
     *
     * @param qualifier1 first qualifier
     * @param qualifier2 second qualifier
     * @return true if equivalent
     */
    @Override
    public boolean areQualifiersEquivalent(Annotation qualifier1, Annotation qualifier2) {
        if (qualifier1 == null || qualifier2 == null) {
            return false;
        }

        if (!qualifier1.annotationType().equals(qualifier2.annotationType())) {
            return false;
        }

        return qualifier1.equals(qualifier2);
    }

    /**
     * Checks if two interceptor bindings are equivalent.
     *
     * <p>Similar to qualifier equivalence but for interceptor bindings.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Annotation b1 = new TransactionalLiteral(TransactionType.REQUIRED);
     * Annotation b2 = new TransactionalLiteral(TransactionType.REQUIRED);
     *
     * beanManager.areInterceptorBindingsEquivalent(b1, b2);  // true
     * }</pre>
     *
     * @param binding1 first binding
     * @param binding2 second binding
     * @return true if equivalent
     */
    @Override
    public boolean areInterceptorBindingsEquivalent(Annotation binding1, Annotation binding2) {
        if (binding1 == null || binding2 == null) {
            return false;
        }

        if (!binding1.annotationType().equals(binding2.annotationType())) {
            return false;
        }

        return binding1.equals(binding2);
    }

    /**
     * Returns the hash code for a qualifier.
     *
     * <p>Used for storing qualifiers in hash-based collections.
     *
     * @param qualifier the qualifier
     * @return hash code
     */
    @Override
    public int getQualifierHashCode(Annotation qualifier) {
        if (qualifier == null) {
            return 0;
        }
        return qualifier.hashCode();
    }

    /**
     * Returns the hash code for an interceptor binding.
     *
     * <p>Used for storing bindings in hash-based collections.
     *
     * @param binding the binding
     * @return hash code
     */
    @Override
    public int getInterceptorBindingHashCode(Annotation binding) {
        if (binding == null) {
            return 0;
        }
        return binding.hashCode();
    }

    /**
     * Returns an ELResolver for CDI beans.
     *
     * <p><b>Note:</b> EL integration not yet implemented.
     *
     * @return EL resolver
     * @throws UnsupportedOperationException always
     * @deprecated EL integration deprecated in CDI 4.1
     */
    @Override
    public ELResolver getELResolver() {
        // EL integration not implemented yet
        throw new UnsupportedOperationException("EL integration not yet implemented");
    }

    /**
     * Wraps an expression factory for CDI integration.
     *
     * <p><b>Note:</b> EL integration not yet implemented.
     *
     * @param expressionFactory the expression factory
     * @return wrapped expression factory
     * @deprecated EL integration deprecated in CDI 4.1
     */
    @Override
    public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory) {
        // EL integration not implemented yet
        return expressionFactory;
    }

    /**
     * Creates an AnnotatedType for a class.
     *
     * <p><b>Note:</b> Portable extension support not yet implemented.
     *
     * @param type the class
     * @param <T> the class type
     * @return annotated type
     * @throws IllegalArgumentException if type is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> AnnotatedType<T> createAnnotatedType(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        // Create a simple AnnotatedType wrapper
        // For full implementation, would need AnnotatedTypeImpl
        throw new UnsupportedOperationException("createAnnotatedType not yet implemented");
    }

    /**
     * Gets an injection target factory for an annotated type.
     *
     * <p><b>Note:</b> Portable extension support not yet implemented.
     *
     * @param annotatedType the annotated type
     * @param <T> the type
     * @return injection target factory
     * @throws IllegalArgumentException if annotatedType is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> InjectionTargetFactory<T> getInjectionTargetFactory(AnnotatedType<T> annotatedType) {
        if (annotatedType == null) {
            throw new IllegalArgumentException("annotatedType cannot be null");
        }

        throw new UnsupportedOperationException("getInjectionTargetFactory not yet implemented");
    }

    /**
     * Gets a producer factory for a field.
     *
     * <p><b>Note:</b> Portable extension support not yet implemented.
     *
     * @param field the field
     * @param declaringBean the declaring bean
     * @param <X> the produced type
     * @return producer factory
     * @throws IllegalArgumentException if field is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> field, Bean<X> declaringBean) {
        if (field == null) {
            throw new IllegalArgumentException("field cannot be null");
        }

        throw new UnsupportedOperationException("getProducerFactory not yet implemented");
    }

    /**
     * Gets a producer factory for a method.
     *
     * <p><b>Note:</b> Portable extension support not yet implemented.
     *
     * @param method the method
     * @param declaringBean the declaring bean
     * @param <X> the produced type
     * @return producer factory
     * @throws IllegalArgumentException if method is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public <X> ProducerFactory<X> getProducerFactory(AnnotatedMethod<? super X> method, Bean<X> declaringBean) {
        if (method == null) {
            throw new IllegalArgumentException("method cannot be null");
        }

        throw new UnsupportedOperationException("getProducerFactory not yet implemented");
    }

    /**
     * Creates bean attributes from an annotated type.
     *
     * <p><b>Note:</b> Portable extension support not yet implemented.
     *
     * @param type the annotated type
     * @param <T> the type
     * @return bean attributes
     * @throws IllegalArgumentException if type is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        throw new UnsupportedOperationException("createBeanAttributes not yet implemented");
    }

    /**
     * Creates bean attributes from an annotated member.
     *
     * <p><b>Note:</b> Portable extension support not yet implemented.
     *
     * @param member the annotated member
     * @return bean attributes
     * @throws IllegalArgumentException if member is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> member) {
        if (member == null) {
            throw new IllegalArgumentException("member cannot be null");
        }

        throw new UnsupportedOperationException("createBeanAttributes not yet implemented");
    }

    /**
     * Creates a bean from attributes and injection target factory.
     *
     * <p><b>Note:</b> Portable extension support not yet implemented.
     *
     * @param attributes the bean attributes
     * @param beanClass the bean class
     * @param injectionTargetFactory the injection target factory
     * @param <T> the bean type
     * @return created bean
     * @throws IllegalArgumentException if any parameter is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<T> beanClass,
                                   InjectionTargetFactory<T> injectionTargetFactory) {
        if (attributes == null) {
            throw new IllegalArgumentException("attributes cannot be null");
        }
        if (beanClass == null) {
            throw new IllegalArgumentException("beanClass cannot be null");
        }
        if (injectionTargetFactory == null) {
            throw new IllegalArgumentException("injectionTargetFactory cannot be null");
        }

        throw new UnsupportedOperationException("createBean not yet implemented");
    }

    /**
     * Creates a bean from attributes and producer factory.
     *
     * <p><b>Note:</b> Portable extension support not yet implemented.
     *
     * @param attributes the bean attributes
     * @param beanClass the bean class
     * @param producerFactory the producer factory
     * @param <T> the bean type
     * @param <X> the producer type
     * @return created bean
     * @throws IllegalArgumentException if any parameter is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T, X> Bean<T> createBean(BeanAttributes<T> attributes, Class<X> beanClass,
                                      ProducerFactory<X> producerFactory) {
        if (attributes == null) {
            throw new IllegalArgumentException("attributes cannot be null");
        }
        if (beanClass == null) {
            throw new IllegalArgumentException("beanClass cannot be null");
        }
        if (producerFactory == null) {
            throw new IllegalArgumentException("producerFactory cannot be null");
        }

        throw new UnsupportedOperationException("createBean not yet implemented");
    }

    /**
     * Creates an injection point from an annotated field.
     *
     * <p><b>Note:</b> Portable extension support not yet implemented.
     *
     * @param field the annotated field
     * @return injection point
     * @throws IllegalArgumentException if field is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public InjectionPoint createInjectionPoint(AnnotatedField<?> field) {
        if (field == null) {
            throw new IllegalArgumentException("field cannot be null");
        }

        throw new UnsupportedOperationException("createInjectionPoint not yet implemented");
    }

    /**
     * Creates an injection point from an annotated parameter.
     *
     * <p><b>Note:</b> Portable extension support not yet implemented.
     *
     * @param parameter the annotated parameter
     * @return injection point
     * @throws IllegalArgumentException if parameter is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public InjectionPoint createInjectionPoint(AnnotatedParameter<?> parameter) {
        if (parameter == null) {
            throw new IllegalArgumentException("parameter cannot be null");
        }

        throw new UnsupportedOperationException("createInjectionPoint not yet implemented");
    }

    /**
     * Gets a CDI extension instance.
     *
     * <p><b>Note:</b> Portable extensions not yet implemented.
     *
     * @param extensionClass the extension class
     * @param <T> the extension type
     * @return extension instance, or null if not found
     * @throws IllegalArgumentException if extensionClass is null
     */
    @Override
    public <T extends Extension> T getExtension(Class<T> extensionClass) {
        if (extensionClass == null) {
            throw new IllegalArgumentException("extensionClass cannot be null");
        }

        // Extensions not yet implemented
        return null;
    }

    /**
     * Creates an interception factory for programmatic interceptor binding.
     *
     * <p><b>Note:</b> Not yet implemented.
     *
     * @param ctx the creational context
     * @param clazz the class to intercept
     * @param <T> the class type
     * @return interception factory
     * @throws IllegalArgumentException if any parameter is null
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> InterceptionFactory<T> createInterceptionFactory(CreationalContext<T> ctx, Class<T> clazz) {
        if (ctx == null) {
            throw new IllegalArgumentException("ctx cannot be null");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz cannot be null");
        }

        throw new UnsupportedOperationException("createInterceptionFactory not yet implemented");
    }

    // ==================== Helper Methods ====================

    /**
     * Extracts qualifier annotations from an array of annotations.
     * If no qualifiers are present, adds @Default.
     */
    private Set<Annotation> extractQualifiers(Annotation[] annotations) {
        Set<Annotation> qualifiers = new HashSet<>();
        for (Annotation ann : annotations) {
            if (hasQualifierAnnotation(ann.annotationType())) {
                qualifiers.add(ann);
            }
        }

        // If no qualifiers specified, use @Default
        if (qualifiers.isEmpty()) {
            qualifiers.add(new com.threeamigos.common.util.implementations.injection.literals.DefaultLiteral());
        }

        return qualifiers;
    }

    /**
     * Checks if a bean has all required qualifiers.
     * The @Any qualifier matches everything.
     */
    private boolean qualifiersMatch(Set<Annotation> required, Set<Annotation> available) {
        for (Annotation req : required) {
            // @Any matches everything
            if (req.annotationType().equals(jakarta.enterprise.inject.Any.class)) {
                continue;
            }

            boolean found = false;
            for (Annotation avail : available) {
                if (areQualifiersEquivalent(req, avail)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if event qualifiers match observer qualifiers.
     * All observer qualifiers must be present in event qualifiers.
     */
    private boolean observerQualifiersMatch(Set<Annotation> eventQualifiers, Set<Annotation> observerQualifiers) {
        // All observer qualifiers must be present in event qualifiers
        for (Annotation observerQual : observerQualifiers) {
            // @Any matches everything
            if (observerQual.annotationType().equals(jakarta.enterprise.inject.Any.class)) {
                continue;
            }

            boolean found = false;
            for (Annotation eventQual : eventQualifiers) {
                if (areQualifiersEquivalent(observerQual, eventQual)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the priority value from @Priority annotation.
     * Returns APPLICATION priority if not specified.
     */
    private int getPriority(Object obj) {
        Class<?> clazz = obj instanceof Bean ? ((Bean<?>) obj).getBeanClass() : obj.getClass();

        if (hasPriorityAnnotation(clazz)) {
            jakarta.annotation.Priority priority = clazz.getAnnotation(jakarta.annotation.Priority.class);
            if (priority != null) {
                return priority.value();
            }
        }

        return jakarta.interceptor.Interceptor.Priority.APPLICATION;
    }

    /**
     * Creates an ObserverMethod wrapper from ObserverMethodInfo.
     */
    @SuppressWarnings("unchecked")
    private <T> ObserverMethod<T> createObserverMethod(ObserverMethodInfo info) {
        // Create a wrapper that implements ObserverMethod
        return new ObserverMethod<T>() {
            @Override
            public Class<?> getBeanClass() {
                return info.getDeclaringBean() != null ?
                    info.getDeclaringBean().getBeanClass() :
                    info.getObserverMethod().getDeclaringClass();
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
            public jakarta.enterprise.event.Reception getReception() {
                return info.getReception();
            }

            @Override
            public jakarta.enterprise.event.TransactionPhase getTransactionPhase() {
                return info.getTransactionPhase();
            }

            @Override
            public int getPriority() {
                return info.getPriority();
            }

            @Override
            public void notify(T event) {
                // Resolve the bean instance and invoke the observer method
                try {
                    Class<?> declaringClass = info.getDeclaringBean() != null ?
                        info.getDeclaringBean().getBeanClass() :
                        info.getObserverMethod().getDeclaringClass();
                    Object instance = beanResolver.resolveDeclaringBeanInstance(declaringClass);
                    info.getObserverMethod().invoke(instance, event);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke observer method", e);
                }
            }

            @Override
            public boolean isAsync() {
                return info.isAsync();
            }
        };
    }

    /**
     * Creates an Interceptor wrapper from InterceptorInfo.
     * Not yet fully implemented.
     */
    private <T> Interceptor<T> createInterceptor(InterceptorInfo info) {
        // For now, return a basic implementation
        // Full implementation would need more metadata
        throw new UnsupportedOperationException("Interceptor creation not yet fully implemented");
    }

    /**
     * Creates a Decorator wrapper from DecoratorInfo.
     * Not yet fully implemented.
     */
    private <T> Decorator<T> createDecorator(DecoratorInfo info) {
        // For now, return a basic implementation
        // Full implementation would need more metadata
        throw new UnsupportedOperationException("Decorator creation not yet fully implemented");
    }

    /**
     * Simple implementation of CreationalContext.
     * Tracks dependent instances for cleanup.
     */
    private static class CreationalContextImpl<T> implements CreationalContext<T> {
        private final List<Object> dependentInstances = new ArrayList<>();

        @Override
        public void push(T incompleteInstance) {
            // Not needed for basic implementation
        }

        @Override
        public void release() {
            // Cleanup dependent instances
            dependentInstances.clear();
        }

        public void addDependentInstance(Object instance) {
            dependentInstances.add(instance);
        }
    }

    /**
     * Adapter to wrap internal ScopeContext as Jakarta Context.
     * Bridges the gap between internal scope management and CDI SPI.
     */
    private static class ScopeContextAdapter implements Context {
        private final com.threeamigos.common.util.implementations.injection.contexts.ScopeContext scopeContext;
        private final Class<? extends Annotation> scope;

        public ScopeContextAdapter(com.threeamigos.common.util.implementations.injection.contexts.ScopeContext scopeContext,
                                   Class<? extends Annotation> scope) {
            this.scopeContext = scopeContext;
            this.scope = scope;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return scope;
        }

        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            if (!(contextual instanceof Bean)) {
                return null;
            }
            return scopeContext.get((Bean<T>) contextual, creationalContext);
        }

        @Override
        public <T> T get(Contextual<T> contextual) {
            if (!(contextual instanceof Bean)) {
                return null;
            }
            return scopeContext.getIfExists((Bean<T>) contextual);
        }

        @Override
        public boolean isActive() {
            return scopeContext.isActive();
        }

        public void destroy(Contextual<?> contextual) {
            // Delegate to scope context destroy
            // Note: ScopeContext doesn't have per-bean destroy, only full scope destroy
            // This is a limitation of the current design
        }
    }

    // ==================== Container Internal Methods ====================

    /**
     * Returns the ContextManager for internal container use.
     * <p>
     * This method is used by extension SPI implementations (e.g., AfterBeanDiscovery)
     * to register custom contexts programmatically.
     * <p>
     * <b>Note:</b> This is an internal API and should not be used by application code.
     * Applications should register custom contexts via portable extensions using
     * {@link AfterBeanDiscovery#addContext(Context)}.
     *
     * @return the context manager
     */
    public ContextManager getContextManager() {
        return contextManager;
    }
}
