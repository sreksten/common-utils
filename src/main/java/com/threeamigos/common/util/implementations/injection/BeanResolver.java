package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.contexts.ContextManager;
import com.threeamigos.common.util.implementations.injection.contexts.ScopeContext;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.literals.DefaultLiteral;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Named;
import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;

/**
 * Resolves dependencies by finding matching beans from the KnowledgeBase.
 * This is the core dependency resolution engine for the CDI container.
 *
 * <p>Special handling for built-in beans:
 * <ul>
 *   <li><b>InjectionPoint</b> - Contextual metadata about current injection point</li>
 *   <li><b>Instance&lt;T&gt;</b> - Programmatic bean lookup</li>
 *   <li><b>Provider&lt;T&gt;</b> - Lazy dependency resolution</li>
 *   <li><b>Event&lt;T&gt;</b> - Programmatic event firing</li>
 * </ul>
 *
 * @author Stefano Reksten
 */
public class BeanResolver implements ProducerBean.DependencyResolver {

    private final KnowledgeBase knowledgeBase;
    private final ContextManager contextManager;
    private final TypeChecker typeChecker;

    // ThreadLocal to pass injection point context during resolution
    private final ThreadLocal<InjectionPoint> currentInjectionPoint = new ThreadLocal<>();

    BeanResolver(KnowledgeBase knowledgeBase, ContextManager contextManager) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.contextManager = Objects.requireNonNull(contextManager, "contextManager cannot be null");
        this.typeChecker = new TypeChecker();
    }

    @Override
    public Object resolve(Type requiredType, Annotation[] qualifiers) {
        // Special handling for InjectionPoint built-in bean
        if (requiredType instanceof Class &&
            jakarta.enterprise.inject.spi.InjectionPoint.class.equals(requiredType)) {
            // Return the current injection point context
            jakarta.enterprise.inject.spi.InjectionPoint ip = currentInjectionPoint.get();
            if (ip == null) {
                throw new IllegalStateException(
                    "InjectionPoint can only be injected during dependency resolution. " +
                    "It cannot be injected into @ApplicationScoped or other normal-scoped beans " +
                    "because it is contextual to the current injection point.");
            }
            return ip;
        }

        // Handle Event<T>, Instance<T> and Provider<T> injection
        if (requiredType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) requiredType;
            Class<?> rawType = (Class<?>) pt.getRawType();

            // Check if it's Event<T>
            if (Event.class.isAssignableFrom(rawType)) {
                Type eventType = pt.getActualTypeArguments()[0];
                Set<Annotation> requiredQualifiers = extractQualifiers(qualifiers);

                return createEventWrapper(eventType, requiredQualifiers);
            }

            // Check if it's a Provider<T> (which includes Instance<T>)
            if (Provider.class.isAssignableFrom(rawType)) {
                Type actualType = pt.getActualTypeArguments()[0];
                Class<?> actualClass = RawTypeExtractor.getRawType(actualType);
                Set<Annotation> requiredQualifiers = extractQualifiers(qualifiers);

                return createProviderWrapper(actualClass, new ArrayList<>(requiredQualifiers));
            }
        }

        // Find matching beans for regular dependencies
        Collection<Bean<?>> candidates = findMatchingBeans(requiredType, qualifiers);

        if (candidates.isEmpty()) {
            throw new RuntimeException(
                "No bean found for type: " + requiredType +
                " with qualifiers: " + Arrays.toString(qualifiers)
            );
        }

        if (candidates.size() > 1) {
            throw new RuntimeException(
                "Ambiguous dependency: multiple beans found for type: " + requiredType +
                " with qualifiers: " + Arrays.toString(qualifiers) +
                ". Matching beans: " + candidates.stream()
                    .map(b -> b.getBeanClass().getName())
                    .collect(Collectors.joining(", "))
            );
        }

        Bean<?> bean = candidates.iterator().next();

        // Get or create an instance from the appropriate scope
        return getInstanceFromScope(bean);
    }

    @Override
    public Object resolveDeclaringBeanInstance(Class<?> declaringClass) {
        // Find the bean for the declaring class
        for (Bean<?> bean : knowledgeBase.getValidBeans()) {
            if (bean.getBeanClass().equals(declaringClass)) {
                return getInstanceFromScope(bean);
            }
        }

        throw new RuntimeException(
            "No bean found for declaring class: " + declaringClass.getName()
        );
    }

    /**
     * Finds all beans matching the required type and qualifiers.
     */
    private Collection<Bean<?>> findMatchingBeans(Type requiredType, Annotation[] qualifiers) {
        List<Bean<?>> matches = new ArrayList<>();

        // Extract qualifier annotations (ignore non-qualifiers)
        Set<Annotation> requiredQualifiers = extractQualifiers(qualifiers);

        // Search through all valid beans
        for (Bean<?> bean : knowledgeBase.getValidBeans()) {
            // Skip beans with validation errors
            if (bean instanceof BeanImpl && ((BeanImpl<?>) bean).hasValidationErrors()) {
                continue;
            }
            if (bean instanceof ProducerBean && ((ProducerBean<?>) bean).hasValidationErrors()) {
                continue;
            }

            // Check type match
            boolean typeMatches = false;
            for (Type beanType : bean.getTypes()) {
                if (typeChecker.isAssignable(requiredType, beanType)) {
                    typeMatches = true;
                    break;
                }
            }

            if (!typeMatches) {
                continue;
            }

            // Check qualifier match
            if (qualifiersMatch(requiredQualifiers, bean.getQualifiers())) {
                matches.add(bean);
            }
        }

        return matches;
    }

    /**
     * Extracts qualifier annotations from an array of annotations.
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
            qualifiers.add(new DefaultLiteral());
        }

        return qualifiers;
    }

    /**
     * Checks if bean qualifiers match the required qualifiers.
     * A bean matches if it has all the required qualifiers.
     */
    private boolean qualifiersMatch(Set<Annotation> requiredQualifiers, Set<Annotation> beanQualifiers) {
        // Special case: @Named requires exact match
        Annotation requiredNamed = findAnnotation(requiredQualifiers, Named.class);
        Annotation beanNamed = findAnnotation(beanQualifiers, Named.class);

        if (requiredNamed != null) {
            if (beanNamed == null) {
                return false;
            }
            // Compare @Named values
            String requiredName = getNamedValue(requiredNamed);
            String beanName = getNamedValue(beanNamed);
            if (!requiredName.equals(beanName)) {
                return false;
            }
        }

        // Check all other qualifiers (ignoring @Any which is always present)
        for (Annotation required : requiredQualifiers) {
            if (required.annotationType().equals(jakarta.enterprise.inject.Any.class)) {
                continue; // @Any matches everything
            }
            if (required instanceof Named) {
                continue; // Already handled above
            }

            // Find matching qualifier in bean
            boolean found = false;
            for (Annotation beanQual : beanQualifiers) {
                if (qualifiersEqual(required, beanQual)) {
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
     * Finds an annotation of a specific type in a set.
     */
    private Annotation findAnnotation(Set<Annotation> annotations, Class<? extends Annotation> annotationType) {
        for (Annotation ann : annotations) {
            if (ann.annotationType().equals(annotationType)) {
                return ann;
            }
        }
        return null;
    }

    /**
     * Gets the value from a @Named annotation.
     */
    private String getNamedValue(Annotation namedAnnotation) {
        try {
            return (String) namedAnnotation.annotationType().getMethod("value").invoke(namedAnnotation);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Checks if two qualifiers are equal, respecting @Nonbinding members.
     *
     * <p>According to CDI 4.1 specification, qualifier members marked with @Nonbinding
     * must be ignored when comparing qualifiers for bean resolution.
     */
    private boolean qualifiersEqual(Annotation q1, Annotation q2) {
        return AnnotationComparator.equals(q1, q2);
    }

    /**
     * Gets or creates an instance from the appropriate scope.
     * <p>
     * For normal scopes (ApplicationScoped, RequestScoped, SessionScoped, ConversationScoped),
     * this returns a CLIENT PROXY instead of the actual instance. The proxy will delegate
     * all method calls to the current contextual instance from the scope.
     * <p>
     * Why proxies are needed:
     * - Allows injecting short-lived beans (RequestScoped) into long-lived beans (ApplicationScoped)
     * - The proxy ensures each method call gets the correct contextual instance
     * - Without proxies, you'd get the wrong instance (e.g., same RequestScoped instance for all requests)
     * <p>
     * For pseudo-scopes (Dependent), this returns the actual instance directly since
     * Dependent beans are created fresh for each injection point and don't need proxies.
     */
    private <T> T getInstanceFromScope(Bean<T> bean) {
        Class<? extends Annotation> scope = bean.getScope();

        // Check if this is a normal scope that requires a client proxy
        if (contextManager.isNormalScope(scope)) {
            // For normal scopes, return a client proxy
            // The proxy will delegate to the contextual instance on each method call
            return contextManager.createClientProxy(bean);
        } else {
            // For pseudo-scopes like @Dependent, return the actual instance
            ScopeContext context = contextManager.getContext(scope);
            CreationalContext<T> creationalContext = new CreationalContextImpl<>();
            return context.get(bean, creationalContext);
        }
    }

    /**
     * Creates a Provider/Instance wrapper for lazy dependency resolution.
     * Since Instance<T> extends Provider<T>, this single method handles both cases.
     * The returned wrapper implements the full Instance<T> interface which includes Provider<T>.
     *
     * @param type the type of instances to provide
     * @param qualifiers the qualifiers to use for resolution
     * @return Instance wrapper (which also implements Provider)
     */
    @SuppressWarnings("unchecked")
    private <T> Instance<T> createProviderWrapper(Class<T> type, Collection<Annotation> qualifiers) {
        // Create a resolution strategy that delegates to BeanResolver
        InstanceImpl.ResolutionStrategy<T> strategy = new InstanceImpl.ResolutionStrategy<T>() {
            @Override
            public T resolveInstance(Class<T> typeToResolve, Collection<Annotation> quals) throws Exception {
                // Convert qualifiers to array
                Annotation[] qualArray = quals.toArray(new Annotation[0]);
                Object resolved = resolve(typeToResolve, qualArray);
                return (T) resolved;
            }

            @Override
            public Collection<Class<? extends T>> resolveImplementations(Class<T> typeToResolve, Collection<Annotation> quals) throws Exception {
                // Find all matching beans
                Annotation[] qualArray = quals.toArray(new Annotation[0]);
                Collection<Bean<?>> beans = findMatchingBeans(typeToResolve, qualArray);

                // Extract bean classes
                List<Class<? extends T>> implementations = new ArrayList<>();
                for (Bean<?> bean : beans) {
                    implementations.add((Class<? extends T>) bean.getBeanClass());
                }
                return implementations;
            }

            @Override
            public void invokePreDestroy(T instance) throws InvocationTargetException, IllegalAccessException {
                // Invoke @PreDestroy via LifecycleMethodHelper
                LifecycleMethodHelper.invokeLifecycleMethod(instance, jakarta.annotation.PreDestroy.class);
            }
        };

        // Look up the Bean metadata from KnowledgeBase so Handle#getBean can return it
        java.util.function.Function<Class<? extends T>, Bean<? extends T>> beanLookup = beanClass -> {
            for (Bean<?> bean : knowledgeBase.getValidBeans()) {
                if (bean.getBeanClass().equals(beanClass)) {
                    @SuppressWarnings("unchecked")
                    Bean<? extends T> cast = (Bean<? extends T>) bean;
                    return cast;
                }
            }
            return null;
        };

        return new InstanceImpl<>(type, qualifiers, strategy, beanLookup);
    }

    /**
     * Creates an Event wrapper for programmatic event firing.
     * This allows injection of Event&lt;T&gt; with appropriate type parameters and qualifiers.
     *
     * @param eventType the type of events to fire
     * @param qualifiers the qualifiers for event filtering
     * @return Event instance configured for the specified type and qualifiers
     */
    @SuppressWarnings("unchecked")
    private <T> Event<T> createEventWrapper(Type eventType, Set<Annotation> qualifiers) {
        return new EventImpl<>(eventType, qualifiers, knowledgeBase, this, contextManager);
    }

    /**
     * Simple implementation of CreationalContext.
     * This tracks dependent instances for cleanup.
     */
    private static class CreationalContextImpl<T> implements CreationalContext<T> {
        private final List<Object> dependentInstances = new ArrayList<>();

        @Override
        public void push(T incompleteInstance) {
            // Not needed for our simple implementation
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

    // ==================== InjectionPoint Context Management ====================

    /**
     * Sets the current injection point context for InjectionPoint bean resolution.
     *
     * <p>This method must be called by BeanImpl before resolving dependencies
     * to provide the correct contextual InjectionPoint metadata.
     *
     * <p><b>Thread Safety:</b> Uses ThreadLocal, safe for concurrent injection.
     *
     * @param injectionPoint the current injection point being resolved
     */
    void setCurrentInjectionPoint(jakarta.enterprise.inject.spi.InjectionPoint injectionPoint) {
        currentInjectionPoint.set(injectionPoint);
    }

    /**
     * Clears the current injection point context after resolution completes.
     *
     * <p>This prevents memory leaks in thread pools and ensures clean state.
     */
    void clearCurrentInjectionPoint() {
        currentInjectionPoint.remove();
    }

    /**
     * Gets the current injection point context (for testing/debugging).
     *
     * @return the current injection point, or null if not set
     */
    jakarta.enterprise.inject.spi.InjectionPoint getCurrentInjectionPoint() {
        return currentInjectionPoint.get();
    }
}
