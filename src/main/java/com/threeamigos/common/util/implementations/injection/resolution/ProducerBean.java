package com.threeamigos.common.util.implementations.injection.resolution;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of Bean for producer methods and producer fields.
 * Producer beans are not directly instantiated - they are created by invoking
 * a @Produces method or accessing a @Produces field on a declaring bean instance.
 *
 * @param <T> the type produced by this producer
 * @author Stefano Reksten
 */
public class ProducerBean<T> implements Bean<T> {

    // The class that declares the producer method/field
    private final Class<?> declaringClass;

    // Either producerMethod OR producerField will be set, not both
    private final Method producerMethod;
    private final Field producerField;

    // The disposer method, if any (only for producer methods)
    private Method disposerMethod;

    // BeanAttributes
    private String name;
    private final Set<Annotation> qualifiers = new HashSet<>();
    private Class<? extends Annotation> scope;
    private final Set<Class<? extends Annotation>> stereotypes = new HashSet<>();
    private final Set<Type> types = new HashSet<>();
    private final boolean alternative;
    private Integer priority; // @Priority value when the alternative is enabled
    private jakarta.enterprise.inject.spi.InjectionTarget<T> customInjectionTarget;

    // Injection points (for producer method parameters)
    private final Set<InjectionPoint> injectionPoints = new HashSet<>();

    // Validation state
    private boolean hasValidationErrors = false;

    // Extension veto state
    private boolean vetoed = false;

    // Reference to dependency resolver (will be set during initialization)
    private DependencyResolver dependencyResolver;

    /**
     * Constructor for producer method bean.
     */
    public ProducerBean(Class<?> declaringClass, Method producerMethod, boolean alternative) {
        this.declaringClass = declaringClass;
        this.producerMethod = producerMethod;
        this.producerField = null;
        this.alternative = alternative;
        this.scope = Dependent.class; // Default scope
    }

    /**
     * Constructor for producer field bean.
     */
    public ProducerBean(Class<?> declaringClass, Field producerField, boolean alternative) {
        this.declaringClass = declaringClass;
        this.producerMethod = null;
        this.producerField = producerField;
        this.alternative = alternative;
        this.scope = Dependent.class; // Default scope
    }

    @Override
    public Class<?> getBeanClass() {
        return declaringClass;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.unmodifiableSet(injectionPoints);
    }

    public void addInjectionPoint(InjectionPoint injectionPoint) {
        injectionPoints.add(injectionPoint);
    }

    public void replaceInjectionPoint(InjectionPoint oldIp, InjectionPoint newIp) {
        if (oldIp != null) {
            injectionPoints.remove(oldIp);
        }
        if (newIp != null) {
            injectionPoints.add(newIp);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name == null) ? "" : name;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.unmodifiableSet(qualifiers);
    }

    public void setQualifiers(Set<Annotation> qualifiers) {
        this.qualifiers.clear();
        if (qualifiers != null) {
            this.qualifiers.addAll(qualifiers);
        }
    }

