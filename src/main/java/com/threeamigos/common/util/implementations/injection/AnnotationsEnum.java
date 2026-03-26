package com.threeamigos.common.util.implementations.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration of JSR-330 (Dependency Injection) and CDI annotations used by the container.
 * Where available, both {@code javax.*} and {@code jakarta.*} variants are supported.
 *
 * <p>Each enum value contains one or more equivalent annotation classes
 * (javax, jakarta, or both).
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
     * Since: JSR-330
     */
    INJECT(javax.inject.Inject.class, jakarta.inject.Inject.class),

    /**
     * Identifies a type that the injector only instantiates once.
     * Maps: {@code javax.inject.Singleton}, {@code jakarta.inject.Singleton}
     * Since: JSR-330
     */
    SINGLETON(javax.inject.Singleton.class, jakarta.inject.Singleton.class),

    /**
     * String-based qualifier annotation.
     * Maps: {@code javax.inject.Named}, {@code jakarta.inject.Named}
     * Since: JSR-330
     */
    NAMED(javax.inject.Named.class, jakarta.inject.Named.class),

    /**
     * Identifies qualifier annotations (meta-annotation).
     * Maps: {@code javax.inject.Qualifier}, {@code jakarta.inject.Qualifier}
     * Since: JSR-330
     */
    QUALIFIER(javax.inject.Qualifier.class, jakarta.inject.Qualifier.class),

    /**
     * Identifies scope annotations (meta-annotation).
     * Maps: {@code javax.inject.Scope}, {@code jakarta.inject.Scope}
     * Since: JSR-330
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
     * Since: CDI 1.0
     */
    NONBINDING(javax.enterprise.util.Nonbinding.class, jakarta.enterprise.util.Nonbinding.class),

    // ==================== JSR-250 Annotations ====================

    /**
     * Lifecycle callback executed after dependency injection.
     * Maps: {@code javax.annotation.PostConstruct}, {@code jakarta.annotation.PostConstruct}
     * Since: Common Annotations 1.0 (used by CDI since CDI 1.0)
     */
    POST_CONSTRUCT(javax.annotation.PostConstruct.class, jakarta.annotation.PostConstruct.class),

    /**
     * Lifecycle callback executed before destruction.
     * Maps: {@code javax.annotation.PreDestroy}, {@code jakarta.annotation.PreDestroy}
     * Since: Common Annotations 1.0 (used by CDI since CDI 1.0)
     */
    PRE_DESTROY(javax.annotation.PreDestroy.class, jakarta.annotation.PreDestroy.class),

    /**
     * Priority annotation for ordering.
     * Maps: {@code javax.annotation.Priority}, {@code jakarta.annotation.Priority}
     * Since: Common Annotations 1.2 (used by CDI since CDI 1.1)
     */
    PRIORITY(javax.annotation.Priority.class, jakarta.annotation.Priority.class),

    // ==================== CDI Annotations ====================

    /**
     * Marks a bean as an alternative implementation.
     * Maps: {@code javax.enterprise.inject.Alternative}, {@code jakarta.enterprise.inject.Alternative}
     * Since: CDI 1.0
     */
    ALTERNATIVE(javax.enterprise.inject.Alternative.class, jakarta.enterprise.inject.Alternative.class),

    /**
     * Built-in qualifier that matches all beans.
     * Maps: {@code javax.enterprise.inject.Any}, {@code jakarta.enterprise.inject.Any}
     * Since: CDI 1.0
     */
    ANY(javax.enterprise.inject.Any.class, jakarta.enterprise.inject.Any.class),

    /**
     * The default qualifier applied when no other qualifier is present.
     * Maps: {@code javax.enterprise.inject.Default}, {@code jakarta.enterprise.inject.Default}
     * Since: CDI 1.0
     */
    DEFAULT(javax.enterprise.inject.Default.class, jakarta.enterprise.inject.Default.class),

    /**
     * Marks a producer method or field.
     * Maps: {@code javax.enterprise.inject.Produces}, {@code jakarta.enterprise.inject.Produces}
     * Since: CDI 1.0
     */
    PRODUCES(javax.enterprise.inject.Produces.class, jakarta.enterprise.inject.Produces.class),

    /**
     * Marks a disposer method parameter.
     * Maps: {@code javax.enterprise.inject.Disposes}, {@code jakarta.enterprise.inject.Disposes}
     * Since: CDI 1.0
     */
    DISPOSES(javax.enterprise.inject.Disposes.class, jakarta.enterprise.inject.Disposes.class),

    /**
     * Marks a class that should not be discovered as a bean.
     * Maps: {@code javax.enterprise.inject.Vetoed}, {@code jakarta.enterprise.inject.Vetoed}
     * Since: CDI 1.1
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
     * Since: CDI 1.0
     */
    TYPED(javax.enterprise.inject.Typed.class, jakarta.enterprise.inject.Typed.class),

    /**
     * Identifies a stereotype annotation (meta-annotation).
     * Maps: {@code javax.enterprise.inject.Stereotype}, {@code jakarta.enterprise.inject.Stereotype}
     * Since: CDI 1.0
     */
    STEREOTYPE(javax.enterprise.inject.Stereotype.class, jakarta.enterprise.inject.Stereotype.class),

    /**
     * Indicates that a bean specializes another bean.
     * Maps: {@code javax.enterprise.inject.Specializes}, {@code jakarta.enterprise.inject.Specializes}
     * Since: CDI 1.0
     */
    SPECIALIZES(javax.enterprise.inject.Specializes.class, jakarta.enterprise.inject.Specializes.class),

    /**
     * Built-in stereotype that combines {@code @Named} and {@code @RequestScoped}.
     * Maps: {@code javax.enterprise.inject.Model}, {@code jakarta.enterprise.inject.Model}
     * Since: CDI 1.0
     */
    MODEL(javax.enterprise.inject.Model.class, jakarta.enterprise.inject.Model.class),

    /**
     * Deprecated qualifier indicating injection of a new instance of a bean.
     * Maps: {@code javax.enterprise.inject.New}
     * Since: CDI 1.0 (deprecated in CDI 1.1, removed from Jakarta CDI)
     */
    NEW(javax.enterprise.inject.New.class),

    /**
     * Built-in qualifier for obtaining bean metadata in interceptors.
     * Maps: {@code javax.enterprise.inject.Intercepted}, {@code jakarta.enterprise.inject.Intercepted}
     * Since: CDI 1.1
     */
    INTERCEPTED(javax.enterprise.inject.Intercepted.class, jakarta.enterprise.inject.Intercepted.class),

    /**
     * Annotation for transient method or constructor parameter references.
     * Maps: {@code javax.enterprise.inject.TransientReference}, {@code jakarta.enterprise.inject.TransientReference}
     * Since: CDI 2.0
     */
    TRANSIENT_REFERENCE(javax.enterprise.inject.TransientReference.class, jakarta.enterprise.inject.TransientReference.class),

    /**
     * Identifies a decorator bean.
     * Maps: {@code javax.decorator.Decorator}, {@code jakarta.decorator.Decorator}
     * Since: CDI 1.0
     */
    DECORATOR(javax.decorator.Decorator.class, jakarta.decorator.Decorator.class),

    /**
     * Marks the delegate injection point in a decorator.
     * Maps: {@code javax.decorator.Delegate}, {@code jakarta.decorator.Delegate}
     * Since: CDI 1.0
     */
    DELEGATE(javax.decorator.Delegate.class, jakarta.decorator.Delegate.class),

    /**
     * Identifies an interceptor bean.
     * Maps: {@code javax.interceptor.Interceptor}, {@code jakarta.interceptor.Interceptor}
     * Since: CDI 1.0
     */
    INTERCEPTOR(javax.interceptor.Interceptor.class, jakarta.interceptor.Interceptor.class),

    /**
     * Identifies interceptor binding annotations (meta-annotation).
     * Maps: {@code javax.interceptor.InterceptorBinding}, {@code jakarta.interceptor.InterceptorBinding}
     * Since: CDI 1.0
     */
    INTERCEPTOR_BINDING(javax.interceptor.InterceptorBinding.class, jakarta.interceptor.InterceptorBinding.class),

    // ==================== CDI Scope Annotations ====================

    /**
     * Dependent pseudo-scope (new instance per injection point).
     * Maps: {@code javax.enterprise.context.Dependent}, {@code jakarta.enterprise.context.Dependent}
     * Since: CDI 1.0
     */
    DEPENDENT(javax.enterprise.context.Dependent.class, jakarta.enterprise.context.Dependent.class),

    /**
     * Application scope (one instance per application).
     * Maps: {@code javax.enterprise.context.ApplicationScoped}, {@code jakarta.enterprise.context.ApplicationScoped}
     * Since: CDI 1.0
     */
    APPLICATION_SCOPED(javax.enterprise.context.ApplicationScoped.class, jakarta.enterprise.context.ApplicationScoped.class),

    /**
     * Request scope (one instance per HTTP request).
     * Maps: {@code javax.enterprise.context.RequestScoped}, {@code jakarta.enterprise.context.RequestScoped}
     * Since: CDI 1.0
     */
    REQUEST_SCOPED(javax.enterprise.context.RequestScoped.class, jakarta.enterprise.context.RequestScoped.class),

    /**
     * Session scope (one instance per HTTP session).
     * Maps: {@code javax.enterprise.context.SessionScoped}, {@code jakarta.enterprise.context.SessionScoped}
     * Since: CDI 1.0
     */
    SESSION_SCOPED(javax.enterprise.context.SessionScoped.class, jakarta.enterprise.context.SessionScoped.class),

    /**
     * Conversation scope (one instance per conversation).
     * Maps: {@code javax.enterprise.context.ConversationScoped}, {@code jakarta.enterprise.context.ConversationScoped}
     * Since: CDI 1.0
     */
    CONVERSATION_SCOPED(javax.enterprise.context.ConversationScoped.class, jakarta.enterprise.context.ConversationScoped.class),

    /**
     * Normal scope meta-annotation.
     * Maps: {@code javax.enterprise.context.NormalScope}, {@code jakarta.enterprise.context.NormalScope}
     * Since: CDI 1.0
     */
    NORMAL_SCOPE(javax.enterprise.context.NormalScope.class, jakarta.enterprise.context.NormalScope.class),

    // ==================== CDI Event Annotations ====================

    /**
     * Qualifier for context initialized lifecycle events.
     * Maps: {@code jakarta.enterprise.context.Initialized}
     * Since: CDI 1.1
     */
    INITIALIZED(jakarta.enterprise.context.Initialized.class),

    /**
     * Qualifier for context before-destroyed lifecycle events.
     * Maps: {@code jakarta.enterprise.context.BeforeDestroyed}
     * Since: CDI 1.1
     */
    BEFORE_DESTROYED(jakarta.enterprise.context.BeforeDestroyed.class),

    /**
     * Qualifier for context destroyed lifecycle events.
     * Maps: {@code jakarta.enterprise.context.Destroyed}
     * Since: CDI 1.1
     */
    DESTROYED(jakarta.enterprise.context.Destroyed.class),

    /**
     * Marks a method parameter that observes CDI events (synchronous).
     * Maps: {@code javax.enterprise.event.Observes}, {@code jakarta.enterprise.event.Observes}
     * Since: CDI 1.0
     */
    OBSERVES(javax.enterprise.event.Observes.class, jakarta.enterprise.event.Observes.class),

    /**
     * Marks a method parameter that observes CDI events (asynchronous).
     * Maps: {@code jakarta.enterprise.event.ObservesAsync}
     * Since: CDI 2.0
     */
    OBSERVES_ASYNC(jakarta.enterprise.event.ObservesAsync.class),

    /**
     * Interceptor binding that activates request context for a method invocation.
     * Maps: {@code jakarta.enterprise.context.control.ActivateRequestContext}
     * Since: CDI 2.0
     */
    ACTIVATE_REQUEST_CONTEXT(jakarta.enterprise.context.control.ActivateRequestContext.class),

    /**
     * Declares a build compatible extension method executed during discovery phase.
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.Discovery}
     * Since: CDI 4.0
     */
    DISCOVERY(jakarta.enterprise.inject.build.compatible.spi.Discovery.class),

    /**
     * Declares a build compatible extension method executed during enhancement phase.
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.Enhancement}
     * Since: CDI 4.0
     */
    ENHANCEMENT(jakarta.enterprise.inject.build.compatible.spi.Enhancement.class),

    /**
     * Declares a build compatible extension method executed during registration phase.
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.Registration}
     * Since: CDI 4.0
     */
    REGISTRATION(jakarta.enterprise.inject.build.compatible.spi.Registration.class),

    /**
     * Declares a build compatible extension method executed during synthesis phase.
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.Synthesis}
     * Since: CDI 4.0
     */
    SYNTHESIS(jakarta.enterprise.inject.build.compatible.spi.Synthesis.class),

    /**
     * Declares a build compatible extension method executed during validation phase.
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.Validation}
     * Since: CDI 4.0
     */
    VALIDATION(jakarta.enterprise.inject.build.compatible.spi.Validation.class),

    /**
     * Declares that a build compatible extension should be ignored when a given
     * portable extension is present.
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.SkipIfPortableExtensionPresent}
     * Since: CDI 4.0
     */
    SKIP_IF_PORTABLE_EXTENSION_PRESENT(
            jakarta.enterprise.inject.build.compatible.spi.SkipIfPortableExtensionPresent.class),

    /**
     * Conditional lookup filter based on a configuration property value.
     * Maps: {@code jakarta.enterprise.inject.LookupIfProperty}
     * Since: CDI 4.0
     */
    LOOKUP_IF_PROPERTY(annotationClass("jakarta.enterprise.inject.LookupIfProperty")),

    /**
     * Conditional lookup filter based on absence or mismatch of a configuration property value.
     * Maps: {@code jakarta.enterprise.inject.LookupUnlessProperty}
     * Since: CDI 4.0
     */
    LOOKUP_UNLESS_PROPERTY(annotationClass("jakarta.enterprise.inject.LookupUnlessProperty"));

    // ==================== Implementation ====================

    private final Set<Class<? extends Annotation>> annotations;

    @SafeVarargs
    AnnotationsEnum(Class<? extends Annotation>... annotationClasses) {
        Set<Class<? extends Annotation>> resolved = new HashSet<>();
        if (annotationClasses != null) {
            for (Class<? extends Annotation> annotationClass : annotationClasses) {
                if (annotationClass != null) {
                    resolved.add(annotationClass);
                }
            }
        }
        this.annotations = Collections.unmodifiableSet(resolved);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> annotationClass(String fqcn) {
        try {
            Class<?> loaded = Class.forName(fqcn);
            if (Annotation.class.isAssignableFrom(loaded)) {
                return (Class<? extends Annotation>) loaded;
            }
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Returns the set of annotation classes for this enum value.
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
     * corresponding to this enum value.
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
     * Checks if the element has a @Nonbinding annotation (javax or jakarta).
     * Used to mark qualifier/interceptor binding annotation members that should be
     * ignored during matching.
     */
    public static boolean hasNonbindingAnnotation(AnnotatedElement element) {
        return NONBINDING.isPresent(element);
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
     * Checks if the element has a @Typed annotation (javax or jakarta).
     * Used to restrict the bean types of a bean per CDI 4.1 Section 2.2.
     */
    public static boolean hasTypedAnnotation(AnnotatedElement element) {
        return TYPED.isPresent(element);
    }

    /**
     * Checks if the element has a @Stereotype annotation (javax or jakarta).
     */
    public static boolean hasStereotypeAnnotation(AnnotatedElement element) {
        return STEREOTYPE.isPresent(element);
    }

    /**
     * Checks if the element has a @Specializes annotation (javax or jakarta).
     */
    public static boolean hasSpecializesAnnotation(AnnotatedElement element) {
        return SPECIALIZES.isPresent(element);
    }

    /**
     * Checks if the element has a @Model annotation (javax or jakarta).
     */
    public static boolean hasModelAnnotation(AnnotatedElement element) {
        return MODEL.isPresent(element);
    }

    /**
     * Checks if the element has a @New annotation (javax).
     */
    public static boolean hasNewAnnotation(AnnotatedElement element) {
        return NEW.isPresent(element);
    }

    /**
     * Checks if the element has an @Intercepted annotation (javax or jakarta).
     */
    public static boolean hasInterceptedAnnotation(AnnotatedElement element) {
        return INTERCEPTED.isPresent(element);
    }

    /**
     * Checks if the element has a @TransientReference annotation (javax or jakarta).
     */
    public static boolean hasTransientReferenceAnnotation(AnnotatedElement element) {
        return TRANSIENT_REFERENCE.isPresent(element);
    }

    /**
     * Checks if the element has a @Decorator annotation (javax or jakarta).
     */
    public static boolean hasDecoratorAnnotation(AnnotatedElement element) {
        return DECORATOR.isPresent(element);
    }

    /**
     * Checks if the element has a @Delegate annotation (javax or jakarta).
     */
    public static boolean hasDelegateAnnotation(AnnotatedElement element) {
        return DELEGATE.isPresent(element);
    }

    /**
     * Checks if the element has an @Interceptor annotation (javax or jakarta).
     */
    public static boolean hasInterceptorAnnotation(AnnotatedElement element) {
        return INTERCEPTOR.isPresent(element);
    }

    /**
     * Checks if the element has an @InterceptorBinding annotation (javax or jakarta).
     */
    public static boolean hasInterceptorBindingAnnotation(AnnotatedElement element) {
        return INTERCEPTOR_BINDING.isPresent(element);
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
     * Checks if the element has an @Initialized annotation (jakarta).
     */
    public static boolean hasInitializedAnnotation(AnnotatedElement element) {
        return INITIALIZED.isPresent(element);
    }

    /**
     * Checks if the element has a @BeforeDestroyed annotation (jakarta).
     */
    public static boolean hasBeforeDestroyedAnnotation(AnnotatedElement element) {
        return BEFORE_DESTROYED.isPresent(element);
    }

    /**
     * Checks if the element has a @Destroyed annotation (jakarta).
     */
    public static boolean hasDestroyedAnnotation(AnnotatedElement element) {
        return DESTROYED.isPresent(element);
    }

    /**
     * Checks if the element has an @Observes annotation (javax or jakarta).
     */
    public static boolean hasObservesAnnotation(AnnotatedElement element) {
        return OBSERVES.isPresent(element);
    }

    /**
     * Checks if the element has an @ObservesAsync annotation (jakarta).
     */
    public static boolean hasObservesAsyncAnnotation(AnnotatedElement element) {
        return OBSERVES_ASYNC.isPresent(element);
    }

    /**
     * Checks if the element has an @ActivateRequestContext annotation.
     */
    public static boolean hasActivateRequestContextAnnotation(AnnotatedElement element) {
        return ACTIVATE_REQUEST_CONTEXT.isPresent(element);
    }

    /**
     * Checks if the element has a @Discovery annotation (jakarta).
     */
    public static boolean hasDiscoveryAnnotation(AnnotatedElement element) {
        return DISCOVERY.isPresent(element);
    }

    /**
     * Checks if the element has an @Enhancement annotation (jakarta).
     */
    public static boolean hasEnhancementAnnotation(AnnotatedElement element) {
        return ENHANCEMENT.isPresent(element);
    }

    /**
     * Checks if the element has a @Registration annotation (jakarta).
     */
    public static boolean hasRegistrationAnnotation(AnnotatedElement element) {
        return REGISTRATION.isPresent(element);
    }

    /**
     * Checks if the element has a @Synthesis annotation (jakarta).
     */
    public static boolean hasSynthesisAnnotation(AnnotatedElement element) {
        return SYNTHESIS.isPresent(element);
    }

    /**
     * Checks if the element has a @Validation annotation (jakarta).
     */
    public static boolean hasValidationAnnotation(AnnotatedElement element) {
        return VALIDATION.isPresent(element);
    }

    /**
     * Checks if the element has a @SkipIfPortableExtensionPresent annotation (jakarta).
     */
    public static boolean hasSkipIfPortableExtensionPresentAnnotation(AnnotatedElement element) {
        return SKIP_IF_PORTABLE_EXTENSION_PRESENT.isPresent(element);
    }

    /**
     * Checks if the element has a @LookupIfProperty annotation (jakarta).
     */
    public static boolean hasLookupIfPropertyAnnotation(AnnotatedElement element) {
        return LOOKUP_IF_PROPERTY.isPresent(element);
    }

    /**
     * Checks if the element has a @LookupUnlessProperty annotation (jakarta).
     */
    public static boolean hasLookupUnlessPropertyAnnotation(AnnotatedElement element) {
        return LOOKUP_UNLESS_PROPERTY.isPresent(element);
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
