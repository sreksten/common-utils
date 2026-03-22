package com.threeamigos.common.util.implementations.injection.spi;

import com.threeamigos.common.util.implementations.injection.scopes.InjectionPointImpl;
import com.threeamigos.common.util.implementations.injection.events.EventImpl;
import com.threeamigos.common.util.implementations.injection.interceptors.InterceptionFactoryImpl;
import com.threeamigos.common.util.implementations.injection.interceptors.InterceptorAwareProxyGenerator;
import com.threeamigos.common.util.implementations.injection.resolution.*;
import com.threeamigos.common.util.implementations.injection.scopes.ContextManager;
import com.threeamigos.common.util.implementations.injection.knowledgebase.DecoratorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.InterceptorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.events.ObserverMethodInfo;
import com.threeamigos.common.util.implementations.injection.spi.spievents.SimpleAnnotatedType;
import com.threeamigos.common.util.implementations.injection.util.AnyLiteral;
import com.threeamigos.common.util.implementations.injection.util.LifecycleMethodHelper;
import com.threeamigos.common.util.implementations.injection.util.RawTypeExtractor;
import com.threeamigos.common.util.implementations.injection.util.TypeClosureHelper;
import com.threeamigos.common.util.implementations.injection.util.tx.TransactionServicesFactory;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.*;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Collectors;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;
import static com.threeamigos.common.util.implementations.injection.util.QualifiersHelper.extractQualifiers;
import static com.threeamigos.common.util.implementations.injection.util.QualifiersHelper.normalizeBeanQualifiers;
import static com.threeamigos.common.util.implementations.injection.util.QualifiersHelper.qualifiersMatch;

