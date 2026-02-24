package com.threeamigos.common.util.implementations.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration of all JSR-330 (Dependency Injection) and CDI 4.1 annotations
 * with support for both javax and jakarta namespaces for backward compatibility.
 *
 * <p>Each enum value contains a set of annotation classes that are semantically equivalent
 * across the javax and jakarta namespaces. This allows the code to work with both old
 * (Java EE) and new (Jakarta EE) annotation sets.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Instead of:
 * if (clazz.isAnnotationPresent(jakarta.enterprise.inject.Vetoed.class) ||
 *     clazz.isAnnotationPresent(javax.enterprise.inject.Vetoed.class)) {
 *     // ...
 * }
 *
 * // Use:
 * if (AnnotationsEnum.hasVetoedAnnotation(clazz)) {
 *     // ...
 * }
 * }</pre>
 *
 * @author Stefano Reksten
 * @see jakarta.inject
 * @see javax.inject
 */
public enum AnnotationsEnum {

    // ==================== JSR-330 Annotations ====================

    /**
     * Identifies injectable constructors, methods, and fields.
     * Maps: {@code javax.inject.Inject}, {@code jakarta.inject.Inject}
     */
    INJECT(javax.inject.Inject.class, jakarta.inject.Inject.class),

    /**
     * Identifies a type that the injector only instantiates once.
     * Maps: {@code javax.inject.Singleton}, {@code jakarta.inject.Singleton}
     */
    SINGLETON(javax.inject.Singleton.class, jakarta.inject.Singleton.class),

    /**
     * String-based qualifier annotation.
     * Maps: {@code javax.inject.Named}, {@code jakarta.inject.Named}
     */
    NAMED(javax.inject.Named.class, jakarta.inject.Named.class),

    /**
     * Identifies qualifier annotations (meta-annotation).
     * Maps: {@code javax.inject.Qualifier}, {@code jakarta.inject.Qualifier}
     */
    QUALIFIER(javax.inject.Qualifier.class, jakarta.inject.Qualifier.class),

    /**
     * Identifies scope annotations (meta-annotation).
     * Maps: {@code javax.inject.Scope}, {@code jakarta.inject.Scope}
     */
    SCOPE(javax.inject.Scope.class, jakarta.inject.Scope.class),

    // ==================== CDI Qualifier/Binding Annotations ====================

    /**
     * Marks qualifier or interceptor binding annotation members that should be ignored during matching.
     * When comparing two qualifier or interceptor binding annotations, members marked with @Nonbinding
     * are not considered in the equality check.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @Qualifier
     * @Retention(RUNTIME)
     * @Target({FIELD, TYPE, METHOD, PARAMETER})
     * public @interface PayBy {
     *     PaymentMethod value();           // Considered during matching
     *     @Nonbinding String description() default "";  // Ignored during matching
     * }
     * }</pre>
     *
     * Maps: {@code javax.enterprise.util.Nonbinding}, {@code jakarta.enterprise.util.Nonbinding}
     */
    NONBINDING(javax.enterprise.util.Nonbinding.class, jakarta.enterprise.util.Nonbinding.class),

    // ==================== JSR-250 Annotations ====================

    /**
     * Lifecycle callback executed after dependency injection.
     * Maps: {@code javax.annotation.PostConstruct}, {@code jakarta.annotation.PostConstruct}
     */
    POST_CONSTRUCT(javax.annotation.PostConstruct.class, jakarta.annotation.PostConstruct.class),

    /**
     * Lifecycle callback executed before destruction.
     * Maps: {@code javax.annotation.PreDestroy}, {@code jakarta.annotation.PreDestroy}
     */
    PRE_DESTROY(javax.annotation.PreDestroy.class, jakarta.annotation.PreDestroy.class),

    /**
     * Priority annotation for ordering.
     * Maps: {@code javax.annotation.Priority}, {@code jakarta.annotation.Priority}
     */
    PRIORITY(javax.annotation.Priority.class, jakarta.annotation.Priority.class),

    // ==================== CDI Annotations ====================

    /**
     * Marks a bean as an alternative implementation.
     * Maps: {@code javax.enterprise.inject.Alternative}, {@code jakarta.enterprise.inject.Alternative}
     */
    ALTERNATIVE(javax.enterprise.inject.Alternative.class, jakarta.enterprise.inject.Alternative.class),

    /**
     * Built-in qualifier that matches all beans.
     * Maps: {@code javax.enterprise.inject.Any}, {@code jakarta.enterprise.inject.Any}
     */
    ANY(javax.enterprise.inject.Any.class, jakarta.enterprise.inject.Any.class),

