package com.threeamigos.common.util.implementations.injection.discovery.validation.bean;

import com.threeamigos.common.util.implementations.injection.annotations.QualifiersHelper;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.resolution.TypeChecker;
import jakarta.enterprise.inject.spi.DefinitionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates.hasNamedAnnotation;

/**
 * Extracted producer/disposer validation rules and specialization matching.
 */
public class ProducerDisposerValidator {

    public interface Ops {
        boolean hasInjectAnnotation(AnnotatedElement element);

        boolean hasProducesAnnotation(AnnotatedElement element);

        boolean hasDisposesAnnotation(AnnotatedElement element);

        boolean hasObservesAnnotation(AnnotatedElement element);

        boolean hasObservesAsyncAnnotation(AnnotatedElement element);

        boolean hasSpecializesAnnotation(AnnotatedElement element);

        Type baseTypeOf(Field field);

        Type baseTypeOf(Method method);

        Type baseTypeOf(Parameter parameter);

        Set<Type> typeClosureOf(Method method);

        Set<Type> typeClosureOf(Field field);

        Annotation[] annotationsOf(AnnotatedElement element);

        void checkProducerTypeValidity(Type type);

        void validateProducerMethodTypeVariableScopeConstraint(Method method);

        void validateProducerFieldTypeVariableScopeConstraint(Field field);

        void checkInjectionTypeValidity(Type type);

        void validateQualifiers(Annotation[] annotations, String location);

        boolean isNotValidNamedInjectionPointUsage(AnnotatedElement injectionPoint);

        boolean isNotValidInjectionPointMetadataUsage(AnnotatedElement injectionPoint, boolean disposerParameter);

        boolean isNotValidInterceptionFactoryInjectionPointUsage(AnnotatedElement element, boolean producerMethodParameter);

        String extractProducerName(AnnotatedElement element);

        Set<Annotation> extractQualifiers(AnnotatedElement element);

        boolean isAlternativeDeclared(AnnotatedElement element);

        boolean isAlternativeEnabled(AnnotatedElement element, Class<?> declaringClass, boolean alternativeDeclared);

        String fmtField(Field field);

        String fmtMethod(Method method);

        String fmtParameter(Parameter parameter);
    }

    private final KnowledgeBase knowledgeBase;
    private final TypeChecker typeChecker;
    private final Ops ops;
    private final Map<String, Method> specializingProducerMethodsBySpecializedSignature;

