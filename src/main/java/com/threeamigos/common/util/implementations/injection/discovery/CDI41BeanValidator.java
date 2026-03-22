package com.threeamigos.common.util.implementations.injection.discovery;

import com.threeamigos.common.util.implementations.injection.*;
import com.threeamigos.common.util.implementations.injection.knowledgebase.DecoratorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.InterceptorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.scopes.InjectionPointImpl;
import com.threeamigos.common.util.implementations.injection.util.QualifiersHelper;
import com.threeamigos.common.util.implementations.injection.resolution.BeanImpl;
import com.threeamigos.common.util.implementations.injection.resolution.ProducerBean;
import com.threeamigos.common.util.implementations.injection.resolution.TypeChecker;
import com.threeamigos.common.util.implementations.injection.util.RawTypeExtractor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.enterprise.context.*;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;

/**
 * Validates that a Java class is a CDI Managed Bean, according to CDI 4.1 rules.
 *
 * <p><b>IMPORTANT: Alternative Bean Enabling (CDI 4.1 Section 5.1.3)</b>
 * <ul>
 *   <li>Without beans.xml support, alternatives MUST have {@literal @}Priority to be enabled</li>
 *   <li>Alternatives without {@literal @}Priority are NOT enabled and will be skipped</li>
 *   <li>This matches CDI 4.1 spec: alternatives enabled via beans.xml OR {@literal @}Priority</li>
 * </ul>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Validate bean class eligibility and constructor rules</li>
 *   <li>Validate injection points (@Inject fields / initializer methods / parameters)</li>
 *   <li>Validate producer fields/methods (@Produces) and ensure illegal combinations are rejected</li>
 *   <li>Check if alternatives are properly enabled (must have {@literal @}Priority without beans.xml)</li>
 *   <li>Report problems to {@link KnowledgeBase} as definition errors or injection errors</li>
 *   <li>Produce a {@link BeanImpl} and register it in the {@link KnowledgeBase} on success</li>
 * </ul>
 */
public class CDI41BeanValidator {

    private final KnowledgeBase knowledgeBase;
    private final BeanTypesExtractor beanTypesExtractor;
    private final TypeChecker typeChecker;
    private Annotation[] overrideAnnotations;

    public CDI41BeanValidator(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.beanTypesExtractor = new BeanTypesExtractor();
        this.typeChecker = new TypeChecker();
    }

    /**
     * Validates the class and returns a Bean if valid, otherwise returns null.
     * If valid, the Bean is registered in the KnowledgeBase.
     */
    <T> BeanImpl<T> validateAndRegister(Class<T> clazz, BeanArchiveMode beanArchiveMode) {
        return validateAndRegister(clazz, beanArchiveMode, null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public BeanImpl<?> validateAndRegisterRaw(Class<?> clazz, BeanArchiveMode beanArchiveMode, AnnotatedType<?> annotatedTypeOverride) {
        return validateAndRegister((Class) clazz, beanArchiveMode, (AnnotatedType) annotatedTypeOverride);
    }

    <T> BeanImpl<T> validateAndRegister(Class<T> clazz,
                                        BeanArchiveMode beanArchiveMode,
                                        AnnotatedType<T> annotatedTypeOverride) {
        Objects.requireNonNull(clazz, "Class cannot be null");

        boolean valid = true;
        try {
            overrideAnnotations = annotatedTypeOverride != null
                    ? annotatedTypeOverride.getAnnotations().toArray(new Annotation[0])
                    : null;

        // CDI 4.1 §2.4.2: scope types with attributes are non-portable.
        validateNonPortableScopeTypes(clazz);
        // CDI 4.1 §5.2.6: qualifier members that are array/annotation-valued should be @Nonbinding.
        validateNonPortableQualifierMembers(clazz);
        // CDI 4.1 §2.8: a stereotype may declare at most one scope.
        validateStereotypeScopeDeclaration(clazz);
        // CDI 4.1 §2.8.1.3: non-empty @Named on stereotype is a definition error.
        validateStereotypeNamedDeclaration(clazz);
        // CDI 4.1 §2.8: stereotype qualifier/@Typed misuse is non-portable.
        validateStereotypeNonPortableDeclarations(clazz);
        // CDI 4.1 §2.8.1.6: target compatibility for stereotypes-with-stereotypes.
        validateStereotypeTargetCompatibility(clazz);
        // CDI Lite §8: only interceptor binding based interception is portable.
        validateNonPortableInterceptionForms(clazz);

        // 1) Bean class eligibility (managed bean type)
        if (!isCandidateBeanClass(clazz, beanArchiveMode)) {
            // Not necessarily an error; just not a bean (CDI scans lots of classes).
            return null;
        }

        // CDI 4.1 §2.8.1.5: if stereotypes declare different priorities,
        // the bean must explicitly declare @Priority.
        validateStereotypePriorityDeclaration(clazz);

        if (hasVetoedAnnotation(clazz)) {
            return null;
        }

        // 2) Basic structural constraints
        if (clazz.isInterface() || clazz.isAnnotation() || clazz.isEnum() || clazz.isPrimitive() || clazz.isArray()) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": is not a valid CDI bean class type");
            return null;
        }

        if (Modifier.isAbstract(clazz.getModifiers())) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": bean class must not be abstract");
            // Abstract classes are not managed beans, but abstract producer/disposer methods are still definition errors.
            for (Method method : clazz.getDeclaredMethods()) {
                if (hasProducesAnnotation(method) && Modifier.isAbstract(method.getModifiers())) {
                    knowledgeBase.addDefinitionError(fmtMethod(method) + ": producer method must not be abstract");
                }
                if (hasDisposesParameter(method) && Modifier.isAbstract(method.getModifiers())) {
                    knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method must not be abstract");
                }
            }
            return null;
        }

