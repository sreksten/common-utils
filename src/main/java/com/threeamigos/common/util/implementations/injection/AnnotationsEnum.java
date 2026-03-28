package com.threeamigos.common.util.implementations.injection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
     * Identifies injectable constructors, methods, and fields.<br/>
     * Maps: {@code javax.inject.Inject}, {@code jakarta.inject.Inject}<br/>
     * Since: JSR-330
     */
    INJECT(javax.inject.Inject.class, jakarta.inject.Inject.class),

    /**
     * Identifies a type that the injector only instantiates once.<br/>
     * Maps: {@code javax.inject.Singleton}, {@code jakarta.inject.Singleton}<br/>
     * Since: JSR-330
     */
    SINGLETON(javax.inject.Singleton.class, jakarta.inject.Singleton.class),

    /**
     * String-based qualifier annotation.<br/>
     * Maps: {@code javax.inject.Named}, {@code jakarta.inject.Named}<br/>
     * Since: JSR-330
     */
    NAMED(javax.inject.Named.class, jakarta.inject.Named.class),

    /**
     * Identifies qualifier annotations (meta-annotation).<br/>
     * Maps: {@code javax.inject.Qualifier}, {@code jakarta.inject.Qualifier}<br/>
     * Since: JSR-330
     */
    QUALIFIER(javax.inject.Qualifier.class, jakarta.inject.Qualifier.class),

    /**
     * Identifies scope annotations (meta-annotation).<br/>
     * Maps: {@code javax.inject.Scope}, {@code jakarta.inject.Scope}<br/>
     * Since: JSR-330
     */
    SCOPE(javax.inject.Scope.class, jakarta.inject.Scope.class),

    // ==================== CDI Qualifier/Binding Annotations ====================

    /**
     * Marks qualifier or interceptor binding annotation members that should be ignored during matching.
     * When comparing two qualifier or interceptor binding annotations, members marked with
     * {@code @Nonbinding} are not considered in the equality check.<br/>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @Qualifier
     * @Retention(RUNTIME)
     * @Target({ FIELD, TYPE, METHOD, PARAMETER ])
     * public @interface PayBy {
     *     PaymentMethod value(); // Considered during matching
     *     @Nonbinding String description() default ""; // Ignored during matching
     * }
     * }</pre>
     *
     * Maps: {@code javax.enterprise.util.Nonbinding}, {@code jakarta.enterprise.util.Nonbinding}<br/>
     * Since: CDI 1.0
     */
    NONBINDING(javax.enterprise.util.Nonbinding.class, jakarta.enterprise.util.Nonbinding.class),

    // ==================== JSR-250 Annotations ====================

    /**
     * Lifecycle callback executed after dependency injection.<br/>
     * Maps: {@code javax.annotation.PostConstruct}, {@code jakarta.annotation.PostConstruct}<br/>
     * Since: Common Annotations 1.0 (used by CDI since CDI 1.0)
     */
    POST_CONSTRUCT(javax.annotation.PostConstruct.class, jakarta.annotation.PostConstruct.class),

    /**
     * Lifecycle callback executed before destruction.<br/>
     * Maps: {@code javax.annotation.PreDestroy}, {@code jakarta.annotation.PreDestroy}<br/>
     * Since: Common Annotations 1.0 (used by CDI since CDI 1.0)
     */
    PRE_DESTROY(javax.annotation.PreDestroy.class, jakarta.annotation.PreDestroy.class),

    /**
     * Priority annotation for ordering.<br/>
     * Maps: {@code javax.annotation.Priority}, {@code jakarta.annotation.Priority}<br/>
     * Since: Common Annotations 1.2 (used by CDI since CDI 1.1)
     */
    PRIORITY(javax.annotation.Priority.class, jakarta.annotation.Priority.class),

    // ==================== CDI Annotations ====================

    /**
     * Marks a bean as an alternative implementation.<br/>
     * Maps: {@code javax.enterprise.inject.Alternative}, {@code jakarta.enterprise.inject.Alternative}<br/>
     * Since: CDI 1.0
     */
    ALTERNATIVE(javax.enterprise.inject.Alternative.class, jakarta.enterprise.inject.Alternative.class),

    /**
     * Built-in qualifier that matches all beans.<br/>
     * Maps: {@code javax.enterprise.inject.Any}, {@code jakarta.enterprise.inject.Any}<br/>
     * Since: CDI 1.0
     */
    ANY(javax.enterprise.inject.Any.class, jakarta.enterprise.inject.Any.class),

    /**
     * The default qualifier applied when no other qualifier is present.<br/>
     * Maps: {@code javax.enterprise.inject.Default}, {@code jakarta.enterprise.inject.Default}<br/>
     * Since: CDI 1.0
     */
    DEFAULT(javax.enterprise.inject.Default.class, jakarta.enterprise.inject.Default.class),

    /**
     * Marks a producer method or field.<br/>
     * Maps: {@code javax.enterprise.inject.Produces}, {@code jakarta.enterprise.inject.Produces}<br/>
     * Since: CDI 1.0
     */
    PRODUCES(javax.enterprise.inject.Produces.class, jakarta.enterprise.inject.Produces.class),

    /**
     * Marks a disposer method parameter.<br/>
     * Maps: {@code javax.enterprise.inject.Disposes}, {@code jakarta.enterprise.inject.Disposes}<br/>
     * Since: CDI 1.0
     */
    DISPOSES(javax.enterprise.inject.Disposes.class, jakarta.enterprise.inject.Disposes.class),

    /**
     * Marks a class that should not be discovered as a bean.<br/>
     * Maps: {@code javax.enterprise.inject.Vetoed}, {@code jakarta.enterprise.inject.Vetoed}<br/>
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
     * Maps: {@code javax.enterprise.inject.Typed}, {@code jakarta.enterprise.inject.Typed}<br/>
     * Since: CDI 1.0
     */
    TYPED(javax.enterprise.inject.Typed.class, jakarta.enterprise.inject.Typed.class),

    /**
     * Identifies a stereotype annotation (meta-annotation).<br/>
     * Maps: {@code javax.enterprise.inject.Stereotype}, {@code jakarta.enterprise.inject.Stereotype}<br/>
     * Since: CDI 1.0
     */
    STEREOTYPE(javax.enterprise.inject.Stereotype.class, jakarta.enterprise.inject.Stereotype.class),

    /**
     * Indicates that a bean specializes another bean.<br/>
     * Maps: {@code javax.enterprise.inject.Specializes}, {@code jakarta.enterprise.inject.Specializes}<br/>
     * Since: CDI 1.0
     */
    SPECIALIZES(javax.enterprise.inject.Specializes.class, jakarta.enterprise.inject.Specializes.class),

    /**
     * Built-in stereotype that combines {@code @Named} and {@code @RequestScoped}.<br/>
     * Maps: {@code javax.enterprise.inject.Model}, {@code jakarta.enterprise.inject.Model}<br/>
     * Since: CDI 1.0
     */
    MODEL(javax.enterprise.inject.Model.class, jakarta.enterprise.inject.Model.class),

    /**
     * Deprecated qualifier indicating injection of a new instance of a bean.<br/>
     * Maps: {@code javax.enterprise.inject.New}<br/>
     * Since: CDI 1.0 (deprecated in CDI 1.1, removed from Jakarta CDI)
     */
    NEW(javax.enterprise.inject.New.class),

    /**
     * Built-in qualifier for getting bean metadata in interceptors.<br/>
     * Maps: {@code javax.enterprise.inject.Intercepted}, {@code jakarta.enterprise.inject.Intercepted}<br/>
     * Since: CDI 1.1
     */
    INTERCEPTED(javax.enterprise.inject.Intercepted.class, jakarta.enterprise.inject.Intercepted.class),

    /**
     * Annotation for transient method or constructor parameter references.<br/>
     * Maps: {@code javax.enterprise.inject.TransientReference}, {@code jakarta.enterprise.inject.TransientReference}<br/>
     * Since: CDI 2.0
     */
    TRANSIENT_REFERENCE(javax.enterprise.inject.TransientReference.class, jakarta.enterprise.inject.TransientReference.class),

    /**
     * Identifies a decorator bean.<br/>
     * Maps: {@code javax.decorator.Decorator}, {@code jakarta.decorator.Decorator}<br/>
     * Since: CDI 1.0
     */
    DECORATOR(javax.decorator.Decorator.class, jakarta.decorator.Decorator.class),

    /**
     * Marks the delegate injection point in a decorator.<br/>
     * Maps: {@code javax.decorator.Delegate}, {@code jakarta.decorator.Delegate}<br/>
     * Since: CDI 1.0
     */
    DELEGATE(javax.decorator.Delegate.class, jakarta.decorator.Delegate.class),

    /**
     * Identifies an interceptor bean.<br/>
     * Maps: {@code javax.interceptor.Interceptor}, {@code jakarta.interceptor.Interceptor}<br/>
     * Since: CDI 1.0
     */
    INTERCEPTOR(javax.interceptor.Interceptor.class, jakarta.interceptor.Interceptor.class),

    /**
     * Identifies interceptor-binding annotations (meta-annotation).<br/>
     * Maps: {@code javax.interceptor.InterceptorBinding}, {@code jakarta.interceptor.InterceptorBinding}<br/>
     * Since: CDI 1.0
     */
    INTERCEPTOR_BINDING(javax.interceptor.InterceptorBinding.class, jakarta.interceptor.InterceptorBinding.class),

    // ==================== CDI Scope Annotations ====================

    /**
     * Dependent pseudo-scope (new instance per injection point).<br/>
     * Maps: {@code javax.enterprise.context.Dependent}, {@code jakarta.enterprise.context.Dependent}<br/>
     * Since: CDI 1.0
     */
    DEPENDENT(javax.enterprise.context.Dependent.class, jakarta.enterprise.context.Dependent.class),

    /**
     * Application scope (one instance per application).<br/>
     * Maps: {@code javax.enterprise.context.ApplicationScoped}, {@code jakarta.enterprise.context.ApplicationScoped}<br/>
     * Since: CDI 1.0
     */
    APPLICATION_SCOPED(javax.enterprise.context.ApplicationScoped.class, jakarta.enterprise.context.ApplicationScoped.class),

    /**
     * Request scope (one instance per HTTP request).<br/>
     * Maps: {@code javax.enterprise.context.RequestScoped}, {@code jakarta.enterprise.context.RequestScoped}<br/>
     * Since: CDI 1.0
     */
    REQUEST_SCOPED(javax.enterprise.context.RequestScoped.class, jakarta.enterprise.context.RequestScoped.class),

    /**
     * Session scope (one instance per HTTP session).<br/>
     * Maps: {@code javax.enterprise.context.SessionScoped}, {@code jakarta.enterprise.context.SessionScoped}<br/>
     * Since: CDI 1.0
     */
    SESSION_SCOPED(javax.enterprise.context.SessionScoped.class, jakarta.enterprise.context.SessionScoped.class),

    /**
     * Conversation scope (one instance per conversation).<br/>
     * Maps: {@code javax.enterprise.context.ConversationScoped}, {@code jakarta.enterprise.context.ConversationScoped}<br/>
     * Since: CDI 1.0
     */
    CONVERSATION_SCOPED(javax.enterprise.context.ConversationScoped.class, jakarta.enterprise.context.ConversationScoped.class),

    /**
     * Normal scope meta-annotation.<br/>
     * Maps: {@code javax.enterprise.context.NormalScope}, {@code jakarta.enterprise.context.NormalScope}<br/>
     * Since: CDI 1.0
     */
    NORMAL_SCOPE(javax.enterprise.context.NormalScope.class, jakarta.enterprise.context.NormalScope.class),

    // ==================== CDI Event Annotations ====================

    /**
     * Qualifier for context-initialized lifecycle events.<br/>
     * Maps: {@code javax.enterprise.context.Initialized}, {@code jakarta.enterprise.context.Initialized}<br/>
     * Since: CDI 1.1
     */
    INITIALIZED(javax.enterprise.context.Initialized.class, jakarta.enterprise.context.Initialized.class),

    /**
     * Qualifier for context before-destroyed lifecycle events.<br/>
     * Maps: {@code javax.enterprise.context.BeforeDestroyed}, {@code jakarta.enterprise.context.BeforeDestroyed}<br/>
     * Since: CDI 1.1
     */
    BEFORE_DESTROYED(annotationClass("javax.enterprise.context.BeforeDestroyed"),
            jakarta.enterprise.context.BeforeDestroyed.class),

    /**
     * Qualifier for context destroyed lifecycle events.<br/>
     * Maps: {@code javax.enterprise.context.Destroyed}, {@code jakarta.enterprise.context.Destroyed}<br/>
     * Since: CDI 1.1
     */
    DESTROYED(javax.enterprise.context.Destroyed.class, jakarta.enterprise.context.Destroyed.class),

    /**
     * Marks a method parameter that observes CDI events (synchronous).<br/>
     * Maps: {@code javax.enterprise.event.Observes}, {@code jakarta.enterprise.event.Observes}<br/>
     * Since: CDI 1.0
     */
    OBSERVES(javax.enterprise.event.Observes.class, jakarta.enterprise.event.Observes.class),

    /**
     * Marks a method parameter that observes CDI events (asynchronous).<br/>
     * Maps: {@code javax.enterprise.event.ObservesAsync}, {@code jakarta.enterprise.event.ObservesAsync}<br/>
     * Since: CDI 2.0
     */
    OBSERVES_ASYNC(annotationClass("javax.enterprise.event.ObservesAsync"),
            jakarta.enterprise.event.ObservesAsync.class),

    /**
     * Interceptor method invoked around business method invocation.<br/>
     * Maps: {@code javax.interceptor.AroundInvoke}, {@code jakarta.interceptor.AroundInvoke}<br/>
     * Since: Interceptors 1.0 (used by CDI since CDI 1.0)
     */
    AROUND_INVOKE(javax.interceptor.AroundInvoke.class, jakarta.interceptor.AroundInvoke.class),

    /**
     * Interceptor method invoked around constructor invocation.<br/>
     * Maps: {@code javax.interceptor.AroundConstruct}, {@code jakarta.interceptor.AroundConstruct}<br/>
     * Since: Interceptors 1.2 (used by CDI since CDI 1.1)
     */
    AROUND_CONSTRUCT(javax.interceptor.AroundConstruct.class, jakarta.interceptor.AroundConstruct.class),

    /**
     * Restricts ProcessAnnotatedType observer delivery to types containing at least one of the specified annotations.<br/>
     * Maps: {@code javax.enterprise.inject.spi.WithAnnotations}, {@code jakarta.enterprise.inject.spi.WithAnnotations}<br/>
     * Since: CDI 1.1
     */
    WITH_ANNOTATIONS(javax.enterprise.inject.spi.WithAnnotations.class,
            jakarta.enterprise.inject.spi.WithAnnotations.class),

    /**
     * Interceptor binding that activates request context for a method invocation.<br/>
     * Maps: {@code javax.enterprise.context.control.ActivateRequestContext},
     * {@code jakarta.enterprise.context.control.ActivateRequestContext}<br/>
     * Since: CDI 2.0
     */
    ACTIVATE_REQUEST_CONTEXT(annotationClass("javax.enterprise.context.control.ActivateRequestContext"),
            jakarta.enterprise.context.control.ActivateRequestContext.class),

    // ==================== Java Meta-Annotations ====================

    /**
     * Marks an annotation as inherited by subclasses.<br/>
     * Maps: {@code java.lang.annotation.Inherited}<br/>
     * Since: Java 5
     */
    INHERITED(java.lang.annotation.Inherited.class),

    /**
     * Specifies valid declaration targets for an annotation.<br/>
     * Maps: {@code java.lang.annotation.Target}<br/>
     * Since: Java 5
     */
    TARGET(java.lang.annotation.Target.class),

    /**
     * Marks an annotation as repeatable.<br/>
     * Maps: {@code java.lang.annotation.Repeatable}<br/>
     * Since: Java 8
     */
    REPEATABLE(java.lang.annotation.Repeatable.class),

    /**
     * Declares a build compatible extension method executed during discovery phase.<br/>
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.Discovery}<br/>
     * Since: CDI 4.0
     */
    DISCOVERY(jakarta.enterprise.inject.build.compatible.spi.Discovery.class),

    /**
     * Declares a build compatible extension method executed during enhancement phase.<br/>
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.Enhancement}<br/>
     * Since: CDI 4.0
     */
    ENHANCEMENT(jakarta.enterprise.inject.build.compatible.spi.Enhancement.class),

    /**
     * Declares a build compatible extension method executed during registration phase.<br/>
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.Registration}<br/>
     * Since: CDI 4.0
     */
    REGISTRATION(jakarta.enterprise.inject.build.compatible.spi.Registration.class),

    /**
     * Declares a build compatible extension method executed during synthesis phase.<br/>
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.Synthesis}<br/>
     * Since: CDI 4.0
     */
    SYNTHESIS(jakarta.enterprise.inject.build.compatible.spi.Synthesis.class),

    /**
     * Declares a build compatible extension method executed during validation phase.<br/>
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.Validation}<br/>
     * Since: CDI 4.0
     */
    VALIDATION(jakarta.enterprise.inject.build.compatible.spi.Validation.class),

    /**
     * Declares that a build-compatible extension should be ignored when a given
     * portable extension is present.<br/>
     * Maps: {@code jakarta.enterprise.inject.build.compatible.spi.SkipIfPortableExtensionPresent}<br/>
     * Since: CDI 4.0
     */
    SKIP_IF_PORTABLE_EXTENSION_PRESENT(
            jakarta.enterprise.inject.build.compatible.spi.SkipIfPortableExtensionPresent.class),

    /**
     * Conditional lookup filter based on a configuration property value.<br/>
     * Maps: {@code jakarta.enterprise.inject.LookupIfProperty}<br/>
     * Since: CDI 4.0
     */
    LOOKUP_IF_PROPERTY(annotationClass("jakarta.enterprise.inject.LookupIfProperty")),

    /**
     * Conditional lookup filter based on absence or mismatch of a configuration property value.<br/>
     * Maps: {@code jakarta.enterprise.inject.LookupUnlessProperty}<br/>
     * Since: CDI 4.0
     */
    LOOKUP_UNLESS_PROPERTY(annotationClass("jakarta.enterprise.inject.LookupUnlessProperty"));

    // ==================== Implementation ====================

    private final Set<Class<? extends Annotation>> annotations;
    private static final Set<Class<? extends Annotation>> DYNAMIC_QUALIFIERS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<Class<? extends Annotation>> DYNAMIC_SCOPES =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<Class<? extends Annotation>> DYNAMIC_STEREOTYPES =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<Class<? extends Annotation>> DYNAMIC_INTERCEPTOR_BINDINGS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

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
    private static Class<? extends Annotation> annotationClass(String fullyQualifiedClassName) {
        try {
            Class<?> loaded = Class.forName(fullyQualifiedClassName);
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
        if (element instanceof Class && Annotation.class.isAssignableFrom((Class<?>) element)) {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> annotationType = (Class<? extends Annotation>) element;
            if (matches(annotationType)) {
                return true;
            }
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
        return QUALIFIER.isPresent(element) || matchesDynamicAnnotation(element, DYNAMIC_QUALIFIERS);
    }

    /**
     * Checks if the element has a @Scope annotation (javax or jakarta).
     */
    public static boolean hasScopeAnnotation(AnnotatedElement element) {
        return SCOPE.isPresent(element) || matchesDynamicAnnotation(element, DYNAMIC_SCOPES);
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
        return STEREOTYPE.isPresent(element) || matchesDynamicAnnotation(element, DYNAMIC_STEREOTYPES);
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
        return INTERCEPTOR_BINDING.isPresent(element) || matchesDynamicAnnotation(element, DYNAMIC_INTERCEPTOR_BINDINGS);
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
     * Checks if the element has one of the built-in normal scope annotations:
     * {@code @ApplicationScoped}, {@code @RequestScoped}, {@code @SessionScoped},
     * or {@code @ConversationScoped} (javax or jakarta).
     */
    public static boolean hasBuiltInNormalScopeAnnotation(AnnotatedElement element) {
        return hasApplicationScopedAnnotation(element) ||
                hasRequestScopedAnnotation(element) ||
                hasSessionScopedAnnotation(element) ||
                hasConversationScopedAnnotation(element);
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
     * Checks if the element has an @AroundInvoke annotation (javax or jakarta).
     */
    public static boolean hasAroundInvokeAnnotation(AnnotatedElement element) {
        return AROUND_INVOKE.isPresent(element);
    }

    /**
     * Checks if the element has an @AroundConstruct annotation (javax or jakarta).
     */
    public static boolean hasAroundConstructAnnotation(AnnotatedElement element) {
        return AROUND_CONSTRUCT.isPresent(element);
    }

    /**
     * Checks if the element has a @WithAnnotations annotation (javax or jakarta).
     */
    public static boolean hasWithAnnotationsAnnotation(AnnotatedElement element) {
        return WITH_ANNOTATIONS.isPresent(element);
    }

    /**
     * Checks if the element has an @ActivateRequestContext annotation.
     */
    public static boolean hasActivateRequestContextAnnotation(AnnotatedElement element) {
        return ACTIVATE_REQUEST_CONTEXT.isPresent(element);
    }

    /**
     * Checks if the element has @Inherited.
     */
    public static boolean hasInheritedAnnotation(AnnotatedElement element) {
        return INHERITED.isPresent(element);
    }

    /**
     * Checks if the element has @Target.
     */
    public static boolean hasTargetAnnotation(AnnotatedElement element) {
        return TARGET.isPresent(element);
    }

    /**
     * Checks if the element has @Repeatable.
     */
    public static boolean hasRepeatableAnnotation(AnnotatedElement element) {
        return REPEATABLE.isPresent(element);
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

    /**
     * Gets the @Priority annotation from the element (supports both javax and jakarta).
     * Returns null if no @Priority annotation is present.
     */
    public static <T extends Annotation> T getPriorityAnnotation(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        for (Class<? extends Annotation> annotationClass : PRIORITY.getAnnotations()) {
            @SuppressWarnings("unchecked")
            T annotation = (T) element.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Gets the @Named annotation from the element (supports both javax and jakarta).
     * Returns null if no @Named annotation is present.
     */
    public static <T extends Annotation> T getNamedAnnotation(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        for (Class<? extends Annotation> annotationClass : NAMED.getAnnotations()) {
            @SuppressWarnings("unchecked")
            T annotation = (T) element.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Gets the @Target annotation from the element.
     */
    public static Target getTargetAnnotation(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        return element.getAnnotation(Target.class);
    }

    /**
     * Gets the @Observes annotation from the element (supports both javax and jakarta).
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getObservesAnnotation(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        for (Class<? extends Annotation> annotationClass : OBSERVES.getAnnotations()) {
            T annotation = (T) element.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Gets the @ObservesAsync annotation from the element (supports both javax and jakarta).
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getObservesAsyncAnnotation(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        for (Class<? extends Annotation> annotationClass : OBSERVES_ASYNC.getAnnotations()) {
            T annotation = (T) element.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Gets the @Registration annotation from the element.
     */
    public static jakarta.enterprise.inject.build.compatible.spi.Registration getRegistrationAnnotation(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        return element.getAnnotation(jakarta.enterprise.inject.build.compatible.spi.Registration.class);
    }

    /**
     * Gets the @Enhancement annotation from the element.
     */
    public static jakarta.enterprise.inject.build.compatible.spi.Enhancement getEnhancementAnnotation(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        return element.getAnnotation(jakarta.enterprise.inject.build.compatible.spi.Enhancement.class);
    }

    /**
     * Gets the @SkipIfPortableExtensionPresent annotation from the element.
     */
    public static jakarta.enterprise.inject.build.compatible.spi.SkipIfPortableExtensionPresent
    getSkipIfPortableExtensionPresentAnnotation(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        return element.getAnnotation(jakarta.enterprise.inject.build.compatible.spi.SkipIfPortableExtensionPresent.class);
    }

    /**
     * Returns the integer value of @Priority (javax or jakarta), or null if absent.
     */
    public static Integer getPriorityValue(AnnotatedElement element) {
        Annotation priority = getPriorityAnnotation(element);
        if (priority == null) {
            return null;
        }
        try {
            Object value = priority.annotationType().getMethod("value").invoke(priority);
            return value instanceof Integer ? (Integer) value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Returns the value of @NormalScope.passivating (javax or jakarta), or null if absent.
     */
    public static Boolean getNormalScopePassivatingValue(AnnotatedElement element) {
        if (element == null) {
            return null;
        }
        Annotation normalScope = null;
        for (Class<? extends Annotation> annotationClass : NORMAL_SCOPE.getAnnotations()) {
            Annotation candidate = element.getAnnotation(annotationClass);
            if (candidate != null) {
                normalScope = candidate;
                break;
            }
        }
        if (normalScope == null) {
            return null;
        }
        try {
            Object value = normalScope.annotationType().getMethod("passivating").invoke(normalScope);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void registerDynamicQualifier(Class<? extends Annotation> qualifierType) {
        registerDynamicAnnotation(qualifierType, DYNAMIC_QUALIFIERS);
    }

    public static void registerDynamicScope(Class<? extends Annotation> scopeType) {
        registerDynamicAnnotation(scopeType, DYNAMIC_SCOPES);
    }

    public static void registerDynamicStereotype(Class<? extends Annotation> stereotypeType) {
        registerDynamicAnnotation(stereotypeType, DYNAMIC_STEREOTYPES);
    }

    public static void registerDynamicInterceptorBinding(Class<? extends Annotation> bindingType) {
        registerDynamicAnnotation(bindingType, DYNAMIC_INTERCEPTOR_BINDINGS);
    }

    private static void registerDynamicAnnotation(Class<? extends Annotation> annotationType,
                                                  Set<Class<? extends Annotation>> sink) {
        if (annotationType == null || sink == null) {
            return;
        }
        sink.add(annotationType);
    }

    private static boolean matchesDynamicAnnotation(AnnotatedElement element,
                                                    Set<Class<? extends Annotation>> dynamicSet) {
        if (!(element instanceof Class)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> annotationType = (Class<? extends Annotation>) element;
        return dynamicSet.contains(annotationType);
    }
}
