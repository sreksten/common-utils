package com.threeamigos.common.util.implementations.injection.discovery.validation.bean;

import com.threeamigos.common.util.implementations.injection.annotations.QualifiersHelper;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.discovery.BeanTypesExtractor;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.resolution.BeanImpl;
import com.threeamigos.common.util.implementations.injection.resolution.GenericTypeResolver;
import com.threeamigos.common.util.implementations.injection.resolution.ProducerBean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Named;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationExtractors.getPriorityValue;
import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates.hasNamedAnnotation;
import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates.normalizeSingletonToApplicationScoped;

/**
 * Extracted bean/producer registration logic from CDI41BeanValidator.
 */
public class BeanRegistrationService {

    public interface Ops {
        boolean isAlternativeDeclared(AnnotatedElement element);

        boolean isAlternativeEnabled(AnnotatedElement element, Class<?> declaringClass, boolean alternativeDeclared);

        Method resolveDirectlyOverriddenProducerMethod(Method method);

        boolean hasSpecializesAnnotation(AnnotatedElement element);

        boolean isSpecializingProducerMethodEnabled(Method method);

        String producerMethodSpecializationSignature(Method method);

        boolean hasDisposesAnnotation(AnnotatedElement element);

        InjectionPoint tryCreateInjectionPoint(AnnotatedElement element, Bean<?> owningBean);

        Method findDisposerForProducer(Class<?> clazz, Set<Type> producerTypes, Set<Annotation> producerQualifiers);

        Annotation[] annotationsOf(AnnotatedElement element);

        boolean isQualifierAnnotationType(Class<? extends Annotation> annotationType);

        boolean isStereotypeAnnotationType(Class<? extends Annotation> annotationType);

        boolean isScopeAnnotationType(Class<? extends Annotation> annotationType);

        Class<? extends Annotation> extractScopeFromStereotype(Class<? extends Annotation> stereotypeClass);

        Integer extractEffectivePriority(Class<?> clazz);

        String extractBeanName(Class<?> clazz);

        Set<Annotation> extractBeanQualifiers(Class<?> clazz);

        Class<? extends Annotation> extractBeanScope(Class<?> clazz, Class<? extends Annotation> discoveredScope);

        void validateManagedBeanPublicFieldScopeConstraint(Class<?> clazz, Class<? extends Annotation> scopeAnnotation);

        void validateManagedBeanGenericTypeScopeConstraint(Class<?> clazz, Class<? extends Annotation> scopeAnnotation);

        void validateProgrammaticPassivatingScopeConstraint(Class<?> clazz, Class<? extends Annotation> scopeAnnotation);

        Set<Class<? extends Annotation>> extractBeanStereotypes(Class<?> clazz);

        void addValidationError(Class<?> clazz, String message);

        void addGenericSelfTypeForManagedBean(BeanImpl<?> bean, Class<?> beanClass);

        void applySpecializationInheritance(BeanImpl<?> bean, Class<?> clazz, BeanArchiveMode beanArchiveMode);

        <T> void populateInjectionMetadata(BeanImpl<T> bean, Class<T> clazz);

        List<Class<?>> collectClassHierarchy(Class<?> clazz);

        boolean hasInjectAnnotation(AnnotatedElement element);

        boolean isOverriddenForInjectionMetadata(Method superMethod, Class<?> leafClass);

        InjectionPoint resolvedInjectionPoint(InjectionPoint delegate, Type resolvedType);

        Annotated annotatedOf(AnnotatedElement element);

        Set<Type> typeClosureOf(Method method);

        Set<Type> typeClosureOf(Field field);

        Type baseTypeOf(Method method);

        Type baseTypeOf(Field field);

        Type baseTypeOf(java.lang.reflect.Parameter parameter);

        String fmtMethod(Method method);

        String fmtField(Field field);
    }

    private final KnowledgeBase knowledgeBase;
    private final BeanTypesExtractor beanTypesExtractor;
    private final Ops ops;
    private final Map<String, ProducerBean<?>> producerBeansByMethodSignature;
    private final Set<String> suppressedSpecializedProducerMethodSignatures;

