package com.threeamigos.common.util.interfaces.injection;

import javax.enterprise.util.TypeLiteral;
import javax.inject.Named;
import javax.inject.Inject;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Any;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * A dependency injector. Given a class, it provides an instance of that class
 * with all dependencies injected.<br/>
 * The constructor for that class must be marked with {@link Inject} and should
 * list all dependencies needed (no method or instance injection at the moment).<br/>
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

    void registerScope(Class<? extends Annotation> scopeAnnotation, ScopeHandler handler);

    void enableAlternative(Class<?> alternativeClass);

    /**
     * To dynamically bind a type to a specific implementation given a set of qualifiers.
     * Useful if the target class cannot be annotated.
     * @param type type to resolve
     * @param qualifiers annotations to qualify the type
     * @param implementation the target class that should be used
     */
    void bind(Type type, Collection<Annotation> qualifiers, Class<?> implementation);

    <T> T inject(Class<T> clazz);

    <T> T inject(TypeLiteral<T> clazz);
}
