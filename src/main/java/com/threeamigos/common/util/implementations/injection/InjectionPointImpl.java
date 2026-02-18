package com.threeamigos.common.util.implementations.injection;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;

import com.threeamigos.common.util.implementations.injection.literals.AnyLiteral;
import com.threeamigos.common.util.implementations.injection.literals.DefaultLiteral;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of CDI InjectionPoint SPI.
 * Represents an injection point in a bean - a field, method parameter, or constructor parameter
 * that will receive an injected value.
 *
 * <p>This class tracks metadata about the injection point including:
 * <ul>
 *   <li>The type to be injected</li>
 *   <li>Qualifiers that disambiguate which bean to inject</li>
 *   <li>Whether the injection point is transient (not serializable)</li>
 *   <li>Whether it's a decorator delegate injection point</li>
 * </ul>
 */
public class InjectionPointImpl<T> implements InjectionPoint {

    private final Member member;
    private final Bean<T> bean;
    private final Type type;
    private final Set<Annotation> qualifiers = new HashSet<>();

    /**
     * True if this injection point is marked as transient (not serializable).
     * Fields marked transient won't be serialized, which affects passivation in CDI.
     */
    private final boolean isTransient;

    /**
     * True if this injection point has a @Delegate annotation (used in decorators).
     * A decorator has exactly one @Delegate injection point that receives the decorated instance.
     */
    private final boolean isDelegate;

    /**
     * Creates an injection point for a field.
     * Extracts qualifiers, checks for transient modifier, and checks for @Delegate annotation.
     *
     * @param field the field being injected
     * @param bean the bean that declares this injection point
     */
    public InjectionPointImpl(Field field, Bean<T> bean) {
        this.member = field;
        this.bean = bean;
        this.type = field.getGenericType();
        this.isTransient = java.lang.reflect.Modifier.isTransient(field.getModifiers());
        this.isDelegate = checkForDelegateAnnotation(field.getAnnotations());
        collectQualifiers(field.getAnnotations());
    }

    /**
     * Creates an injection point for a method or constructor parameter.
     * Extracts qualifiers and checks for @Delegate annotation.
     * Parameters cannot be transient (only fields can).
     *
     * @param parameter the parameter being injected
     * @param bean the bean that declares this injection point
     */
    public InjectionPointImpl(Parameter parameter, Bean<T> bean) {
        this.member = parameter.getDeclaringExecutable();
        this.bean = bean;
        this.type = parameter.getParameterizedType();
        this.isTransient = false; // Parameters cannot be transient
        this.isDelegate = checkForDelegateAnnotation(parameter.getAnnotations());
        collectQualifiers(parameter.getAnnotations());
    }

    /**
     * Collects all qualifier annotations from the injection point.
     * Per CDI spec:
     * - If no qualifiers are present, @Default is added
     * - @Any is always added (built-in qualifier that matches all beans)
     * - @Delegate is NOT a qualifier, so it's excluded from the qualifiers set
     *
     * @param annotations all annotations present on the injection point
     */
    private void collectQualifiers(Annotation[] annotations) {
        for (Annotation ann : annotations) {
            // Skip @Delegate - it's not a qualifier, it's a decorator marker
            if (ann.annotationType().getName().equals("jakarta.decorator.Delegate") ||
                ann.annotationType().getName().equals("javax.decorator.Delegate")) {
                continue;
            }

            if (hasQualifierAnnotation(ann.annotationType())) {
                qualifiers.add(ann);
            }
        }

        // CDI defaulting rules: if no qualifier present, add @Default; always include @Any
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
        qualifiers.add(new AnyLiteral());
    }

    /**
     * Checks if the injection point has a @Delegate annotation.
     * @Delegate is used in decorators to mark the injection point that receives the decorated bean.
     * Per CDI spec, a decorator must have exactly one @Delegate injection point.
     *
     * @param annotations all annotations present on the injection point
     * @return true if @Delegate annotation is present
     */
    private boolean checkForDelegateAnnotation(Annotation[] annotations) {
        for (Annotation ann : annotations) {
            // Check both jakarta and javax namespaces for backward compatibility
            if (ann.annotationType().getName().equals("jakarta.decorator.Delegate") ||
                ann.annotationType().getName().equals("javax.decorator.Delegate")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.unmodifiableSet(qualifiers);
    }

    public void addQualifier(Annotation qualifier) {
        qualifiers.add(qualifier);
    }

    @Override
    public Bean<T> getBean() {
        return bean;
    }

    @Override
    public Member getMember() {
        return member;
    }

    @Override
    public Annotated getAnnotated() {
        // TODO: Implement Annotated wrapper when needed for portable extensions
        // The Annotated interface provides access to annotations in a type-safe way
        // For now, returning null is acceptable as it's mainly used by advanced CDI features
        return null;
    }

    /**
     * Returns true if this injection point is marked with @Delegate annotation.
     * @Delegate is used exclusively in decorators to identify the injection point
     * that receives the decorated bean instance.
     *
     * <p><b>CDI Decorator Pattern:</b>
     * <pre>{@code
     * @Decorator
     * public class LoggingDecorator implements MyService {
     *     @Inject @Delegate
     *     private MyService delegate; // This injection point returns true for isDelegate()
     *
     *     public void doWork() {
     *         log("Before");
     *         delegate.doWork(); // Delegate to actual implementation
     *         log("After");
     *     }
     * }
     * }</pre>
     *
     * @return true if this injection point has @Delegate annotation
     */
    @Override
    public boolean isDelegate() {
        return isDelegate;
    }

    /**
     * Returns true if this is a transient field injection point.
     * Transient fields are not serialized, which has implications for passivation in CDI.
     *
     * <p><b>Why this matters:</b>
     * In CDI, passivating scopes (SessionScoped, ConversationScoped) require that beans
     * can be serialized. If a passivating bean has non-transient injection points of
     * non-passivation-capable dependencies, it's a deployment error.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @SessionScoped
     * public class UserSession implements Serializable {
     *     @Inject
     *     private transient Logger logger; // OK - transient, won't be serialized
     *
     *     @Inject
     *     private Database db; // ERROR if Database is not Serializable
     * }
     * }</pre>
     *
     * @return true if this is a transient field (parameters are never transient)
     */
    @Override
    public boolean isTransient() {
        return isTransient;
    }
}