    public BeanRegistrationService(KnowledgeBase knowledgeBase,
                                   BeanTypesExtractor beanTypesExtractor,
                                   Ops ops,
                                   Map<String, ProducerBean<?>> producerBeansByMethodSignature,
                                   Set<String> suppressedSpecializedProducerMethodSignatures) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.beanTypesExtractor = Objects.requireNonNull(beanTypesExtractor, "beanTypesExtractor cannot be null");
        this.ops = Objects.requireNonNull(ops, "ops cannot be null");
        this.producerBeansByMethodSignature = Objects.requireNonNull(
                producerBeansByMethodSignature, "producerBeansByMethodSignature cannot be null");
        this.suppressedSpecializedProducerMethodSignatures = Objects.requireNonNull(
                suppressedSpecializedProducerMethodSignatures, "suppressedSpecializedProducerMethodSignatures cannot be null");
    }

    public void createAndRegisterProducerBean(Class<?> declaringClass,
                                              Method producerMethod,
                                              Field producerField,
                                              AnnotatedType<?> currentAnnotatedTypeOverride) {
        AnnotatedElement element = (producerMethod != null) ? producerMethod : producerField;

        boolean annotatedAlternative = ops.isAlternativeDeclared(element);
        boolean classAlternative = ops.isAlternativeDeclared(declaringClass);
        boolean producerAlternativeDeclared = annotatedAlternative || classAlternative;
        boolean alternativeEnabled = true;
        if (producerAlternativeDeclared) {
            AnnotatedElement enablementElement = annotatedAlternative ? element : declaringClass;
            alternativeEnabled = ops.isAlternativeEnabled(enablementElement, declaringClass, true);
        }

        ProducerBean<?> producerBean;
        if (producerMethod != null) {
            producerBean = new ProducerBean<>(declaringClass, producerMethod, annotatedAlternative);
            producerBean.setAlternativeEnabled(alternativeEnabled);

            Method specializedProducerMethod = ops.resolveDirectlyOverriddenProducerMethod(producerMethod);
            boolean specializesProducerMethod = ops.hasSpecializesAnnotation(producerMethod) && specializedProducerMethod != null;

            String producerName = extractProducerName(producerMethod);
            Set<Annotation> producerQualifiers = extractQualifiers(producerMethod);

            if (specializesProducerMethod) {
                String inheritedName = extractProducerName(specializedProducerMethod);
                if (producerName.isEmpty() && !inheritedName.isEmpty()) {
                    producerName = inheritedName;
                }
                producerQualifiers.addAll(extractQualifiers(specializedProducerMethod));
            }

            producerBean.setName(producerName);
            producerBean.setQualifiers(synchronizeNamedQualifier(
                    QualifiersHelper.normalizeBeanQualifiers(producerQualifiers), producerName));
            producerBean.setScope(extractScope(producerMethod));
            producerBean.setStereotypes(extractStereotypes(producerMethod));

            BeanTypesExtractor.ExtractionResult producerTypes =
                    beanTypesExtractor.extractProducerBeanTypes(ops.baseTypeOf(producerMethod), producerMethod);
            for (String error : producerTypes.getDefinitionErrors()) {
                knowledgeBase.addDefinitionError(ops.fmtMethod(producerMethod) + ": " + error);
            }
            Set<Type> resolvedProducerTypes = new LinkedHashSet<>(
                    resolveProducerMethodBeanTypes(producerMethod, producerTypes.getTypes(), currentAnnotatedTypeOverride));
            if (specializesProducerMethod) {
                BeanTypesExtractor.ExtractionResult specializedProducerTypes =
                        beanTypesExtractor.extractProducerBeanTypes(ops.baseTypeOf(specializedProducerMethod), specializedProducerMethod);
                for (String error : specializedProducerTypes.getDefinitionErrors()) {
                    knowledgeBase.addDefinitionError(ops.fmtMethod(specializedProducerMethod) + ": " + error);
                }
                resolvedProducerTypes.addAll(
                        resolveProducerMethodBeanTypes(
                                specializedProducerMethod,
                                specializedProducerTypes.getTypes(),
                                currentAnnotatedTypeOverride));
            }
            producerBean.setTypes(resolvedProducerTypes);

            for (java.lang.reflect.Parameter param : producerMethod.getParameters()) {
                if (!ops.hasDisposesAnnotation(param)) {
                    InjectionPoint ip = ops.tryCreateInjectionPoint(param, producerBean);
                    if (ip != null) {
                        producerBean.addInjectionPoint(ip);
                    }
                }
            }
        } else if (producerField != null) {
            producerBean = new ProducerBean<>(declaringClass, producerField, annotatedAlternative);
            producerBean.setAlternativeEnabled(alternativeEnabled);

            String producerName = extractProducerName(producerField);
            producerBean.setName(producerName);
            producerBean.setQualifiers(synchronizeNamedQualifier(
                    QualifiersHelper.normalizeBeanQualifiers(extractQualifiers(producerField)), producerName));
            producerBean.setScope(extractScope(producerField));
            producerBean.setStereotypes(extractStereotypes(producerField));

            BeanTypesExtractor.ExtractionResult producerTypes =
                    beanTypesExtractor.extractProducerBeanTypes(ops.baseTypeOf(producerField), producerField);
            for (String error : producerTypes.getDefinitionErrors()) {
                knowledgeBase.addDefinitionError(ops.fmtField(producerField) + ": " + error);
            }
            producerBean.setTypes(resolveProducerFieldBeanTypes(producerField, producerTypes.getTypes(), currentAnnotatedTypeOverride));
        } else {
            throw new IllegalArgumentException("Either producerMethod or producerField must be non-null");
        }

        Method disposer = ops.findDisposerForProducer(declaringClass, producerBean.getTypes(), producerBean.getQualifiers());
        if (disposer != null) {
            producerBean.setDisposerMethod(disposer);
            Set<Integer> disposerPositions = new LinkedHashSet<>();
            java.lang.reflect.Parameter[] disposerParameters = disposer.getParameters();
            for (int i = 0; i < disposerParameters.length; i++) {
                java.lang.reflect.Parameter parameter = disposerParameters[i];
                if (ops.hasDisposesAnnotation(parameter)) {
                    disposerPositions.add(i);
                    continue;
                }
                InjectionPoint ip = ops.tryCreateInjectionPoint(parameter, producerBean);
                if (ip != null) {
                    producerBean.addInjectionPoint(ip);
                }
            }
            producerBean.setDisposerParameterPositions(disposerPositions);
        }

        if (knowledgeBase.isTypeVetoed(declaringClass)) {
            producerBean.setVetoed(true);
            System.out.println("[CDI41BeanValidator] Producer bean marked as vetoed (declaring class vetoed): " +
                    declaringClass.getName() + " -> " +
                    (producerMethod != null ? producerMethod.getName() : producerField.getName()));
        }

        Integer priorityValue = getPriorityValue(element);
        if (priorityValue == null) {
            priorityValue = ops.extractEffectivePriority(declaringClass);
        }
        if (priorityValue != null) {
            producerBean.setPriority(priorityValue);
        }

        if (producerMethod != null) {
            String producerMethodSignature = ops.producerMethodSpecializationSignature(producerMethod);
            producerBeansByMethodSignature.put(producerMethodSignature, producerBean);

            if (suppressedSpecializedProducerMethodSignatures.contains(producerMethodSignature)) {
                return;
            }

            Method specializedProducerMethod = ops.resolveDirectlyOverriddenProducerMethod(producerMethod);
            if (ops.hasSpecializesAnnotation(producerMethod)
                    && specializedProducerMethod != null
                    && ops.isSpecializingProducerMethodEnabled(producerMethod)) {
                String specializedSignature = ops.producerMethodSpecializationSignature(specializedProducerMethod);
                suppressedSpecializedProducerMethodSignatures.add(specializedSignature);
                ProducerBean<?> specializedProducerBean = producerBeansByMethodSignature.get(specializedSignature);
                if (specializedProducerBean != null) {
                    knowledgeBase.getProducerBeans().remove(specializedProducerBean);
                    knowledgeBase.getBeans().remove(specializedProducerBean);
                }
            }
        }

        knowledgeBase.addProducerBean(producerBean);
    }

    public <T> BeanImpl<T> createAndRegisterManagedBean(Class<T> clazz,
                                                        BeanArchiveMode beanArchiveMode,
                                                        AnnotatedType<T> annotatedTypeOverride,
                                                        AnnotatedType<?> currentAnnotatedTypeOverride,
                                                        boolean alternative,
                                                        boolean alternativeEnabled,
                                                        boolean valid,
                                                        Class<? extends Annotation> discoveredBeanScope) {
        BeanImpl<T> bean = new BeanImpl<>(clazz, alternative);
        if (annotatedTypeOverride != null) {
            bean.setAnnotatedTypeMetadata(annotatedTypeOverride);
        }
        bean.setAlternativeEnabled(alternativeEnabled);
        bean.setPriority(ops.extractEffectivePriority(clazz));

        if (!valid) {
            bean.setHasValidationErrors(true);
        }

        if (knowledgeBase.isTypeVetoed(clazz)) {
            bean.setVetoed(true);
            System.out.println("[CDI41BeanValidator] Bean marked as vetoed: " + clazz.getName());
        }

        String beanName = ops.extractBeanName(clazz);
        bean.setName(beanName);
        bean.setQualifiers(synchronizeNamedQualifier(ops.extractBeanQualifiers(clazz), beanName));
        Class<? extends Annotation> effectiveBeanScope = ops.extractBeanScope(clazz, discoveredBeanScope);
        ops.validateManagedBeanPublicFieldScopeConstraint(clazz, effectiveBeanScope);
        ops.validateManagedBeanGenericTypeScopeConstraint(clazz, effectiveBeanScope);
        ops.validateProgrammaticPassivatingScopeConstraint(clazz, effectiveBeanScope);
        bean.setScope(effectiveBeanScope);

        BeanTypesExtractor.ExtractionResult managedBeanTypes = beanTypesExtractor.extractManagedBeanTypes(clazz);
        for (String error : managedBeanTypes.getDefinitionErrors()) {
            ops.addValidationError(clazz, error);
        }
        Set<Type> managedTypes = managedBeanTypes.getTypes();
        if (currentAnnotatedTypeOverride != null && currentAnnotatedTypeOverride.getJavaClass().equals(clazz)) {
            Set<Type> overrideTypes = new LinkedHashSet<>(currentAnnotatedTypeOverride.getTypeClosure());
            if (!overrideTypes.isEmpty()) {
                managedTypes = overrideTypes;
            }
        }
        bean.setTypes(managedTypes);
        ops.addGenericSelfTypeForManagedBean(bean, clazz);
        ops.applySpecializationInheritance(bean, clazz, beanArchiveMode);

        bean.setStereotypes(ops.extractBeanStereotypes(clazz));
        ops.populateInjectionMetadata(bean, clazz);

        List<Class<?>> hierarchy = ops.collectClassHierarchy(clazz);
        for (Class<?> declaringClass : hierarchy) {
            for (Field field : declaringClass.getDeclaredFields()) {
                if (ops.hasInjectAnnotation(field)) {
                    InjectionPoint ip = ops.resolvedInjectionPoint(
                            ops.tryCreateInjectionPoint(field, bean),
                            GenericTypeResolver.resolve(ops.baseTypeOf(field), clazz, field.getDeclaringClass()));
                    if (ip != null) {
                        bean.addInjectionPoint(ip);
                    }
                }
            }
        }
        for (Class<?> declaringClass : hierarchy) {
            for (Method method : declaringClass.getDeclaredMethods()) {
                if (!ops.hasInjectAnnotation(method)) {
                    continue;
                }
                if (ops.isOverriddenForInjectionMetadata(method, clazz)) {
                    continue;
                }
                for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                    InjectionPoint ip = ops.resolvedInjectionPoint(
                            ops.tryCreateInjectionPoint(parameter, bean),
                            GenericTypeResolver.resolve(ops.baseTypeOf(parameter), clazz, method.getDeclaringClass()));
                    if (ip != null) {
                        bean.addInjectionPoint(ip);
                    }
                }
            }
        }
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (ops.hasInjectAnnotation(constructor)) {
                for (java.lang.reflect.Parameter parameter : constructor.getParameters()) {
                    InjectionPoint ip = ops.resolvedInjectionPoint(
                            ops.tryCreateInjectionPoint(parameter, bean),
                            GenericTypeResolver.resolve(ops.baseTypeOf(parameter), clazz, constructor.getDeclaringClass()));
                    if (ip != null) {
                        bean.addInjectionPoint(ip);
                    }
                }
            }
        }

        knowledgeBase.addBean(bean);
        return bean;
    }

    public String extractProducerName(AnnotatedElement element) {
        Annotation named = findNamedQualifier(ops.annotationsOf(element));
        if (named != null) {
            String value = readNamedValue(named);
            if (value != null && !value.isEmpty()) {
                return value;
            }
            if (element instanceof Field) {
                return ((Field) element).getName();
            } else if (element instanceof Method) {
                String methodName = ((Method) element).getName();
                if (methodName.startsWith("get") && methodName.length() > 3) {
                    return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                }
                return methodName;
            }
        }
        return "";
    }

    public Set<Annotation> extractQualifiers(AnnotatedElement element) {
        Annotation[] annotations = ops.annotationsOf(element);
        Set<Annotation> qualifiers = QualifiersHelper.extractQualifierAnnotations(annotations);
        for (Annotation annotation : annotations) {
            if (ops.isQualifierAnnotationType(annotation.annotationType())) {
                qualifiers.add(annotation);
            }
        }
        return QualifiersHelper.normalizeBeanQualifiers(qualifiers);
    }

    public Set<Annotation> synchronizeNamedQualifier(Set<Annotation> qualifiers, String beanName) {
        Set<Annotation> normalized = new LinkedHashSet<>();
        Annotation existingNamed = null;
        if (qualifiers != null) {
            for (Annotation qualifier : qualifiers) {
                if (qualifier == null) {
                    continue;
                }
                if (hasNamedAnnotation(qualifier.annotationType())) {
                    if (existingNamed == null) {
                        existingNamed = qualifier;
                    }
                    continue;
                }
                normalized.add(qualifier);
            }
        }

        if (existingNamed == null) {
            return normalized;
        }

        String resolvedName = normalizeBeanName(beanName);
        if (resolvedName == null) {
            normalized.add(existingNamed);
            return normalized;
        }

        Class<? extends Annotation> namedType = existingNamed.annotationType();
        normalized.add(createNamedQualifier(namedType, resolvedName));
        return normalized;
    }

    public Class<? extends Annotation> extractScope(AnnotatedElement element) {
        List<Class<? extends Annotation>> directScopes = new ArrayList<>();
        for (Annotation ann : ops.annotationsOf(element)) {
            Class<? extends Annotation> annotationType = ann.annotationType();
            if (ops.isScopeAnnotationType(annotationType)) {
                directScopes.add(annotationType);
            }
        }

        if (directScopes.size() > 1) {
            String scopeNames = directScopes.stream()
                    .map(scope -> "@" + scope.getSimpleName())
                    .collect(Collectors.joining(", "));
            knowledgeBase.addDefinitionError(describeAnnotatedElement(element) +
                    ": declares multiple scope annotations: " + scopeNames);
        }

        if (!directScopes.isEmpty()) {
            return normalizeSingletonToApplicationScoped(directScopes.get(0));
        }

        Class<? extends Annotation> inheritedScope = null;
        for (Annotation ann : ops.annotationsOf(element)) {
            Class<? extends Annotation> annotationType = ann.annotationType();
            if (!ops.isStereotypeAnnotationType(annotationType)) {
                continue;
            }

            Class<? extends Annotation> stereotypeScope = ops.extractScopeFromStereotype(annotationType);
            if (stereotypeScope == null) {
                continue;
            }

            if (inheritedScope == null) {
                inheritedScope = stereotypeScope;
            } else if (!inheritedScope.equals(stereotypeScope)) {
                knowledgeBase.addDefinitionError(describeAnnotatedElement(element) +
                        ": conflicting scopes inherited from stereotypes (" +
                        inheritedScope.getName() + " vs " + stereotypeScope.getName() + ")");
            }
        }

        if (inheritedScope != null) {
            return normalizeSingletonToApplicationScoped(inheritedScope);
        }

        return Dependent.class;
    }

    public Set<Class<? extends Annotation>> extractStereotypes(AnnotatedElement element) {
        Set<Class<? extends Annotation>> stereotypes = new HashSet<>();
        for (Annotation annotation : ops.annotationsOf(element)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (ops.isStereotypeAnnotationType(annotationType)) {
                stereotypes.add(annotationType);
            }
        }
        return stereotypes;
    }

    private Set<Type> resolveProducerMethodBeanTypes(Method producerMethod,
                                                     Set<Type> extractedTypes,
                                                     AnnotatedType<?> currentAnnotatedTypeOverride) {
        if (producerMethod == null || currentAnnotatedTypeOverride == null) {
            return extractedTypes;
        }
        Annotated annotated = ops.annotatedOf(producerMethod);
        if (annotated == null) {
            return extractedTypes;
        }
        Set<Type> overrideTypes = ops.typeClosureOf(producerMethod);
        if (overrideTypes.isEmpty()) {
            return extractedTypes;
        }
        Set<Type> resolved = new LinkedHashSet<>(overrideTypes);
        resolved.add(Object.class);
        return resolved;
    }

    private Set<Type> resolveProducerFieldBeanTypes(Field producerField,
                                                    Set<Type> extractedTypes,
                                                    AnnotatedType<?> currentAnnotatedTypeOverride) {
        if (producerField == null || currentAnnotatedTypeOverride == null) {
            return extractedTypes;
        }
        Annotated annotated = ops.annotatedOf(producerField);
        if (annotated == null) {
            return extractedTypes;
        }
        Set<Type> overrideTypes = ops.typeClosureOf(producerField);
        if (overrideTypes.isEmpty()) {
            return extractedTypes;
        }
        return new LinkedHashSet<>(overrideTypes);
    }

    private Annotation findNamedQualifier(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (hasNamedAnnotation(annotation.annotationType())) {
                return annotation;
            }
        }
        return null;
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

    private String normalizeBeanName(String beanName) {
        if (beanName == null) {
            return null;
        }
        String trimmed = beanName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Annotation createNamedQualifier(Class<? extends Annotation> namedType, String value) {
        final Class<? extends Annotation> effectiveType = namedType == null ? Named.class : namedType;
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            switch (methodName) {
                case "annotationType":
                    return effectiveType;
                case "value":
                    return value;
                case "equals":
                    Object other = args == null || args.length == 0 ? null : args[0];
                    if (!effectiveType.isInstance(other)) {
                        return false;
                    }
                    try {
                        Method otherValueMethod = effectiveType.getMethod("value");
                        Object otherValue = otherValueMethod.invoke(other);
                        return Objects.equals(value, otherValue);
                    } catch (ReflectiveOperationException ignored) {
                        return false;
                    }
                case "hashCode":
                    return (127 * "value".hashCode()) ^ Objects.hashCode(value);
                case "toString":
                    return "@" + effectiveType.getName() + "(value=" + value + ")";
            }
            throw new UnsupportedOperationException("Unsupported @Named literal method: " + methodName);
        };
        return (Annotation) Proxy.newProxyInstance(
                effectiveType.getClassLoader(),
                new Class<?>[]{effectiveType},
                handler);
    }

    private String describeAnnotatedElement(AnnotatedElement element) {
        if (element instanceof Field) {
            return ops.fmtField((Field) element);
        }
        if (element instanceof Method) {
            return ops.fmtMethod((Method) element);
        }
        if (element instanceof Class) {
            return ((Class<?>) element).getName();
        }
        return element.toString();
    }
}
