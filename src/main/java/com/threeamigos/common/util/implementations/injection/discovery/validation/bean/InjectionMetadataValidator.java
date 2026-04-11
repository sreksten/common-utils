package com.threeamigos.common.util.implementations.injection.discovery.validation.bean;

import com.threeamigos.common.util.implementations.injection.annotations.AnnotationsEnum;
import com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.annotation.Nonnull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates.hasNamedAnnotation;
import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationsEnum.DECORATED;
import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationsEnum.INTERCEPTED;

/**
 * Extracted constructor/injection metadata validation rules.
 */
public class InjectionMetadataValidator {

    public interface Ops {
        boolean hasInjectAnnotation(AnnotatedElement element);

        boolean hasProducesAnnotation(AnnotatedElement element);

        boolean hasDisposesAnnotation(AnnotatedElement element);

        boolean hasObservesAnnotation(AnnotatedElement element);

        boolean hasObservesAsyncAnnotation(AnnotatedElement element);

        boolean hasDecoratorAnnotation(Class<?> clazz);

        boolean hasDelegateAnnotation(AnnotatedElement element);

        boolean hasAnyParameterWithDisposesAnnotation(Method method);

        Type baseTypeOf(Field field);

        Type baseTypeOf(Method method);

        Type baseTypeOf(Parameter parameter);

        Annotation[] annotationsOf(AnnotatedElement element);

        void checkInjectionTypeValidity(Type type);

        void checkInjectionTypeValidity(Type type, boolean allowTypeVariableArguments, Class<?> declaringBeanClass);

        void validateQualifiers(Annotation[] annotations, String location);

        boolean allowsBeanClassTypeVariableArguments(Type injectionType, Class<?> declaringBeanClass);

        boolean hasDefaultQualifier(Annotation[] annotations);

        boolean hasQualifier(Annotation[] annotations, AnnotationsEnum qualifierType);

        boolean declaresNonDependentScope(Class<?> clazz);

        boolean isInterceptorClass(Class<?> clazz);

        Type findDecoratorDelegateType(Class<?> decoratorClass);

        String fmtField(Field field);

        String fmtMethod(Method method);

        String fmtConstructor(Constructor<?> constructor);

        String fmtParameter(Parameter parameter);

        String safeParamName(Parameter parameter);
    }

    private final KnowledgeBase knowledgeBase;
    private final Ops ops;