/**
 * Implementation of the CDI 4.1 BeanManager interface.
 *
 * <p>This class provides access to the CDI container's core functionality, including:
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
        this.beanResolver = new BeanResolver(knowledgeBase, contextManager,
            TransactionServicesFactory.create());
        this.typeChecker = new TypeChecker();
    }

    /**
     * Returns the KnowledgeBase used by this BeanManager.
     * This is used internally by extension events to propagate definition errors.
     *
     * @return the knowledge base
     */
    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public BeanResolver getBeanResolver() {
        return beanResolver;
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
        if (!bean.getTypes().contains(beanType)) {
            throw new IllegalArgumentException(
                "beanType " + beanType + " is not a bean type of bean " + bean.getBeanClass().getName());
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
     * @return set of matching beans (can be empty, never null)
     * @throws IllegalArgumentException if beanType is null
     */
    @Override
    public Set<Bean<?>> getBeans(Type beanType, Annotation... qualifiers) {
        if (beanType == null) {
            throw new IllegalArgumentException("beanType cannot be null");
        }
        if (beanType instanceof TypeVariable<?>) {
            throw new IllegalArgumentException("beanType cannot be a type variable: " + beanType);
        }
        validateRequiredQualifiers(qualifiers);

        Set<Annotation> requiredQualifiers = extractQualifiers(qualifiers);
        Set<Bean<?>> matchingBeans = new HashSet<>();

        for (Bean<?> bean : knowledgeBase.getValidBeans()) {
            if (!isBeanEnabledForResolution(bean)) {
                continue;
            }

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

        return applySpecializationFiltering(matchingBeans);
    }

    private void validateRequiredQualifiers(Annotation[] qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            return;
        }
        Set<Class<? extends Annotation>> seenNonRepeatable = new HashSet<Class<? extends Annotation>>();
        for (Annotation qualifier : qualifiers) {
            if (qualifier == null) {
                continue;
            }
            Class<? extends Annotation> qualifierType = qualifier.annotationType();
            if (!isQualifier(qualifierType)) {
                throw new IllegalArgumentException("Annotation is not a qualifier type: " + qualifierType.getName());
            }
            if (qualifierType.getAnnotation(Repeatable.class) == null) {
                if (!seenNonRepeatable.add(qualifierType)) {
                    throw new IllegalArgumentException("Duplicate non-repeating qualifier: " + qualifierType.getName());
                }
            }
        }
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
            if (!isBeanEnabledForResolution(bean)) {
                continue;
            }
            if (name.equals(bean.getName())) {
                namedBeans.add(bean);
            }
        }

        return applySpecializationFiltering(namedBeans);
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

        Set<Bean<? extends X>> filteredBeans = applySpecializationFiltering(beans);
        if (filteredBeans.isEmpty()) {
            return null;
        }

        List<Bean<? extends X>> enabledBeans = new ArrayList<>();
        for (Bean<? extends X> bean : filteredBeans) {
            if (isBeanEnabledForResolution(bean)) {
                enabledBeans.add(bean);
            }
        }

        if (enabledBeans.isEmpty()) {
            return null;
        }

        if (enabledBeans.size() == 1) {
            return enabledBeans.get(0);
        }

        // Apply CDI 4.1 resolution rules:
        // 1. Filter enabled beans (non-alternatives or enabled alternatives)
        // 2. Select highest priority alternative if any exist
        // 3. If multiple alternatives with same priority, return null (ambiguous)

        List<Bean<? extends X>> alternatives = new ArrayList<>();
        List<Bean<? extends X>> nonAlternatives = new ArrayList<>();

        for (Bean<? extends X> bean : enabledBeans) {
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
                    throw new jakarta.enterprise.inject.AmbiguousResolutionException(
                        "Ambiguous dependency: multiple alternatives with same highest priority " + highestPriority);
                }
            }

            return highest;
        }

        // No alternatives, check non-alternatives
        if (nonAlternatives.size() == 1) {
            return nonAlternatives.get(0);
        }

        // Multiple non-alternatives = ambiguous
        throw new jakarta.enterprise.inject.AmbiguousResolutionException(
            "Ambiguous dependency: multiple beans match and no alternative can resolve ambiguity");
    }

    /**
     * Basic specialization filtering: if a candidate bean specializes its direct superclass,
     * the specialized superclass bean is removed from candidate sets.
     */
    private <X> Set<Bean<? extends X>> applySpecializationFiltering(Set<Bean<? extends X>> candidates) {
        if (candidates == null || candidates.size() < 2) {
            return candidates;
        }

        Set<Class<?>> specializedSuperclasses = new HashSet<>();
        for (Bean<? extends X> candidate : candidates) {
            Class<?> beanClass = candidate.getBeanClass();
            if (beanClass != null && hasSpecializesAnnotation(beanClass)) {
                specializedSuperclasses.addAll(collectSpecializedSuperclasses(beanClass));
            }
        }

        if (specializedSuperclasses.isEmpty()) {
            return candidates;
        }

        Set<Bean<? extends X>> filtered = new HashSet<>();
        for (Bean<? extends X> candidate : candidates) {
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

    private boolean hasSpecializesAnnotation(Class<?> beanClass) {
        for (Annotation annotation : beanClass.getAnnotations()) {
            String annotationName = annotation.annotationType().getName();
            if ("jakarta.enterprise.inject.Specializes".equals(annotationName) ||
                    "javax.enterprise.inject.Specializes".equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBeanEnabledForResolution(Bean<?> bean) {
        if (bean == null) {
            return false;
        }
        if (!bean.isAlternative()) {
            return true;
        }
        if (bean instanceof BeanImpl) {
            return ((BeanImpl<?>) bean).isAlternativeEnabled();
        }
        if (bean instanceof ProducerBean) {
            return ((ProducerBean<?>) bean).isAlternativeEnabled();
        }
        // For unknown Bean implementations keep backward-compatible behavior.
        return true;
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
        if (event.getClass().getTypeParameters().length > 0) {
            throw new IllegalArgumentException(
                "Runtime event type contains unresolvable type variables: " + event.getClass().getName());
        }
        validateRequiredQualifiers(qualifiers);

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
        validateInterceptorBindings(interceptorBindings);

        // Convert varargs to Set for query
        Set<Annotation> requiredBindings = new HashSet<>(Arrays.asList(interceptorBindings));

        // Use KnowledgeBase query method - already filters by type and bindings, and sorts by priority
        List<InterceptorInfo> matchingInfos = requiredBindings.isEmpty()
                ? knowledgeBase.getInterceptorsByType(type)
                : knowledgeBase.getInterceptorsByBindingsAndType(type, requiredBindings);

        // Convert InterceptorInfo to Interceptor<?> beans
        return matchingInfos.stream()
                .map(this::createInterceptor)
                .collect(Collectors.toList());
    }

    private void validateInterceptorBindings(Annotation[] interceptorBindings) {
        if (interceptorBindings == null || interceptorBindings.length == 0) {
            throw new IllegalArgumentException("At least one interceptor binding is required");
        }
        Set<Class<? extends Annotation>> seenNonRepeatable = new HashSet<Class<? extends Annotation>>();
        for (Annotation binding : interceptorBindings) {
            if (binding == null) {
                throw new IllegalArgumentException("Interceptor binding cannot be null");
            }
            Class<? extends Annotation> bindingType = binding.annotationType();
            if (!isInterceptorBinding(bindingType)) {
                throw new IllegalArgumentException("Annotation is not an interceptor binding type: " + bindingType.getName());
            }
            if (bindingType.getAnnotation(Repeatable.class) == null) {
                if (!seenNonRepeatable.add(bindingType)) {
                    throw new IllegalArgumentException("Duplicate non-repeating interceptor binding: " + bindingType.getName());
                }
            }
        }
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
        // Check both annotation-based and programmatically registered scopes
        return hasScopeAnnotation(annotationType) ||
               hasNormalScopeAnnotation(annotationType) ||
               knowledgeBase.isRegisteredScope(annotationType);
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
        // Check both annotation-based and programmatically registered qualifiers
        return hasQualifierAnnotation(annotationType) ||
               knowledgeBase.isRegisteredQualifier(annotationType);
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
        // Check both annotation-based and programmatically registered interceptor bindings
        return com.threeamigos.common.util.implementations.injection.AnnotationsEnum
                   .hasActivateRequestContextAnnotation(annotationType) ||
               annotationType.isAnnotationPresent(jakarta.interceptor.InterceptorBinding.class) ||
               knowledgeBase.isRegisteredInterceptorBinding(annotationType);
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
            com.threeamigos.common.util.implementations.injection.scopes.ScopeContext scopeContext =
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
     * <p>The returned Event has type Object with @Default qualifier.
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
        // Create an Event<Object> with @Default specified qualifier
        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(jakarta.enterprise.inject.Default.Literal.INSTANCE);
        qualifiers.add(new AnyLiteral());

        return new EventImpl<>(Object.class, qualifiers, knowledgeBase, beanResolver, contextManager, beanResolver.getTransactionServices());
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
        // Create an Instance<Object> with @Default specified qualifier
        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(jakarta.enterprise.inject.Default.Literal.INSTANCE);
        qualifiers.add(new AnyLiteral());

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
        Annotation[] qualifierArray = qualifiers.toArray(new Annotation[0]);

        // Special case for InjectionPoint built-in bean
        if (requiredType instanceof Class &&
                jakarta.enterprise.inject.spi.InjectionPoint.class.equals(requiredType)) {
            if (isDefaultQualified(qualifiers)) {
                Bean<?> owningBean = injectionPoint.getBean();
                if (owningBean == null) {
                    throw new jakarta.enterprise.inject.spi.DefinitionException(
                            "InjectionPoint with qualifier @Default is not allowed for non-bean injection targets");
                }

                Class<? extends Annotation> owningScope = owningBean.getScope();
                if (owningScope != null && !Dependent.class.equals(owningScope) &&
                        !"javax.enterprise.context.Dependent".equals(owningScope.getName())) {
                    throw new jakarta.enterprise.inject.spi.DefinitionException(
                            "Bean " + owningBean.getBeanClass().getName() +
                                    " declares scope @" + owningScope.getSimpleName() +
                                    " and may not inject InjectionPoint with qualifier @Default");
                }
            }
            return injectionPoint;
        }

        if (beanResolver != null) {
            beanResolver.setCurrentInjectionPoint(injectionPoint);
        }
        try {
            // Special case: inject Instance<T> handles for lazy/programmatic lookup.
            if (requiredType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) requiredType;
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class && (
                        Event.class.isAssignableFrom((Class<?>) rawType) ||
                        Instance.class.isAssignableFrom((Class<?>) rawType) ||
                        jakarta.inject.Provider.class.isAssignableFrom((Class<?>) rawType) ||
                        Bean.class.equals(rawType) ||
                        Interceptor.class.equals(rawType))) {
                    return beanResolver != null
                            ? beanResolver.resolve(requiredType, qualifierArray)
                            : createInstance();
                }
            }

            Set<Bean<?>> beans = getBeans(requiredType, qualifierArray);
            Bean<?> bean = resolve(beans);

            if (bean == null) {
                throw new jakarta.enterprise.inject.UnsatisfiedResolutionException(
                        "No bean found for injection point: " + injectionPoint);
            }

            if (bean.getScope() == Dependent.class && ctx instanceof CreationalContextImpl) {
                @SuppressWarnings("unchecked")
                Bean<Object> dependentBean = (Bean<Object>) bean;
                CreationalContext<Object> childContext = createCreationalContext(dependentBean);
                Object instance = dependentBean.create(childContext);
                @SuppressWarnings("unchecked")
                CreationalContextImpl<Object> parentContext = (CreationalContextImpl<Object>) ctx;
                parentContext.addDependentInstance(dependentBean, instance, childContext);
                return instance;
            }

            return getReference(bean, requiredType, ctx);
        } finally {
            if (beanResolver != null) {
                beanResolver.clearCurrentInjectionPoint();
            }
        }
    }

    private boolean isDefaultQualified(Set<Annotation> qualifiers) {
        if (qualifiers == null || qualifiers.isEmpty()) {
            return true;
        }
        for (Annotation qualifier : qualifiers) {
            String name = qualifier.annotationType().getName();
            if ("jakarta.enterprise.inject.Default".equals(name) ||
                    "javax.enterprise.inject.Default".equals(name)) {
                return true;
            }
        }
        return false;
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

        return new SimpleAnnotatedType<>(type);
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

        return new InjectionTargetFactoryImpl<>(annotatedType, this);
    }

    /**
     * Gets a producer factory for a field.
     *
     * <p>The factory can be used to create Producer instances that handle
     * producer field invocation and lifecycle.
     *
     * @param field the producer field
     * @param declaringBean the declaring bean
     * @param <X> the produced type
     * @return producer factory
     * @throws IllegalArgumentException if field is null
     */
    @Override
    public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> field, Bean<X> declaringBean) {
        if (field == null) {
            throw new IllegalArgumentException("field cannot be null");
        }

        return new ProducerFactoryImpl<>(field, this);
    }

    /**
     * Gets a producer factory for a method.
     *
     * <p>The factory can be used to create Producer instances that handle
     * producer method invocation and lifecycle.
     *
     * @param method the producer method
     * @param declaringBean the declaring bean
     * @param <X> the produced type
     * @return producer factory
     * @throws IllegalArgumentException if method is null
     */
    @Override
    public <X> ProducerFactory<X> getProducerFactory(AnnotatedMethod<? super X> method, Bean<X> declaringBean) {
        if (method == null) {
            throw new IllegalArgumentException("method cannot be null");
        }

        return new ProducerFactoryImpl<>(method, this);
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

        // Extract metadata from AnnotatedType
        String name = extractName(type);
        Set<Annotation> qualifiers = normalizeBeanQualifiers(type.getAnnotations());
        Class<? extends Annotation> scope = extractScopeFromAnnotated(type);
        Set<Class<? extends Annotation>> stereotypes = extractStereotypesFromAnnotated(type);
        Set<Type> types = TypeClosureHelper.extractTypesFromClass(type.getJavaClass());
        boolean alternative = type.isAnnotationPresent(jakarta.enterprise.inject.Alternative.class);

        return new BeanAttributesImpl<>(name, qualifiers, scope, stereotypes, types, alternative);
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

        // Extract metadata from AnnotatedMember (producer field or method)
        String name = extractName(member);
        Set<Annotation> qualifiers = normalizeBeanQualifiers(member.getAnnotations());
        Class<? extends Annotation> scope = extractScopeFromAnnotated(member);
        Set<Class<? extends Annotation>> stereotypes = extractStereotypesFromAnnotated(member);
        Set<Type> types = extractTypesFromMember(member);
        boolean alternative = member.isAnnotationPresent(jakarta.enterprise.inject.Alternative.class);

        return new BeanAttributesImpl<>(name, qualifiers, scope, stereotypes, types, alternative);
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

        // Create the injection target from the factory
        InjectionTarget<T> injectionTarget = injectionTargetFactory.createInjectionTarget(null);

        // Create a synthetic bean that uses the injection target for lifecycle management
        return new SyntheticBeanImpl<>(attributes, beanClass, injectionTarget);
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

        // Create the producer from the factory
        Producer<T> producer = producerFactory.createProducer(null);

        // Create a synthetic producer bean that uses the producer for instance creation
        return new SyntheticProducerBeanImpl<>(attributes, beanClass, producer);
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

        // Create an injection point from the annotated field
        // Note: We pass null for the Bean since this is a programmatically created injection point
        return new InjectionPointImpl<>(field.getJavaMember(), null);
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

        // Create an injection point from the annotated parameter
        // Note: We pass null for the Bean since this is a programmatically created injection point
        return new InjectionPointImpl<>(parameter.getJavaParameter(), null);
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

        // Create InterceptorAwareProxyGenerator for creating proxies
        InterceptorAwareProxyGenerator proxyGenerator = new InterceptorAwareProxyGenerator();

        // Return InterceptionFactory implementation
        return new InterceptionFactoryImpl<>(clazz, ctx, this, proxyGenerator);
    }

    // ==================== Helper Methods ====================

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

        if (obj instanceof SyntheticProducerBeanImpl) {
            Bean<?> originalProducerBean = findOriginalProducerBean((Bean<?>) obj);
            if (originalProducerBean instanceof ProducerBean) {
                ProducerBean<?> producerBean = (ProducerBean<?>) originalProducerBean;
                Integer memberPriority = extractPriorityFromProducerMember(producerBean);
                if (memberPriority != null) {
                    return memberPriority;
                }

                Integer producerPriority = producerBean.getPriority();
                if (producerPriority != null) {
                    return producerPriority;
                }

                Integer declaringPriority = extractPriorityFromClass(producerBean.getDeclaringClass());
                if (declaringPriority != null) {
                    return declaringPriority;
                }
            }
        }

        if (obj instanceof ProducerBean) {
            ProducerBean<?> producerBean = (ProducerBean<?>) obj;
            Integer memberPriority = extractPriorityFromProducerMember(producerBean);
            if (memberPriority != null) {
                return memberPriority;
            }

            Integer producerPriority = producerBean.getPriority();
            if (producerPriority != null) {
                return producerPriority;
            }

            Integer declaringPriority = extractPriorityFromClass(producerBean.getDeclaringClass());
            if (declaringPriority != null) {
                return declaringPriority;
            }
        }

        if (obj instanceof BeanImpl) {
            Integer beanPriority = ((BeanImpl<?>) obj).getPriority();
            if (beanPriority != null) {
                return beanPriority;
            }
        }

        Integer classPriority = extractPriorityFromClass(clazz);
        if (classPriority != null) {
            return classPriority;
        }

        return jakarta.interceptor.Interceptor.Priority.APPLICATION;
    }

    private Bean<?> findOriginalProducerBean(Bean<?> syntheticBean) {
        for (ProducerBean<?> producerBean : knowledgeBase.getProducerBeans()) {
            if (!Objects.equals(producerBean.getBeanClass(), syntheticBean.getBeanClass())) {
                continue;
            }
            if (!Objects.equals(producerBean.getTypes(), syntheticBean.getTypes())) {
                continue;
            }
            if (!Objects.equals(producerBean.getQualifiers(), syntheticBean.getQualifiers())) {
                continue;
            }
            return producerBean;
        }
        return null;
    }

    private Integer extractPriorityFromProducerMember(ProducerBean<?> producerBean) {
        if (producerBean.getProducerMethod() != null) {
            return extractPriorityFromAnnotations(producerBean.getProducerMethod().getAnnotations());
        }
        if (producerBean.getProducerField() != null) {
            return extractPriorityFromAnnotations(producerBean.getProducerField().getAnnotations());
        }
        return null;
    }

    private Integer extractPriorityFromClass(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return extractPriorityFromAnnotations(clazz.getAnnotations());
    }

    private Integer extractPriorityFromAnnotations(Annotation[] annotations) {
        if (annotations == null) {
            return null;
        }
        for (Annotation annotation : annotations) {
            String annotationTypeName = annotation.annotationType().getName();
            if (jakarta.annotation.Priority.class.getName().equals(annotationTypeName) ||
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

    /**
     * Creates an ObserverMethod wrapper from ObserverMethodInfo.
     */
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
     */
    private <T> Interceptor<T> createInterceptor(InterceptorInfo info) {
        final Class<?> interceptorClass = info.getInterceptorClass();
        final Set<Annotation> bindings = new HashSet<Annotation>(info.getInterceptorBindings());
        return new Interceptor<T>() {
            @Override
            public Set<Annotation> getInterceptorBindings() {
                return Collections.unmodifiableSet(bindings);
            }

            @Override
            public boolean intercepts(InterceptionType type) {
                if (type == null) {
                    return false;
                }
                switch (type) {
                    case AROUND_INVOKE:
                        return info.getAroundInvokeMethod() != null;
                    case AROUND_CONSTRUCT:
                        return info.getAroundConstructMethod() != null;
                    case POST_CONSTRUCT:
                        return info.getPostConstructMethod() != null;
                    case PRE_DESTROY:
                        return info.getPreDestroyMethod() != null;
                    default:
                        return false;
                }
            }

            @Override
            public Object intercept(InterceptionType type, T instance, jakarta.interceptor.InvocationContext ctx) throws Exception {
                Method interceptorMethod;
                switch (type) {
                    case AROUND_INVOKE:
                        interceptorMethod = info.getAroundInvokeMethod();
                        break;
                    case AROUND_CONSTRUCT:
                        interceptorMethod = info.getAroundConstructMethod();
                        break;
                    case POST_CONSTRUCT:
                        interceptorMethod = info.getPostConstructMethod();
                        break;
                    case PRE_DESTROY:
                        interceptorMethod = info.getPreDestroyMethod();
                        break;
                    default:
                        interceptorMethod = null;
                }
                if (interceptorMethod == null) {
                    return ctx != null ? ctx.proceed() : null;
                }
                if (!interceptorMethod.isAccessible()) {
                    interceptorMethod.setAccessible(true);
                }
                try {
                    return interceptorMethod.invoke(instance, ctx);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable target = e.getTargetException();
                    if (target instanceof Exception) {
                        throw (Exception) target;
                    }
                    throw new RuntimeException(target);
                }
            }

            @Override
            public Class<?> getBeanClass() {
                return interceptorClass;
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return Collections.emptySet();
            }

            @Override
            public T create(CreationalContext<T> context) {
                try {
                    @SuppressWarnings("unchecked")
                    T created = (T) interceptorClass.getDeclaredConstructor().newInstance();
                    return created;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create interceptor instance: " + interceptorClass.getName(), e);
                }
            }

            @Override
            public void destroy(T instance, CreationalContext<T> context) {
                if (context != null) {
                    context.release();
                }
            }

            @Override
            public Set<Type> getTypes() {
                Set<Type> types = new HashSet<Type>();
                types.add(interceptorClass);
                types.add(Object.class);
                return Collections.unmodifiableSet(types);
            }

            @Override
            public Set<Annotation> getQualifiers() {
                Set<Annotation> qualifiers = new HashSet<Annotation>();
                qualifiers.add(jakarta.enterprise.inject.Default.Literal.INSTANCE);
                qualifiers.add(jakarta.enterprise.inject.Any.Literal.INSTANCE);
                return qualifiers;
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return Dependent.class;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return Collections.emptySet();
            }

            @Override
            public boolean isAlternative() {
                return false;
            }
        };
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
        private final List<DependentEntry> dependentInstances = new ArrayList<>();

        @Override
        public void push(T incompleteInstance) {
            // Not needed for basic implementation
        }

        @Override
        public void release() {
            // Destroy dependent instances in reverse creation order.
            for (int i = dependentInstances.size() - 1; i >= 0; i--) {
                DependentEntry entry = dependentInstances.get(i);
                try {
                    entry.bean.destroy(entry.instance, entry.creationalContext);
                } catch (Exception ignored) {
                    // Continue destroying remaining dependents.
                }
            }
            dependentInstances.clear();
        }

        public void addDependentInstance(Object instance) {
            if (instance != null) {
                dependentInstances.add(new DependentEntry(null, instance, null));
            }
        }

        public void addDependentInstance(Bean<Object> bean, Object instance, CreationalContext<Object> creationalContext) {
            if (bean != null && instance != null) {
                dependentInstances.add(new DependentEntry(bean, instance, creationalContext));
            }
        }

        public List<Object> getDependentInstances() {
            List<Object> instances = new ArrayList<>(dependentInstances.size());
            for (DependentEntry entry : dependentInstances) {
                instances.add(entry.instance);
            }
            return Collections.unmodifiableList(instances);
        }

        private static class DependentEntry {
            private final Bean<Object> bean;
            private final Object instance;
            private final CreationalContext<Object> creationalContext;

            private DependentEntry(Bean<Object> bean, Object instance, CreationalContext<Object> creationalContext) {
                this.bean = bean;
                this.instance = instance;
                this.creationalContext = creationalContext;
            }
        }
    }

    /**
     * Adapter to wrap internal ScopeContext as Jakarta Context.
     * Bridges the gap between internal scope management and CDI SPI.
     */
    private static class ScopeContextAdapter implements AlterableContext {
        private final com.threeamigos.common.util.implementations.injection.scopes.ScopeContext scopeContext;
        private final Class<? extends Annotation> scope;

        public ScopeContextAdapter(com.threeamigos.common.util.implementations.injection.scopes.ScopeContext scopeContext,
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
            if (!scopeContext.isActive()) {
                throw new jakarta.enterprise.context.ContextNotActiveException(
                    "Context not active for scope: " + scope.getName()
                );
            }
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
            if (!scopeContext.isActive()) {
                throw new jakarta.enterprise.context.ContextNotActiveException(
                    "Context not active for scope: " + scope.getName()
                );
            }
            scopeContext.destroy(contextual);
        }
    }

    // ==================== Metadata Extraction Helper Methods ====================

    /**
     * Extracts the bean name from an Annotated element.
     * Returns empty string if no @Named annotation is present.
     */
    private String extractName(jakarta.enterprise.inject.spi.Annotated annotated) {
        if (annotated.isAnnotationPresent(jakarta.inject.Named.class)) {
            jakarta.inject.Named named = annotated.getAnnotation(jakarta.inject.Named.class);
            String value = named.value();
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
            // Default name: simple class name with first character lower-cased
            if (annotated instanceof jakarta.enterprise.inject.spi.AnnotatedType) {
                Class<?> clazz = ((jakarta.enterprise.inject.spi.AnnotatedType<?>) annotated).getJavaClass();
                return decapitalize(clazz.getSimpleName());
            }
        }
        return "";
    }

    /**
     * Extracts scope from an Annotated element.
     * Returns @Dependent if no scope is present.
     */
    private Class<? extends Annotation> extractScopeFromAnnotated(jakarta.enterprise.inject.spi.Annotated annotated) {
        for (Annotation ann : annotated.getAnnotations()) {
            if (isScopeAnnotationType(ann.annotationType())) {
                return ann.annotationType();
            }
        }
        return jakarta.enterprise.context.Dependent.class;
    }

    /**
     * Extracts stereotypes from an Annotated element.
     */
    private Set<Class<? extends Annotation>> extractStereotypesFromAnnotated(jakarta.enterprise.inject.spi.Annotated annotated) {
        Set<Class<? extends Annotation>> stereotypes = new HashSet<>();
        for (Annotation ann : annotated.getAnnotations()) {
            if (isStereotypeAnnotationType(ann.annotationType())) {
                stereotypes.add(ann.annotationType());
            }
        }
        return stereotypes;
    }

    /**
     * Extracts bean types from an AnnotatedMember (producer field or method).
     */
    private Set<Type> extractTypesFromMember(jakarta.enterprise.inject.spi.AnnotatedMember<?> member) {
        Set<Type> types = new HashSet<>();
        Type baseType = member.getBaseType();
        types.add(baseType);

        // If it's a class type, add its hierarchy
        if (baseType instanceof Class) {
            Class<?> clazz = (Class<?>) baseType;
            while (clazz != null && clazz != Object.class) {
                types.add(clazz);
                types.addAll(Arrays.asList(clazz.getGenericInterfaces()));
                clazz = clazz.getSuperclass();
            }
        }

        types.add(Object.class);
        return types;
    }

    /**
     * Decapitalizes a string following CDI conventions.
     */
    private String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Checks if an annotation type is a scope annotation.
     */
    private boolean isScopeAnnotationType(Class<? extends Annotation> annotationType) {
        return annotationType.isAnnotationPresent(jakarta.inject.Scope.class) ||
               annotationType.isAnnotationPresent(jakarta.enterprise.context.NormalScope.class) ||
               knowledgeBase.isRegisteredScope(annotationType);
    }

    /**
     * Checks if an annotation type is a stereotype annotation.
     */
    private boolean isStereotypeAnnotationType(Class<? extends Annotation> annotationType) {
        return annotationType.isAnnotationPresent(jakarta.enterprise.inject.Stereotype.class);
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
