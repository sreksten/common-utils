package com.threeamigos.common.util.implementations.injection.discovery.validation.bean;

import com.threeamigos.common.util.implementations.injection.annotations.AnnotationComparator;
import com.threeamigos.common.util.implementations.injection.knowledgebase.DecoratorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.InterceptorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.util.RawTypeExtractor;
import com.threeamigos.common.util.implementations.injection.util.TypeClosureHelper;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.InjectionPoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates.hasInterceptorBindingAnnotation;

/**
 * Extracted interceptor/decorator definition validation and registration rules.
 */
public class InterceptorDecoratorDefinitionValidator {

    public interface Ops {
        Annotation[] annotationsOf(Class<?> clazz);

        Annotation[] annotationsOf(AnnotatedElement element);

        Integer getPriorityValue(Class<?> clazz);

        boolean hasObservesAnnotation(AnnotatedElement element);

        boolean hasObservesAsyncAnnotation(AnnotatedElement element);

        boolean hasAroundInvokeAnnotation(AnnotatedElement element);

        boolean hasAroundConstructAnnotation(AnnotatedElement element);

        boolean hasPostConstructAnnotation(AnnotatedElement element);

        boolean hasPreDestroyAnnotation(AnnotatedElement element);

        boolean hasInjectAnnotation(AnnotatedElement element);

        boolean hasDelegateAnnotation(AnnotatedElement element);

        boolean isStereotypeAnnotationType(Class<? extends Annotation> annotationType);

        String fmtField(Field field);

        String fmtMethod(Method method);

        String fmtConstructor(Constructor<?> constructor);

        InjectionPoint tryCreateInjectionPoint(AnnotatedElement element);
    }

    private final KnowledgeBase knowledgeBase;
    private final Ops ops;

