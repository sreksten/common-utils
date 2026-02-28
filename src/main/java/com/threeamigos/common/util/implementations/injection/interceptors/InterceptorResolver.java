package com.threeamigos.common.util.implementations.injection.interceptors;

import com.threeamigos.common.util.implementations.injection.AnnotationsEnum;
import com.threeamigos.common.util.implementations.injection.knowledgebase.InterceptorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.inject.spi.InterceptionType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Resolves interceptors for target beans, methods, and constructors.
 *
 * <p>This class is responsible for determining which interceptors should be applied to a given
 * bean or method based on:
 * <ul>
 *   <li>Interceptor bindings present on the target class/method</li>
 *   <li>Interceptor bindings inherited from stereotypes</li>
 *   <li>The type of interception (AROUND_INVOKE, AROUND_CONSTRUCT, POST_CONSTRUCT, PRE_DESTROY)</li>
 *   <li>Priority ordering</li>
 * </ul>
 *
 * <p><b>CDI 4.1 Interceptor Resolution Rules:</b>
 * <ul>
 *   <li>Class-level bindings apply to all business methods</li>
 *   <li>Method-level bindings override class-level bindings (not merge)</li>
 *   <li>Stereotype bindings are inherited by the bean class</li>
 *   <li>An interceptor matches if ALL of its bindings are present on the target</li>
 *   <li>Interceptors are sorted by priority (lower value = earlier execution)</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * InterceptorResolver resolver = new InterceptorResolver(knowledgeBase);
 *
 * // Resolve interceptors for a business method
 * List<InterceptorInfo> interceptors = resolver.resolve(
 *     BankAccount.class,
 *     withdrawMethod,
 *     InterceptionType.AROUND_INVOKE
 * );
 * // Returns: [TransactionalInterceptor, LoggingInterceptor, SecurityInterceptor]
 * }</pre>
 *
 * @see InterceptorInfo
 * @see KnowledgeBase
 * @see InterceptionType
 */
public class InterceptorResolver {

    private final KnowledgeBase knowledgeBase;

    /**
     * Creates an interceptor resolver.
     *
     * @param knowledgeBase the knowledge base containing interceptor metadata
     */
    public InterceptorResolver(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
    }

    /**
     * Resolves interceptors for a target bean class/method.
     *
     * <p>This is the primary resolution method that considers both class-level and method-level
     * interceptor bindings.
     *
     * <p><b>Resolution Strategy:</b>
     * <ol>
     *   <li>If method is not null and has interceptor bindings → use method bindings only</li>
     *   <li>Otherwise → use class bindings (including inherited from stereotypes)</li>
     *   <li>Query KnowledgeBase for matching interceptors</li>
     *   <li>Return sorted list (already sorted by priority)</li>
     * </ol>
     *
     * @param targetClass the bean class being intercepted
     * @param method the method being intercepted (null for constructor/lifecycle interception)
     * @param interceptionType the type of interception
     * @return sorted list of matching interceptors (can be empty, never null)
     */
    public List<InterceptorInfo> resolve(
            Class<?> targetClass,
            Method method,
            InterceptionType interceptionType) {

        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(interceptionType, "interceptionType cannot be null");

        // Determine which bindings to use
        Set<Annotation> targetBindings;

        if (method != null && hasInterceptorBindings(method)) {
            // Method-level bindings OVERRIDE class-level bindings (CDI spec)
            targetBindings = extractInterceptorBindings(method);
        } else {
            // Use class-level bindings (includes stereotype inheritance)
            targetBindings = extractInterceptorBindings(targetClass);
        }

        // If no bindings, no interceptors apply
        if (targetBindings.isEmpty()) {
            return Collections.emptyList();
        }

        // Query KnowledgeBase (already sorted by priority)
        return knowledgeBase.getInterceptorsByBindingsAndType(interceptionType, targetBindings);
    }

    /**
     * Resolves interceptors for a target class only (no method-specific bindings).
     *
     * <p>This is used for constructor interception and lifecycle callbacks, where there
     * is no method to check for method-level bindings.
     *
     * @param targetClass the bean class
     * @param interceptionType the type of interception
     * @return sorted list of matching interceptors
     */
    public List<InterceptorInfo> resolveForClass(Class<?> targetClass, InterceptionType interceptionType) {
        return resolve(targetClass, null, interceptionType);
    }

    /**
     * Extracts interceptor bindings from an annotated element (class or method).
     *
     * <p>An interceptor binding is an annotation that is itself annotated with @InterceptorBinding.
     *
     * <p>For classes, this also includes interceptor bindings inherited from stereotypes.
     *
     * @param element the annotated element (class or method)
     * @return set of interceptor binding annotations
     */
    private Set<Annotation> extractInterceptorBindings(AnnotatedElement element) {
        if (element instanceof Class) {
            return extractInterceptorBindingsFromHierarchy((Class<?>) element);
        }

        Set<Annotation> bindings = new HashSet<>();
        for (Annotation annotation : element.getAnnotations()) {
            if (isInterceptorBinding(annotation.annotationType())) {
                bindings.add(annotation);
            }
        }
        return bindings;
    }