    /**
     * The default qualifier applied when no other qualifier is present.
     * Maps: {@code javax.enterprise.inject.Default}, {@code jakarta.enterprise.inject.Default}
     */
    DEFAULT(javax.enterprise.inject.Default.class, jakarta.enterprise.inject.Default.class),

    /**
     * Marks a producer method or field.
     * Maps: {@code javax.enterprise.inject.Produces}, {@code jakarta.enterprise.inject.Produces}
     */
    PRODUCES(javax.enterprise.inject.Produces.class, jakarta.enterprise.inject.Produces.class),

    /**
     * Marks a disposer method parameter.
     * Maps: {@code javax.enterprise.inject.Disposes}, {@code jakarta.enterprise.inject.Disposes}
     */
    DISPOSES(javax.enterprise.inject.Disposes.class, jakarta.enterprise.inject.Disposes.class),

    /**
     * Marks a class that should not be discovered as a bean.
     * Maps: {@code javax.enterprise.inject.Vetoed}, {@code jakarta.enterprise.inject.Vetoed}
     */
    VETOED(javax.enterprise.inject.Vetoed.class, jakarta.enterprise.inject.Vetoed.class),

    /**
     * Restricts the bean types of a bean to only the types specified in the annotation value.
     * Per CDI 4.1 Section 2.2, this allows fine-grained control over which types from the
     * class hierarchy are included in the bean's type set.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @ApplicationScoped
     * @Typed(PaymentProcessor.class)  // Only injectable as PaymentProcessor
     * public class PayPalProcessor implements PaymentProcessor, EmailSender {
     *     // Not injectable as EmailSender or PayPalProcessor
     * }
     * }</pre>
     *
     * Maps: {@code javax.enterprise.inject.Typed}, {@code jakarta.enterprise.inject.Typed}
     */
    TYPED(javax.enterprise.inject.Typed.class, jakarta.enterprise.inject.Typed.class),

    /**
     * Identifies a stereotype annotation (meta-annotation).
     * Maps: {@code javax.enterprise.inject.Stereotype}, {@code jakarta.enterprise.inject.Stereotype}
     */
    STEREOTYPE(javax.enterprise.inject.Stereotype.class, jakarta.enterprise.inject.Stereotype.class),

    /**
     * Identifies a decorator bean.
     * Maps: {@code javax.decorator.Decorator}, {@code jakarta.decorator.Decorator}
     */
    DECORATOR(javax.decorator.Decorator.class, jakarta.decorator.Decorator.class),

    /**
     * Identifies an interceptor bean.
     * Maps: {@code javax.interceptor.Interceptor}, {@code jakarta.interceptor.Interceptor}
     */
    INTERCEPTOR(javax.interceptor.Interceptor.class, jakarta.interceptor.Interceptor.class),

    // ==================== CDI Scope Annotations ====================

    /**
     * Dependent pseudo-scope (new instance per injection point).
     * Maps: {@code javax.enterprise.context.Dependent}, {@code jakarta.enterprise.context.Dependent}
     */
    DEPENDENT(javax.enterprise.context.Dependent.class, jakarta.enterprise.context.Dependent.class),

    /**
     * Application scope (one instance per application).
     * Maps: {@code javax.enterprise.context.ApplicationScoped}, {@code jakarta.enterprise.context.ApplicationScoped}
     */
    APPLICATION_SCOPED(javax.enterprise.context.ApplicationScoped.class, jakarta.enterprise.context.ApplicationScoped.class),

    /**
     * Request scope (one instance per HTTP request).
     * Maps: {@code javax.enterprise.context.RequestScoped}, {@code jakarta.enterprise.context.RequestScoped}
     */
    REQUEST_SCOPED(javax.enterprise.context.RequestScoped.class, jakarta.enterprise.context.RequestScoped.class),

    /**
     * Session scope (one instance per HTTP session).
     * Maps: {@code javax.enterprise.context.SessionScoped}, {@code jakarta.enterprise.context.SessionScoped}
     */
    SESSION_SCOPED(javax.enterprise.context.SessionScoped.class, jakarta.enterprise.context.SessionScoped.class),

    /**
     * Conversation scope (one instance per conversation).
     * Maps: {@code javax.enterprise.context.ConversationScoped}, {@code jakarta.enterprise.context.ConversationScoped}
     */
    CONVERSATION_SCOPED(javax.enterprise.context.ConversationScoped.class, jakarta.enterprise.context.ConversationScoped.class),