        if (clazz.isLocalClass() || clazz.isAnonymousClass() || clazz.isSynthetic()) {
            // Compiler-generated helper classes can appear during discovery and must be ignored.
            return null;
        }

        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": non-static inner classes are not valid CDI beans");
            return null;
        }

        validateManagedBeanSpecializationConstraint(clazz, beanArchiveMode);

        // 3) Scope sanity (at most one scope) + capture it for bean construction
        Class<? extends Annotation> beanScope = null;
        try {
            beanScope = validateScopeAnnotations(clazz);
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": " + e.getMessage());
            valid = false;
        }

        boolean isInterceptor = hasInterceptorAnnotation(clazz);
        boolean isDecorator = hasDecoratorAnnotation(clazz);

        // 4) Validate producers and injection points on fields/methods
        boolean hasInjectionPoints = false;

        // Track producers to detect ambiguous overloads (same qualifiers + same raw/boxed type)
        Map<String, String> producerSignatureOwners = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            boolean inject = hasInjectAnnotation(field);
            boolean produces = hasProducesAnnotation(field);

            if (inject) {
                hasInjectionPoints = true;
                valid &= validateInjectField(field);
            }

            if (produces) {
                if (isInterceptor) {
                    knowledgeBase.addDefinitionError(fmtField(field) +
                            ": interceptor may not declare producer fields");
                    valid = false;
                } else {
                    valid &= validateProducerField(field);

                    // Ambiguity check for overloaded producers
                    String signatureKey = producerSignatureKey(field.getGenericType(), extractQualifiers(field));
                    if (producerSignatureOwners.containsKey(signatureKey)) {
                        knowledgeBase.addDefinitionError(fmtField(field) +
                            ": producer conflicts with " + producerSignatureOwners.get(signatureKey) +
                            " (same bean type/qualifiers). Overloaded producers for the same bean are not allowed.");
                        valid = false;
                    } else {
                        producerSignatureOwners.put(signatureKey, fmtField(field));
                    }

                    // Create and register ProducerBean for this producer field
                    createAndRegisterProducerBean(clazz, null, field);
                }
            }

            // Disallow illegal combos proactively
            if (inject && produces) {
                knowledgeBase.addDefinitionError(fmtField(field) + ": may not declare both @Inject and @Produces");
                valid = false;
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            boolean inject = hasInjectAnnotation(method);
            boolean produces = hasProducesAnnotation(method);
            boolean disposes = hasDisposesParameter(method);

            if (inject) {
                hasInjectionPoints = true;
                valid &= validateInitializerMethod(method);
            }

            if (produces) {
                if (isInterceptor) {
                    knowledgeBase.addDefinitionError(fmtMethod(method) +
                            ": interceptor may not declare producer methods");
                    valid = false;
                } else {
                    valid &= validateProducerMethod(method);

                    // Ambiguity check for overloaded producers
                    String signatureKey = producerSignatureKey(method.getGenericReturnType(), extractQualifiers(method));
                    if (producerSignatureOwners.containsKey(signatureKey)) {
                        knowledgeBase.addDefinitionError(fmtMethod(method) +
                            ": producer conflicts with " + producerSignatureOwners.get(signatureKey) +
                            " (same bean type/qualifiers). Overloaded producers for the same bean are not allowed.");
                        valid = false;
                    } else {
                        producerSignatureOwners.put(signatureKey, fmtMethod(method));
                    }

                    // Create and register ProducerBean for this producer method
                    createAndRegisterProducerBean(clazz, method, null);
                }
            }

            if (disposes) {
                if (isInterceptor) {
                    knowledgeBase.addDefinitionError(fmtMethod(method) +
                            ": interceptor may not declare disposer methods");
                    valid = false;
                } else if (!produces) {
                    valid &= validateDisposerMethod(method);
                    valid &= validateDisposerMethodHasMatchingProducer(clazz, method);
                }
            }

            if (inject && produces) {
                knowledgeBase.addDefinitionError(fmtMethod(method) + ": may not declare both @Inject and @Produces");
                valid = false;
            }
        }

        // 5) Constructor rules (only if it is a bean OR it has injection points / producers)
        boolean hasInjectConstructor = Arrays.stream(clazz.getDeclaredConstructors())
                .anyMatch(AnnotationsEnum::hasInjectAnnotation);
        boolean relevantClass = hasInjectionPoints || hasAnyProducer(clazz) || hasInjectConstructor;
        if (relevantClass) {
            @SuppressWarnings("unchecked")
            Constructor<T> ctor = (Constructor<T>) findBeanConstructor(clazz);
            if (ctor == null) {
                valid = false;
            } else {
                knowledgeBase.addConstructor(clazz, ctor);
            }
        }

        // 6) Check for @Interceptor and @Decorator (not managed beans)
        if (isInterceptor) {
            if (isAlternativeDeclared(clazz)) {
                throw new NonPortableBehaviourException(clazz.getName() +
                        ": interceptor is declared as @Alternative. Alternative interceptors are non-portable.");
            }
            String interceptorName = extractBeanName(clazz);
            if (interceptorName != null && !interceptorName.isEmpty()) {
                throw new NonPortableBehaviourException(clazz.getName() +
                        ": interceptor declares bean name '" + interceptorName +
                        "'. Named interceptors are non-portable.");
            }
            if (beanScope != null && !Dependent.class.equals(beanScope)) {
                throw new NonPortableBehaviourException(clazz.getName() +
                        ": interceptor declares scope @" + beanScope.getSimpleName() +
                        ". Interceptors with any scope other than @Dependent are non-portable.");
            }
            knowledgeBase.addInterceptor(clazz);
            // Validate and register interceptor metadata
            validateAndRegisterInterceptor(clazz);
            // Interceptors are not managed beans - return null (no bean to register)
            return null;
        }

        if (isDecorator) {
            if (beanScope != null && !Dependent.class.equals(beanScope)) {
                throw new NonPortableBehaviourException(clazz.getName() +
                        ": decorator declares scope @" + beanScope.getSimpleName() +
                        ". Decorators with any scope other than @Dependent are non-portable.");
            }
            // Validate decorator-specific rules before registering
            // Per CDI spec: A decorator must have exactly one @Delegate injection point
            validateDecoratorDelegateInjectionPoints(clazz);

            knowledgeBase.addDecorator(clazz);
            // Validate and register decorator metadata
            validateAndRegisterDecorator(clazz);
            // Decorators are not managed beans - return null (no bean to register)
            return null;
        }

        // 7) Check if this is an alternative bean
        boolean alternative = isAlternativeDeclared(clazz);
        boolean alternativeEnabled = isAlternativeEnabled(clazz, clazz, alternative);

        // Build and register Bean (even if invalid, to track all beans)
        // Mark alternative based on annotation; enablement affects resolution elsewhere.
        BeanImpl<T> bean = new BeanImpl<>(clazz, alternative);
        bean.setAlternativeEnabled(alternativeEnabled);
        bean.setPriority(extractEffectivePriority(clazz));

        // Mark bean as having validation errors if validation failed
        if (!valid) {
            bean.setHasValidationErrors(true);
        }

        // Mark bean as vetoed if the type was vetoed by an extension
        if (knowledgeBase.isTypeVetoed(clazz)) {
            bean.setVetoed(true);
            System.out.println("[CDI41BeanValidator] Bean marked as vetoed: " + clazz.getName());
        }

        // Populate BeanAttributes (now that BeanImpl supports it)
        bean.setName(extractBeanName(clazz));
        bean.setQualifiers(extractBeanQualifiers(clazz));
        Class<? extends Annotation> effectiveBeanScope = extractBeanScope(clazz, beanScope);
        validateManagedBeanPublicFieldScopeConstraint(clazz, effectiveBeanScope);
        validateManagedBeanGenericTypeScopeConstraint(clazz, effectiveBeanScope);
        bean.setScope(effectiveBeanScope);

        BeanTypesExtractor.ExtractionResult managedBeanTypes = beanTypesExtractor.extractManagedBeanTypes(clazz);
        for (String error : managedBeanTypes.getDefinitionErrors()) {
            addValidationError(clazz, error);
            valid = false;
        }
        bean.setTypes(managedBeanTypes.getTypes());

        bean.setStereotypes(extractBeanStereotypes(clazz));

        // Populate injection metadata for bean creation
        populateInjectionMetadata(bean, clazz);

        // Collect injection points with real metadata so CDI41InjectionValidator can resolve dependencies
        for (Field field : clazz.getDeclaredFields()) {
            if (hasInjectAnnotation(field)) {
                InjectionPoint ip = tryCreateInjectionPoint(field, bean);
                if (ip != null) {
                    bean.addInjectionPoint(ip);
                }
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (hasInjectAnnotation(method)) {
                for (Parameter p : method.getParameters()) {
                    InjectionPoint ip = tryCreateInjectionPoint(p, bean);
                    if (ip != null) {
                        bean.addInjectionPoint(ip);
                    }
                }
            }
        }
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (hasInjectAnnotation(c)) {
                for (Parameter p : c.getParameters()) {
                    InjectionPoint ip = tryCreateInjectionPoint(p, bean);
                    if (ip != null) {
                        bean.addInjectionPoint(ip);
                    }
                }
            }
        }

        // Always register the bean, even if it has validation errors
        // This allows resolution to detect if an invalid bean is actually needed
        knowledgeBase.addBean(bean);

        // Return the bean (even if invalid) so it's tracked
        return bean;
        } finally {
            overrideAnnotations = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void validateNonPortableScopeTypes(Class<?> clazz) {
        // Validate scope type declaration itself.
        if (clazz.isAnnotation()) {
            Class<? extends Annotation> annotationType = (Class<? extends Annotation>) clazz;
            if (isScopeAnnotationType(annotationType) && annotationType.getDeclaredMethods().length > 0) {
                throw new NonPortableBehaviourException(annotationType.getName() +
                        ": scope type declares attributes. Scope types with attributes are non-portable.");
            }
        }

        // Validate any scope used by this class.
        for (Annotation annotation : annotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (isScopeAnnotationType(annotationType) && annotationType.getDeclaredMethods().length > 0) {
                throw new NonPortableBehaviourException(clazz.getName() +
                        ": declares non-portable scope type @" + annotationType.getSimpleName() +
                        " which has attributes.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateNonPortableQualifierMembers(Class<?> clazz) {
        if (!clazz.isAnnotation()) {
            return;
        }

        Class<? extends Annotation> annotationType = (Class<? extends Annotation>) clazz;
        if (!hasQualifierAnnotation(annotationType)) {
            return;
        }

        for (Method member : annotationType.getDeclaredMethods()) {
            if (hasNonbindingAnnotation(member)) {
                continue;
            }

            Class<?> returnType = member.getReturnType();
            if (returnType.isArray() || returnType.isAnnotation()) {
                throw new NonPortableBehaviourException(annotationType.getName() +
                        ": qualifier member '" + member.getName() + "' has non-portable type " +
                        returnType.getTypeName() + " and must be annotated @Nonbinding");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateStereotypeScopeDeclaration(Class<?> clazz) {
        if (!clazz.isAnnotation()) {
            return;
        }

        Class<? extends Annotation> annotationType = (Class<? extends Annotation>) clazz;
        if (!hasMetaAnnotation(annotationType, Stereotype.class)) {
            return;
        }

        Set<Class<? extends Annotation>> declaredScopes = new LinkedHashSet<>();
        collectStereotypeScopes(annotationType, declaredScopes, new HashSet<>());

        if (declaredScopes.size() > 1) {
            String scopeNames = declaredScopes.stream()
                    .map(scope -> "@" + scope.getSimpleName())
                    .collect(Collectors.joining(", "));
            throw new DefinitionException(annotationType.getName() +
                    ": stereotype declares multiple scopes: " + scopeNames);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateStereotypeNamedDeclaration(Class<?> clazz) {
        if (!clazz.isAnnotation()) {
            return;
        }

        Class<? extends Annotation> annotationType = (Class<? extends Annotation>) clazz;
        if (!hasMetaAnnotation(annotationType, Stereotype.class)) {
            return;
        }

        validateStereotypeNamedDeclaration(annotationType, new HashSet<>());
    }

    private void validateNonPortableInterceptionForms(Class<?> clazz) {
        if (hasLegacyInterceptorDeclaration(annotationsOf(clazz))) {
            throw new NonPortableBehaviourException(clazz.getName() +
                    ": uses @Interceptors/@ExcludeClassInterceptors/@ExcludeDefaultInterceptors. " +
                    "These interception forms are non-portable in CDI Lite.");
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (hasLegacyInterceptorDeclaration(method.getAnnotations())) {
                throw new NonPortableBehaviourException(fmtMethod(method) +
                        ": uses @Interceptors/@ExcludeClassInterceptors/@ExcludeDefaultInterceptors. " +
                        "These interception forms are non-portable in CDI Lite.");
            }
        }
    }

    private boolean hasLegacyInterceptorDeclaration(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            String annotationName = annotation.annotationType().getName();
            if ("jakarta.interceptor.Interceptors".equals(annotationName) ||
                "javax.interceptor.Interceptors".equals(annotationName) ||
                "jakarta.interceptor.ExcludeClassInterceptors".equals(annotationName) ||
                "javax.interceptor.ExcludeClassInterceptors".equals(annotationName) ||
                "jakarta.interceptor.ExcludeDefaultInterceptors".equals(annotationName) ||
                "javax.interceptor.ExcludeDefaultInterceptors".equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private void validateStereotypeNamedDeclaration(Class<? extends Annotation> stereotypeType,
                                                    Set<Class<? extends Annotation>> visited) {
        if (!visited.add(stereotypeType)) {
            return;
        }

        Named named = stereotypeType.getAnnotation(Named.class);
        if (named != null) {
            String value = named.value();
            if (value != null && !value.trim().isEmpty()) {
                throw new DefinitionException(stereotypeType.getName() +
                        ": stereotype declares non-empty @Named(\"" + value + "\")");
            }
        }

        for (Annotation meta : stereotypeType.getAnnotations()) {
            Class<? extends Annotation> metaType = meta.annotationType();
            if (hasMetaAnnotation(metaType, Stereotype.class)) {
                validateStereotypeNamedDeclaration(metaType, visited);
            }
        }
    }

    /**
     * CDI 4.1 §2.8.1.5:
     * If a bean has multiple stereotypes (directly/indirectly/transitively) that declare
     * different priority values, the bean must explicitly declare @Priority.
     */
    private void validateStereotypePriorityDeclaration(Class<?> clazz) {
        if (extractDeclaredPriorityFromClass(clazz) != null) {
            // Explicit bean @Priority always wins over stereotype priorities.
            return;
        }

        Set<Integer> stereotypePriorities = collectStereotypePriorityValues(clazz);
        if (stereotypePriorities.size() > 1) {
            String priorityValues = stereotypePriorities.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new DefinitionException(clazz.getName() +
                    ": stereotypes declare different @Priority values (" +
                    priorityValues +
                    "). Bean must explicitly declare @Priority.");
        }
    }

    /**
     * CDI 4.1 §2.8.1.6:
     * Stereotypes declared @Target(TYPE) may not be applied to stereotypes that can target
     * METHOD and/or FIELD.
     */
    @SuppressWarnings("unchecked")
    private void validateStereotypeTargetCompatibility(Class<?> clazz) {
        if (!clazz.isAnnotation()) {
            return;
        }

        Class<? extends Annotation> annotationType = (Class<? extends Annotation>) clazz;
        if (!hasMetaAnnotation(annotationType, Stereotype.class)) {
            return;
        }

        Set<ElementType> declaredTargets = declaredTargetElements(annotationType);
        boolean canTargetMethodOrField = declaredTargets.contains(ElementType.METHOD) ||
                declaredTargets.contains(ElementType.FIELD);
        if (!canTargetMethodOrField) {
            return;
        }

        Set<String> invalidStereotypes = new LinkedHashSet<>();
        collectTypeOnlyStereotypes(annotationType, invalidStereotypes, new HashSet<>());
        if (!invalidStereotypes.isEmpty()) {
            throw new DefinitionException(annotationType.getName() +
                    ": declares stereotype(s) " + String.join(", ", invalidStereotypes) +
                    " with @Target(TYPE), which is not allowed for stereotypes targeting METHOD/FIELD.");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateStereotypeNonPortableDeclarations(Class<?> clazz) {
        if (!clazz.isAnnotation()) {
            return;
        }

        Class<? extends Annotation> annotationType = (Class<? extends Annotation>) clazz;
        if (!hasMetaAnnotation(annotationType, Stereotype.class)) {
            return;
        }

        if (hasTypedAnnotation(annotationType)) {
            throw new NonPortableBehaviourException(annotationType.getName() +
                    ": stereotype is annotated with @Typed");
        }

        List<String> illegalQualifiers = new ArrayList<>();
        for (Annotation meta : annotationType.getAnnotations()) {
            Class<? extends Annotation> metaType = meta.annotationType();
            if (hasQualifierAnnotation(metaType) && !isNamedQualifierType(metaType)) {
                illegalQualifiers.add("@" + metaType.getSimpleName());
            }
        }

        if (!illegalQualifiers.isEmpty()) {
            throw new NonPortableBehaviourException(annotationType.getName() +
                    ": stereotype declares qualifier(s) other than @Named: " +
                    String.join(", ", illegalQualifiers));
        }
    }

    private boolean isNamedQualifierType(Class<? extends Annotation> annotationType) {
        return Named.class.equals(annotationType) ||
                "javax.inject.Named".equals(annotationType.getName());
    }

    private void collectStereotypeScopes(Class<? extends Annotation> stereotypeType,
                                         Set<Class<? extends Annotation>> scopes,
                                         Set<Class<? extends Annotation>> visited) {
        if (!visited.add(stereotypeType)) {
            return;
        }

        for (Annotation meta : stereotypeType.getAnnotations()) {
            Class<? extends Annotation> metaType = meta.annotationType();
            if (isScopeAnnotationType(metaType)) {
                scopes.add(metaType);
            } else if (hasMetaAnnotation(metaType, Stereotype.class)) {
                collectStereotypeScopes(metaType, scopes, visited);
            }
        }
    }

    private void collectTypeOnlyStereotypes(Class<? extends Annotation> stereotypeType,
                                            Set<String> invalidStereotypes,
                                            Set<Class<? extends Annotation>> visited) {
        if (!visited.add(stereotypeType)) {
            return;
        }

        for (Annotation meta : stereotypeType.getAnnotations()) {
            Class<? extends Annotation> metaType = meta.annotationType();
            if (!hasMetaAnnotation(metaType, Stereotype.class)) {
                continue;
            }

            if (isTypeOnlyTarget(metaType)) {
                invalidStereotypes.add("@" + metaType.getSimpleName());
            }

            collectTypeOnlyStereotypes(metaType, invalidStereotypes, visited);
        }
    }

    private Set<ElementType> declaredTargetElements(Class<? extends Annotation> annotationType) {
        Target target = annotationType.getAnnotation(Target.class);
        if (target == null || target.value() == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(Arrays.asList(target.value()));
    }

    private boolean isTypeOnlyTarget(Class<? extends Annotation> annotationType) {
        Set<ElementType> targetElements = declaredTargetElements(annotationType);
        return targetElements.size() == 1 && targetElements.contains(ElementType.TYPE);
    }

    private Set<Integer> collectStereotypePriorityValues(Class<?> clazz) {
        Set<Integer> priorities = new LinkedHashSet<>();
        Set<Class<? extends Annotation>> visited = new HashSet<>();

        for (Annotation annotation : annotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (hasMetaAnnotation(annotationType, Stereotype.class)) {
                collectStereotypePriorityValues(annotationType, priorities, visited);
            }
        }

        return priorities;
    }

    private void collectStereotypePriorityValues(Class<? extends Annotation> stereotypeType,
                                                 Set<Integer> priorities,
                                                 Set<Class<? extends Annotation>> visited) {
        if (!visited.add(stereotypeType)) {
            return;
        }

        Integer declaredPriority = extractDeclaredPriority(stereotypeType.getAnnotations());
        if (declaredPriority != null) {
            priorities.add(declaredPriority);
        }

        for (Annotation meta : stereotypeType.getAnnotations()) {
            Class<? extends Annotation> metaType = meta.annotationType();
            if (hasMetaAnnotation(metaType, Stereotype.class)) {
                collectStereotypePriorityValues(metaType, priorities, visited);
            }
        }
    }

    private Integer extractEffectivePriority(Class<?> clazz) {
        Integer explicitPriority = extractDeclaredPriorityFromClass(clazz);
        if (explicitPriority != null) {
            return explicitPriority;
        }

        Set<Integer> stereotypePriorities = collectStereotypePriorityValues(clazz);
        if (stereotypePriorities.size() == 1) {
            return stereotypePriorities.iterator().next();
        }

        return null;
    }

    private Integer extractDeclaredPriorityFromClass(Class<?> clazz) {
        return extractDeclaredPriority(annotationsOf(clazz));
    }

    private Integer extractDeclaredPriority(Annotation[] annotations) {
        if (annotations == null) {
            return null;
        }

        for (Annotation annotation : annotations) {
            String annotationTypeName = annotation.annotationType().getName();
            if (Priority.class.getName().equals(annotationTypeName) ||
                    "javax.annotation.Priority".equals(annotationTypeName)) {
                return readPriorityValue(annotation);
            }
        }

        return null;
    }

    private Integer readPriorityValue(Annotation priorityAnnotation) {
        try {
            Method valueMethod = priorityAnnotation.annotationType().getMethod("value");
            Object value = valueMethod.invoke(priorityAnnotation);
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (ReflectiveOperationException ignored) {
            // Ignore malformed annotation implementations and treat as absent.
        }
        return null;
    }

    private Annotation[] annotationsOf(Class<?> clazz) {
        return overrideAnnotations != null ? overrideAnnotations : clazz.getAnnotations();
    }

    private Annotation[] declaredAnnotationsOf(Class<?> clazz) {
        Annotation[] declaredAnnotations = clazz.getDeclaredAnnotations();
        if (overrideAnnotations == null) {
            return declaredAnnotations;
        }

        // keep direct declarations and only add override annotations that are not inherited
        // from superclasses. This preserves extension-added annotations while avoiding
        // inherited annotations being treated as directly declared metadata.
        List<Annotation> merged = new ArrayList<>(Arrays.asList(declaredAnnotations));
        Set<Class<? extends Annotation>> seenTypes = merged.stream()
                .map(Annotation::annotationType)
                .collect(Collectors.toSet());

        for (Annotation annotation : overrideAnnotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (seenTypes.contains(annotationType)) {
                continue;
            }
            if (isInheritedFromSuperclass(clazz, annotationType)) {
                continue;
            }
            merged.add(annotation);
            seenTypes.add(annotationType);
        }

        return merged.toArray(new Annotation[0]);
    }

    private boolean isInheritedFromSuperclass(Class<?> clazz, Class<? extends Annotation> annotationType) {
        if (!annotationType.isAnnotationPresent(Inherited.class)) {
            return false;
        }

        Class<?> current = clazz.getSuperclass();
        while (current != null && current != Object.class) {
            if (current.isAnnotationPresent(annotationType)) {
                return true;
            }
            current = current.getSuperclass();
        }

        return false;
    }

    private String extractBeanName(Class<?> clazz) {
        // CDI: direct @Named without value defaults to simple name with first character lower-cased.
        for (Annotation annotation : annotationsOf(clazz)) {
            if (annotation.annotationType().equals(Named.class)) {
                return defaultedBeanName(readNamedValue(annotation), clazz);
            }
        }

        // CDI 4.1 §2.6.2: empty @Named declared by a stereotype also assigns a default bean name.
        Set<Class<? extends Annotation>> visited = new HashSet<>();
        for (Annotation annotation : annotationsOf(clazz)) {
            Class<? extends Annotation> at = annotation.annotationType();
            if (hasMetaAnnotation(at, Stereotype.class)) {
                String stereotypeName = extractNameFromStereotype(at, clazz, visited);
                if (stereotypeName != null) {
                    return stereotypeName;
                }
            }
        }

        return "";
    }

    private String defaultedBeanName(String rawName, Class<?> beanClass) {
        if (rawName != null && !rawName.trim().isEmpty()) {
            return rawName.trim();
        }
        return decapitalize(beanClass.getSimpleName());
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

    private String extractNameFromStereotype(Class<? extends Annotation> stereotypeAnnotation,
                                             Class<?> beanClass,
                                             Set<Class<? extends Annotation>> visited) {
        if (!visited.add(stereotypeAnnotation)) {
            return null;
        }

        Named named = stereotypeAnnotation.getAnnotation(Named.class);
        if (named != null) {
            return defaultedBeanName(named.value(), beanClass);
        }

        for (Annotation meta : stereotypeAnnotation.getAnnotations()) {
            Class<? extends Annotation> metaType = meta.annotationType();
            if (hasMetaAnnotation(metaType, Stereotype.class)) {
                String nested = extractNameFromStereotype(metaType, beanClass, visited);
                if (nested != null) {
                    return nested;
                }
            }
        }

        return null;
    }

    private Set<Annotation> extractBeanQualifiers(Class<?> clazz) {
        Set<Annotation> result = QualifiersHelper.extractQualifierAnnotations(annotationsOf(clazz));

        // Then, inherit qualifiers from stereotypes
        for (Annotation a : annotationsOf(clazz)) {
            if (hasMetaAnnotation(a.annotationType(), Stereotype.class)) {
                result.addAll(extractQualifiersFromStereotype(a.annotationType()));
            }
        }

        return QualifiersHelper.normalizeBeanQualifiers(result);
    }

    private Class<? extends Annotation> extractBeanScope(Class<?> clazz) {
        // 1) Direct scope declared on the bean class.
        for (Annotation annotation : declaredAnnotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (isScopeAnnotationType(annotationType)) {
                return annotationType;
            }
        }

        // 2) CDI scope inheritance special rule:
        // nearest superclass declaring ANY scope blocks farther ancestors.
        Class<? extends Annotation> inheritedClassScope = resolveInheritedScopeByCdiRules(clazz);
        if (inheritedClassScope != null) {
            return inheritedClassScope;
        }

        // 3) No class scope -> resolve from stereotypes (must be consistent).
        Class<? extends Annotation> inheritedScope = null;
        for (Annotation a : annotationsOf(clazz)) {
            if (hasMetaAnnotation(a.annotationType(), Stereotype.class)) {
                Class<? extends Annotation> stereotypeScope = extractScopeFromStereotype(a.annotationType());
                if (stereotypeScope == null) {
                    continue;
                }

                if (inheritedScope == null) {
                    inheritedScope = stereotypeScope;
                } else if (!inheritedScope.equals(stereotypeScope)) {
                    knowledgeBase.addDefinitionError(clazz.getName() +
                        ": conflicting scopes inherited from stereotypes (" +
                        inheritedScope.getName() + " vs " + stereotypeScope.getName() +
                        "). Declare an explicit scope on the bean to resolve.");
                    // Keep the first to avoid NPEs in downstream processing
                }
            }
        }

        if (inheritedScope != null) {
            return inheritedScope;
        }

        // Default scope for managed beans is @Dependent.
        return Dependent.class;
    }

    private Class<? extends Annotation> resolveInheritedScopeByCdiRules(Class<?> clazz) {
        Class<?> current = clazz.getSuperclass();

        while (current != null && current != Object.class) {
            Class<? extends Annotation> declaredScope = firstDeclaredScope(current);
            if (declaredScope != null) {
                // A scope declaration on an intermediate class blocks farther ancestors.
                return declaredScope.isAnnotationPresent(Inherited.class) ? declaredScope : null;
            }
            current = current.getSuperclass();
        }

        return null;
    }

    private Class<? extends Annotation> firstDeclaredScope(Class<?> clazz) {
        for (Annotation annotation : clazz.getDeclaredAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (isScopeAnnotationType(annotationType)) {
                return annotationType;
            }
        }
        return null;
    }

    private Set<Class<? extends Annotation>> extractBeanStereotypes(Class<?> clazz) {
        Set<Class<? extends Annotation>> stereotypes = new HashSet<>();
        for (Annotation a : annotationsOf(clazz)) {
            Class<? extends Annotation> at = a.annotationType();
            if (hasMetaAnnotation(at, Stereotype.class)) {
                stereotypes.add(at);
            }
        }
        return stereotypes;
    }

    private String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Extracts scope from a stereotype annotation.
     * Stereotypes can define a default scope. This method recursively checks for scope annotations
     * on the stereotype itself, supporting nested stereotypes.
     *
     * @param stereotypeClass the stereotype annotation class
     * @return the scope annotation class, or null if no scope is defined
     */
    private Class<? extends Annotation> extractScopeFromStereotype(Class<? extends Annotation> stereotypeClass) {
        // First check if stereotype directly has a scope annotation
        for (Annotation a : stereotypeClass.getAnnotations()) {
            Class<? extends Annotation> at = a.annotationType();
            if (isScopeAnnotationType(at)) {
                return at;
            }
        }

        // Then check nested stereotypes (stereotype can be annotated with another stereotype)
        for (Annotation a : stereotypeClass.getAnnotations()) {
            if (hasMetaAnnotation(a.annotationType(), Stereotype.class)) {
                Class<? extends Annotation> nestedScope = extractScopeFromStereotype(a.annotationType());
                if (nestedScope != null) {
                    return nestedScope;
                }
            }
        }

        return null;
    }

    /**
     * Extracts qualifiers from a stereotype annotation.
     * Stereotypes can define default qualifiers. This method recursively collects qualifiers
     * from the stereotype and any nested stereotypes.
     *
     * @param stereotypeClass the stereotype annotation class
     * @return set of qualifier annotations
     */
    private Set<Annotation> extractQualifiersFromStereotype(Class<? extends Annotation> stereotypeClass) {
        Set<Annotation> qualifiers = QualifiersHelper.extractQualifierAnnotations(stereotypeClass.getAnnotations())
                .stream()
                // CDI 4.1 §2.8.1.3: @Named declared by stereotype does not become a bean qualifier.
                .filter(annotation -> !Named.class.equals(annotation.annotationType()))
                .collect(Collectors.toSet());

        // Recursively collect from nested stereotypes
        for (Annotation a : stereotypeClass.getAnnotations()) {
            if (hasMetaAnnotation(a.annotationType(), Stereotype.class)) {
                qualifiers.addAll(extractQualifiersFromStereotype(a.annotationType()));
            }
        }

        return qualifiers;
    }


    // -----------------------
    // Validation: constructors
    // -----------------------

    private Constructor<?> findBeanConstructor(Class<?> clazz) {
        List<Constructor<?>> injectCtors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(AnnotationsEnum::hasInjectAnnotation)
                .collect(Collectors.toList());

        if (injectCtors.size() > 1) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": declares more than one constructor annotated with @Inject");
            return null;
        }

        if (injectCtors.size() == 1) {
            Constructor<?> c = injectCtors.get(0);

            // Conservative CDI rule: bean constructor must not be private.
            if (Modifier.isPrivate(c.getModifiers())) {
                knowledgeBase.addDefinitionError(clazz.getName() + ": @Inject constructor must not be private");
                return null;
            }

            if (!validateBeanConstructorParameters(c)) {
                return null;
            }
            return c;
        }

        // No @Inject constructor: must have a non-private no-arg constructor
        Optional<Constructor<?>> noArg = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == 0)
                .findFirst();

        if (!noArg.isPresent()) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": has no @Inject constructor and no no-arg constructor");
            return null;
        }

        Constructor<?> c = noArg.get();
        if (Modifier.isPrivate(c.getModifiers())) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": no-arg constructor must not be private");
            return null;
        }

        return c;
    }

    private boolean validateBeanConstructorParameters(Constructor<?> constructor) {
        boolean valid = true;
        for (Parameter p : constructor.getParameters()) {
            if (hasDisposesAnnotation(p)) {
                knowledgeBase.addDefinitionError(fmtParameter(p) +
                        ": bean constructor parameter may not be annotated @Disposes");
                valid = false;
                continue;
            }
            if (hasObservesAnnotation(p)) {
                knowledgeBase.addDefinitionError(fmtParameter(p) +
                        ": bean constructor parameter may not be annotated @Observes");
                valid = false;
                continue;
            }
            if (hasObservesAsyncAnnotation(p)) {
                knowledgeBase.addDefinitionError(fmtParameter(p) +
                        ": bean constructor parameter may not be annotated @ObservesAsync");
                valid = false;
                continue;
            }
            if (!validateInjectionPointMetadataUsage(p, false)) {
                valid = false;
            }
        }

        if (!hasValidInjectionParameters(constructor.getParameters(), fmtConstructor(constructor))) {
            valid = false;
        }

        return valid;
    }

    // -----------------------
    // Validation: @Inject
    // -----------------------

    private boolean validateInjectField(Field field) {
        boolean valid = true;

        if (Modifier.isFinal(field.getModifiers())) {
            knowledgeBase.addDefinitionError(fmtField(field) + ": final fields are not valid injection points");
            valid = false;
        }

        // CDI is stricter than JSR-330 about certain injection points; keep conservative:
        if (Modifier.isStatic(field.getModifiers())) {
            knowledgeBase.addDefinitionError(fmtField(field) + ": static field injection is not a valid CDI injection point");
            valid = false;
        }

        try {
            checkInjectionTypeValidity(field.getGenericType());
        } catch (IllegalArgumentException e) {
            knowledgeBase.addInjectionError(fmtField(field) + ": " + e.getMessage());
            valid = false;
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(fmtField(field) + ": " + e.getMessage());
            valid = false;
        }

        try {
            validateQualifiers(field.getAnnotations(), fmtField(field));
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(fmtField(field) + ": " + e.getMessage());
            valid = false;
        }

        if (!validateNamedInjectionPointUsage(field)) {
            valid = false;
        }

        if (!validateInjectionPointMetadataUsage(field, false)) {
            valid = false;
        }

        return valid;
    }

    private boolean validateInitializerMethod(Method method) {
        boolean valid = true;

        // Initializer method constraints (conservative CDI 4.1)
        if (Modifier.isAbstract(method.getModifiers())) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": cannot inject into an abstract initializer method");
            valid = false;
        }

        if (Modifier.isStatic(method.getModifiers())) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": static initializer methods are not valid CDI injection points");
            valid = false;
        }

        if (method.getTypeParameters().length > 0) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": generic methods are not valid CDI initializer methods");
            valid = false;
        }

        // CDI forbids combining initializer methods with certain roles
        if (hasProducesAnnotation(method)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": initializer method may not be annotated @Produces");
            valid = false;
        }
        if (hasAnyParameterWithDisposesAnnotation(method)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": initializer method may not declare a @Disposes parameter");
            valid = false;
        }
        for (Parameter p : method.getParameters()) {
            if (hasObservesAnnotation(p)) {
                knowledgeBase.addDefinitionError(fmtParameter(p) +
                        ": initializer method parameter may not be annotated @Observes");
                valid = false;
                continue;
            }
            if (hasObservesAsyncAnnotation(p)) {
                knowledgeBase.addDefinitionError(fmtParameter(p) +
                        ": initializer method parameter may not be annotated @ObservesAsync");
                valid = false;
            }
        }

        if (!hasValidInjectionParameters(method.getParameters(), fmtMethod(method))) {
            valid = false;
        }

        return valid;
    }

    private boolean hasValidInjectionParameters(Parameter[] parameters, String owner) {
        boolean valid = true;
        for (Parameter p : parameters) {
            // @Disposes/@Observes etc. are not "injection" parameters; if present in wrong place, handled elsewhere.
            if (hasDisposesAnnotation(p)) {
                // handled at producer validation
                continue;
            }

            try {
                checkInjectionTypeValidity(p.getParameterizedType());
            } catch (IllegalArgumentException e) {
                knowledgeBase.addInjectionError(fmtParameter(p) + ": " + e.getMessage());
                valid = false;
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(fmtParameter(p) + ": " + e.getMessage());
                valid = false;
            }

            try {
                validateQualifiers(p.getAnnotations(), owner + " parameter " + safeParamName(p));
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(fmtParameter(p) + ": " + e.getMessage());
                valid = false;
            }

            if (!validateNamedInjectionPointUsage(p)) {
                valid = false;
            }

            if (!validateInjectionPointMetadataUsage(p, false)) {
                valid = false;
            }
        }
        return valid;
    }

    // -----------------------
    // Validation: producers
    // -----------------------

    private boolean validateProducerField(Field field) {
        boolean valid = true;

        // Disallow @Inject on producer fields (handled also by combo check)
        if (hasInjectAnnotation(field)) {
            knowledgeBase.addDefinitionError(fmtField(field) + ": producer field may not be annotated @Inject");
            valid = false;
        }

        // Producer fields should not declare type variables / wildcards
        try {
            checkProducerTypeValidity(field.getGenericType());
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(fmtField(field) + ": " + e.getMessage());
            valid = false;
        }

        try {
            validateProducerFieldTypeVariableScopeConstraint(field);
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(fmtField(field) + ": " + e.getMessage());
            valid = false;
        }

        return valid;
    }

    private boolean validateProducerMethod(Method method) {
        boolean valid = true;

        if (Modifier.isAbstract(method.getModifiers())) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": producer method must not be abstract");
            valid = false;
        }

        if (method.getTypeParameters().length > 0) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": producer method must not be generic");
            valid = false;
        }

        if (method.isVarArgs()) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": producer method must not be varargs");
            valid = false;
        }

        // Important rule you explicitly asked for:
        if (hasInjectAnnotation(method)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": producer method must not be annotated @Inject");
            valid = false;
        }

        if (!validateSpecializingProducerMethodConstraint(method)) {
            valid = false;
        }

        // Validate return type (no type variables/wildcards)
        try {
            checkProducerTypeValidity(method.getGenericReturnType());
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": " + e.getMessage());
            valid = false;
        }

        try {
            validateProducerMethodTypeVariableScopeConstraint(method);
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": " + e.getMessage());
            valid = false;
        }

        // Producer method parameters must not be annotated with @Disposes, @Observes, or @ObservesAsync.
        for (Parameter p : method.getParameters()) {
            if (hasDisposesAnnotation(p)) {
                knowledgeBase.addDefinitionError(fmtParameter(p) +
                        ": producer method parameter may not be annotated @Disposes");
                valid = false;
                continue;
            }
            if (hasObservesAnnotation(p)) {
                knowledgeBase.addDefinitionError(fmtParameter(p) +
                        ": producer method parameter may not be annotated @Observes");
                valid = false;
                continue;
            }
            if (hasObservesAsyncAnnotation(p)) {
                knowledgeBase.addDefinitionError(fmtParameter(p) +
                        ": producer method parameter may not be annotated @ObservesAsync");
                valid = false;
                continue;
            }

            // normal injection parameter of a producer method
            try {
                checkInjectionTypeValidity(p.getParameterizedType());
            } catch (IllegalArgumentException e) {
                knowledgeBase.addInjectionError(fmtParameter(p) + ": " + e.getMessage());
                valid = false;
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(fmtParameter(p) + ": " + e.getMessage());
                valid = false;
            }
            try {
                validateQualifiers(p.getAnnotations(), fmtMethod(method));
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(fmtParameter(p) + ": " + e.getMessage());
                valid = false;
            }

            if (!validateNamedInjectionPointUsage(p)) {
                valid = false;
            }

            if (!validateInjectionPointMetadataUsage(p, false)) {
                valid = false;
            }
        }

        return valid;
    }

    /**
     * CDI 4.1 §15.2: a producer method annotated @Specializes must be non-static and directly override
     * another producer method.
     */
    private boolean validateSpecializingProducerMethodConstraint(Method method) {
        if (!hasSpecializesAnnotation(method)) {
            return true;
        }

        if (Modifier.isStatic(method.getModifiers())) {
            knowledgeBase.addDefinitionError(fmtMethod(method) +
                    ": producer method annotated @Specializes must be non-static");
            return false;
        }

        Class<?> declaringClass = method.getDeclaringClass();
        Class<?> directSuperclass = declaringClass.getSuperclass();
        if (directSuperclass == null || Object.class.equals(directSuperclass)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) +
                    ": producer method annotated @Specializes must directly override another producer method");
            return false;
        }

        Method overridden;
        try {
            overridden = directSuperclass.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            knowledgeBase.addDefinitionError(fmtMethod(method) +
                    ": producer method annotated @Specializes must directly override another producer method");
            return false;
        }

        if (!hasProducesAnnotation(overridden)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) +
                    ": producer method annotated @Specializes must directly override another producer method");
            return false;
        }

        return true;
    }

    /**
     * Validates a disposer method according to CDI 4.1 Section 3.4.
     * <p>
     * CDI 4.1 Disposer Method Rules:
     * <ul>
     *   <li>Must have exactly one parameter annotated with @Disposes</li>
     *   <li>Must not be annotated with @Produces or @Inject</li>
     *   <li>Must not be abstract</li>
     *   <li>Must not declare type parameters (not a generic method)</li>
     *   <li>The @Disposes parameter type must match a producer method's return type</li>
     *   <li>Other parameters are treated as injection points</li>
     *   <li>@Disposes parameter qualifiers must match corresponding producer qualifiers</li>
     * </ul>
     *
     * @param method the method to validate
     * @return true if valid, false otherwise
     */
    private boolean validateDisposerMethod(Method method) {
        boolean valid = true;

        // Rule 1: Must not be abstract
        if (Modifier.isAbstract(method.getModifiers())) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method must not be abstract");
            valid = false;
        }

        // Rule 2: Must not be generic
        if (method.getTypeParameters().length > 0) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method must not be generic");
            valid = false;
        }

        // Rule 3: Must not be annotated with @Produces
        if (hasProducesAnnotation(method)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method may not be annotated @Produces");
            valid = false;
        }

        // Rule 4: Must not be annotated with @Inject
        if (hasInjectAnnotation(method)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method may not be annotated @Inject");
            valid = false;
        }

        // Rule 5: Must have exactly one @Disposes parameter
        int disposesCount = 0;
        Parameter disposesParam = null;
        for (Parameter p : method.getParameters()) {
            if (hasDisposesAnnotation(p)) {
                disposesCount++;
                disposesParam = p;
            }
        }

        if (disposesCount == 0) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method must have exactly one @Disposes parameter (found 0)");
            valid = false;
        } else if (disposesCount > 1) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method must have exactly one @Disposes parameter (found " + disposesCount + ")");
            valid = false;
        }

        // Rule 6: Validate @Disposes parameter type
        if (disposesParam != null) {
            try {
                checkInjectionTypeValidity(disposesParam.getParameterizedType());
            } catch (IllegalArgumentException e) {
                knowledgeBase.addInjectionError(fmtParameter(disposesParam) + ": " + e.getMessage());
                valid = false;
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(fmtParameter(disposesParam) + ": " + e.getMessage());
                valid = false;
            }
        }

        // Rule 7: Disposer method parameters must not be annotated with @Observes or @ObservesAsync.
        for (Parameter p : method.getParameters()) {
            if (hasObservesAnnotation(p)) {
                knowledgeBase.addDefinitionError(fmtParameter(p) +
                        ": disposer method parameter may not be annotated @Observes");
                valid = false;
            }
            if (hasObservesAsyncAnnotation(p)) {
                knowledgeBase.addDefinitionError(fmtParameter(p) +
                        ": disposer method parameter may not be annotated @ObservesAsync");
                valid = false;
            }
        }

        // Rule 8: Validate other parameters as injection points
        for (Parameter p : method.getParameters()) {
            if (!hasDisposesAnnotation(p)) {
                // Regular injection parameter
                try {
                    checkInjectionTypeValidity(p.getParameterizedType());
                } catch (IllegalArgumentException e) {
                    knowledgeBase.addInjectionError(fmtParameter(p) + ": " + e.getMessage());
                    valid = false;
                } catch (DefinitionException e) {
                    knowledgeBase.addDefinitionError(fmtParameter(p) + ": " + e.getMessage());
                    valid = false;
                }

                try {
                    validateQualifiers(p.getAnnotations(), fmtMethod(method));
                } catch (DefinitionException e) {
                    knowledgeBase.addDefinitionError(fmtParameter(p) + ": " + e.getMessage());
                    valid = false;
                }

                if (!validateNamedInjectionPointUsage(p)) {
                    valid = false;
                }

                if (!validateInjectionPointMetadataUsage(p, true)) {
                    valid = false;
                }
            }
        }

        // Rule 9: Validate that a matching producer exists
        // Note: This is checked during producer-disposer linking phase, not here

        return valid;
    }

    private boolean validateInjectionPointMetadataUsage(AnnotatedElement injectionPoint, boolean disposerParameter) {
        boolean valid = true;

        if (isInjectionPointMetadataType(injectionPoint) && hasDefaultQualifier(injectionPoint.getAnnotations())) {
            if (disposerParameter) {
                knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                        ": disposer method injection point of type InjectionPoint with qualifier @Default is not allowed");
                valid = false;
            } else {
                Class<?> declaringClass = declaringClassOf(injectionPoint);
                if (declaringClass != null && declaresNonDependentScope(declaringClass)) {
                    knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                            ": bean declares scope other than @Dependent and may not inject InjectionPoint with qualifier @Default");
                    valid = false;
                }
            }
        }

        if (isBeanMetadataType(injectionPoint) || isInterceptorMetadataType(injectionPoint)) {
            valid = validateBeanAndInterceptorMetadataUsage(injectionPoint, disposerParameter) && valid;
        }

        return valid;
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
        boolean interceptorDeclaringClass = declaringClass != null && isInterceptorClass(declaringClass);
        boolean interceptedQualified = hasQualifier(injectionPoint.getAnnotations(), Intercepted.class.getName());
        boolean defaultQualified = hasDefaultQualifier(injectionPoint.getAnnotations());
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
                if (declaringClass == null || !isSameType(metadataArgument, declaringClass)) {
                    String expected = declaringClass == null ? "<unknown>" : declaringClass.getTypeName();
                    knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                            ": Bean/Interceptor metadata with qualifier @Default must declare type parameter " + expected);
                    return false;
                }
                return true;
            }

            if (isProducerMethodParameter(injectionPoint)) {
                Method producerMethod = (Method) ((Parameter) injectionPoint).getDeclaringExecutable();
                Type expectedType = producerMethod.getGenericReturnType();
                if (!isSameType(metadataArgument, expectedType)) {
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

    private Type metadataRawType(AnnotatedElement injectionPoint) {
        Type genericType = metadataGenericType(injectionPoint);
        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }
        return ((ParameterizedType) genericType).getRawType();
    }

    private Type metadataGenericType(AnnotatedElement injectionPoint) {
        if (injectionPoint instanceof Field) {
            return ((Field) injectionPoint).getGenericType();
        }
        if (injectionPoint instanceof Parameter) {
            return ((Parameter) injectionPoint).getParameterizedType();
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
        return hasInjectAnnotation(method) && !hasProducesAnnotation(method);
    }

    private boolean isProducerMethodParameter(AnnotatedElement injectionPoint) {
        if (!(injectionPoint instanceof Parameter)) {
            return false;
        }
        Executable executable = ((Parameter) injectionPoint).getDeclaringExecutable();
        return executable instanceof Method && hasProducesAnnotation((Method) executable);
    }

    private boolean isSameType(Type left, Type right) {
        if (left == null || right == null) {
            return false;
        }
        return left.getTypeName().equals(right.getTypeName());
    }

    private boolean isInterceptorClass(Class<?> clazz) {
        for (Annotation annotation : annotationsOf(clazz)) {
            String name = annotation.annotationType().getName();
            if ("jakarta.interceptor.Interceptor".equals(name) ||
                    "javax.interceptor.Interceptor".equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasQualifier(Annotation[] annotations, String qualifierTypeName) {
        Set<Annotation> qualifiers = QualifiersHelper.extractQualifierAnnotations(annotations);
        for (Annotation qualifier : qualifiers) {
            if (qualifier.annotationType().getName().equals(qualifierTypeName)) {
                return true;
            }
        }
        return false;
    }

    private boolean declaresNonDependentScope(Class<?> clazz) {
        for (Annotation annotation : annotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.equals(Dependent.class) ||
                    "javax.enterprise.context.Dependent".equals(annotationType.getName())) {
                continue;
            }
            if (hasMetaAnnotation(annotationType, Scope.class) ||
                    hasMetaAnnotation(annotationType, NormalScope.class)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDefaultQualifier(Annotation[] annotations) {
        Set<Annotation> qualifiers = QualifiersHelper.extractQualifierAnnotations(annotations);
        if (qualifiers.isEmpty()) {
            return true;
        }
        for (Annotation qualifier : qualifiers) {
            String typeName = qualifier.annotationType().getName();
            if ("jakarta.enterprise.inject.Default".equals(typeName) ||
                    "javax.enterprise.inject.Default".equals(typeName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyProducer(Class<?> clazz) {
        for (Field f : clazz.getDeclaredFields()) {
            if (hasProducesAnnotation(f)) return true;
        }
        for (Method m : clazz.getDeclaredMethods()) {
            if (hasProducesAnnotation(m)) return true;
        }
        return false;
    }

    /**
     * Returns true when a class is declared as an alternative directly or via stereotype.
     */
    private boolean isAlternativeDeclared(Class<?> clazz) {
        if (hasAlternativeAnnotation(clazz)) {
            return true;
        }

        Set<Class<? extends Annotation>> visited = new HashSet<>();
        for (Annotation annotation : annotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (hasMetaAnnotation(annotationType, Stereotype.class) &&
                stereotypeDeclaresAlternative(annotationType, visited)) {
                return true;
            }
        }

        return false;
    }

    private boolean stereotypeDeclaresAlternative(Class<? extends Annotation> stereotypeType,
                                                  Set<Class<? extends Annotation>> visited) {
        if (!visited.add(stereotypeType)) {
            return false;
        }

        if (hasMetaAnnotation(stereotypeType, Alternative.class)) {
            return true;
        }

        for (Annotation meta : stereotypeType.getAnnotations()) {
            Class<? extends Annotation> metaType = meta.annotationType();
            if (hasMetaAnnotation(metaType, Stereotype.class) &&
                stereotypeDeclaresAlternative(metaType, visited)) {
                return true;
            }
        }

        return false;
    }

    /**
     * CDI 4.1 alternative enabling helper usable for bean classes and producer members.
     *
     * <p>Alternatives can be enabled by:
     * <ul>
     *   <li>{@code @Priority}</li>
     *   <li>beans.xml {@code <alternatives>}</li>
     *   <li>programmatic enablement via {@link Syringe#enableAlternative(Class)}</li>
     * </ul>
     */
    private boolean isAlternativeEnabled(AnnotatedElement element,
                                         Class<?> declaringClass,
                                         boolean alternativeDeclared) {
        if (!alternativeDeclared) {
            return false;
        }

        // Producer member-level @Priority enables member alternatives.
        Priority priority = element.getAnnotation(Priority.class);
        if (priority != null) {
            return true;
        }

        // Class-level checks (bean class itself or producer declaring class).
        if (declaringClass != null && isAlternativeEnabledForClass(declaringClass)) {
            return true;
        }

        // Fallback for class elements when declaringClass is null.
        if (element instanceof Class) {
            return isAlternativeEnabledForClass((Class<?>) element);
        }

        return false;
    }

    private boolean isAlternativeEnabledForClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (extractEffectivePriority(clazz) != null) {
            return true;
        }

        if (knowledgeBase.isAlternativeEnabledProgrammatically(clazz.getName())) {
            return true;
        }

        if (knowledgeBase.isAlternativeEnabledInBeansXml(clazz.getName())) {
            return true;
        }

        for (Annotation annotation : annotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (hasMetaAnnotation(annotationType, Stereotype.class)) {
                if (knowledgeBase.isAlternativeEnabledProgrammatically(annotationType.getName()) ||
                    knowledgeBase.isAlternativeEnabledInBeansXml(annotationType.getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    // -----------------------
    // Type / annotation checks
    // -----------------------

    /**
     * Validates that the class declares at most one scope annotation and returns it.
     *
     * @return the discovered scope annotation type, or null if none is present.
     * @throws DefinitionException if more than one scope annotation is present.
     */
    private Class<? extends Annotation> validateScopeAnnotations(Class<?> clazz) {
        List<Class<? extends Annotation>> scopes = Arrays.stream(declaredAnnotationsOf(clazz))
                .map(Annotation::annotationType)
                .filter(this::isScopeAnnotationType)
                .collect(Collectors.toList());

        if (scopes.size() > 1) {
            String scopeNames = scopes.stream().map(s -> "@" + s.getSimpleName()).collect(Collectors.joining(", "));
            throw new DefinitionException("declares multiple scope annotations: " + scopeNames);
        }

        return scopes.isEmpty() ? null : scopes.get(0);
    }

    private Class<? extends Annotation> extractBeanScope(Class<?> clazz, Class<? extends Annotation> discoveredScope) {
        if (discoveredScope != null) {
            return discoveredScope;
        }

        // No explicit scope: resolve default scope from stereotypes (if any), otherwise @Dependent.
        return extractBeanScope(clazz);
    }

    /**
     * CDI 4.1 §3.1: A managed bean with a non-static public field must declare a pseudo-scope.
     * A normal scope on such a bean is a definition error.
     */
    private void validateManagedBeanPublicFieldScopeConstraint(Class<?> clazz,
                                                               Class<? extends Annotation> scopeAnnotation) {
        if (scopeAnnotation == null || !isNormalScope(scopeAnnotation)) {
            return;
        }

        List<String> invalidFields = Arrays.stream(clazz.getFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !field.isSynthetic())
                .map(field -> field.getDeclaringClass().getName() + "#" + field.getName())
                .collect(Collectors.toList());

        if (!invalidFields.isEmpty()) {
            throw new DefinitionException(clazz.getName() +
                    ": declares normal scope @" + scopeAnnotation.getSimpleName() +
                    " and non-static public field(s) " + String.join(", ", invalidFields) +
                    ". Such beans must declare a pseudo-scope (e.g. @Dependent or @Singleton).");
        }
    }

    /**
     * CDI 4.1 §3.1: a managed bean with a parameterized bean class must have @Dependent scope.
     */
    private void validateManagedBeanGenericTypeScopeConstraint(Class<?> clazz,
                                                               Class<? extends Annotation> scopeAnnotation) {
        if (clazz.getTypeParameters().length == 0) {
            return;
        }

        if (scopeAnnotation == null ||
                Dependent.class.equals(scopeAnnotation) ||
                "javax.enterprise.context.Dependent".equals(scopeAnnotation.getName())) {
            return;
        }

        throw new DefinitionException(clazz.getName() +
                ": managed bean class is generic and declares scope @" +
                scopeAnnotation.getSimpleName() +
                ". Generic managed beans must have @Dependent scope.");
    }

    private boolean isNormalScope(Class<? extends Annotation> scopeAnnotation) {
        if (scopeAnnotation == null) {
            return false;
        }

        String scopeName = scopeAnnotation.getName();
        if ("jakarta.enterprise.context.ApplicationScoped".equals(scopeName) ||
                "jakarta.enterprise.context.RequestScoped".equals(scopeName) ||
                "jakarta.enterprise.context.SessionScoped".equals(scopeName) ||
                "jakarta.enterprise.context.ConversationScoped".equals(scopeName) ||
                "javax.enterprise.context.ApplicationScoped".equals(scopeName) ||
                "javax.enterprise.context.RequestScoped".equals(scopeName) ||
                "javax.enterprise.context.SessionScoped".equals(scopeName) ||
                "javax.enterprise.context.ConversationScoped".equals(scopeName)) {
            return true;
        }

        return scopeAnnotation.isAnnotationPresent(jakarta.enterprise.context.NormalScope.class) ||
                scopeAnnotation.isAnnotationPresent(javax.enterprise.context.NormalScope.class);
    }

    /**
     * CDI 4.1 §15.1: a managed bean annotated @Specializes must directly extend another managed bean.
     * If this is not true, the container must treat it as a definition error.
     */
    private void validateManagedBeanSpecializationConstraint(Class<?> clazz, BeanArchiveMode beanArchiveMode) {
        if (!hasSpecializesAnnotation(clazz)) {
            return;
        }

        Class<?> directSuperclass = clazz.getSuperclass();
        if (directSuperclass == null || Object.class.equals(directSuperclass)) {
            knowledgeBase.addDefinitionError(clazz.getName() +
                    ": declares @Specializes but does not directly extend another managed bean");
            return;
        }

        BeanArchiveMode superMode = knowledgeBase.getBeanArchiveMode(directSuperclass);
        if (superMode == null) {
            superMode = beanArchiveMode;
        }

        boolean managedBeanSuperclass = isCandidateBeanClass(directSuperclass, superMode)
                && !Modifier.isAbstract(directSuperclass.getModifiers())
                && !hasInterceptorAnnotation(directSuperclass)
                && !hasDecoratorAnnotation(directSuperclass);

        if (!managedBeanSuperclass) {
            knowledgeBase.addDefinitionError(clazz.getName() +
                    ": declares @Specializes but direct superclass " + directSuperclass.getName() +
                    " is not a managed bean");
        }
    }

    private boolean isScopeAnnotationType(Class<? extends Annotation> at) {
        // CDI scopes are meta-annotated with @Scope or @NormalScope
        return hasMetaAnnotation(at, Scope.class)
                || hasMetaAnnotation(at, NormalScope.class)
                // plus common built-ins
                || at.equals(Dependent.class)
                || at.equals(ApplicationScoped.class)
                || at.equals(RequestScoped.class)
                || at.equals(SessionScoped.class)
                || at.equals(ConversationScoped.class);
    }

    private void validateQualifiers(Annotation[] annotations, String location) {
        // CDI allows multiple qualifiers, but they must actually be qualifiers (meta-annotated @Qualifier)
        // and you can't repeat the *same* qualifier type twice.
        List<Annotation> qualifiers = Arrays.stream(annotations)
                .filter(a -> isQualifierAnnotationType(a.annotationType()))
                .collect(Collectors.toList());

        Map<Class<? extends Annotation>, Long> counts = qualifiers.stream()
                .collect(Collectors.groupingBy(Annotation::annotationType, Collectors.counting()));

        List<String> duplicates = counts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> "@" + e.getKey().getSimpleName())
                .collect(Collectors.toList());

        if (!duplicates.isEmpty()) {
            throw new DefinitionException(location + ": duplicate qualifier annotations: " + String.join(", ", duplicates));
        }
    }

    /**
     * CDI 4.1 §3.9:
     * - If an injected field declares @Named with no value, the field name is assumed.
     * - Any other injection point declaring @Named with no value is a definition error.
     */
    private boolean validateNamedInjectionPointUsage(AnnotatedElement injectionPoint) {
        Annotation named = findNamedQualifier(injectionPoint.getAnnotations());
        if (named == null) {
            return true;
        }

        String namedValue = readNamedValue(named).trim();
        if (!namedValue.isEmpty()) {
            return true;
        }

        if (injectionPoint instanceof Field) {
            return true;
        }

        knowledgeBase.addDefinitionError(describeInjectionPoint(injectionPoint) +
                ": @Named injection point must declare a non-empty value on non-field injection points");
        return false;
    }

    private Annotation findNamedQualifier(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (isNamedQualifierType(annotation.annotationType())) {
                return annotation;
            }
        }
        return null;
    }

    private String describeInjectionPoint(AnnotatedElement injectionPoint) {
        if (injectionPoint instanceof Parameter) {
            return fmtParameter((Parameter) injectionPoint);
        }
        if (injectionPoint instanceof Field) {
            return fmtField((Field) injectionPoint);
        }
        if (injectionPoint instanceof Method) {
            return fmtMethod((Method) injectionPoint);
        }
        if (injectionPoint instanceof Constructor) {
            return fmtConstructor((Constructor<?>) injectionPoint);
        }
        return injectionPoint.toString();
    }

    private boolean isQualifierAnnotationType(Class<? extends Annotation> at) {
        return hasMetaAnnotation(at, Qualifier.class);
    }

    /**
     * Validates that a producer type is legal according to CDI 4.1 Section 3.3.2.
     * <p>
     * Rules:
     * <ul>
     *   <li>The type itself cannot be a wildcard or type variable</li>
     *   <li>Array producer types cannot have a type-variable component type</li>
     *   <li>Producer types cannot contain wildcard type parameters</li>
     * </ul>
     *
     * @param type the producer return/field type to validate
     * @throws DefinitionException if the type is invalid
     */
    private void checkProducerTypeValidity(Type type) {
        checkProducerTypeValidity(type, true);
    }

    private void checkProducerTypeValidity(Type type, boolean topLevel) {
        if (type instanceof WildcardType) {
            throw new DefinitionException("type may not contain a wildcard (" + type.getTypeName() + ")");
        }
        if (type instanceof TypeVariable) {
            if (topLevel) {
                throw new DefinitionException("type may not be a type variable (" + type.getTypeName() + ")");
            }
            return;
        }
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            if (componentType instanceof TypeVariable) {
                throw new DefinitionException("array component type may not be a type variable (" +
                        componentType.getTypeName() + ")");
            }
            checkProducerTypeValidity(componentType, false);
            return;
        }
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                checkProducerTypeValidity(clazz.getComponentType(), false);
            }
            return;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class) {
                Class<?> rawClass = (Class<?>) rawType;
                if (rawClass.getTypeParameters().length != parameterizedType.getActualTypeArguments().length) {
                    throw new DefinitionException("parameterized type must specify all type parameters (" +
                            type.getTypeName() + ")");
                }
            }

            // CDI 4.1: each type parameter of a parameterized producer return type must be specified
            // either as an actual type argument or as a type variable. Raw generic arguments are invalid.
            for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
                if (typeArgument instanceof Class &&
                        ((Class<?>) typeArgument).getTypeParameters().length > 0) {
                    throw new DefinitionException("parameterized producer type contains raw generic argument (" +
                            typeArgument.getTypeName() + ")");
                }

                // Nested parameterized arguments are validated recursively.
                checkProducerTypeValidity(typeArgument, false);
            }
        }
    }

    /**
     * CDI 4.1 §3.2: a producer method whose return type is a parameterized type that contains
     * a type variable must declare @Dependent scope.
     */
    private void validateProducerMethodTypeVariableScopeConstraint(Method method) {
        Type returnType = method.getGenericReturnType();
        if (!(returnType instanceof ParameterizedType)) {
            return;
        }

        if (!containsTypeVariable(((ParameterizedType) returnType).getActualTypeArguments())) {
            return;
        }

        Class<? extends Annotation> scope = extractScope(method, Dependent.class);
        if (scope == null ||
                Dependent.class.equals(scope) ||
                "javax.enterprise.context.Dependent".equals(scope.getName())) {
            return;
        }

        throw new DefinitionException("producer method with parameterized return type containing a type variable " +
                "must declare @Dependent scope, but declares @" + scope.getSimpleName());
    }

    /**
     * CDI 4.1 §3.3: a producer field whose type is a parameterized type that contains
     * a type variable must declare @Dependent scope.
     */
    private void validateProducerFieldTypeVariableScopeConstraint(Field field) {
        Type fieldType = field.getGenericType();
        if (!(fieldType instanceof ParameterizedType)) {
            return;
        }

        if (!containsTypeVariable(((ParameterizedType) fieldType).getActualTypeArguments())) {
            return;
        }

        Class<? extends Annotation> scope = extractScope(field, Dependent.class);
        if (scope == null ||
                Dependent.class.equals(scope) ||
                "javax.enterprise.context.Dependent".equals(scope.getName())) {
            return;
        }

        throw new DefinitionException("producer field with parameterized type containing a type variable " +
                "must declare @Dependent scope, but declares @" + scope.getSimpleName());
    }

    private boolean containsTypeVariable(Type[] types) {
        for (Type type : types) {
            if (containsTypeVariable(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTypeVariable(Type type) {
        if (type instanceof TypeVariable) {
            return true;
        }
        if (type instanceof ParameterizedType) {
            return containsTypeVariable(((ParameterizedType) type).getActualTypeArguments());
        }
        if (type instanceof GenericArrayType) {
            return containsTypeVariable(((GenericArrayType) type).getGenericComponentType());
        }
        if (type instanceof Class && ((Class<?>) type).isArray()) {
            return containsTypeVariable(((Class<?>) type).getComponentType());
        }
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            return containsTypeVariable(wildcardType.getLowerBounds()) ||
                    containsTypeVariable(wildcardType.getUpperBounds());
        }
        return false;
    }

    /**
     * Builds a signature key for a producer based on bean type and qualifiers.
     * Uses boxed type names to treat primitive/boxed as equivalent for ambiguity checks.
     */
    private String producerSignatureKey(Type type, Set<Annotation> qualifiers) {
        Class<?> raw = RawTypeExtractor.getRawType(type);
        if (raw.isPrimitive()) {
            raw = getBoxedType(raw);
        }
        String qualKey = qualifiers.stream()
                .filter(q -> hasMetaAnnotation(q.annotationType(), Qualifier.class))
                .map(q -> "@" + q.annotationType().getName())
                .sorted()
                .collect(Collectors.joining(","));
        return raw.getName() + "|" + qualKey;
    }

    private void checkInjectionTypeValidity(Type type) {
        if (type instanceof Class && jakarta.enterprise.inject.Instance.class.equals(type)) {
            throw new DefinitionException("injection point of raw type Instance is not allowed");
        }
        if (type instanceof Class && jakarta.enterprise.event.Event.class.equals(type)) {
            throw new DefinitionException("injection point of raw type Event is not allowed");
        }

        if (type instanceof TypeVariable) {
            throw new DefinitionException("injection point may not be a type variable (" + type.getTypeName() + ")");
        }

        Class<?> raw = RawTypeExtractor.getRawType(type);

        if (raw.isArray()) {
            throw new IllegalArgumentException("cannot inject arrays directly");
        }
        if (raw.isEnum()) {
            throw new IllegalArgumentException("cannot inject an enum");
        }
        if (raw.isSynthetic()) {
            throw new IllegalArgumentException("cannot inject a synthetic class");
        }
        if (raw.isLocalClass()) {
            throw new IllegalArgumentException("cannot inject a local class");
        }
        if (raw.isAnonymousClass()) {
            throw new IllegalArgumentException("cannot inject an anonymous class");
        }
        if (raw.isMemberClass() && !Modifier.isStatic(raw.getModifiers())) {
            throw new IllegalArgumentException("cannot inject a non-static inner class");
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            for (Type arg : pt.getActualTypeArguments()) {
                if (arg instanceof TypeVariable) {
                    throw new DefinitionException("injection point may not contain type variable arguments (" + arg.getTypeName() + ")");
                }
            }
        }
    }

    // -----------------------
    // "Is it a bean?" helpers
    // -----------------------

    private boolean isCandidateBeanClass(Class<?> clazz, BeanArchiveMode beanArchiveMode) {
        if (clazz == null || hasVetoedAnnotation(clazz) || beanArchiveMode == BeanArchiveMode.NONE) {
            return false;
        }

        // Types that can never be bean classes: annotations (stereotypes, scopes, qualifiers),
        // interfaces, enums, primitives, and arrays. Skip them early to avoid spurious definition errors.
        if (clazz.isAnnotation() || clazz.isInterface() || clazz.isEnum() || clazz.isPrimitive() || clazz.isArray()) {
            return false;
        }

        // Bean-defining annotations per CDI 4.1 §2.5.1:
        // - @ApplicationScoped and @RequestScoped
        // - all other normal scopes
        // - @Interceptor
        // - stereotypes
        // - @Dependent
        // Note: pseudo-scopes (except @Dependent) are NOT bean-defining annotations.
        if (hasBeanDefiningAnnotation(clazz)
                // Kept for backward compatibility with existing discovery behavior in this implementation.
                || hasDecoratorAnnotation(clazz)
                || hasAlternativeAnnotation(clazz)) {
            return true;
        }

        // CDI 4.1 Bean Archive Modes:
        // - IMPLICIT: Bean-defining annotation is REQUIRED (annotated discovery mode)
        // - EXPLICIT: ALL classes with suitable constructors are beans (all discovery mode)
        if (beanArchiveMode == BeanArchiveMode.EXPLICIT) {
            // In explicit bean archives, a class is a bean if it has a no-args constructor
            // (or an @Inject-annotated constructor, but that's checked elsewhere)
            return hasNoArgsConstructor(clazz);
        }

        // In implicit/trimmed mode, bean-defining annotation is required
        return false;
    }

    private boolean hasBeanDefiningAnnotation(Class<?> clazz) {
        for (Annotation annotation : annotationsOf(clazz)) {
            if (isBeanDefiningAnnotationType(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBeanDefiningAnnotationType(Class<? extends Annotation> annotationType) {
        // Explicitly listed built-ins
        if (annotationType.equals(Dependent.class)
                || annotationType.equals(ApplicationScoped.class)
                || annotationType.equals(RequestScoped.class)) {
            return true;
        }

        // "all other normal scope types"
        if (hasMetaAnnotation(annotationType, NormalScope.class)) {
            return true;
        }

        // @Interceptor
        if (INTERCEPTOR.matches(annotationType)) {
            return true;
        }

        // "all stereotype annotations"
        return hasMetaAnnotation(annotationType, Stereotype.class);
    }

    /**
     * Checks if the class has a no-args constructor (public, protected, package-private, or private).
     *
     * @param clazz the class to check
     * @return true if the class has a no-args constructor
     */
    private boolean hasNoArgsConstructor(Class<?> clazz) {
        try {
            // Try to get any no-args constructor (public, protected, package, or private)
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == 0) {
                    return true;
                }
            }
            // If no explicit constructors are defined, Java provides a default no-args constructor
            return constructors.length == 0;
        } catch (SecurityException e) {
            // If we can't access constructors due to security restrictions, assume no no-args constructor
            return false;
        }
    }

    // -----------------------
    // Formatting
    // -----------------------

    private String fmtField(Field field) {
        return "Field " + field.getName() + " of class " + field.getDeclaringClass().getName();
    }

    private String fmtMethod(Method method) {
        return "Method " + method.getName() + " of class " + method.getDeclaringClass().getName();
    }

    private String fmtConstructor(Constructor<?> c) {
        return "Constructor of class " + c.getDeclaringClass().getName();
    }

    private String fmtParameter(Parameter parameter) {
        Executable ex = parameter.getDeclaringExecutable();
        return "Parameter " + safeParamName(parameter) + " of " + ex.getName() + " of class " + ex.getDeclaringClass().getName();
    }

    private String safeParamName(Parameter p) {
        // Parameter names may be synthetic unless compiled with -parameters
        return (p.isNamePresent() ? p.getName() : "<param>");
    }

    /**
     * Adds a validation error for a class to the knowledge base.
     */
    private void addValidationError(Class<?> clazz, String message) {
        knowledgeBase.addDefinitionError(clazz.getName() + ": " + message);
    }

    // -----------------------
    // Annotation utilities
    // -----------------------

    private boolean hasAnyParameterWithDisposesAnnotation(Method method) {
        for (Parameter p : method.getParameters()) {
            if (hasDisposesAnnotation(p)) return true;
        }
        return false;
    }

    private boolean hasMetaAnnotation(Class<? extends Annotation> annotationType, Class<? extends Annotation> metaAnnotationType) {
        return annotationType.isAnnotationPresent(metaAnnotationType);
    }

    /**
     * Validates that a decorator has exactly one @Delegate injection point.
     *
     * <p><b>CDI 4.1 Decorator Rules (Section 8.3):</b>
     * <ul>
     *   <li>A decorator must have exactly one @Delegate injection point</li>
     *   <li>The @Delegate injection point must be an @Inject field, initializer method parameter, or constructor parameter</li>
     *   <li>The @Delegate injection point defines which types the decorator can decorate</li>
     * </ul>
     *
     * <p><b>Example valid decorator:</b>
     * <pre>{@code
     * @Decorator
     * public class LoggingDecorator implements MyService {
     *     @Inject @Delegate
     *     private MyService delegate; // Exactly one @Delegate injection point
     *
     *     public void doWork() {
     *         log("Before");
     *         delegate.doWork();
     *         log("After");
     *     }
     * }
     * }</pre>
     *
     * @param clazz the decorator class to validate
     */
    private void validateDecoratorDelegateInjectionPoints(Class<?> clazz) {
        int delegateCount = 0;

        // Check @Inject fields for @Delegate
        for (Field field : clazz.getDeclaredFields()) {
            if (hasInjectAnnotation(field)) {
                if (hasDelegateAnnotation(field)) {
                    delegateCount++;
                }
            }
        }

        // Check @Inject method parameters for @Delegate
        for (Method method : clazz.getDeclaredMethods()) {
            if (hasInjectAnnotation(method)) {
                for (Parameter param : method.getParameters()) {
                    if (hasDelegateAnnotation(param)) {
                        delegateCount++;
                    }
                }
            }
        }

        // Check constructor parameters for @Delegate
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (hasInjectAnnotation(constructor)) {
                for (Parameter param : constructor.getParameters()) {
                    if (hasDelegateAnnotation(param)) {
                        delegateCount++;
                    }
                }
            }
        }

        // Validate exactly one @Delegate injection point
        if (delegateCount == 0) {
            knowledgeBase.addDefinitionError(clazz.getName() +
                    ": Decorator must have exactly one @Delegate injection point (found 0). " +
                    "Add @Inject @Delegate to a field, method parameter, or constructor parameter.");
        } else if (delegateCount > 1) {
            knowledgeBase.addDefinitionError(clazz.getName() +
                    ": Decorator must have exactly one @Delegate injection point (found " + delegateCount + "). " +
                    "Only one @Delegate injection point is allowed per decorator.");
        }
    }

    /**
     * Checks if an annotated element (field or parameter) has @Delegate annotation.
     * A @Delegate can be either a jakarta.decorator.Delegate or a javax.decorator.Delegate.
     *
     * @param element the field or parameter to check
     * @return true if @Delegate annotation is present
     */
    private boolean hasDelegateAnnotation(java.lang.reflect.AnnotatedElement element) {
        for (Annotation ann : element.getAnnotations()) {
            String name = ann.annotationType().getName();
            if (name.equals("jakarta.decorator.Delegate") || name.equals("javax.decorator.Delegate")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates and registers a ProducerBean for a producer method or field.
     *
     * @param declaringClass the class containing the producer
     * @param producerMethod the producer method (null if this is a field producer)
     * @param producerField the producer field (null if this is a method producer)
     */
    private void createAndRegisterProducerBean(Class<?> declaringClass, Method producerMethod, Field producerField) {
        AnnotatedElement element = (producerMethod != null) ? producerMethod : producerField;

        // Determine alternative status at element level
        boolean annotatedAlternative = isAlternativeDeclared(declaringClass) || hasAlternativeAnnotation(element);
        boolean alternativeEnabled = isAlternativeEnabled(element, declaringClass, annotatedAlternative);

        // Create ProducerBean
        ProducerBean<?> producerBean;
        if (producerMethod != null) {
            producerBean = new ProducerBean<>(declaringClass, producerMethod, annotatedAlternative);
            producerBean.setAlternativeEnabled(alternativeEnabled);

            // Set bean attributes from producer method annotations
            producerBean.setName(extractProducerName(producerMethod));
            producerBean.setQualifiers(extractQualifiers(producerMethod));
            producerBean.setScope(extractScope(producerMethod, Dependent.class));
            producerBean.setStereotypes(extractStereotypes(producerMethod));

            BeanTypesExtractor.ExtractionResult producerTypes =
                    beanTypesExtractor.extractProducerBeanTypes(producerMethod.getGenericReturnType());
            for (String error : producerTypes.getDefinitionErrors()) {
                knowledgeBase.addDefinitionError(fmtMethod(producerMethod) + ": " + error);
            }
            producerBean.setTypes(producerTypes.getTypes());

            // CDI 4.1 Section 3.10: Add InjectionPoint metadata for producer method parameters
            // Producer method parameters are injection points and should have InjectionPoint metadata
            for (Parameter param : producerMethod.getParameters()) {
                // Skip @Disposes parameters - they are not injection points
                if (!hasDisposesAnnotation(param)) {
                    InjectionPoint ip = tryCreateInjectionPoint(param, producerBean);
                    if (ip != null) {
                        producerBean.addInjectionPoint(ip);
                    }
                }
            }
        } else if (producerField != null) {
            producerBean = new ProducerBean<>(declaringClass, producerField, annotatedAlternative);
            producerBean.setAlternativeEnabled(alternativeEnabled);

            // Set bean attributes from producer field annotations
            producerBean.setName(extractProducerName(producerField));
            producerBean.setQualifiers(extractQualifiers(producerField));
            producerBean.setScope(extractScope(producerField, Dependent.class));
            producerBean.setStereotypes(extractStereotypes(producerField));

            BeanTypesExtractor.ExtractionResult producerTypes =
                    beanTypesExtractor.extractProducerBeanTypes(producerField.getGenericType());
            for (String error : producerTypes.getDefinitionErrors()) {
                knowledgeBase.addDefinitionError(fmtField(producerField) + ": " + error);
            }
            producerBean.setTypes(producerTypes.getTypes());
        } else {
            throw new IllegalArgumentException("Either producerMethod or producerField must be non-null");
        }

        // Find and set the disposer method if present
        Type producedType = (producerMethod != null)
                ? producerMethod.getGenericReturnType()
                : producerField.getGenericType();
        Method disposer = findDisposerForProducer(declaringClass, producedType, producerBean.getQualifiers());
        if (disposer != null) {
            producerBean.setDisposerMethod(disposer);
        }

        // Mark producer bean as vetoed if the declaring class was vetoed by an extension
        if (knowledgeBase.isTypeVetoed(declaringClass)) {
            producerBean.setVetoed(true);
            System.out.println("[CDI41BeanValidator] Producer bean marked as vetoed (declaring class vetoed): " +
                declaringClass.getName() + " -> " +
                (producerMethod != null ? producerMethod.getName() : producerField.getName()));
        }

        // Capture @Priority for enabled alternatives (used during resolution ordering)
        Integer priorityValue = extractDeclaredPriority(element.getAnnotations());
        if (priorityValue == null) {
            priorityValue = extractEffectivePriority(declaringClass);
        }
        if (priorityValue != null) {
            producerBean.setPriority(priorityValue);
        }

        // Register in KnowledgeBase
        knowledgeBase.addProducerBean(producerBean);
    }

    /**
     * Extracts producer name from @Named annotation, or returns empty string.
     */
    private String extractProducerName(AnnotatedElement element) {
        Named named = element.getAnnotation(Named.class);
        if (named != null) {
            String value = named.value();
            if (value != null && !value.isEmpty()) {
                return value;
            }
            // For fields/methods, default name is the member name
            if (element instanceof Field) {
                return ((Field) element).getName();
            } else if (element instanceof Method) {
                String methodName = ((Method) element).getName();
                // Strip "get" prefix if present (JavaBeans convention)
                if (methodName.startsWith("get") && methodName.length() > 3) {
                    return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                }
                return methodName;
            }
        }
        return "";
    }

    /**
     * Extracts qualifiers from an annotated element.
     */
    private Set<Annotation> extractQualifiers(AnnotatedElement element) {
        return QualifiersHelper.extractBeanQualifiers(element.getAnnotations());
    }

    /**
     * Extracts stereotypes declared on a producer member.
     */
    private Set<Class<? extends Annotation>> extractStereotypes(AnnotatedElement element) {
        Set<Class<? extends Annotation>> stereotypes = new HashSet<>();
        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (hasMetaAnnotation(annotationType, Stereotype.class)) {
                stereotypes.add(annotationType);
            }
        }
        return stereotypes;
    }

    /**
     * Extracts scope from an annotated element, or returns default scope.
     */
    private Class<? extends Annotation> extractScope(AnnotatedElement element, Class<? extends Annotation> defaultScope) {
        List<Class<? extends Annotation>> directScopes = new ArrayList<>();
        for (Annotation ann : element.getAnnotations()) {
            Class<? extends Annotation> annotationType = ann.annotationType();
            if (hasMetaAnnotation(annotationType, Scope.class) ||
                hasMetaAnnotation(annotationType, jakarta.inject.Scope.class) ||
                hasMetaAnnotation(annotationType, NormalScope.class) ||
                hasMetaAnnotation(annotationType, javax.enterprise.context.NormalScope.class)) {
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
            return directScopes.get(0);
        }

        Class<? extends Annotation> inheritedScope = null;
        for (Annotation ann : element.getAnnotations()) {
            Class<? extends Annotation> annotationType = ann.annotationType();
            if (!hasMetaAnnotation(annotationType, Stereotype.class)) {
                continue;
            }

            Class<? extends Annotation> stereotypeScope = extractScopeFromStereotype(annotationType);
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
            return inheritedScope;
        }

        return defaultScope;
    }

    private String describeAnnotatedElement(AnnotatedElement element) {
        if (element instanceof Field) {
            return fmtField((Field) element);
        }
        if (element instanceof Method) {
            return fmtMethod((Method) element);
        }
        if (element instanceof Class) {
            return ((Class<?>) element).getName();
        }
        return element.toString();
    }

    /**
     * Finds the disposer method for a producer member by matching disposed parameter type and qualifiers.
     */
    private Method findDisposerForProducer(Class<?> clazz,
                                           Type producerType,
                                           Set<Annotation> producerQualifiers) {
        List<Method> matches = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            Parameter disposesParameter = getDisposesParameter(method);
            if (disposesParameter == null) {
                continue;
            }

            if (matchesDisposesParameter(disposesParameter, producerType, producerQualifiers)) {
                matches.add(method);
            }
        }

        if (matches.size() <= 1) {
            return matches.isEmpty() ? null : matches.get(0);
        }

        knowledgeBase.addDefinitionError(clazz.getName() +
                ": multiple disposer methods match producer type " + producerType.getTypeName() +
                " and qualifiers " + formatQualifiers(producerQualifiers));
        return matches.get(0);
    }

    private boolean validateDisposerMethodHasMatchingProducer(Class<?> clazz, Method disposerMethod) {
        Parameter disposesParameter = getDisposesParameter(disposerMethod);
        if (disposesParameter == null) {
            return false;
        }

        Type disposesType = disposesParameter.getParameterizedType();
        Set<Annotation> requiredQualifiers = QualifiersHelper.extractQualifiers(disposesParameter.getAnnotations());

        for (Method producerMethod : clazz.getDeclaredMethods()) {
            if (!hasProducesAnnotation(producerMethod)) {
                continue;
            }
            if (matchesDisposesParameter(disposesParameter,
                    producerMethod.getGenericReturnType(),
                    extractQualifiers(producerMethod))) {
                return true;
            }
        }

        for (Field producerField : clazz.getDeclaredFields()) {
            if (!hasProducesAnnotation(producerField)) {
                continue;
            }
            if (matchesDisposesParameter(disposesParameter,
                    producerField.getGenericType(),
                    extractQualifiers(producerField))) {
                return true;
            }
        }

        knowledgeBase.addDefinitionError(fmtMethod(disposerMethod) +
                ": @Disposes parameter type/qualifiers do not match any producer method return type or producer field type " +
                "(type=" + disposesType.getTypeName() + ", qualifiers=" + formatQualifiers(requiredQualifiers) + ")");
        return false;
    }

    private boolean matchesDisposesParameter(Parameter disposesParameter,
                                             Type producerType,
                                             Set<Annotation> producerQualifiers) {
        Set<Annotation> requiredQualifiers = QualifiersHelper.extractQualifiers(disposesParameter.getAnnotations());
        if (!QualifiersHelper.qualifiersMatch(requiredQualifiers, producerQualifiers)) {
            return false;
        }

        Type disposesType = disposesParameter.getParameterizedType();
        try {
            return typeChecker.isAssignable(disposesType, producerType);
        } catch (DefinitionException e) {
            return false;
        } catch (IllegalStateException e) {
            knowledgeBase.addDefinitionError(fmtParameter(disposesParameter) +
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

    private Parameter getDisposesParameter(Method method) {
        for (Parameter param : method.getParameters()) {
            if (hasDisposesAnnotation(param)) {
                return param;
            }
        }
        return null;
    }

    /**
     * Checks if a method has a parameter annotated with @Disposes.
     */
    private boolean hasDisposesParameter(Method method) {
        for (Parameter param : method.getParameters()) {
            if (hasDisposesAnnotation(param)) {
                return true;
            }
        }
        return false;
    }

    private Class<?> getBoxedType(Class<?> primitive) {
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == double.class) return Double.class;
        if (primitive == float.class) return Float.class;
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == char.class) return Character.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == short.class) return Short.class;
        if (primitive == void.class) return Void.class;
        return primitive;
    }

    // -----------------------
    /**
     * Populates injection metadata in BeanImpl for use during bean creation.
     * This includes:
     * - @Inject constructor (or no-args constructor)
     * - @Inject fields (from entire class hierarchy)
     * - @Inject methods (from entire class hierarchy, excluding overridden)
     * - @PostConstruct method
     * - @PreDestroy method
     */
    private <T> void populateInjectionMetadata(BeanImpl<T> bean, Class<T> clazz) {
        // 1. Find and set constructor per JSR-330 rules (only in the bean class itself)
        // - If there's an @Inject constructor, use it
        // - Otherwise, leave null (BeanImpl will use no-args constructor)
        Constructor<T> injectConstructor = null;

        // Look for @Inject constructor
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (hasInjectAnnotation(c)) {
                @SuppressWarnings("unchecked")
                Constructor<T> typedConstructor = (Constructor<T>) c;
                injectConstructor = typedConstructor;
                break;
            }
        }

        bean.setInjectConstructor(injectConstructor);

        // 2. Collect @Inject fields from the entire hierarchy (superclass → subclass)
        // Fields are inherited, so we need to collect from all classes in the hierarchy
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (hasInjectAnnotation(field)) {
                    bean.addInjectField(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // 3. Collect @Inject methods from the entire hierarchy (superclass → subclass)
        // Methods can be inherited and overridden, so collect from all classes
        currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (hasInjectAnnotation(method)) {
                    bean.addInjectMethod(method);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // 4. Find all @PostConstruct methods in hierarchy (superclass → subclass order)
        // Per Interceptors Specification 1.2+: All @PostConstruct methods in the hierarchy are invoked
        // unless overridden by a subclass
        findAllLifecycleMethods(clazz, PostConstruct.class, bean, true);

        // 5. Find all @PreDestroy methods in hierarchy (superclass → subclass order during discovery)
        // Per Interceptors Specification 1.2+: All @PreDestroy methods in the hierarchy are invoked
        // unless overridden by a subclass. They will be executed in reverse order (subclass → superclass).
        findAllLifecycleMethods(clazz, PreDestroy.class, bean, false);

        // 6. Find all @PrePassivate methods in hierarchy for passivating scopes
        // NOTE: @PrePassivate is an EJB annotation (jakarta.ejb), NOT CDI 4.1 standard.
        // This is optional support for EJB integration. CDI 4.1 uses writeObject()/readObject().
        findAllPassivationMethods(clazz, bean, true);

        // 7. Find all @PostActivate methods in hierarchy for passivating scopes
        // NOTE: @PostActivate is an EJB annotation (jakarta.ejb), NOT CDI 4.1 standard.
        // This is optional support for EJB integration. CDI 4.1 uses writeObject()/readObject().
        findAllPassivationMethods(clazz, bean, false);
    }

    /**
     * Finds all lifecycle methods (@PostConstruct or @PreDestroy) in the class hierarchy.
     * <p>
     * <b>Interceptors Specification 1.2+ / CDI 4.1 Section 7.1:</b>
     * <ul>
     *   <li>Lifecycle methods are discovered in superclass → subclass order</li>
     *   <li>If a subclass overrides a superclass lifecycle method, only the overriding method is invoked</li>
     *   <li>Multiple lifecycle methods can exist in the hierarchy (one per class level)</li>
     *   <li>@PostConstruct: executed superclass → subclass</li>
     *   <li>@PreDestroy: executed subclass → superclass (reversed at invocation time)</li>
     * </ul>
     *
     * @param clazz the bean class
     * @param lifecycleAnnotation the lifecycle annotation class (@PostConstruct or @PreDestroy)
     * @param bean the bean being populated
     * @param isPostConstruct true if @PostConstruct, false if @PreDestroy
     */
    private void findAllLifecycleMethods(Class<?> clazz,
                                         Class<? extends Annotation> lifecycleAnnotation,
                                         BeanImpl<?> bean,
                                         boolean isPostConstruct) {
        // Build class hierarchy: superclass → subclass
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            hierarchy.add(0, current); // Add at beginning to get superclass → subclass order
            current = current.getSuperclass();
        }

        // Process in superclass → subclass order
        for (int i = 0; i < hierarchy.size(); i++) {
            Class<?> currentClass = hierarchy.get(i);
            Method foundMethod = null;

            for (Method method : currentClass.getDeclaredMethods()) {
                boolean hasLifecycle = isPostConstruct ?
                    hasPostConstructAnnotation(method) :
                    hasPreDestroyAnnotation(method);

                if (hasLifecycle) {
                    // Validate lifecycle method rules:
                    // - Must have no parameters
                    // - Must not be static
                    // - Return type is ignored (can be void or any type)
                    if (method.getParameterCount() != 0) {
                        knowledgeBase.addDefinitionError(
                            fmtMethod(method) + ": " + lifecycleAnnotation.getSimpleName() +
                            " method must have no parameters"
                        );
                        return;
                    }
                    if (Modifier.isStatic(method.getModifiers())) {
                        knowledgeBase.addDefinitionError(
                            fmtMethod(method) + ": " + lifecycleAnnotation.getSimpleName() +
                            " method must not be static"
                        );
                        return;
                    }

                    // Check if multiple lifecycle methods in same class (not allowed)
                    if (foundMethod != null) {
                        knowledgeBase.addDefinitionError(
                            currentClass.getName() + ": multiple " +
                            lifecycleAnnotation.getSimpleName() +
                            " methods found in same class (only one allowed per class)"
                        );
                        return;
                    }

                    foundMethod = method;
                }
            }

            if (foundMethod != null) {
                // A lifecycle method is inherited only if no subclass overrides it.
                if (!isOverriddenBySubclass(foundMethod, hierarchy, i + 1)) {
                    if (isPostConstruct) {
                        bean.addPostConstructMethod(foundMethod);
                    } else {
                        bean.addPreDestroyMethod(foundMethod);
                    }
                }
            }
        }
    }

    private boolean isOverriddenBySubclass(Method method, List<Class<?>> hierarchy, int startIndex) {
        if (Modifier.isPrivate(method.getModifiers())) {
            return false;
        }

        for (int i = startIndex; i < hierarchy.size(); i++) {
            Class<?> subclass = hierarchy.get(i);
            Method candidate = findDeclaredMethod(subclass, method.getName(), method.getParameterTypes());
            if (candidate == null) {
                continue;
            }

            if (Modifier.isStatic(candidate.getModifiers())) {
                continue;
            }

            if (isOverridableFromSubclass(method, subclass)) {
                return true;
            }
        }

        return false;
    }

    private Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private boolean isOverridableFromSubclass(Method method, Class<?> subclass) {
        int modifiers = method.getModifiers();

        if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
            return true;
        }

        if (Modifier.isPrivate(modifiers)) {
            return false;
        }

        // package-private: only overridable in the same package
        return packageName(method.getDeclaringClass()).equals(packageName(subclass));
    }

    private String packageName(Class<?> clazz) {
        Package pkg = clazz.getPackage();
        return pkg == null ? "" : pkg.getName();
    }

    /**
     * Finds all passivation lifecycle methods (@PrePassivate or @PostActivate) in the class hierarchy.
     * <p>
     * <b>IMPORTANT:</b> @PrePassivate and @PostActivate are EJB annotations (jakarta.ejb), NOT CDI 4.1 standard.
     * CDI 4.1 Section 6.6 only requires beans in passivating scopes to implement Serializable and relies on
     * Java's standard {@code writeObject()}/{@code readObject()} methods. This method provides optional support
     * for EJB-style callbacks as a convenience for applications using both CDI and EJB.
     * <p>
     * <b>CDI 4.1 Standard Approach:</b> Beans should use Java's serialization callbacks instead:
     * <ul>
     *   <li>{@code private void writeObject(ObjectOutputStream)} instead of @PrePassivate</li>
     *   <li>{@code private void readObject(ObjectInputStream)} instead of @PostActivate</li>
     * </ul>
     * <p>
     * <b>EJB Callback Behavior (when jakarta.ejb is on classpath):</b>
     * <ul>
     *   <li>@PrePassivate methods are invoked before the session is serialized</li>
     *   <li>@PostActivate methods are invoked after the session is deserialized</li>
     *   <li>Execution order: superclass → subclass (same as @PostConstruct)</li>
     *   <li>Override detection: if a subclass overrides a superclass method, only the overriding method is invoked</li>
     * </ul>
     * <p>
     * These callbacks allow beans to:
     * <ul>
     *   <li>Close non-serializable resources before passivation (database connections, file handles)</li>
     *   <li>Re-open resources after activation</li>
     *   <li>Transform state to serializable form</li>
     * </ul>
     *
     * @param clazz the bean class
     * @param bean the bean implementation to populate with lifecycle methods
     * @param isPrePassivate true for @PrePassivate, false for @PostActivate
     */
    private void findAllPassivationMethods(Class<?> clazz, BeanImpl<?> bean, boolean isPrePassivate) {
        // Try to load jakarta.ejb.PrePassivate and jakarta.ejb.PostActivate annotations
        Class<? extends Annotation> annotationClass;
        try {
            if (isPrePassivate) {
                annotationClass = Class.forName("jakarta.ejb.PrePassivate").asSubclass(Annotation.class);
            } else {
                annotationClass = Class.forName("jakarta.ejb.PostActivate").asSubclass(Annotation.class);
            }
        } catch (ClassNotFoundException e) {
            // jakarta.ejb not on classpath - passivation callbacks not supported
            // This is acceptable - not all applications need passivating scopes
            return;
        }

        // Build class hierarchy: superclass → subclass
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            hierarchy.add(0, current); // Add at beginning for superclass-first order
            current = current.getSuperclass();
        }

        // Track seen signatures for override detection
        Set<String> seenSignatures = new HashSet<>();

        // Process in superclass → subclass order
        for (Class<?> currentClass : hierarchy) {
            Method foundMethod = findPassivationMethodInClass(currentClass, annotationClass);

            if (foundMethod != null) {
                String signature = getMethodSignature(foundMethod);

                // Skip if overridden by subclass
                if (!seenSignatures.contains(signature)) {
                    seenSignatures.add(signature);

                    // Validate method signature
                    if (foundMethod.getParameterCount() != 0) {
                        addValidationError(currentClass,
                            (isPrePassivate ? "@PrePassivate" : "@PostActivate") +
                            " method must have no parameters: " + foundMethod.getName());
                        continue;
                    }

                    if (Modifier.isStatic(foundMethod.getModifiers())) {
                        addValidationError(currentClass,
                            (isPrePassivate ? "@PrePassivate" : "@PostActivate") +
                            " method cannot be static: " + foundMethod.getName());
                        continue;
                    }

                    // Add to bean's passivation method list
                    if (isPrePassivate) {
                        bean.addPrePassivateMethod(foundMethod);
                    } else {
                        bean.addPostActivateMethod(foundMethod);
                    }
                }
            }
        }
    }

    /**
     * Finds a passivation lifecycle method in a single class (not hierarchy).
     *
     * @param clazz the class to search
     * @param annotationClass the annotation class (@PrePassivate or @PostActivate)
     * @return the found method, or null if none found
     */
    private Method findPassivationMethodInClass(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Method found = null;
        int count = 0;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationClass)) {
                found = method;
                count++;
            }
        }

        // Validate: at most one passivation method per class
        if (count > 1) {
            addValidationError(clazz,
                "Class cannot have more than one @" + annotationClass.getSimpleName() +
                " method, found " + count);
            return null;
        }

        return found;
    }

    /**
     * Gets a method signature for override detection.
     * Format: "methodName(paramType1,paramType2,...)"
     *
     * @param method the method
     * @return the method signature string
     */
    private String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder(method.getName());
        sb.append("(");
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(paramTypes[i].getName());
        }
        sb.append(")");
        return sb.toString();
    }

    // InjectionPoint best-effort creation
    // -----------------------

    private InjectionPoint tryCreateInjectionPoint(AnnotatedElement element, Bean<?> owningBean) {
        try {
            if (element instanceof Field) {
                return new InjectionPointImpl<>((Field) element, owningBean);
            }
            if (element instanceof Parameter) {
                return new InjectionPointImpl<>((Parameter) element, owningBean);
            }
        } catch (Throwable ignored) {
            // Best-effort only. Bean can still be registered without concrete injection point objects.
        }
        return null;
    }

    // -----------------------
    // Interceptor Validation
    // -----------------------

    /**
     * Validates and registers interceptor metadata.
     *
     * <p><b>CDI 4.1 Interceptor Requirements (Section 9):</b>
     * <ul>
     *   <li>Must have @Interceptor annotation</li>
     *   <li>Must have at least one interceptor binding annotation</li>
     *   <li>Must have exactly one @AroundInvoke, @AroundConstruct, @PostConstruct, or @PreDestroy method</li>
     *   <li>Can optionally have @Priority for ordering (defaults to Integer.MAX_VALUE)</li>
     * </ul>
     *
     * @param clazz the interceptor class to validate
     */
    private void validateAndRegisterInterceptor(Class<?> clazz) {
        boolean valid = true;

        // Extract interceptor bindings
        Set<Annotation> interceptorBindings = extractInterceptorBindings(clazz);
        if (interceptorBindings.isEmpty()) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": @Interceptor must have at least one interceptor binding annotation");
            valid = false;
        }

        // Extract priority
        int priority = Integer.MAX_VALUE;
        Priority priorityAnnotation = clazz.getAnnotation(Priority.class);
        if (priorityAnnotation != null) {
            priority = priorityAnnotation.value();
        }

        // Find interceptor methods
        Method aroundInvokeMethod = findAroundInvokeMethod(clazz);
        Method aroundConstructMethod = findAroundConstructMethod(clazz);
        Method postConstructMethod = findPostConstructMethod(clazz);
        Method preDestroyMethod = findPreDestroyMethod(clazz);

        // Validate that at least one interceptor method exists
        if (aroundInvokeMethod == null && aroundConstructMethod == null &&
            postConstructMethod == null && preDestroyMethod == null) {
            knowledgeBase.addDefinitionError(clazz.getName() +
                ": @Interceptor must have at least one interceptor method (@AroundInvoke, @AroundConstruct, @PostConstruct, or @PreDestroy)");
            valid = false;
        }

        // Validate signatures per CDI spec
        if (aroundInvokeMethod != null && !isValidAroundInvoke(aroundInvokeMethod)) {
            knowledgeBase.addDefinitionError(fmtMethod(aroundInvokeMethod) +
                    ": @AroundInvoke must be non-static, return Object, and accept a single InvocationContext parameter");
            valid = false;
        }
        if (aroundConstructMethod != null && !isValidAroundConstruct(aroundConstructMethod)) {
            knowledgeBase.addDefinitionError(fmtMethod(aroundConstructMethod) +
                    ": @AroundConstruct must be non-static, return Object, and accept a single InvocationContext parameter");
            valid = false;
        }
        if (postConstructMethod != null && !isValidInterceptorLifecycleMethod(postConstructMethod)) {
            knowledgeBase.addDefinitionError(fmtMethod(postConstructMethod) +
                    ": @PostConstruct interceptor method must be non-static, void/Object, and take a single InvocationContext parameter");
            valid = false;
        }
        if (preDestroyMethod != null && !isValidInterceptorLifecycleMethod(preDestroyMethod)) {
            knowledgeBase.addDefinitionError(fmtMethod(preDestroyMethod) +
                    ": @PreDestroy interceptor method must be non-static, void/Object, and take a single InvocationContext parameter");
            valid = false;
        }

        // Only register if valid
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

    private boolean isValidAroundInvoke(Method m) {
        return !Modifier.isStatic(m.getModifiers())
                && m.getReturnType().equals(Object.class)
                && m.getParameterCount() == 1
                && jakarta.interceptor.InvocationContext.class.isAssignableFrom(m.getParameterTypes()[0]);
    }

    private boolean isValidAroundConstruct(Method m) {
        // TCK uses void-returning @AroundConstruct in AbstractInterceptor; allow both Object and void
        boolean returnOk = m.getReturnType().equals(Object.class) || m.getReturnType().equals(void.class);
        return !Modifier.isStatic(m.getModifiers())
                && returnOk
                && m.getParameterCount() == 1
                && jakarta.interceptor.InvocationContext.class.isAssignableFrom(m.getParameterTypes()[0]);
    }

    /**
     * CDI 4.1 / Jakarta Interceptors 2.2: lifecycle interceptor methods declared
     * on interceptor classes must be non-static, non-final, return void or Object,
     * and accept exactly one InvocationContext parameter.
     *
     * Note: lifecycle methods declared on target beans follow different rules
     * (void, no-args). This validator is only used for interceptor classes.
     */
    private boolean isValidInterceptorLifecycleMethod(Method m) {
        boolean returnOk = m.getReturnType().equals(void.class) || m.getReturnType().equals(Object.class);
        return !Modifier.isStatic(m.getModifiers())
                && returnOk
                && m.getParameterCount() == 1
                && jakarta.interceptor.InvocationContext.class.isAssignableFrom(m.getParameterTypes()[0]);
    }

    /**
     * Extracts interceptor binding annotations from the class.
     * An interceptor binding is an annotation that is itself annotated with @InterceptorBinding.
     */
    private Set<Annotation> extractInterceptorBindings(Class<?> clazz) {
        Set<Annotation> bindings = new HashSet<>();
        for (Annotation annotation : annotationsOf(clazz)) {
            if (isInterceptorBinding(annotation.annotationType())) {
                bindings.add(annotation);
            }
        }
        return bindings;
    }

    /**
     * Checks if an annotation type is an interceptor binding.
     */
    private boolean isInterceptorBinding(Class<? extends Annotation> annotationType) {
        return annotationType.isAnnotationPresent(jakarta.interceptor.InterceptorBinding.class);
    }

    /**
     * Finds the @AroundInvoke method in the interceptor class.
     * CDI spec: must have signature Object method(InvocationContext) throws Exception
     */
    private Method findAroundInvokeMethod(Class<?> clazz) {
        for (Method method : getAllMethods(clazz)) {
            if (method.isAnnotationPresent(jakarta.interceptor.AroundInvoke.class)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Finds the @AroundConstruct method in the interceptor class.
     * CDI spec: must have signature void method(InvocationContext) throws Exception
     */
    private Method findAroundConstructMethod(Class<?> clazz) {
        for (Method method : getAllMethods(clazz)) {
            if (method.isAnnotationPresent(jakarta.interceptor.AroundConstruct.class)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Finds the @PostConstruct method in the interceptor class (lifecycle callback).
     */
    private Method findPostConstructMethod(Class<?> clazz) {
        for (Method method : getAllMethods(clazz)) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Finds the @PreDestroy method in the interceptor class (lifecycle callback).
     */
    private Method findPreDestroyMethod(Class<?> clazz) {
        for (Method method : getAllMethods(clazz)) {
            if (method.isAnnotationPresent(PreDestroy.class)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Returns all declared methods in the class hierarchy, stopping at Object.
     */
    private List<Method> getAllMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Collections.addAll(methods, current.getDeclaredMethods());
            current = current.getSuperclass();
        }
        return methods;
    }

    // -----------------------
    // Decorator Validation
    // -----------------------

    /**
     * Validates and registers decorator metadata.
     *
     * <p><b>CDI 4.1 Decorator Requirements (Section 8):</b>
     * <ul>
     *   <li>Must have @Decorator annotation</li>
     *   <li>Must have exactly ONE @Delegate injection point</li>
     *   <li>Must implement or extend the decorated types</li>
     *   <li>Can optionally have @Priority for ordering (defaults to Integer.MAX_VALUE)</li>
     * </ul>
     *
     * @param clazz the decorator class to validate
     */
    private void validateAndRegisterDecorator(Class<?> clazz) {
        // Extract priority
        int priority = Integer.MAX_VALUE;
        Priority priorityAnnotation = clazz.getAnnotation(Priority.class);
        if (priorityAnnotation != null) {
            priority = priorityAnnotation.value();
        }

        // Find the @Delegate injection point
        InjectionPoint delegateInjectionPoint = findDelegateInjectionPoint(clazz);
        if (delegateInjectionPoint == null) {
            // Error already reported by validateDecoratorDelegateInjectionPoints
            return;
        }

        // Extract decorated types from @Delegate injection point
        Set<Type> decoratedTypes = extractDecoratedTypes(clazz, delegateInjectionPoint);

        // Validate that decorator implements all abstract methods of decorated types
        validateDecoratorAbstractMethods(clazz, decoratedTypes);

        // Register decorator info
        DecoratorInfo info = new DecoratorInfo(
            clazz,
            decoratedTypes,
            priority,
            delegateInjectionPoint
        );
        knowledgeBase.addDecoratorInfo(info);
    }

    /**
     * CDI 4.1 Section 8.1: A decorator must implement all abstract methods of the decorated types
     * (excluding methods declared on java.lang.Object).
     */
    private void validateDecoratorAbstractMethods(Class<?> decoratorClass, Set<Type> decoratedTypes) {
        if (decoratedTypes == null || decoratedTypes.isEmpty()) {
            return;
        }

        for (Type type : decoratedTypes) {
            if (!(type instanceof Class)) {
                continue; // Skip non-class types for implementation check
            }
            Class<?> decoratedClass = (Class<?>) type;

            for (Method m : decoratedClass.getMethods()) {
                // Only consider abstract, non-Object methods
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
                            ": abstract method " + fmtMethod(m) +
                            " must be implemented with a concrete method in the decorator.");
                    }
                } catch (NoSuchMethodException e) {
                    knowledgeBase.addDefinitionError(decoratorClass.getName() +
                        ": missing implementation for abstract method " + fmtMethod(m) +
                        " from decorated type " + decoratedClass.getName());
                }
            }
        }
    }

    /**
     * Finds the @Delegate injection point in the decorator class.
     */
    private InjectionPoint findDelegateInjectionPoint(Class<?> clazz) {
        // Check fields
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(jakarta.decorator.Delegate.class)) {
                return tryCreateInjectionPoint(field, null);
            }
        }

        // Check constructor parameters
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            for (Parameter parameter : constructor.getParameters()) {
                if (parameter.isAnnotationPresent(jakarta.decorator.Delegate.class)) {
                    return tryCreateInjectionPoint(parameter, null);
                }
            }
        }

        // Check method parameters
        for (Method method : clazz.getDeclaredMethods()) {
            if (hasInjectAnnotation(method)) {
                for (Parameter parameter : method.getParameters()) {
                    if (parameter.isAnnotationPresent(jakarta.decorator.Delegate.class)) {
                        return tryCreateInjectionPoint(parameter, null);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Extracts decorated types from the decorator class.
     * The decorated types are the interfaces/classes that the decorator implements/extends.
     */
    private Set<Type> extractDecoratedTypes(Class<?> clazz, InjectionPoint delegateInjectionPoint) {

        // Add all interfaces
        Set<Type> decoratedTypes = new HashSet<>(Arrays.asList(clazz.getGenericInterfaces()));

        // Add superclass (if not Object)
        Type superclass = clazz.getGenericSuperclass();
        if (superclass != null && superclass != Object.class) {
            decoratedTypes.add(superclass);
        }

        return decoratedTypes;
    }


}
