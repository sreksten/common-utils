package com.threeamigos.common.util.implementations.injection.resolution;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;

import com.threeamigos.common.util.implementations.injection.util.LifecycleMethodHelper;
import com.threeamigos.common.util.implementations.injection.scopes.InjectionPointImpl;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private boolean alternativeEnabled;
    private Integer priority; // @Priority value when the alternative is enabled
    private jakarta.enterprise.inject.spi.InjectionTarget<T> customInjectionTarget;

    // Injection points (for producer method parameters)
    private final Set<InjectionPoint> injectionPoints = new HashSet<>();
    private final Map<Object, List<Object>> producerMethodDependentArguments =
            Collections.synchronizedMap(new IdentityHashMap<Object, List<Object>>());

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
        this.alternativeEnabled = !alternative;
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
        this.alternativeEnabled = !alternative;
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

    public boolean isAlternativeEnabled() {
        return !alternative || alternativeEnabled;
    }

    public void setAlternativeEnabled(boolean alternativeEnabled) {
        this.alternativeEnabled = alternativeEnabled;
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
                T produced = invokeProducerMethod(declaringInstance, creationalContext);
                validateProducerMethodNullProduct(produced);
                validatePassivationRequirementsForProducedValue(produced);
                return produced;
            } else if (producerField != null) {
                T produced = accessProducerField(declaringInstance);
                validateProducerFieldNullProduct(produced);
                validatePassivationRequirementsForProducedValue(produced);
                return produced;
            } else {
                throw new IllegalStateException("ProducerBean has neither method nor field");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = unwrapInvocationCause(e);
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new CreationException("Failed to create instance from producer", cause);
        }
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        if (instance == null) {
            return;
        }

        Throwable ignored = null;
        try {
            // Invoke the disposer method if present
            if (disposerMethod != null) {
                invokeDisposerMethod(instance);
            }
            destroyTrackedProducerMethodDependentArguments(instance);
        } catch (Exception e) {
            ignored = e;
        } finally {
            try {
                // Release CreationalContext
                if (creationalContext != null) {
                    creationalContext.release();
                }
            } catch (Exception e) {
                if (ignored == null) {
                    ignored = e;
                }
            } finally {
                DestroyedInstanceTracker.markDestroyed(instance);
            }
        }
    }

    private Throwable unwrapInvocationCause(Exception e) {
        if (e instanceof java.lang.reflect.InvocationTargetException) {
            Throwable target = ((java.lang.reflect.InvocationTargetException) e).getTargetException();
            return target != null ? target : e;
        }
        return e;
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
            if (InjectionPoint.class.equals(parameters[i].getType())) {
                args[i] = resolveProducerInjectionPoint(parameters[i]);
            } else {
                args[i] = resolveProducerParameter(parameters[i]);
            }
        }

        T produced = null;
        boolean invocationSucceeded = false;
        try {
            produced = (T) invokeOnRuntimeMethod(declaringInstance, producerMethod, args);
            invocationSucceeded = true;
            return produced;
        } finally {
            if (invocationSucceeded && produced != null) {
                trackDependentProducerParametersForProducedInstance(parameters, args, produced);
            } else {
                destroyDependentInvocationParameters(parameters, args, false);
            }
            destroyDependentDeclaringInstance(declaringInstance);
        }
    }

    /**
     * Accesses the producer field to get an instance.
     */
    @SuppressWarnings("unchecked")
    private T accessProducerField(Object declaringInstance) throws Exception {
        producerField.setAccessible(true);
        try {
            return (T) producerField.get(declaringInstance);
        } finally {
            destroyDependentDeclaringInstance(declaringInstance);
        }
    }

    private void validateProducerMethodNullProduct(T produced) {
        if (produced != null || producerMethod == null || isDependentScope(scope)) {
            return;
        }

        String scopeName = (scope == null) ? "<unknown>" : scope.getSimpleName();
        throw new IllegalProductException(
                "Producer method " + producerMethod.getName() +
                " of class " + declaringClass.getName() +
                " returned null but declares non-@Dependent scope @" + scopeName);
    }

    private void validateProducerFieldNullProduct(T produced) {
        if (produced != null || producerField == null || isDependentScope(scope)) {
            return;
        }

        String scopeName = (scope == null) ? "<unknown>" : scope.getSimpleName();
        throw new IllegalProductException(
                "Producer field " + producerField.getName() +
                " of class " + declaringClass.getName() +
                " contains null but declares non-@Dependent scope @" + scopeName);
    }

    private boolean isDependentScope(Class<? extends Annotation> scopeType) {
        return scopeType != null && (
                Dependent.class.equals(scopeType) ||
                "javax.enterprise.context.Dependent".equals(scopeType.getName()));
    }

    private void validatePassivationRequirementsForProducedValue(T produced) {
        if (produced == null || produced instanceof Serializable) {
            return;
        }

        if (isPassivatingScope(scope)) {
            throw new IllegalProductException(
                    "Producer " + describeProducerMember() +
                            " declares passivating scope @" + scope.getSimpleName() +
                            " but returned non-serializable value of type " + produced.getClass().getName()
            );
        }

        if (isDependentScope(scope) && isPassivationCapableDependencyRequiredAtCurrentInjectionPoint()) {
            throw new IllegalProductException(
                    "Producer " + describeProducerMember() +
                            " declares @Dependent scope and returned non-serializable value of type " +
                            produced.getClass().getName() +
                            " for an injection point that requires a passivation-capable dependency"
            );
        }
    }

    private String describeProducerMember() {
        if (producerMethod != null) {
            return "method " + producerMethod.getName() + " of class " + declaringClass.getName();
        }
        if (producerField != null) {
            return "field " + producerField.getName() + " of class " + declaringClass.getName();
        }
        return "member of class " + declaringClass.getName();
    }

    private boolean isPassivationCapableDependencyRequiredAtCurrentInjectionPoint() {
        if (!(dependencyResolver instanceof BeanResolver)) {
            return false;
        }

        InjectionPoint injectionPoint = ((BeanResolver) dependencyResolver).getCurrentInjectionPoint();
        if (injectionPoint == null) {
            return false;
        }
        if (injectionPoint.isTransient()) {
            return false;
        }
        if (hasTransientReference(injectionPoint)) {
            return false;
        }

        Bean<?> owningBean = injectionPoint.getBean();
        if (owningBean == null) {
            return false;
        }

        return isPassivatingScope(owningBean.getScope());
    }

    private boolean hasTransientReference(InjectionPoint injectionPoint) {
        if (injectionPoint.getAnnotated() == null || injectionPoint.getAnnotated().getAnnotations() == null) {
            return false;
        }
        for (Annotation annotation : injectionPoint.getAnnotated().getAnnotations()) {
            String annotationName = annotation.annotationType().getName();
            if ("jakarta.enterprise.inject.TransientReference".equals(annotationName) ||
                    "javax.enterprise.inject.TransientReference".equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPassivatingScope(Class<? extends Annotation> scopeType) {
        if (scopeType == null) {
            return false;
        }
        Boolean passivating = com.threeamigos.common.util.implementations.injection.AnnotationsEnum
                .getNormalScopePassivatingValue(scopeType);
        if (passivating != null) {
            return passivating;
        }
        String name = scopeType.getName();
        return "jakarta.enterprise.context.SessionScoped".equals(name) ||
                "jakarta.enterprise.context.ConversationScoped".equals(name) ||
                "javax.enterprise.context.SessionScoped".equals(name) ||
                "javax.enterprise.context.ConversationScoped".equals(name);
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
                args[i] = resolveDisposerParameter(parameters[i]);
            }
        }

        try {
            invokeOnRuntimeMethod(declaringInstance, disposerMethod, args);
        } finally {
            destroyDependentInvocationParameters(parameters, args, true);
            destroyDependentDeclaringInstance(declaringInstance);
        }
    }

    private Object invokeOnRuntimeMethod(Object targetInstance, Method method, Object[] args) throws Exception {
        Method invocable = method;
        if (targetInstance != null && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
            Method resolved = findMethodInHierarchy(targetInstance.getClass(), method.getName(), method.getParameterTypes());
            if (resolved != null) {
                invocable = resolved;
            }
        }
        invocable.setAccessible(true);
        return invocable.invoke(targetInstance, args);
    }

    private Method findMethodInHierarchy(Class<?> type, String methodName, Class<?>[] parameterTypes) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private void destroyDependentDeclaringInstance(Object declaringInstance) throws Exception {
        if (declaringInstance == null) {
            return;
        }
        if (!isDependentDeclaringClass(declaringInstance.getClass())) {
            return;
        }
        LifecycleMethodHelper.invokeLifecycleMethod(declaringInstance, PreDestroy.class);
    }

    private boolean isDependentDeclaringClass(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (com.threeamigos.common.util.implementations.injection.AnnotationsEnum.hasDependentAnnotation(type)) {
            return true;
        }
        for (Annotation annotation : type.getAnnotations()) {
            if ("javax.enterprise.context.Dependent".equals(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private void destroyDependentInvocationParameters(Parameter[] parameters, Object[] args, boolean skipDisposesParameter)
            throws Exception {
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            if (skipDisposesParameter && hasDisposesAnnotation(parameter)) {
                continue;
            }
            if (!isDependentParameter(parameter)) {
                continue;
            }
            LifecycleMethodHelper.invokeLifecycleMethod(arg, PreDestroy.class);
        }
    }

    private boolean isDependentParameter(Parameter parameter) {
        Class<?> parameterType = parameter.getType();
        if (parameterType == null) {
            return false;
        }
        if (com.threeamigos.common.util.implementations.injection.AnnotationsEnum.hasDependentAnnotation(parameterType)) {
            return true;
        }
        for (Annotation annotation : parameterType.getAnnotations()) {
            String annotationName = annotation.annotationType().getName();
            if ("javax.enterprise.context.Dependent".equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private void trackDependentProducerParametersForProducedInstance(
            Parameter[] parameters,
            Object[] args,
            Object produced
    ) {
        List<Object> tracked = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            if (!isDependentParameter(parameter)) {
                continue;
            }
            tracked.add(arg);
        }
        if (!tracked.isEmpty()) {
            producerMethodDependentArguments.put(produced, tracked);
        }
    }

    private void destroyTrackedProducerMethodDependentArguments(Object produced) throws Exception {
        List<Object> tracked = producerMethodDependentArguments.remove(produced);
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        for (Object dependent : tracked) {
            if (dependent == null) {
                continue;
            }
            LifecycleMethodHelper.invokeLifecycleMethod(dependent, PreDestroy.class);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object resolveProducerParameter(Parameter parameter) {
        if (dependencyResolver instanceof BeanResolver) {
            BeanResolver beanResolver = (BeanResolver) dependencyResolver;
            BeanImpl syntheticDeclaringBean = new BeanImpl(declaringClass, false);
            beanResolver.setCurrentInjectionPoint(new InjectionPointImpl(parameter, syntheticDeclaringBean));
            try {
                return dependencyResolver.resolve(
                        parameter.getParameterizedType(),
                        parameter.getAnnotations()
                );
            } finally {
                beanResolver.clearCurrentInjectionPoint();
            }
        }

        return dependencyResolver.resolve(
                parameter.getParameterizedType(),
                parameter.getAnnotations()
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private InjectionPoint resolveProducerInjectionPoint(Parameter parameter) {
        if (dependencyResolver instanceof BeanResolver) {
            BeanResolver beanResolver = (BeanResolver) dependencyResolver;
            InjectionPoint current = beanResolver.getCurrentInjectionPoint();
            if (current != null) {
                return current;
            }
        }

        BeanImpl syntheticDeclaringBean = new BeanImpl(declaringClass, false);
        return new InjectionPointImpl(parameter, syntheticDeclaringBean);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object resolveDisposerParameter(Parameter parameter) {
        if (dependencyResolver instanceof BeanResolver) {
            BeanResolver beanResolver = (BeanResolver) dependencyResolver;
            BeanImpl syntheticDeclaringBean = new BeanImpl(declaringClass, false);
            beanResolver.setCurrentInjectionPoint(new InjectionPointImpl(parameter, syntheticDeclaringBean));
            try {
                return dependencyResolver.resolve(
                        parameter.getParameterizedType(),
                        parameter.getAnnotations()
                );
            } finally {
                beanResolver.clearCurrentInjectionPoint();
            }
        }

        return dependencyResolver.resolve(
                parameter.getParameterizedType(),
                parameter.getAnnotations()
        );
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
}