    /**
     * Normal scope meta-annotation.
     * Maps: {@code javax.enterprise.context.NormalScope}, {@code jakarta.enterprise.context.NormalScope}
     */
    NORMAL_SCOPE(javax.enterprise.context.NormalScope.class, jakarta.enterprise.context.NormalScope.class),

    // ==================== CDI Event Annotations ====================

    /**
     * Marks a method parameter that observes CDI events (synchronous).
     * Maps: {@code javax.enterprise.event.Observes}, {@code jakarta.enterprise.event.Observes}
     */
    OBSERVES(javax.enterprise.event.Observes.class, jakarta.enterprise.event.Observes.class),

    /**
     * Marks a method parameter that observes CDI events (asynchronous).
     * Note: Only available in jakarta.enterprise (CDI 2.0+), not in older javax.enterprise (CDI 1.2).
     * Maps: {@code jakarta.enterprise.event.ObservesAsync}
     */
    OBSERVES_ASYNC(jakarta.enterprise.event.ObservesAsync.class);

    // ==================== Implementation ====================

    private final Set<Class<? extends Annotation>> annotations;

    @SafeVarargs
    AnnotationsEnum(Class<? extends Annotation>... annotationClasses) {
        this.annotations = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(annotationClasses)));
    }

    /**
     * Returns the set of annotation classes (both javax and jakarta) for this enum value.
     *
     * @return immutable set of annotation classes
     */
    public Set<Class<? extends Annotation>> getAnnotations() {
        return annotations;
    }

    /**
     * Checks if the given element is annotated with any of the annotations
     * corresponding to this enum value.
     *
     * @param element the annotated element to check
     * @return true if the element has any of the annotations, false otherwise
     */
    public boolean isPresent(AnnotatedElement element) {
        if (element == null) {
            return false;
        }
        for (Class<? extends Annotation> annotationClass : annotations) {
            if (element.isAnnotationPresent(annotationClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given annotation class matches any of the annotations
     * corresponding to this enum value (either javax or jakarta variant).
     *
     * @param annotationClass the annotation class to check
     * @return true if the annotation class matches, false otherwise
     */
    public boolean matches(Class<? extends Annotation> annotationClass) {
        return annotations.contains(annotationClass);
    }

    // ==================== Static Helper Methods ====================

    /**
     * Checks if the element has an @Inject annotation (javax or jakarta).
     */
    public static boolean hasInjectAnnotation(AnnotatedElement element) {
        return INJECT.isPresent(element);
    }

    /**
     * Checks if the element has a @Singleton annotation (javax or jakarta).
     */
    public static boolean hasSingletonAnnotation(AnnotatedElement element) {
        return SINGLETON.isPresent(element);
    }

    /**
     * Checks if the element has a @Named annotation (javax or jakarta).
     */
    public static boolean hasNamedAnnotation(AnnotatedElement element) {
        return NAMED.isPresent(element);
    }

    /**
     * Checks if the element has a @Qualifier annotation (javax or jakarta).
     */
    public static boolean hasQualifierAnnotation(AnnotatedElement element) {
        return QUALIFIER.isPresent(element);
    }

    /**
     * Checks if the element has a @Scope annotation (javax or jakarta).
     */
    public static boolean hasScopeAnnotation(AnnotatedElement element) {
        return SCOPE.isPresent(element);
    }

    /**
     * Checks if the element has a @PostConstruct annotation (javax or jakarta).
     */
    public static boolean hasPostConstructAnnotation(AnnotatedElement element) {
        return POST_CONSTRUCT.isPresent(element);
    }

    /**
     * Checks if the element has a @PreDestroy annotation (javax or jakarta).
     */
    public static boolean hasPreDestroyAnnotation(AnnotatedElement element) {
        return PRE_DESTROY.isPresent(element);
    }

    /**
     * Checks if the element has a @Priority annotation (javax or jakarta).
     */
    public static boolean hasPriorityAnnotation(AnnotatedElement element) {
        return PRIORITY.isPresent(element);
    }

    /**
     * Checks if the element has an @Alternative annotation (javax or jakarta).
     */
    public static boolean hasAlternativeAnnotation(AnnotatedElement element) {
        return ALTERNATIVE.isPresent(element);
    }

    /**
     * Checks if the element has an @Any annotation (javax or jakarta).
     */
    public static boolean hasAnyAnnotation(AnnotatedElement element) {
        return ANY.isPresent(element);
    }

    /**
     * Checks if the element has a @Default annotation (javax or jakarta).
     */
    public static boolean hasDefaultAnnotation(AnnotatedElement element) {
        return DEFAULT.isPresent(element);
    }

    /**
     * Checks if the element has a @Produces annotation (javax or jakarta).
     */
    public static boolean hasProducesAnnotation(AnnotatedElement element) {
        return PRODUCES.isPresent(element);
    }

    /**
     * Checks if the element has a @Disposes annotation (javax or jakarta).
     */
    public static boolean hasDisposesAnnotation(AnnotatedElement element) {
        return DISPOSES.isPresent(element);
    }

    /**
     * Checks if the element has a @Vetoed annotation (javax or jakarta).
     */
    public static boolean hasVetoedAnnotation(AnnotatedElement element) {
        return VETOED.isPresent(element);
    }

    /**
     * Checks if the element has a @Stereotype annotation (javax or jakarta).
     */
    public static boolean hasStereotypeAnnotation(AnnotatedElement element) {
        return STEREOTYPE.isPresent(element);
    }

    /**
     * Checks if the element has a @Decorator annotation (javax or jakarta).
     */
    public static boolean hasDecoratorAnnotation(AnnotatedElement element) {
        return DECORATOR.isPresent(element);
    }

    /**
     * Checks if the element has an @Interceptor annotation (javax or jakarta).
     */
    public static boolean hasInterceptorAnnotation(AnnotatedElement element) {
        return INTERCEPTOR.isPresent(element);
    }

    /**
     * Checks if the element has a @Dependent annotation (javax or jakarta).
     */
    public static boolean hasDependentAnnotation(AnnotatedElement element) {
        return DEPENDENT.isPresent(element);
    }

    /**
     * Checks if the element has an @ApplicationScoped annotation (javax or jakarta).
     */
    public static boolean hasApplicationScopedAnnotation(AnnotatedElement element) {
        return APPLICATION_SCOPED.isPresent(element);
    }

    /**
     * Checks if the element has a @RequestScoped annotation (javax or jakarta).
     */
    public static boolean hasRequestScopedAnnotation(AnnotatedElement element) {
        return REQUEST_SCOPED.isPresent(element);
    }

    /**
     * Checks if the element has a @SessionScoped annotation (javax or jakarta).
     */
    public static boolean hasSessionScopedAnnotation(AnnotatedElement element) {
        return SESSION_SCOPED.isPresent(element);
    }

    /**
     * Checks if the element has a @ConversationScoped annotation (javax or jakarta).
     */
    public static boolean hasConversationScopedAnnotation(AnnotatedElement element) {
        return CONVERSATION_SCOPED.isPresent(element);
    }

    /**
     * Checks if the element has a @NormalScope annotation (javax or jakarta).
     */
    public static boolean hasNormalScopeAnnotation(AnnotatedElement element) {
        return NORMAL_SCOPE.isPresent(element);
    }

    /**
     * Checks if the element has an @Observes annotation (javax or jakarta).
     */
    public static boolean hasObservesAnnotation(AnnotatedElement element) {
        return OBSERVES.isPresent(element);
    }

    /**
     * Checks if the element has an @ObservesAsync annotation (javax or jakarta).
     */
    public static boolean hasObservesAsyncAnnotation(AnnotatedElement element) {
        return OBSERVES_ASYNC.isPresent(element);
    }

    /**
     * Checks if the element has a @Nonbinding annotation (javax or jakarta).
     * Used to mark qualifier/interceptor binding annotation members that should be
     * ignored during matching.
     */
    public static boolean hasNonbindingAnnotation(AnnotatedElement element) {
        return NONBINDING.isPresent(element);
    }

    /**
     * Checks if the element has a @Typed annotation (javax or jakarta).
     * Used to restrict the bean types of a bean per CDI 4.1 Section 2.2.
     */
    public static boolean hasTypedAnnotation(AnnotatedElement element) {
        return TYPED.isPresent(element);
    }

    /**
     * Gets the @Typed annotation from the element (supports both javax and jakarta).
     * Returns null if no @Typed annotation is present.
     *
     * @param element the annotated element to check
     * @return the @Typed annotation instance, or null if not present
     */
    public static <T extends Annotation> T getTypedAnnotation(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        for (Class<? extends Annotation> annotationClass : TYPED.getAnnotations()) {
            @SuppressWarnings("unchecked")
            T annotation = (T) element.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }
}