    public ProducerDisposerValidator(KnowledgeBase knowledgeBase,
                              TypeChecker typeChecker,
                              Ops ops,
                              Map<String, Method> specializingProducerMethodsBySpecializedSignature) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.typeChecker = Objects.requireNonNull(typeChecker, "typeChecker cannot be null");
        this.ops = Objects.requireNonNull(ops, "ops cannot be null");
        this.specializingProducerMethodsBySpecializedSignature = Objects.requireNonNull(
                specializingProducerMethodsBySpecializedSignature,
                "specializingProducerMethodsBySpecializedSignature cannot be null");
    }

    public boolean validateProducerField(Field field) {
        boolean valid = true;

        if (ops.hasInjectAnnotation(field)) {
            knowledgeBase.addDefinitionError(ops.fmtField(field) + ": producer field may not be annotated @Inject");
            valid = false;
        }

        try {
            ops.checkProducerTypeValidity(ops.baseTypeOf(field));
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(ops.fmtField(field) + ": " + e.getMessage());
            valid = false;
        }

        try {
            ops.validateProducerFieldTypeVariableScopeConstraint(field);
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(ops.fmtField(field) + ": " + e.getMessage());
            valid = false;
        }

        return valid;
    }

    public boolean validateProducerMethod(Method method) {
        boolean valid = true;

        if (Modifier.isAbstract(method.getModifiers())) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": producer method must not be abstract");
            valid = false;
        }

        if (method.getTypeParameters().length > 0) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": producer method must not be generic");
            valid = false;
        }

        if (method.isVarArgs()) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": producer method must not be varargs");
            valid = false;
        }

        if (ops.hasInjectAnnotation(method)) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": producer method must not be annotated @Inject");
            valid = false;
        }

        if (!validateSpecializingProducerMethodConstraint(method)) {
            valid = false;
        }

        try {
            ops.checkProducerTypeValidity(ops.baseTypeOf(method));
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": " + e.getMessage());
            valid = false;
        }

        try {
            ops.validateProducerMethodTypeVariableScopeConstraint(method);
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": " + e.getMessage());
            valid = false;
        }

        for (Parameter parameter : method.getParameters()) {
            if (ops.hasDisposesAnnotation(parameter)) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                        ": producer method parameter may not be annotated @Disposes");
                valid = false;
                continue;
            }
            if (ops.hasObservesAnnotation(parameter)) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                        ": producer method parameter may not be annotated @Observes");
                valid = false;
                continue;
            }
            if (ops.hasObservesAsyncAnnotation(parameter)) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                        ": producer method parameter may not be annotated @ObservesAsync");
                valid = false;
                continue;
            }

            try {
                ops.checkInjectionTypeValidity(ops.baseTypeOf(parameter));
            } catch (IllegalArgumentException e) {
                knowledgeBase.addInjectionError(ops.fmtParameter(parameter) + ": " + e.getMessage());
                valid = false;
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) + ": " + e.getMessage());
                valid = false;
            }
            try {
                ops.validateQualifiers(ops.annotationsOf(parameter), ops.fmtMethod(method));
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) + ": " + e.getMessage());
                valid = false;
            }

            if (ops.isNotValidNamedInjectionPointUsage(parameter)) {
                valid = false;
            }

            if (ops.isNotValidInjectionPointMetadataUsage(parameter, false)) {
                valid = false;
            }
            if (ops.isNotValidInterceptionFactoryInjectionPointUsage(parameter, true)) {
                valid = false;
            }
        }

        return valid;
    }

    public boolean validateDisposerMethod(Method method) {
        boolean valid = true;

        if (Modifier.isAbstract(method.getModifiers())) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": disposer method must not be abstract");
            valid = false;
        }

        if (method.getTypeParameters().length > 0) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": disposer method must not be generic");
            valid = false;
        }

        if (ops.hasProducesAnnotation(method)) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": disposer method may not be annotated @Produces");
            valid = false;
        }

        if (ops.hasInjectAnnotation(method)) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": disposer method may not be annotated @Inject");
            valid = false;
        }

        int disposesCount = 0;
        Parameter disposesParam = null;
        for (Parameter parameter : method.getParameters()) {
            if (ops.hasDisposesAnnotation(parameter)) {
                disposesCount++;
                disposesParam = parameter;
            }
        }

        if (disposesCount == 0) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": disposer method must have exactly one @Disposes parameter (found 0)");
            valid = false;
        } else if (disposesCount > 1) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) + ": disposer method must have exactly one @Disposes parameter (found " + disposesCount + ")");
            valid = false;
        }

        if (disposesParam != null) {
            try {
                ops.checkInjectionTypeValidity(ops.baseTypeOf(disposesParam));
            } catch (IllegalArgumentException e) {
                knowledgeBase.addInjectionError(ops.fmtParameter(disposesParam) + ": " + e.getMessage());
                valid = false;
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(disposesParam) + ": " + e.getMessage());
                valid = false;
            }
        }

        for (Parameter parameter : method.getParameters()) {
            if (ops.hasObservesAnnotation(parameter)) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                        ": disposer method parameter may not be annotated @Observes");
                valid = false;
            }
            if (ops.hasObservesAsyncAnnotation(parameter)) {
                knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) +
                        ": disposer method parameter may not be annotated @ObservesAsync");
                valid = false;
            }
        }

        for (Parameter parameter : method.getParameters()) {
            if (!ops.hasDisposesAnnotation(parameter)) {
                try {
                    ops.checkInjectionTypeValidity(ops.baseTypeOf(parameter));
                } catch (IllegalArgumentException e) {
                    knowledgeBase.addInjectionError(ops.fmtParameter(parameter) + ": " + e.getMessage());
                    valid = false;
                } catch (DefinitionException e) {
                    knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) + ": " + e.getMessage());
                    valid = false;
                }

                try {
                    ops.validateQualifiers(ops.annotationsOf(parameter), ops.fmtMethod(method));
                } catch (DefinitionException e) {
                    knowledgeBase.addDefinitionError(ops.fmtParameter(parameter) + ": " + e.getMessage());
                    valid = false;
                }

                if (ops.isNotValidNamedInjectionPointUsage(parameter)) {
                    valid = false;
                }

                if (ops.isNotValidInjectionPointMetadataUsage(parameter, true)) {
                    valid = false;
                }
            }
        }

        return valid;
    }

    public boolean validateSpecializingProducerMethodConstraint(Method method) {
        if (!ops.hasSpecializesAnnotation(method)) {
            return true;
        }

        if (Modifier.isStatic(method.getModifiers())) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) +
                    ": producer method annotated @Specializes must be non-static");
            return false;
        }

        Class<?> declaringClass = method.getDeclaringClass();
        Class<?> directSuperclass = declaringClass.getSuperclass();
        if (directSuperclass == null || Object.class.equals(directSuperclass)) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) +
                    ": producer method annotated @Specializes must directly override another producer method");
            return false;
        }

        Method overridden = resolveDirectlyOverriddenProducerMethod(method);
        if (overridden == null) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) +
                    ": producer method annotated @Specializes must directly override another producer method");
            return false;
        }

        String inheritedName = ops.extractProducerName(overridden);
        if (!inheritedName.isEmpty() && declaresBeanNameExplicitly(method)) {
            knowledgeBase.addDefinitionError(ops.fmtMethod(method) +
                    ": specializing producer method may not explicitly declare @Named when specialized producer has name '" +
                    inheritedName + "'");
            return false;
        }

        if (isSpecializingProducerMethodEnabled(method)) {
            String specializedSignature = producerMethodSpecializationSignature(overridden);
            Method previousSpecializer = specializingProducerMethodsBySpecializedSignature.get(specializedSignature);
            if (previousSpecializer != null && !previousSpecializer.equals(method)) {
                knowledgeBase.addError(ops.fmtMethod(method) +
                        ": inconsistent specialization. Both " + ops.fmtMethod(previousSpecializer) +
                        " and " + ops.fmtMethod(method) + " specialize " + ops.fmtMethod(overridden));
                return false;
            }
            specializingProducerMethodsBySpecializedSignature.put(specializedSignature, method);
        }

        return true;
    }

    public Method resolveDirectlyOverriddenProducerMethod(Method method) {
        if (method == null) {
            return null;
        }
        Class<?> declaringClass = method.getDeclaringClass();
        Class<?> directSuperclass = declaringClass.getSuperclass();
        if (directSuperclass == null || Object.class.equals(directSuperclass)) {
            return null;
        }
        try {
            Method overridden = directSuperclass.getDeclaredMethod(method.getName(), method.getParameterTypes());
            if (!ops.hasProducesAnnotation(overridden)) {
                return null;
            }
            return overridden;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public boolean isSpecializingProducerMethodEnabled(Method method) {
        if (method == null) {
            return false;
        }
        Class<?> declaringClass = method.getDeclaringClass();
        boolean methodAlternative = ops.isAlternativeDeclared(method);
        boolean classAlternative = ops.isAlternativeDeclared(declaringClass);
        if (!methodAlternative && !classAlternative) {
            return true;
        }
        AnnotatedElement enablementElement = methodAlternative ? method : declaringClass;
        return ops.isAlternativeEnabled(enablementElement, declaringClass, true);
    }

    public String producerMethodSpecializationSignature(Method method) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getDeclaringClass().getName())
                .append("#")
                .append(method.getName())
                .append("(");
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                signature.append(",");
            }
            signature.append(parameterTypes[i].getName());
        }
        signature.append(")");
        return signature.toString();
    }

    public Method findDisposerForProducer(Class<?> clazz,
                                          Set<Type> producerTypes,
                                          Set<Annotation> producerQualifiers) {
        List<Method> matches = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            Parameter disposesParameter = getDisposesParameter(method);
            if (disposesParameter == null) {
                continue;
            }

            for (Type producerType : producerTypes) {
                if (matchesDisposesParameter(disposesParameter, producerType, producerQualifiers)) {
                    matches.add(method);
                    break;
                }
            }
        }

        if (matches.size() <= 1) {
            return matches.isEmpty() ? null : matches.get(0);
        }

        knowledgeBase.addDefinitionError(clazz.getName() +
                ": multiple disposer methods match producer types " + producerTypes +
                " and qualifiers " + formatQualifiers(producerQualifiers));
        return matches.get(0);
    }

    public boolean validateDisposerMethodHasMatchingProducer(Class<?> clazz, Method disposerMethod) {
        Parameter disposesParameter = getDisposesParameter(disposerMethod);
        if (disposesParameter == null) {
            return false;
        }

        Type disposesType = ops.baseTypeOf(disposesParameter);
        Set<Annotation> requiredQualifiers = QualifiersHelper.extractQualifiers(ops.annotationsOf(disposesParameter));

        for (Method producerMethod : clazz.getDeclaredMethods()) {
            if (!ops.hasProducesAnnotation(producerMethod)) {
                continue;
            }
            for (Type producerType : ops.typeClosureOf(producerMethod)) {
                if (matchesDisposesParameter(disposesParameter, producerType, ops.extractQualifiers(producerMethod))) {
                    return true;
                }
            }
        }

        for (Field producerField : clazz.getDeclaredFields()) {
            if (!ops.hasProducesAnnotation(producerField)) {
                continue;
            }
            for (Type producerType : ops.typeClosureOf(producerField)) {
                if (matchesDisposesParameter(disposesParameter, producerType, ops.extractQualifiers(producerField))) {
                    return true;
                }
            }
        }

        knowledgeBase.addDefinitionError(ops.fmtMethod(disposerMethod) +
                ": @Disposes parameter type/qualifiers do not match any producer method return type or producer field type " +
                "(type=" + disposesType.getTypeName() + ", qualifiers=" + formatQualifiers(requiredQualifiers) + ")");
        return false;
    }

    public Parameter getDisposesParameter(Method method) {
        for (Parameter param : method.getParameters()) {
            if (ops.hasDisposesAnnotation(param)) {
                return param;
            }
        }
        return null;
    }

    public boolean hasDisposesParameter(Method method) {
        for (Parameter param : method.getParameters()) {
            if (ops.hasDisposesAnnotation(param)) {
                return true;
            }
        }
        return false;
    }

    private boolean declaresBeanNameExplicitly(AnnotatedElement element) {
        if (element == null) {
            return false;
        }
        for (Annotation annotation : ops.annotationsOf(element)) {
            if (annotation != null && hasNamedAnnotation(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesDisposesParameter(Parameter disposesParameter,
                                             Type producerType,
                                             Set<Annotation> producerQualifiers) {
        Set<Annotation> requiredQualifiers = QualifiersHelper.extractQualifiers(ops.annotationsOf(disposesParameter));
        if (!QualifiersHelper.qualifiersMatch(requiredQualifiers, producerQualifiers)) {
            return false;
        }

        Type disposesType = ops.baseTypeOf(disposesParameter);
        try {
            return typeChecker.isAssignable(disposesType, producerType);
        } catch (DefinitionException e) {
            return false;
        } catch (IllegalStateException e) {
            knowledgeBase.addDefinitionError(ops.fmtParameter(disposesParameter) +
                    ": failed to compare @Disposes type against producer type " + producerType.getTypeName() +
                    " (" + e.getMessage() + ")");
            return false;
        }
    }

    private String formatQualifiers(Set<Annotation> qualifiers) {
        return qualifiers.stream()
                .map(q -> "@" + q.annotationType().getSimpleName())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