    public void addQualifier(Annotation qualifier) {
        if (qualifier != null) {
            this.qualifiers.add(qualifier);
        }
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    public void setScope(Class<? extends Annotation> scope) {
        this.scope = scope;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.unmodifiableSet(stereotypes);
    }

    public void setStereotypes(Set<Class<? extends Annotation>> stereotypes) {
        this.stereotypes.clear();
        if (stereotypes != null) {
            this.stereotypes.addAll(stereotypes);
        }
    }

    @Override
    public Set<Type> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    public void setTypes(Set<Type> types) {
        this.types.clear();
        if (types != null) {
            this.types.addAll(types);
        }
    }

    @Override
    public boolean isAlternative() {
        return alternative;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getPriority() {
        return priority;
    }

    // Accessors are provided later in the class; keep single-source of truth to avoid duplicates

    @Override
    public T create(CreationalContext<T> creationalContext) {
        try {
            if (dependencyResolver == null) {
                throw new IllegalStateException(
                    "ProducerBean dependency resolver not set. " +
                    "This should be set during container initialization."
                );
            }

            // 1. Get or create the declaring bean instance
            Object declaringInstance = dependencyResolver.resolveDeclaringBeanInstance(declaringClass);

            // 2. Invoke producer method or access producer field
            if (producerMethod != null) {
                return invokeProducerMethod(declaringInstance, creationalContext);
            } else if (producerField != null) {
                return accessProducerField(declaringInstance);
            } else {
                throw new IllegalStateException("ProducerBean has neither method nor field");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance from producer", e);
        }
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        if (instance == null) {
            return;
        }

        try {
            // Invoke the disposer method if present
            if (disposerMethod != null) {
                invokeDisposerMethod(instance);
            }

            // Release CreationalContext
            if (creationalContext != null) {
                creationalContext.release();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to destroy producer instance", e);
        }
    }

    /**
     * Invokes the producer method to create an instance.
     */
    @SuppressWarnings("unchecked")
    private T invokeProducerMethod(Object declaringInstance, CreationalContext<T> creationalContext) throws Exception {
        producerMethod.setAccessible(true);

        // Resolve method parameters
        Parameter[] parameters = producerMethod.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            args[i] = dependencyResolver.resolve(
                parameters[i].getParameterizedType(),
                parameters[i].getAnnotations()
            );
        }

        return (T) producerMethod.invoke(declaringInstance, args);
    }

    /**
     * Accesses the producer field to get an instance.
     */
    @SuppressWarnings("unchecked")
    private T accessProducerField(Object declaringInstance) throws Exception {
        producerField.setAccessible(true);
        return (T) producerField.get(declaringInstance);
    }

    /**
     * Invokes the disposer method to destroy an instance.
     */
    private void invokeDisposerMethod(T instance) throws Exception {
        if (disposerMethod == null) {
            return;
        }

        disposerMethod.setAccessible(true);

        // Get declaring bean instance
        Object declaringInstance = dependencyResolver.resolveDeclaringBeanInstance(declaringClass);

        // Resolve disposer method parameters
        Parameter[] parameters = disposerMethod.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            // The @Disposes parameter gets the instance being disposed
            if (hasDisposesAnnotation(parameters[i])) {
                args[i] = instance;
            } else {
                // Other parameters are normal injection points
                args[i] = dependencyResolver.resolve(
                    parameters[i].getParameterizedType(),
                    parameters[i].getAnnotations()
                );
            }
        }

        disposerMethod.invoke(declaringInstance, args);
    }

    // Getters and setters

    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    public Method getProducerMethod() {
        return producerMethod;
    }

    public Field getProducerField() {
        return producerField;
    }

    public boolean isMethod() {
        return producerMethod != null;
    }

    public boolean isField() {
        return producerField != null;
    }

    public Method getDisposerMethod() {
        return disposerMethod;
    }

    public void setDisposerMethod(Method disposerMethod) {
        this.disposerMethod = disposerMethod;
    }

    public boolean hasValidationErrors() {
        return hasValidationErrors;
    }

    public void setHasValidationErrors(boolean hasValidationErrors) {
        this.hasValidationErrors = hasValidationErrors;
    }

    /**
     * Returns true if this producer bean was vetoed by an extension.
     * Vetoed beans should not be available for injection.
     */
    public boolean isVetoed() {
        return vetoed;
    }

    /**
     * Marks this producer bean as vetoed by an extension.
     */
    public void setVetoed(boolean vetoed) {
        this.vetoed = vetoed;
    }

    public void setDependencyResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    /**
     * Interface for resolving dependencies during producer invocation.
     * This will be implemented by the container to provide dependency lookup.
     */
    public interface DependencyResolver {
        /**
         * Resolves a dependency by type and qualifiers.
         */
        Object resolve(Type type, Annotation[] qualifiers);

        /**
         * Gets or creates an instance of the declaring bean.
         */
        Object resolveDeclaringBeanInstance(Class<?> declaringClass);
    }
}