    public InjectionMetadataValidator(KnowledgeBase knowledgeBase, Ops ops) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.ops = Objects.requireNonNull(ops, "ops cannot be null");
    }

    public Constructor<?> findBeanConstructor(Class<?> clazz) {
        List<Constructor<?>> constructors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(ops::hasInjectAnnotation)
                .collect(Collectors.toList());

        if (constructors.size() > 1) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": declares more than one constructor annotated with @Inject");
            return null;
        }

        if (constructors.size() == 1) {
            Constructor<?> constructor = constructors.get(0);
            if (Modifier.isPrivate(constructor.getModifiers())) {
                knowledgeBase.addDefinitionError(clazz.getName() + ": @Inject constructor must not be private");
                return null;
            }

            if (!validateBeanConstructorParameters(constructor)) {
                return null;
            }
            return constructor;
        }

        Optional<Constructor<?>> noArg = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == 0)
                .findFirst();

        if (!noArg.isPresent()) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": has no @Inject constructor and no no-arg constructor");
            return null;
        }

        Constructor<?> constructor = noArg.get();
        if (Modifier.isPrivate(constructor.getModifiers())) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": no-arg constructor must not be private");
            return null;
        }

        return constructor;
    }

    public boolean validateIllegalConstructorParameterAnnotations(Class<?> clazz) {
        boolean valid = true;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (ops.hasInjectAnnotation(constructor)) {
                continue;
            }
            for (Parameter parameter : constructor.getParameters()) {
                if (ops.hasDisposesAnnotation(parameter)) {
                    knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                            ": bean constructor parameter may not be annotated @Disposes");
                    valid = false;
                }
                if (ops.hasObservesAnnotation(parameter)) {
                    knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                            ": bean constructor parameter may not be annotated @Observes");
                    valid = false;
                }
                if (ops.hasObservesAsyncAnnotation(parameter)) {
                    knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                            ": bean constructor parameter may not be annotated @ObservesAsync");
                    valid = false;
                }
            }
        }
        return valid;
    }

    public boolean validateInjectField(Field field) {
        boolean valid = true;

        if (Modifier.isFinal(field.getModifiers())) {
            knowledgeBase.addDefinitionError(ops.fmtField(field) + ": final fields are not valid injection points");
            valid = false;
        }

        if (Modifier.isStatic(field.getModifiers())) {
            knowledgeBase.addDefinitionError(ops.fmtField(field) + ": static field injection is not a valid CDI injection point");
            valid = false;
        }

        Type fieldBaseType = ops.baseTypeOf(field);
        boolean allowTypeVariableArguments =
                (ops.hasDecoratorAnnotation(field.getDeclaringClass()) && ops.hasDelegateAnnotation(field))
                        || ops.allowsBeanClassTypeVariableArguments(fieldBaseType, field.getDeclaringClass());
        try {
            ops.checkInjectionTypeValidity(fieldBaseType, allowTypeVariableArguments, field.getDeclaringClass());
        } catch (IllegalArgumentException e) {
            knowledgeBase.addInjectionError(ops.fmtField(field) + ": " + e.getMessage());
            valid = false;
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(ops.fmtField(field) + ": " + e.getMessage());
            valid = false;
        }

        try {
            ops.validateQualifiers(ops.annotationsOf(field), ops.fmtField(field));
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(ops.fmtField(field) + ": " + e.getMessage());
            valid = false;
        }

        if (isNotValidNamedInjectionPointUsage(field)) {
            valid = false;
        }

        if (isNotValidInjectionPointMetadataUsage(field, false)) {
            valid = false;
        }
        if (isNotValidInterceptionFactoryInjectionPointUsage(field, false)) {
            valid = false;
        }

        return valid;
    }

    public boolean validateInitializerMethod(Method method) {
        boolean valid = true;

        if (Modifier.isAbstract(method.getModifiers())) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": cannot inject into an abstract initializer method");
            valid = false;
        }

        if (Modifier.isStatic(method.getModifiers())) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": static initializer methods are not valid CDI injection points");
            valid = false;
        }

        if (method.getTypeParameters().length > 0) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": generic methods are not valid CDI initializer methods");
            valid = false;
        }

        if (ops.hasProducesAnnotation(method)) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": initializer method may not be annotated @Produces");
            valid = false;
        }
        if (ops.hasAnyParameterWithDisposesAnnotation(method)) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": initializer method may not declare a @Disposes parameter");
            valid = false;
        }
        for (Parameter parameter : method.getParameters()) {
            if (ops.hasObservesAnnotation(parameter)) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                        ": initializer method parameter may not be annotated @Observes");
                valid = false;
                continue;
            }
            if (ops.hasObservesAsyncAnnotation(parameter)) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                        ": initializer method parameter may not be annotated @ObservesAsync");
                valid = false;
            }
        }

        if (hasNotValidInjectionParameters(method.getParameters(), ops.fmtMethod(method))) {
            valid = false;
        }

        return valid;
    }

    public boolean hasNotValidInjectionParameters(Parameter[] parameters, String owner) {
        boolean valid = true;
        for (Parameter parameter : parameters) {
            if (ops.hasDisposesAnnotation(parameter)) {
                continue;
            }

            try {
                Type parameterBaseType = ops.baseTypeOf(parameter);
                boolean allowTypeVariableArguments =
                        (ops.hasDecoratorAnnotation(parameter.getDeclaringExecutable().getDeclaringClass()) &&
                                ops.hasDelegateAnnotation(parameter))
                                || ops.allowsBeanClassTypeVariableArguments(parameterBaseType,
                                parameter.getDeclaringExecutable().getDeclaringClass());
                ops.checkInjectionTypeValidity(parameterBaseType, allowTypeVariableArguments,
                        parameter.getDeclaringExecutable().getDeclaringClass());
            } catch (IllegalArgumentException e) {
                knowledgeBase.addInjectionError(ops.fmtParameter(parameter) + ": " + e.getMessage());
                valid = false;
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) + ": " + e.getMessage());
                valid = false;
            }

            try {
                ops.validateQualifiers(ops.annotationsOf(parameter), owner + " parameter " + ops.safeParamName(parameter));
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) + ": " + e.getMessage());
                valid = false;
            }

            if (isNotValidNamedInjectionPointUsage(parameter)) {
                valid = false;
            }

            if (isNotValidInjectionPointMetadataUsage(parameter, false)) {
                valid = false;
            }
            if (isNotValidInterceptionFactoryInjectionPointUsage(parameter, false)) {
                valid = false;
            }
        }
        return !valid;
    }

    public boolean isNotValidInterceptionFactoryInjectionPointUsage(AnnotatedElement element,
                                                                    boolean producerMethodParameter) {
        Type type = null;
        Annotation[] annotations = null;

        if (element instanceof Field) {
            Field field = (Field) element;
            type = ops.baseTypeOf(field);
            annotations = ops.annotationsOf(field);
        } else if (element instanceof Parameter) {
            Parameter parameter = (Parameter) element;
            type = ops.baseTypeOf(parameter);
            annotations = ops.annotationsOf(parameter);
        }

        if (!isInterceptionFactoryType(type)) {
            return false;
        }

        if (!ops.hasDefaultQualifier(annotations)) {
            return false;
        }

        if (!producerMethodParameter) {
            knowledgeBase.addDefinitionError(
                    describeInjectionPoint(element) +
                            ": injection point of type InterceptionFactory with @Default must be a producer method parameter");
            return true;
        }

        Class<?> argClass = getArgClass(type);
        if (argClass.isInterface() || argClass.isAnnotation() || argClass.isArray() || argClass.isPrimitive()) {
            throw new NonPortableBehaviourException(
                    "Non-portable behavior: InterceptionFactory injection point type parameter must be a Java class");
        }

        return false;
    }

    public boolean isNotValidInjectionPointMetadataUsage(AnnotatedElement injectionPoint, boolean disposerParameter) {
        boolean valid = true;

        if (isInjectionPointMetadataType(injectionPoint) && ops.hasDefaultQualifier(ops.annotationsOf(injectionPoint))) {
            if (disposerParameter) {
                knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                        ": disposer method injection point of type InjectionPoint with qualifier @Default is not allowed");
                valid = false;
            } else {
                Class<?> declaringClass = declaringClassOf(injectionPoint);
                if (declaringClass != null && ops.declaresNonDependentScope(declaringClass)) {
                    knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                            ": bean declares scope other than @Dependent and may not inject InjectionPoint with qualifier @Default");
                    valid = false;
                }
            }
        }

        if (isBeanMetadataType(injectionPoint) || isInterceptorMetadataType(injectionPoint)) {
            valid = validateBeanAndInterceptorMetadataUsage(injectionPoint, disposerParameter) && valid;
        }

        if (isDecoratorMetadataType(injectionPoint) || isDecoratedBeanMetadataType(injectionPoint)) {
            valid = validateDecoratorMetadataUsage(injectionPoint, disposerParameter) && valid;
        }

        return !valid;
    }

    public boolean isNotValidNamedInjectionPointUsage(AnnotatedElement injectionPoint) {
        Annotation named = findNamedQualifier(ops.annotationsOf(injectionPoint));
        if (named == null) {
            return false;
        }

        String namedValue = readNamedValue(named).trim();
        if (!namedValue.isEmpty()) {
            return false;
        }

        if (injectionPoint instanceof Field) {
            return false;
        }

        knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                ": @Named injection point must declare a non-empty value on non-field injection points");
        return true;
    }

    private boolean validateBeanConstructorParameters(Constructor<?> constructor) {
        boolean valid = true;
        for (Parameter parameter : constructor.getParameters()) {
            if (ops.hasDisposesAnnotation(parameter)) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                        ": bean constructor parameter may not be annotated @Disposes");
                valid = false;
                continue;
            }
            if (ops.hasObservesAnnotation(parameter)) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                        ": bean constructor parameter may not be annotated @Observes");
                valid = false;
                continue;
            }
            if (ops.hasObservesAsyncAnnotation(parameter)) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                        ": bean constructor parameter may not be annotated @ObservesAsync");
                valid = false;
                continue;
            }
            if (isNotValidInjectionPointMetadataUsage(parameter, false)) {
                valid = false;
            }
            if (isNotValidInterceptionFactoryInjectionPointUsage(parameter, false)) {
                valid = false;
            }
        }

        if (hasNotValidInjectionParameters(constructor.getParameters(), ops.fmtConstructor(constructor))) {
            valid = false;
        }

        return valid;
    }

    private boolean isInterceptionFactoryType(Type type) {
        if (type instanceof Class) {
            return InterceptionFactory.class.equals(type);
        }
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            return raw instanceof Class && InterceptionFactory.class.equals(raw);
        }
        return false;
    }

    @Nonnull
    private static Class<?> getArgClass(Type type) {
        if (!(type instanceof ParameterizedType)) {
            throw new NonPortableBehaviourException(
                    "Non-portable behavior: InterceptionFactory injection point must declare a Java class type parameter");
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] args = parameterizedType.getActualTypeArguments();
        if (args == null || args.length != 1 || !(args[0] instanceof Class)) {
            throw new NonPortableBehaviourException(
                    "Non-portable behavior: InterceptionFactory injection point type parameter must be a Java class");
        }
        return (Class<?>) args[0];
    }

    private boolean isInjectionPointMetadataType(AnnotatedElement injectionPoint) {
        if (injectionPoint instanceof Field) {
            return InjectionPoint.class.equals(((Field) injectionPoint).getType());
        }
        if (injectionPoint instanceof Parameter) {
            return InjectionPoint.class.equals(((Parameter) injectionPoint).getType());
        }
        return false;
    }

    private Class<?> declaringClassOf(AnnotatedElement injectionPoint) {
        if (injectionPoint instanceof Field) {
            return ((Field) injectionPoint).getDeclaringClass();
        }
        if (injectionPoint instanceof Parameter) {
            return ((Parameter) injectionPoint).getDeclaringExecutable().getDeclaringClass();
        }
        return null;
    }

    private boolean validateBeanAndInterceptorMetadataUsage(AnnotatedElement injectionPoint, boolean disposerParameter) {
        boolean beanMetadata = isBeanMetadataType(injectionPoint);
        boolean interceptorMetadata = isInterceptorMetadataType(injectionPoint);
        if (!beanMetadata && !interceptorMetadata) {
            return true;
        }

        Class<?> declaringClass = declaringClassOf(injectionPoint);
        boolean interceptorDeclaringClass = declaringClass != null && ops.isInterceptorClass(declaringClass);
        boolean interceptedQualified = ops.hasQualifier(ops.annotationsOf(injectionPoint), INTERCEPTED);
        boolean defaultQualified = ops.hasDefaultQualifier(ops.annotationsOf(injectionPoint));
        Type metadataArgument = getSingleTypeArgument(injectionPoint);

        if (interceptorMetadata && !interceptorDeclaringClass) {
            knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                    ": Interceptor metadata may only be injected into interceptor instances");
            return false;
        }

        if (interceptedQualified) {
            if (!beanMetadata) {
                knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                        ": qualifier @Intercepted may only be used with Bean metadata");
                return false;
            }
            if (!interceptorDeclaringClass) {
                knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                        ": Bean metadata with qualifier @Intercepted may only be injected into interceptor instances");
                return false;
            }
            if (!isUnboundedWildcard(metadataArgument)) {
                knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                        ": Bean metadata with qualifier @Intercepted must use an unbounded wildcard type parameter");
                return false;
            }
            return true;
        }

        if (disposerParameter) {
            knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                    ": disposer method parameter may not inject Bean or Interceptor metadata");
            return false;
        }

        if (defaultQualified) {
            if (injectionPoint instanceof Field ||
                    isBeanConstructorParameter(injectionPoint) ||
                    isInitializerMethodParameter(injectionPoint)) {
                if (declaringClass == null || isNotSameType(metadataArgument, declaringClass)) {
                    String expected = declaringClass == null ? "<unknown>" : declaringClass.getTypeName();
                    knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                            ": Bean/Interceptor metadata with qualifier @Default must declare type parameter " + expected);
                    return false;
                }
                return true;
            }

            if (isProducerMethodParameter(injectionPoint)) {
                Method producerMethod = (Method) ((Parameter) injectionPoint).getDeclaringExecutable();
                Type expectedType = ops.baseTypeOf(producerMethod);
                if (isNotSameType(metadataArgument, expectedType)) {
                    knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                            ": producer method parameter Bean metadata type must match producer return type " +
                            expectedType.getTypeName());
                    return false;
                }
                return true;
            }
        }

        return true;
    }

    private boolean isBeanMetadataType(AnnotatedElement injectionPoint) {
        Type rawType = metadataRawType(injectionPoint);
        return rawType instanceof Class && Bean.class.equals(rawType);
    }

    private boolean isInterceptorMetadataType(AnnotatedElement injectionPoint) {
        Type rawType = metadataRawType(injectionPoint);
        return rawType instanceof Class && Interceptor.class.equals(rawType);
    }

    private boolean isDecoratorMetadataType(AnnotatedElement injectionPoint) {
        Type rawType = metadataRawType(injectionPoint);
        return rawType instanceof Class && Decorator.class.equals(rawType);
    }

    private boolean isDecoratedBeanMetadataType(AnnotatedElement injectionPoint) {
        return isBeanMetadataType(injectionPoint)
                && ops.hasQualifier(ops.annotationsOf(injectionPoint), DECORATED);
    }

    private Type metadataRawType(AnnotatedElement injectionPoint) {
        Type genericType = metadataGenericType(injectionPoint);
        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }
        return ((ParameterizedType) genericType).getRawType();
    }

    private Type metadataGenericType(AnnotatedElement injectionPoint) {
        if (injectionPoint instanceof Field) {
            return ops.baseTypeOf((Field) injectionPoint);
        }
        if (injectionPoint instanceof Parameter) {
            return ops.baseTypeOf((Parameter) injectionPoint);
        }
        return null;
    }

    private Type getSingleTypeArgument(AnnotatedElement injectionPoint) {
        Type genericType = metadataGenericType(injectionPoint);
        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }
        Type[] args = ((ParameterizedType) genericType).getActualTypeArguments();
        return args.length == 1 ? args[0] : null;
    }

    private boolean isUnboundedWildcard(Type type) {
        if (!(type instanceof WildcardType)) {
            return false;
        }
        WildcardType wildcardType = (WildcardType) type;
        Type[] uppers = wildcardType.getUpperBounds();
        Type[] lowers = wildcardType.getLowerBounds();
        return lowers.length == 0 && uppers.length == 1 && Object.class.equals(uppers[0]);
    }

    private boolean isBeanConstructorParameter(AnnotatedElement injectionPoint) {
        if (!(injectionPoint instanceof Parameter)) {
            return false;
        }
        return ((Parameter) injectionPoint).getDeclaringExecutable() instanceof Constructor;
    }

    private boolean isInitializerMethodParameter(AnnotatedElement injectionPoint) {
        if (!(injectionPoint instanceof Parameter)) {
            return false;
        }
        Executable executable = ((Parameter) injectionPoint).getDeclaringExecutable();
        if (!(executable instanceof Method)) {
            return false;
        }
        Method method = (Method) executable;
        return ops.hasInjectAnnotation(method) && !ops.hasProducesAnnotation(method);
    }

    private boolean isProducerMethodParameter(AnnotatedElement injectionPoint) {
        if (!(injectionPoint instanceof Parameter)) {
            return false;
        }
        Executable executable = ((Parameter) injectionPoint).getDeclaringExecutable();
        return executable instanceof Method && ops.hasProducesAnnotation(executable);
    }

    private boolean validateDecoratorMetadataUsage(AnnotatedElement injectionPoint, boolean disposerParameter) {
        if (disposerParameter) {
            knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                    ": disposer method parameter may not inject Decorator metadata or @Decorated Bean metadata");
            return false;
        }

        Class<?> declaringClass = declaringClassOf(injectionPoint);
        if (!ops.hasDecoratorAnnotation(declaringClass)) {
            knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                    ": Decorator metadata and @Decorated Bean metadata may only be injected into decorator instances");
            return false;
        }

        Type metadataArgument = getSingleTypeArgument(injectionPoint);

        if (isDecoratorMetadataType(injectionPoint) && ops.hasDefaultQualifier(ops.annotationsOf(injectionPoint))) {
            if (isNotSameType(metadataArgument, declaringClass)) {
                knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                        ": Decorator metadata with qualifier @Default must declare type parameter " +
                        declaringClass.getTypeName());
                return false;
            }
        }

        if (isDecoratedBeanMetadataType(injectionPoint)) {
            Type delegateType = ops.findDecoratorDelegateType(declaringClass);
            if (delegateType == null || isNotSameType(metadataArgument, delegateType)) {
                String expected = delegateType == null ? "<unknown>" : delegateType.getTypeName();
                knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                        ": @Decorated Bean metadata must declare the delegate type " + expected);
                return false;
            }
        }

        return true;
    }

    private boolean isNotSameType(Type left, Type right) {
        if (left == null || right == null) {
            return true;
        }
        return !left.getTypeName().equals(right.getTypeName());
    }

    private Annotation findNamedQualifier(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (hasNamedAnnotation(annotation.annotationType())) {
                return annotation;
            }
        }
        return null;
    }

    private String describeInjectionPoint(AnnotatedElement injectionPoint) {
        if (injectionPoint instanceof Parameter) {
            return ops.fmtParameter((Parameter) injectionPoint);
        }
        if (injectionPoint instanceof Field) {
            return ops.fmtField((Field) injectionPoint);
        }
        if (injectionPoint instanceof Method) {
            return ops.fmtMethod((Method) injectionPoint);
        }
        if (injectionPoint instanceof Constructor) {
            return ops.fmtConstructor((Constructor<?>) injectionPoint);
        }
        return injectionPoint.toString();
    }

    private String readNamedValue(Annotation namedAnnotation) {
        try {
            Method value = namedAnnotation.annotationType().getMethod("value");
            Object raw = value.invoke(namedAnnotation);
            return raw == null ? "" : raw.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }
}
