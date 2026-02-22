package com.threeamigos.common.util.interfaces.injection;

import com.threeamigos.common.util.implementations.injection.scopehandlers.ScopeHandler;
import jakarta.annotation.Nonnull;

import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Any;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * A dependency injector. Given a class, it provides an instance of that class
 * with all dependencies injected.<br/>
 * The constructor for that class must be marked with {@link Inject} and should
 * list all dependencies needed.<br/>
 * If the class is an interface or an abstract class, an implementation is provided
 * chosen from those that are available in the classpath.<br/>
 * If more than one implementation is available, all but one should be marked as
 * {@link Named}, otherwise an exception will be thrown.<br/>
 * Use {@link Named} to annotate parameters to specify alternative implementations.<br/>
 * When a parameter is an {@link Instance}, use {@link Any} to get all possible
 * implementations, omit it to retrieve the default implementation or use {@link Named}.<br/>
 * To build the class, the Injector uses reflection to instantiate the class,
 * recursively injecting the parameters with their own dependencies.<br/>
 *
 * @author Stefano Reksten
 */
public interface Injector {

    // Setup methods

    /**
     * Register a custom scope handler.
     * @param scopeAnnotation an Annotation for the scope
     * @param handler a ScopeHandler implementation
     */
    void registerScope(@Nonnull Class<? extends Annotation> scopeAnnotation, @Nonnull ScopeHandler handler);

    /**
     * To dynamically bind a type to a specific implementation given a set of qualifiers.
     * Useful if the target class cannot be annotated.
     * @param type type to resolve
     * @param qualifiers annotations to qualify the type
     * @param implementation the target class that should be used
     */
    void bind(@Nonnull Type type, @Nonnull Collection<Annotation> qualifiers, @Nonnull Class<?> implementation);

    /**
     * Enable alternative implementation for a class.
     * @param alternativeClass the class to enable alternative implementation for
     */
    void enableAlternative(@Nonnull Class<?> alternativeClass);

    // Injection methods

    <T> T inject(@Nonnull Class<T> clazz);

    <T> T inject(@Nonnull TypeLiteral<T> clazz);

    void shutdown();
}
