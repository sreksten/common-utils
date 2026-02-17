package com.threeamigos.common.util.implementations.injection;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class BeanImpl<T> implements Bean<T> {

    // Bean
    private final Class<T> beanClass;
    private final Set<InjectionPoint> injectionPoints = new HashSet<>();

    // BeanAttributes
    private String name;
    private final Set<Annotation> qualifiers = new HashSet<>();
    private Class<? extends Annotation> scope;
    private final Set<Class<? extends Annotation>> stereotypes = new HashSet<>();
    private final Set<Type> types = new HashSet<>();
    private final boolean alternative;

    // Validation state
    private boolean hasValidationErrors = false;

    // Injection metadata (set by validator)
    private Constructor<T> injectConstructor;
    private final Set<Field> injectFields = new HashSet<>();
    private final Set<Method> injectMethods = new HashSet<>();
    private Method postConstructMethod;
    private Method preDestroyMethod;

    // Dependency resolver (set during container initialization)
    private ProducerBean.DependencyResolver dependencyResolver;

    public BeanImpl(Class<T> beanClass, boolean alternative) {
        this.beanClass = beanClass;
        this.name = "";
        this.scope = null;
        this.alternative = alternative;
    }

    @Override
    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.unmodifiableSet(injectionPoints);
    }

    public void addInjectionPoint(InjectionPoint injectionPoint) {
        injectionPoints.add(injectionPoint);
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

    public void addStereotype(Class<? extends Annotation> stereotype) {
        if (stereotype != null) {
            this.stereotypes.add(stereotype);
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

    public void addType(Type type) {
        if (type != null) {
            this.types.add(type);
        }
    }

    @Override
    public boolean isAlternative() {
        return alternative;
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        try {
            // 1. Create instance via constructor injection
            T instance = createInstance(creationalContext);

            // 2. Perform field injection
            performFieldInjection(instance, creationalContext);

            // 3. Perform method injection (initializer methods)
            performMethodInjection(instance, creationalContext);

            // 4. Call @PostConstruct if present
            invokePostConstruct(instance);

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean instance of " + beanClass.getName(), e);
        }
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        if (instance == null) {
            return;
        }

        try {
            // 1. Call @PreDestroy if present
            invokePreDestroy(instance);

            // 2. Release CreationalContext (destroys dependent objects)
            if (creationalContext != null) {
                creationalContext.release();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to destroy bean instance of " + beanClass.getName(), e);
        }
    }

    /**
     * Creates instance via constructor injection.
     * Uses @Inject constructor if present, otherwise uses no-args constructor.
     */
    private T createInstance(CreationalContext<T> creationalContext) throws Exception {
        Constructor<T> constructor = injectConstructor;

        if (constructor == null) {
            // Use no-args constructor
            constructor = beanClass.getDeclaredConstructor();
        }

        constructor.setAccessible(true);

        // Resolve constructor parameters
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            args[i] = resolveInjectionPoint(parameters[i].getParameterizedType(),
                    parameters[i].getAnnotations(), creationalContext);
        }

        return constructor.newInstance(args);
    }

    /**
     * Performs field injection for all @Inject fields.
     * Processes fields in hierarchy order (superclass → subclass) per CDI 4.1 spec.
     */
    private void performFieldInjection(T instance, CreationalContext<T> creationalContext) throws Exception {
        // Build hierarchy: superclass first, then subclass
        List<Class<?>> hierarchy = LifecycleMethodHelper.buildHierarchy(instance);

        // Inject fields in hierarchy order (parent → child)
        for (Class<?> clazz : hierarchy) {
            for (Field field : injectFields) {
                // Only process fields declared by this specific class in the hierarchy
                if (!field.getDeclaringClass().equals(clazz)) {
                    continue;
                }

                field.setAccessible(true);
                Object value = resolveInjectionPoint(field.getGenericType(),
                        field.getAnnotations(), creationalContext);
                field.set(instance, value);
            }
        }
    }

    /**
     * Performs method injection for all @Inject methods.
     * Processes methods in hierarchy order (superclass → subclass) per CDI 4.1 spec.
     * Skips overridden methods per JSR-330 rules.
     */
    private void performMethodInjection(T instance, CreationalContext<T> creationalContext) throws Exception {
        // Build hierarchy: superclass first, then subclass
        List<Class<?>> hierarchy = LifecycleMethodHelper.buildHierarchy(instance);

        // Inject methods in hierarchy order (parent → child)
        for (Class<?> clazz : hierarchy) {
            for (Method method : injectMethods) {
                // Only process methods declared by this specific class in the hierarchy
                if (!method.getDeclaringClass().equals(clazz)) {
                    continue;
                }

                // Skip overridden methods per JSR-330 (method already processed in parent)
                if (isOverridden(method, instance.getClass())) {
                    continue;
                }

                method.setAccessible(true);
                Parameter[] parameters = method.getParameters();
                Object[] args = new Object[parameters.length];

                for (int i = 0; i < parameters.length; i++) {
                    args[i] = resolveInjectionPoint(parameters[i].getParameterizedType(),
                            parameters[i].getAnnotations(), creationalContext);
                }

                method.invoke(instance, args);
            }
        }
    }

    /**
     * Checks if a method is overridden in a subclass (JSR-330 rules).
     * Private methods are never considered overridden.
     * Package-private methods must be in the same package to be overridden.
     */
    private boolean isOverridden(Method superMethod, Class<?> leafClass) {
        if (Modifier.isPrivate(superMethod.getModifiers())) {
            return false;
        }
        if (superMethod.getDeclaringClass().equals(leafClass)) {
            return false;
        }

        // Search for the method in the leaf class hierarchy
        Class<?> current = leafClass;
        while (current != null && current != superMethod.getDeclaringClass()) {
            try {
                Method subMethod = current.getDeclaredMethod(superMethod.getName(), superMethod.getParameterTypes());
                if (!subMethod.equals(superMethod)) {
                    // Check package-private rules
                    boolean isSuperPackagePrivate = !Modifier.isPublic(superMethod.getModifiers()) &&
                            !Modifier.isProtected(superMethod.getModifiers()) &&
                            !Modifier.isPrivate(superMethod.getModifiers());

                    if (isSuperPackagePrivate) {
                        // Package-private method is only overridden if in same package
                        String superPackage = superMethod.getDeclaringClass().getPackage() != null ?
                                superMethod.getDeclaringClass().getPackage().getName() : "";
                        String subPackage = subMethod.getDeclaringClass().getPackage() != null ?
                                subMethod.getDeclaringClass().getPackage().getName() : "";
                        return superPackage.equals(subPackage);
                    }

                    return true; // Method is overridden
                }
            } catch (NoSuchMethodException e) {
                // Method not found in this class, continue to superclass
            }
            current = current.getSuperclass();
        }

        return false;
    }

    /**
     * Invokes @PostConstruct method if present.
     */
    private void invokePostConstruct(T instance) throws Exception {
        if (postConstructMethod != null) {
            postConstructMethod.setAccessible(true);
            postConstructMethod.invoke(instance);
        }
    }

    /**
     * Invokes @PreDestroy method if present.
     */
    private void invokePreDestroy(T instance) throws Exception {
        if (preDestroyMethod != null) {
            preDestroyMethod.setAccessible(true);
            preDestroyMethod.invoke(instance);
        }
    }

    /**
     * Resolves an injection point by finding a matching bean from the knowledge base.
     *
     * @param type the required type
     * @param annotations the qualifier annotations
     * @param creationalContext the creational context
     * @return the resolved bean instance
     */
    private Object resolveInjectionPoint(Type type, Annotation[] annotations,
                                         CreationalContext<T> creationalContext) {
        if (dependencyResolver == null) {
            throw new IllegalStateException(
                "BeanImpl dependency resolver not set. " +
                "This should be set during container initialization for bean: " + beanClass.getName()
            );
        }

        return dependencyResolver.resolve(type, annotations);
    }

    /**
     * Returns whether this bean has validation errors.
     * A bean with validation errors should not be used for dependency resolution,
     * but the error should only be reported if the bean is actually needed.
     *
     * @return true if the bean has validation errors, false otherwise
     */
    public boolean hasValidationErrors() {
        return hasValidationErrors;
    }

    /**
     * Marks this bean as having validation errors.
     * This should be called during validation if any CDI constraint is violated.
     */
    public void setHasValidationErrors(boolean hasValidationErrors) {
        this.hasValidationErrors = hasValidationErrors;
    }

    // Injection metadata getters/setters (used by CDI41BeanValidator)

    public Constructor<T> getInjectConstructor() {
        return injectConstructor;
    }

    public void setInjectConstructor(Constructor<T> injectConstructor) {
        this.injectConstructor = injectConstructor;
    }

    public Set<Field> getInjectFields() {
        return Collections.unmodifiableSet(injectFields);
    }

    public void addInjectField(Field field) {
        if (field != null) {
            this.injectFields.add(field);
        }
    }

    public Set<Method> getInjectMethods() {
        return Collections.unmodifiableSet(injectMethods);
    }

    public void addInjectMethod(Method method) {
        if (method != null) {
            this.injectMethods.add(method);
        }
    }

    public Method getPostConstructMethod() {
        return postConstructMethod;
    }

    public void setPostConstructMethod(Method postConstructMethod) {
        this.postConstructMethod = postConstructMethod;
    }

    public Method getPreDestroyMethod() {
        return preDestroyMethod;
    }

    public void setPreDestroyMethod(Method preDestroyMethod) {
        this.preDestroyMethod = preDestroyMethod;
    }

    public void setDependencyResolver(ProducerBean.DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }
}