package com.threeamigos.common.util.implementations.injection.decorators;

import com.threeamigos.common.util.implementations.injection.knowledgebase.DecoratorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Named;
import jakarta.inject.Qualifier;

import com.threeamigos.common.util.implementations.injection.util.AnnotationComparator;
import com.threeamigos.common.util.implementations.injection.util.DefaultLiteral;

/**
 * Resolves decorators for target beans based on type matching.
 *
 * <p>This class is responsible for determining which decorators should be applied to a given
 * bean based on:
 * <ul>
 *   <li>Type compatibility (decorator implements the same interface/class as the bean)</li>
 *   <li>Qualifier matching (decorators can have qualifiers)</li>
 *   <li>Priority ordering (lower priority = outer decorator, executed first)</li>
 * </ul>
 *
 * <p><b>CDI 4.1 Decorator Resolution Rules:</b>
 * <ul>
 *   <li>A decorator applies if its decorated types match the bean's types</li>
 *   <li>Decorators are type-based, not annotation-based (unlike interceptors)</li>
 *   <li>Multiple decorators can apply to the same bean</li>
 *   <li>Decorators are ordered by priority (lower value = earlier/outer execution)</li>
 *   <li>Each decorator receives a @Delegate injection point referencing the next decorator or bean</li>
 * </ul>
 *
 * <p><b>Decorator vs Interceptor Resolution:</b>
 * <table>
 * <tr><th>Aspect</th><th>Interceptors</th><th>Decorators</th></tr>
 * <tr><td>Matching</td><td>Annotation bindings</td><td>Type compatibility</td></tr>
 * <tr><td>Granularity</td><td>Per-method</td><td>Per-bean (all methods)</td></tr>
 * <tr><td>Delegation</td><td>InvocationContext.proceed()</td><td>@Delegate injection</td></tr>
 * </table>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * DecoratorResolver resolver = new DecoratorResolver(knowledgeBase);
 *
 * // Resolve decorators for a PaymentProcessor bean
 * Set<Type> beanTypes = Set.of(PaymentProcessor.class, Serializable.class);
 * Set<Annotation> qualifiers = Set.of(new DefaultLiteral());
 * List<DecoratorInfo> decorators = resolver.resolve(beanTypes, qualifiers);
 * // Returns: [TimingDecorator, LoggingDecorator] (sorted by priority)
 * }</pre>
 *
 * <p><b>Execution Order Example:</b>
 * <pre>
 * If decorators are resolved as [Decorator1(priority=100), Decorator2(priority=200)]:
 *
 * Client → Decorator1 → Decorator2 → Actual Bean
 *                                    ← Return Value
 *
 * Decorator1 wraps Decorator2 wraps Actual Bean
 * </pre>
 *
 * @see DecoratorInfo
 * @see KnowledgeBase
 * @see DecoratorChain
 * @author Stefano Reksten
 */
public class DecoratorResolver {

    private final KnowledgeBase knowledgeBase;

    /**
     * Creates a decorator resolver.
     *
     * @param knowledgeBase the knowledge base containing decorator metadata
     * @throws NullPointerException if knowledgeBase is null
     */
    public DecoratorResolver(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
    }

    /**
     * Resolves decorators for a bean with the given types and qualifiers.
     *
     * <p>This method:
     * <ol>
     *   <li>Queries all decorators from KnowledgeBase</li>
     *   <li>Filters decorators that match the bean's types</li>
     *   <li>Filters decorators that match the bean's qualifiers</li>
     *   <li>Sorts decorators by priority (lower priority = outer decorator)</li>
     * </ol>
     *
     * <p><b>Type Matching:</b>
     * A decorator matches if ANY of its decorated types are assignable from ANY of the bean's types.
     *
     * <p><b>Priority Ordering:</b>
     * Decorators are sorted by priority (ascending). Lower priority decorators execute first:
     * <pre>
     * Priority 100 (outer) → Priority 200 → Priority 300 (inner) → Bean
     * </pre>
     *
     * @param beanTypes the set of types implemented by the bean
     * @param qualifiers the set of qualifiers on the bean
     * @return list of matching decorators sorted by priority (can be empty, never null)
     * @throws NullPointerException if beanTypes or qualifiers is null
     */
    public List<DecoratorInfo> resolve(Set<Type> beanTypes, Set<Annotation> qualifiers) {
        Objects.requireNonNull(beanTypes, "beanTypes cannot be null");
        Objects.requireNonNull(qualifiers, "qualifiers cannot be null");

        if (beanTypes.isEmpty()) {
            return Collections.emptyList();
        }

        return knowledgeBase.getDecoratorInfos().stream()
            .filter(this::isEnabled) // must be enabled via beans.xml or @Priority
            .filter(decorator -> matchesTypes(decorator, beanTypes))
            .filter(decorator -> matchesQualifiers(decorator, qualifiers))
            .sorted(decoratorOrderingComparator())
            .collect(Collectors.toList());
    }

    /**
     * Comparator implementing CDI ordering rules:
     * 1) Decorators listed in beans.xml come first, in listed order across archives.
     * 2) Remaining decorators are ordered by @Priority (ascending).
     * 3) Tiebreaker: class name for determinism.
     */
    private Comparator<DecoratorInfo> decoratorOrderingComparator() {
        return Comparator
            .comparingInt((DecoratorInfo d) -> {
                int order = knowledgeBase.getDecoratorBeansXmlOrder(d.getDecoratorClass());
                return order >= 0 ? order : Integer.MAX_VALUE;
            })
            .thenComparingInt(DecoratorInfo::getPriority)
            .thenComparing(di -> di.getDecoratorClass().getName());
    }