    public InterceptorDecoratorDefinitionValidator(KnowledgeBase knowledgeBase, Ops ops) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.ops = Objects.requireNonNull(ops, "ops cannot be null");
    }

    public void validateAndRegisterInterceptor(Class<?> clazz) {
        boolean valid = true;
        boolean interceptorEnabled = isInterceptorEnabledAtDeployment(clazz);

        Set<Annotation> interceptorBindings = extractInterceptorBindings(clazz);
        if (interceptorBindings.isEmpty()) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": @Interceptor must have at least one interceptor binding annotation");
            valid = false;
        }

        int priority = Integer.MAX_VALUE;
        Integer declaredPriority = ops.getPriorityValue(clazz);
        if (declaredPriority != null) {
            priority = declaredPriority;
        }

        Method aroundInvokeMethod = findAroundInvokeMethod(clazz);
        Method aroundConstructMethod = findAroundConstructMethod(clazz);
        Method postConstructMethod = findPostConstructMethod(clazz);
        Method preDestroyMethod = findPreDestroyMethod(clazz);

        if (aroundInvokeMethod == null && aroundConstructMethod == null &&
                postConstructMethod == null && preDestroyMethod == null) {
            if (interceptorEnabled) {
                knowledgeBase.addDefinitionError(clazz.getName() +
                        ": @Interceptor must have at least one interceptor method (@AroundInvoke, @AroundConstruct, @PostConstruct, or @PreDestroy)");
            }
            valid = false;
        }

        if (aroundInvokeMethod != null && !isValidAroundInvoke(aroundInvokeMethod)) {
            if (interceptorEnabled) {
                knowledgeBase.addDefinitionError(ops.fmtMethod(aroundInvokeMethod) +
                        ": @AroundInvoke must be non-static, return Object, and accept a single InvocationContext parameter");
            }
            valid = false;
        }
        if (aroundConstructMethod != null && !isValidAroundConstruct(aroundConstructMethod)) {
            if (interceptorEnabled) {
                knowledgeBase.addDefinitionError(ops.fmtMethod(aroundConstructMethod) +
                        ": @AroundConstruct must be non-static, return Object, and accept a single InvocationContext parameter");
            }
            valid = false;
        }
        if (postConstructMethod != null && isNotValidInterceptorLifecycleMethod(postConstructMethod)) {
            if (interceptorEnabled) {
                knowledgeBase.addDefinitionError(ops.fmtMethod(postConstructMethod) +
                        ": @PostConstruct interceptor method must be non-static, void/Object, and take a single InvocationContext parameter");
            }
            valid = false;
        }
        if (preDestroyMethod != null && isNotValidInterceptorLifecycleMethod(preDestroyMethod)) {
            if (interceptorEnabled) {
                knowledgeBase.addDefinitionError(ops.fmtMethod(preDestroyMethod) +
                        ": @PreDestroy interceptor method must be non-static, void/Object, and take a single InvocationContext parameter");
            }
            valid = false;
        }

        if (declaresObserverMethod(clazz)) {
            valid = false;
        }

        if (valid) {
            InterceptorInfo info = new InterceptorInfo(
                    clazz,
                    interceptorBindings,
                    priority,
                    aroundInvokeMethod,
                    aroundConstructMethod,
                    postConstructMethod,
                    preDestroyMethod
            );
            knowledgeBase.addInterceptorInfo(info);
        }
    }

    public void validateConflictingInterceptorBindings(Class<?> clazz) {
        Map<Class<? extends Annotation>, Annotation> collectedBindings = new LinkedHashMap<>();
        Set<Class<? extends Annotation>> directBindingTypes = new HashSet<>();
        for (Annotation annotation : ops.annotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (isInterceptorBinding(annotationType)) {
                directBindingTypes.add(annotationType);
                collectInterceptorBinding(annotation, collectedBindings, new HashSet<>(), clazz.getName(), directBindingTypes);
            }
        }
        for (Annotation annotation : ops.annotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (isStereotypeAnnotationType(annotationType)) {
                collectStereotypeInterceptorBindings(annotationType, collectedBindings, new HashSet<>(), clazz.getName(), directBindingTypes);
            }
        }
    }

    public void validateDecoratorDelegateInjectionPoints(Class<?> clazz) {
        int delegateCount = 0;

        for (Field field : clazz.getDeclaredFields()) {
            if (ops.hasDelegateAnnotation(field)) {
                if (!ops.hasInjectAnnotation(field)) {
                    knowledgeBase.addDefinitionError(clazz.getName() +
                            ": @Delegate field " + ops.fmtField(field) + " must be an injected field (@Inject)");
                } else {
                    delegateCount++;
                }
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            for (Parameter param : method.getParameters()) {
                if (ops.hasDelegateAnnotation(param)) {
                    if (!ops.hasInjectAnnotation(method)) {
                        knowledgeBase.addDefinitionError(clazz.getName() +
                                ": @Delegate parameter in method " + ops.fmtMethod(method) +
                                " must be declared on an initializer method annotated @Inject");
                    } else {
                        delegateCount++;
                    }
                }
            }
        }

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            for (Parameter param : constructor.getParameters()) {
                if (ops.hasDelegateAnnotation(param)) {
                    if (!ops.hasInjectAnnotation(constructor)) {
                        knowledgeBase.addDefinitionError(clazz.getName() +
                                ": @Delegate parameter in constructor " + ops.fmtConstructor(constructor) +
                                " must be declared on the bean constructor (@Inject)");
                    } else {
                        delegateCount++;
                    }
                }
            }
        }

        if (delegateCount == 0) {
            if (countInjectableDecoratorInjectionPoints(clazz) == 1) {
                return;
            }
            knowledgeBase.addDefinitionError(clazz.getName() +
                    ": Decorator must have exactly one @Delegate injection point (found 0). " +
                    "Add @Inject @Delegate to a field, method parameter, or constructor parameter.");
        } else if (delegateCount > 1) {
            knowledgeBase.addDefinitionError(clazz.getName() +
                    ": Decorator must have exactly one @Delegate injection point (found " + delegateCount + "). " +
                    "Only one @Delegate injection point is allowed per decorator.");
        }
    }

    public void validateDecoratorDoesNotDeclareObserverMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            for (Parameter parameter : method.getParameters()) {
                if (ops.hasObservesAnnotation(parameter) || ops.hasObservesAsyncAnnotation(parameter)) {
                    knowledgeBase.addDefinitionError(clazz.getName() +
                            ": decorators may not declare observer methods. Found observer parameter in " +
                            ops.fmtMethod(method));
                    return;
                }
            }
        }
    }

    public boolean hasAnyDelegateAnnotation(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (ops.hasDelegateAnnotation(field)) {
                return true;
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            for (Parameter parameter : method.getParameters()) {
                if (ops.hasDelegateAnnotation(parameter)) {
                    return true;
                }
            }
        }

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            for (Parameter parameter : constructor.getParameters()) {
                if (ops.hasDelegateAnnotation(parameter)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void validateAndRegisterDecorator(Class<?> clazz) {
        int priority = Integer.MAX_VALUE;
        Integer priorityAnnotation = ops.getPriorityValue(clazz);
        if (priorityAnnotation != null) {
            priority = priorityAnnotation;
        }

        InjectionPoint delegateInjectionPoint = findDelegateInjectionPoint(clazz);
        if (delegateInjectionPoint == null) {
            return;
        }

        Set<Type> decoratedTypes = extractDecoratedTypes(clazz);
        if (decoratedTypes.isEmpty()) {
            knowledgeBase.addDefinitionError(clazz.getName() +
                    ": decorator must declare at least one decorated type (interface bean type excluding java.io.Serializable)");
            return;
        }

        validateDecoratorDelegateTypeCompatibility(clazz, delegateInjectionPoint, decoratedTypes);
        validateDecoratorAbstractMethods(clazz, decoratedTypes);

        DecoratorInfo info = new DecoratorInfo(
                clazz,
                decoratedTypes,
                priority,
                delegateInjectionPoint
        );
        knowledgeBase.addDecoratorInfo(info);
    }

    private boolean declaresObserverMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            for (Parameter parameter : method.getParameters()) {
                if (ops.hasObservesAnnotation(parameter) || ops.hasObservesAsyncAnnotation(parameter)) {
                    knowledgeBase.addDefinitionError(clazz.getName()
                            + ": interceptors may not declare observer methods. Found observer parameter in "
                            + ops.fmtMethod(method));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInterceptorEnabledAtDeployment(Class<?> interceptorClass) {
        if (interceptorClass == null) {
            return false;
        }
        if (knowledgeBase.getApplicationInterceptorOrder(interceptorClass) >= 0) {
            return true;
        }
        if (knowledgeBase.getInterceptorBeansXmlOrder(interceptorClass) >= 0) {
            return true;
        }
        return ops.getPriorityValue(interceptorClass) != null;
    }

    private boolean isValidAroundInvoke(Method m) {
        return !Modifier.isStatic(m.getModifiers())
                && m.getReturnType().equals(Object.class)
                && m.getParameterCount() == 1
                && jakarta.interceptor.InvocationContext.class.isAssignableFrom(m.getParameterTypes()[0]);
    }

    private boolean isValidAroundConstruct(Method m) {
        boolean returnOk = m.getReturnType().equals(Object.class) || m.getReturnType().equals(void.class);
        return !Modifier.isStatic(m.getModifiers())
                && returnOk
                && m.getParameterCount() == 1
                && jakarta.interceptor.InvocationContext.class.isAssignableFrom(m.getParameterTypes()[0]);
    }

    private boolean isNotValidInterceptorLifecycleMethod(Method m) {
        boolean returnOk = m.getReturnType().equals(void.class) || m.getReturnType().equals(Object.class);
        return Modifier.isStatic(m.getModifiers())
                || !returnOk
                || m.getParameterCount() != 1
                || !jakarta.interceptor.InvocationContext.class.isAssignableFrom(m.getParameterTypes()[0]);
    }

    private Set<Annotation> extractInterceptorBindings(Class<?> clazz) {
        Set<Annotation> bindings = new HashSet<>();
        for (Annotation annotation : ops.annotationsOf(clazz)) {
            if (isInterceptorBinding(annotation.annotationType())) {
                bindings.add(annotation);
            }
        }
        return bindings;
    }

    private boolean isInterceptorBinding(Class<? extends Annotation> annotationType) {
        return hasInterceptorBindingAnnotation(annotationType) ||
                knowledgeBase.isRegisteredInterceptorBinding(annotationType);
    }

    private boolean isStereotypeAnnotationType(Class<? extends Annotation> annotationType) {
        return ops.isStereotypeAnnotationType(annotationType) ||
                knowledgeBase.isRegisteredStereotype(annotationType);
    }

    private void collectStereotypeInterceptorBindings(Class<? extends Annotation> stereotypeType,
                                                      Map<Class<? extends Annotation>, Annotation> collectedBindings,
                                                      Set<Class<? extends Annotation>> visitedStereotypes,
                                                      String location,
                                                      Set<Class<? extends Annotation>> directBindingTypes) {
        if (!visitedStereotypes.add(stereotypeType)) {
            return;
        }
        try {
            for (Annotation stereotypeAnnotation : stereotypeType.getAnnotations()) {
                Class<? extends Annotation> annotationType = stereotypeAnnotation.annotationType();
                if (isInterceptorBinding(annotationType)) {
                    collectInterceptorBinding(stereotypeAnnotation, collectedBindings, new HashSet<>(), location, directBindingTypes);
                } else if (isStereotypeAnnotationType(annotationType)) {
                    collectStereotypeInterceptorBindings(annotationType, collectedBindings, visitedStereotypes, location, directBindingTypes);
                }
            }
        } finally {
            visitedStereotypes.remove(stereotypeType);
        }
    }

    private void collectInterceptorBinding(Annotation binding,
                                           Map<Class<? extends Annotation>, Annotation> collectedBindings,
                                           Set<Class<? extends Annotation>> visitedBindings,
                                           String location,
                                           Set<Class<? extends Annotation>> directBindingTypes) {
        Class<? extends Annotation> bindingType = binding.annotationType();
        Annotation existing = collectedBindings.get(bindingType);
        if (existing != null &&
                !AnnotationComparator.equals(existing, binding)) {
            if (directBindingTypes.contains(bindingType)) {
                return;
            }
            throw new DefinitionException(location + ": conflicting interceptor binding values for @" +
                    bindingType.getSimpleName());
        }
        collectedBindings.putIfAbsent(bindingType, binding);

        if (!visitedBindings.add(bindingType)) {
            return;
        }
        try {
            for (Annotation metaAnnotation : bindingType.getAnnotations()) {
                if (isInterceptorBinding(metaAnnotation.annotationType())) {
                    collectInterceptorBinding(metaAnnotation, collectedBindings, visitedBindings, location, directBindingTypes);
                }
            }
        } finally {
            visitedBindings.remove(bindingType);
        }
    }

    private Method findAroundInvokeMethod(Class<?> clazz) {
        for (Method method : getAllMethods(clazz)) {
            if (ops.hasAroundInvokeAnnotation(method)) {
                return method;
            }
        }
        return null;
    }

    private Method findAroundConstructMethod(Class<?> clazz) {
        for (Method method : getAllMethods(clazz)) {
            if (ops.hasAroundConstructAnnotation(method)) {
                return method;
            }
        }
        return null;
    }

    private Method findPostConstructMethod(Class<?> clazz) {
        for (Method method : getAllMethods(clazz)) {
            if (ops.hasPostConstructAnnotation(method)) {
                return method;
            }
        }
        return null;
    }

    private Method findPreDestroyMethod(Class<?> clazz) {
        for (Method method : getAllMethods(clazz)) {
            if (ops.hasPreDestroyAnnotation(method)) {
                return method;
            }
        }
        return null;
    }

    private List<Method> getAllMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Collections.addAll(methods, current.getDeclaredMethods());
            current = current.getSuperclass();
        }
        return methods;
    }

    private void validateDecoratorAbstractMethods(Class<?> decoratorClass, Set<Type> decoratedTypes) {
        if (decoratedTypes == null || decoratedTypes.isEmpty()) {
            return;
        }

        Set<String> decoratedMethodSignatures = new HashSet<>();
        for (Type type : decoratedTypes) {
            Class<?> decoratedClass = rawTypeOf(type);
            if (decoratedClass == null) {
                continue;
            }
            for (Method method : decoratedClass.getMethods()) {
                if (method.getDeclaringClass().equals(Object.class)) {
                    continue;
                }
                decoratedMethodSignatures.add(methodSignature(method));
            }
        }

        for (Method method : getAllMethods(decoratorClass)) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            if (method.getDeclaringClass().equals(Object.class)) {
                continue;
            }
            if (!decoratedMethodSignatures.contains(methodSignature(method))) {
                knowledgeBase.addDefinitionError(decoratorClass.getName() +
                        ": abstract method " + ops.fmtMethod(method) +
                        " is not declared by any decorated type");
            }
        }

        if (Modifier.isAbstract(decoratorClass.getModifiers())) {
            return;
        }

        for (Type type : decoratedTypes) {
            Class<?> decoratedClass = rawTypeOf(type);
            if (decoratedClass == null) {
                continue;
            }

            for (Method m : decoratedClass.getMethods()) {
                if (!Modifier.isAbstract(m.getModifiers())) {
                    continue;
                }
                if (m.getDeclaringClass().equals(Object.class)) {
                    continue;
                }

                try {
                    Method impl = decoratorClass.getMethod(m.getName(), m.getParameterTypes());
                    if (Modifier.isAbstract(impl.getModifiers())) {
                        knowledgeBase.addDefinitionError(decoratorClass.getName() +
                                ": abstract method " + ops.fmtMethod(m) +
                                " must be implemented with a concrete method in the decorator.");
                    }
                } catch (NoSuchMethodException e) {
                    knowledgeBase.addDefinitionError(decoratorClass.getName() +
                            ": missing implementation for abstract method " + ops.fmtMethod(m) +
                            " from decorated type " + decoratedClass.getName());
                }
            }
        }
    }

    private InjectionPoint findDelegateInjectionPoint(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (ops.hasDelegateAnnotation(field)) {
                return ops.tryCreateInjectionPoint(field);
            }
        }

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            for (Parameter parameter : constructor.getParameters()) {
                if (ops.hasDelegateAnnotation(parameter)) {
                    return ops.tryCreateInjectionPoint(parameter);
                }
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (ops.hasInjectAnnotation(method)) {
                for (Parameter parameter : method.getParameters()) {
                    if (ops.hasDelegateAnnotation(parameter)) {
                        return ops.tryCreateInjectionPoint(parameter);
                    }
                }
            }
        }

        if (countInjectableDecoratorInjectionPoints(clazz) == 1) {
            return findSingleInjectableDecoratorInjectionPoint(clazz);
        }
        return null;
    }

    private int countInjectableDecoratorInjectionPoints(Class<?> clazz) {
        int count = 0;

        for (Field field : clazz.getDeclaredFields()) {
            if (ops.hasInjectAnnotation(field)) {
                count++;
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (!ops.hasInjectAnnotation(method)) {
                continue;
            }
            count += method.getParameterCount();
        }

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (!ops.hasInjectAnnotation(constructor)) {
                continue;
            }
            count += constructor.getParameterCount();
        }

        return count;
    }

    private InjectionPoint findSingleInjectableDecoratorInjectionPoint(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (ops.hasInjectAnnotation(field)) {
                return ops.tryCreateInjectionPoint(field);
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (!ops.hasInjectAnnotation(method)) {
                continue;
            }
            for (Parameter parameter : method.getParameters()) {
                return ops.tryCreateInjectionPoint(parameter);
            }
        }

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (!ops.hasInjectAnnotation(constructor)) {
                continue;
            }
            for (Parameter parameter : constructor.getParameters()) {
                return ops.tryCreateInjectionPoint(parameter);
            }
        }

        return null;
    }

    private Set<Type> extractDecoratedTypes(Class<?> clazz) {
        Set<Type> decoratedTypes = new HashSet<>();
        Set<Type> typeClosure = TypeClosureHelper.extractTypesFromClass(clazz);
        for (Type type : typeClosure) {
            Class<?> raw = rawTypeOf(type);
            if (raw == null || !raw.isInterface()) {
                continue;
            }
            if (raw.equals(Object.class)
                    || raw.equals(java.io.Serializable.class)
                    || raw.equals(jakarta.enterprise.inject.spi.Decorator.class)) {
                continue;
            }
            decoratedTypes.add(type);
        }
        decoratedTypes.removeIf(t -> java.io.Serializable.class.equals(rawTypeOf(t)));
        return decoratedTypes;
    }

    private void validateDecoratorDelegateTypeCompatibility(
            Class<?> decoratorClass,
            InjectionPoint delegateInjectionPoint,
            Set<Type> decoratedTypes) {
        Type delegateType = delegateInjectionPoint.getType();
        if (delegateType == null) {
            return;
        }

        for (Type decoratedType : decoratedTypes) {
            if (!delegateTypeCoversDecoratedType(delegateType, decoratedType)) {
                knowledgeBase.addDefinitionError(decoratorClass.getName() +
                        ": delegate type " + delegateType.getTypeName() +
                        " does not implement/extend decorated type " + decoratedType.getTypeName() +
                        " with matching type parameters");
            }
        }
    }

    private boolean delegateTypeCoversDecoratedType(Type delegateType, Type decoratedType) {
        Class<?> delegateRaw = rawTypeOf(delegateType);
        Class<?> decoratedRaw = rawTypeOf(decoratedType);
        if (delegateRaw == null || decoratedRaw == null || !decoratedRaw.isAssignableFrom(delegateRaw)) {
            return false;
        }

        if (!(decoratedType instanceof ParameterizedType)) {
            return true;
        }

        Type viewOnDecoratedRaw = findTypeInHierarchy(delegateType, decoratedRaw, new HashSet<>());
        if (viewOnDecoratedRaw == null) {
            return false;
        }

        return isBeanTypeAssignableToDelegateType(decoratedType, viewOnDecoratedRaw);
    }

    private boolean isBeanTypeAssignableToDelegateType(Type beanType, Type delegateType) {
        if (beanType == null || delegateType == null) {
            return false;
        }

        Class<?> beanRaw = rawTypeOf(beanType);
        Class<?> delegateRaw = rawTypeOf(delegateType);
        if (delegateRaw == null || !delegateRaw.equals(beanRaw)) {
            return false;
        }

        if (beanType instanceof Class && delegateType instanceof ParameterizedType) {
            for (Type delegateArg : ((ParameterizedType) delegateType).getActualTypeArguments()) {
                if (!isObjectOrUnboundedTypeVariable(delegateArg)) {
                    return false;
                }
            }
            return true;
        }

        if (beanType instanceof ParameterizedType && delegateType instanceof ParameterizedType) {
            Type[] beanArgs = ((ParameterizedType) beanType).getActualTypeArguments();
            Type[] delegateArgs = ((ParameterizedType) delegateType).getActualTypeArguments();
            if (beanArgs.length != delegateArgs.length) {
                return false;
            }
            for (int i = 0; i < beanArgs.length; i++) {
                if (!matchesDelegateParameter(beanArgs[i], delegateArgs[i])) {
                    return false;
                }
            }
            return true;
        }

        if (beanType instanceof Class && delegateType instanceof Class) {
            return true;
        }

        return beanType.equals(delegateType);
    }

    private boolean matchesDelegateParameter(Type beanParam, Type delegateParam) {
        if (isActualType(beanParam) && isActualType(delegateParam)) {
            Class<?> beanRaw = rawTypeOf(beanParam);
            Class<?> delegateRaw = rawTypeOf(delegateParam);
            if (beanRaw == null || !beanRaw.equals(delegateRaw)) {
                return false;
            }
            if (beanParam instanceof ParameterizedType && delegateParam instanceof ParameterizedType) {
                return isBeanTypeAssignableToDelegateType(beanParam, delegateParam);
            }
            return true;
        }

        if (delegateParam instanceof WildcardType && isActualType(beanParam)) {
            return wildcardMatches((WildcardType) delegateParam, beanParam);
        }

        if (delegateParam instanceof WildcardType && beanParam instanceof TypeVariable<?>) {
            Type beanUpperBound = firstUpperBound((TypeVariable<?>) beanParam);
            return wildcardMatches((WildcardType) delegateParam, beanUpperBound);
        }

        if (delegateParam instanceof TypeVariable<?> && beanParam instanceof TypeVariable<?>) {
            Type delegateUpper = firstUpperBound((TypeVariable<?>) delegateParam);
            Type beanUpper = firstUpperBound((TypeVariable<?>) beanParam);
            return isAssignable(beanUpper, delegateUpper);
        }

        if (delegateParam instanceof TypeVariable<?> && isActualType(beanParam)) {
            Type delegateUpper = firstUpperBound((TypeVariable<?>) delegateParam);
            return isAssignable(beanParam, delegateUpper);
        }

        return false;
    }

    private boolean wildcardMatches(WildcardType wildcard, Type candidate) {
        for (Type upper : wildcard.getUpperBounds()) {
            if (!Object.class.equals(upper) && !isAssignable(candidate, upper)) {
                return false;
            }
        }
        for (Type lower : wildcard.getLowerBounds()) {
            if (!isAssignable(lower, candidate)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAssignable(Type from, Type to) {
        Class<?> fromRaw = rawTypeOf(from);
        Class<?> toRaw = rawTypeOf(to);
        return fromRaw != null && toRaw != null && toRaw.isAssignableFrom(fromRaw);
    }

    private Type firstUpperBound(TypeVariable<?> variable) {
        Type[] bounds = variable.getBounds();
        return bounds.length == 0 ? Object.class : bounds[0];
    }

    private boolean isActualType(Type type) {
        return type instanceof Class<?> || type instanceof ParameterizedType;
    }

    private boolean isObjectOrUnboundedTypeVariable(Type type) {
        if (Object.class.equals(type)) {
            return true;
        }
        if (type instanceof TypeVariable<?>) {
            TypeVariable<?> tv = (TypeVariable<?>) type;
            Type[] bounds = tv.getBounds();
            return bounds.length == 0 || (bounds.length == 1 && Object.class.equals(bounds[0]));
        }
        return false;
    }

    private Type findTypeInHierarchy(Type source, Class<?> targetRaw, Set<Type> visited) {
        if (source == null || !visited.add(source)) {
            return null;
        }

        Class<?> raw = rawTypeOf(source);
        if (raw == null) {
            return null;
        }
        if (raw.equals(targetRaw)) {
            return source;
        }

        for (Type type : raw.getGenericInterfaces()) {
            Type found = findTypeInHierarchy(type, targetRaw, visited);
            if (found != null) {
                return found;
            }
        }

        return findTypeInHierarchy(raw.getGenericSuperclass(), targetRaw, visited);
    }

    private String methodSignature(Method method) {
        return method.getName() + Arrays.toString(method.getParameterTypes());
    }

    private Class<?> rawTypeOf(Type type) {
        try {
            return RawTypeExtractor.getRawType(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