    /**
     * Extracts interceptor bindings from a stereotype annotation.
     *
     * <p>Stereotypes can declare interceptor bindings, which are then inherited by beans
     * that use the stereotype.
     *
     * @param stereotypeClass the stereotype annotation class
     * @return set of interceptor bindings declared on the stereotype
     */
    private Set<Annotation> extractInterceptorBindingsFromStereotype(Class<? extends Annotation> stereotypeClass) {
        Set<Annotation> bindings = new HashSet<>();

        for (Annotation annotation : stereotypeClass.getAnnotations()) {
            if (isInterceptorBinding(annotation.annotationType())) {
                bindings.add(annotation);
            }

            // Handle nested stereotypes (stereotype on stereotype)
            if (AnnotationsEnum.hasStereotypeAnnotation(annotation.annotationType())) {
                bindings.addAll(extractInterceptorBindingsFromStereotype(annotation.annotationType()));
            }
        }

        return bindings;
    }

    /**
     * Collects interceptor bindings from a class and its type hierarchy (superclasses and interfaces).
     *
     * <p>CDI 4.1 specifies that interceptor bindings are inherited. This method walks the full
     * hierarchy to ensure bindings declared on generic superclasses or implemented interfaces
     * are considered during resolution.</p>
     */
    private Set<Annotation> extractInterceptorBindingsFromHierarchy(Class<?> type) {
        Set<Annotation> bindings = new HashSet<>();
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> stack = new ArrayDeque<>();
        if (type != null) {
            stack.push(type);
        }

        while (!stack.isEmpty()) {
            Class<?> current = stack.pop();
            if (current == null || !visited.add(current)) {
                continue;
            }

            for (Annotation annotation : current.getAnnotations()) {
                if (isInterceptorBinding(annotation.annotationType())) {
                    bindings.add(annotation);
                }
                if (AnnotationsEnum.hasStereotypeAnnotation(annotation.annotationType())) {
                    bindings.addAll(extractInterceptorBindingsFromStereotype(annotation.annotationType()));
                }
            }

            // Traverse superclass first (preserves natural overriding semantics)
            stack.push(current.getSuperclass());
            // Then traverse interfaces (may include generic parents)
            for (Class<?> iface : current.getInterfaces()) {
                stack.push(iface);
            }
        }

        return bindings;
    }

    /**
     * Checks if an annotated element has any interceptor bindings.
     *
     * @param element the annotated element
     * @return true if the element has at least one interceptor binding
     */
    private boolean hasInterceptorBindings(AnnotatedElement element) {
        for (Annotation annotation : element.getAnnotations()) {
            if (isInterceptorBinding(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an annotation type is an interceptor binding.
     *
     * <p>An interceptor binding is an annotation annotated with @InterceptorBinding.
     *
     * @param annotationType the annotation type to check
     * @return true if the annotation is an interceptor binding
     */
    private boolean isInterceptorBinding(Class<? extends Annotation> annotationType) {
        // Check both jakarta and javax namespaces for backward compatibility
        return annotationType.isAnnotationPresent(jakarta.interceptor.InterceptorBinding.class) ||
               annotationType.isAnnotationPresent(javax.interceptor.InterceptorBinding.class);
    }

    /**
     * Returns all interceptor binding types that are currently registered in the system.
     *
     * <p>This is useful for diagnostic purposes or for validating interceptor configurations.
     *
     * @return set of all interceptor binding annotation types
     */
    public Set<Class<? extends Annotation>> getAllInterceptorBindingTypes() {
        return knowledgeBase.getAllInterceptorBindingTypes();
    }

    /**
     * Checks if a specific class has any interceptors that would apply to it.
     *
     * <p>This does not check individual methods - only class-level bindings.
     *
     * @param targetClass the class to check
     * @param interceptionType the type of interception
     * @return true if at least one interceptor would apply
     */
    public boolean hasInterceptors(Class<?> targetClass, InterceptionType interceptionType) {
        return !resolveForClass(targetClass, interceptionType).isEmpty();
    }

    /**
     * Checks if a specific method has any interceptors that would apply to it.
     *
     * <p>This considers both method-level and class-level bindings.
     *
     * @param targetClass the class containing the method
     * @param method the method to check
     * @return true if at least one interceptor would apply
     */
    public boolean hasInterceptors(Class<?> targetClass, Method method) {
        return !resolve(targetClass, method, InterceptionType.AROUND_INVOKE).isEmpty();
    }

    @Override
    public String toString() {
        return "InterceptorResolver{" +
                "totalInterceptors=" + knowledgeBase.getInterceptorInfos().size() +
                ", bindingTypes=" + knowledgeBase.getAllInterceptorBindingTypes().size() +
                '}';
    }
}