    /**
     * A decorator is enabled if it is listed in any beans.xml OR has @Priority.
     */
    private boolean isEnabled(DecoratorInfo decorator) {
        int beansXmlOrder = knowledgeBase.getDecoratorBeansXmlOrder(decorator.getDecoratorClass());
        return beansXmlOrder >= 0 || decorator.getPriority() != Integer.MAX_VALUE;
    }

    /**
     * Checks if a decorator matches any of the bean's types.
     *
     * <p>A decorator matches if ANY of its decorated types are compatible with ANY of the bean's types.
     * Type compatibility is checked using Class.isAssignableFrom().
     *
     * <p><b>Examples:</b>
     * <ul>
     *   <li>Bean implements PaymentProcessor → Decorator decorates PaymentProcessor → MATCH</li>
     *   <li>Bean implements CreditCardProcessor extends PaymentProcessor → Decorator decorates PaymentProcessor → MATCH</li>
     *   <li>Bean implements OrderService → Decorator decorates PaymentProcessor → NO MATCH</li>
     * </ul>
     *
     * @param decorator the decorator to check
     * @param beanTypes the bean's types
     * @return true if the decorator matches any bean type
     */
    private boolean matchesTypes(DecoratorInfo decorator, Set<Type> beanTypes) {
        Set<Type> decoratedTypes = decorator.getDecoratedTypes();

        // Check if any decorated type matches any bean type
        for (Type decoratedType : decoratedTypes) {
            for (Type beanType : beanTypes) {
                if (isTypeCompatible(decoratedType, beanType)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if two types are compatible for decorator matching.
     *
     * <p>Type compatibility rules:
     * <ul>
     *   <li>If both are Class objects, use isAssignableFrom()</li>
     *   <li>Otherwise, use simple equality (for generic types)</li>
     * </ul>
     *
     * @param decoratedType the type that the decorator decorates
     * @param beanType the bean's type
     * @return true if the decorator can decorate the bean
     */
    private boolean isTypeCompatible(Type decoratedType, Type beanType) {
        // If both are raw classes, use assignability check
        if (decoratedType instanceof Class && beanType instanceof Class) {
            Class<?> decoratedClass = (Class<?>) decoratedType;
            Class<?> beanClass = (Class<?>) beanType;

            // Decorator can decorate if the bean implements/extends the decorated type
            return decoratedClass.isAssignableFrom(beanClass);
        }

        // For generic types, use simple equality
        return decoratedType.equals(beanType);
    }

    /**
     * Checks if a decorator matches the bean's qualifiers per CDI 4.1.
     *
     * <p>Qualifier matching uses the qualifiers on the decorator's single {@code @Delegate}
     * injection point. A decorator applies only if the bean has all qualifiers declared on
     * that delegate injection point (including {@code @Named} value matching). {@code @Any}
     * is ignored; {@code @Default} is assumed when no qualifier is present.</p>
     *
     * @param decorator the decorator to check
     * @param qualifiers the bean's qualifiers
     * @return true if the bean's qualifiers satisfy the delegate's qualifier set
     */
    private boolean matchesQualifiers(DecoratorInfo decorator, Set<Annotation> qualifiers) {
        Set<Annotation> required = normalizeQualifiers(decorator.getDelegateInjectionPoint().getQualifiers());
        Set<Annotation> available = normalizeQualifiers(qualifiers);
        return qualifiersMatch(required, available);
    }

    private Set<Annotation> normalizeQualifiers(Collection<Annotation> anns) {
        Set<Annotation> qualifiers = anns == null ? new HashSet<>() :
                anns.stream()
                        .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                        .collect(Collectors.toSet());
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
        return qualifiers;
    }

    private boolean qualifiersMatch(Set<Annotation> requiredQualifiers, Set<Annotation> beanQualifiers) {
        Annotation requiredNamed = findAnnotation(requiredQualifiers, Named.class);
        Annotation beanNamed = findAnnotation(beanQualifiers, Named.class);

        if (requiredNamed != null) {
            if (beanNamed == null) {
                return false;
            }
            String requiredName = getNamedValue(requiredNamed);
            String beanName = getNamedValue(beanNamed);
            if (!requiredName.equals(beanName)) {
                return false;
            }
        }

        for (Annotation required : requiredQualifiers) {
            if (required.annotationType().equals(jakarta.enterprise.inject.Any.class)) {
                continue;
            }
            if (required instanceof Named) {
                continue;
            }
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

    private Annotation findAnnotation(Set<Annotation> annotations, Class<? extends Annotation> type) {
        for (Annotation ann : annotations) {
            if (ann.annotationType().equals(type)) {
                return ann;
            }
        }
        return null;
    }

    private String getNamedValue(Annotation namedAnnotation) {
        try {
            return (String) namedAnnotation.annotationType().getMethod("value").invoke(namedAnnotation);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean qualifiersEqual(Annotation q1, Annotation q2) {
        return AnnotationComparator.equals(q1, q2);
    }

    /**
     * Checks if any decorators would apply to a bean with the given types.
     *
     * <p>This is a convenience method for checking if decorator resolution is needed.
     *
     * @param beanTypes the bean's types
     * @param qualifiers the bean's qualifiers
     * @return true if at least one decorator applies
     */
    public boolean hasDecorators(Set<Type> beanTypes, Set<Annotation> qualifiers) {
        return !resolve(beanTypes, qualifiers).isEmpty();
    }

    /**
     * Returns all decorators registered in the knowledge base.
     *
     * <p>This is useful for debugging and validation purposes.
     *
     * @return collection of all decorators
     */
    public Collection<DecoratorInfo> getAllDecorators() {
        return knowledgeBase.getDecoratorInfos();
    }
}
