package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.knowledgebase.DecoratorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.InterceptorInfo;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.literals.AnyLiteral;
import com.threeamigos.common.util.implementations.injection.literals.DefaultLiteral;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.enterprise.context.*;
import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.Annotation;
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
    private Annotation[] overrideAnnotations;

    CDI41BeanValidator(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
    }

    /**
     * Validates the class and returns a Bean if valid, otherwise returns null.
     * If valid, the Bean is registered in the KnowledgeBase.
     */
    <T> BeanImpl<T> validateAndRegister(Class<T> clazz, BeanArchiveMode beanArchiveMode) {
        return validateAndRegister(clazz, beanArchiveMode, null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    BeanImpl<?> validateAndRegisterRaw(Class<?> clazz, BeanArchiveMode beanArchiveMode, AnnotatedType<?> annotatedTypeOverride) {
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

        // 1) Bean class eligibility (managed bean type)
        if (!isCandidateBeanClass(clazz, beanArchiveMode)) {
            // Not necessarily an error; just not a bean (CDI scans lots of classes).
            return null;
        }

        if (hasVetoedAnnotation(clazz)) {
            return null;
        }

        // 2) Basic structural constraints
        if (clazz.isInterface() || clazz.isAnnotation() || clazz.isEnum() || clazz.isPrimitive() || clazz.isArray()) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": is not a valid CDI bean class type");
            return null;
        }

        if (Modifier.isAbstract(clazz.getModifiers())) {
            // Abstract classes are not managed beans (they can define producers, but not be instantiated as beans).
            // We treat this as "not a bean" rather than a hard error to avoid polluting KB during scanning.
            return null;
        }

        if (clazz.isLocalClass() || clazz.isAnonymousClass() || clazz.isSynthetic()) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": is not a valid CDI bean class (local/anonymous/synthetic)");
            return null;
        }

        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": non-static inner classes are not valid CDI beans");
            return null;
        }

        // 3) Scope sanity (at most one scope) + capture it for bean construction
        Class<? extends Annotation> beanScope = null;
        try {
            beanScope = validateScopeAnnotations(clazz);
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(clazz.getName() + ": " + e.getMessage());
            valid = false;
        }

        // 4) Validate producers and injection points on fields/methods
        boolean hasInjectionPoints = false;

        for (Field field : clazz.getDeclaredFields()) {
            boolean inject = hasInjectAnnotation(field);
            boolean produces = hasProducesAnnotation(field);

            if (inject) {
                hasInjectionPoints = true;
                valid &= validateInjectField(field);
            }

            if (produces) {
                valid &= validateProducerField(field);
                // Create and register ProducerBean for this producer field
                createAndRegisterProducerBean(clazz, null, field);
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
                valid &= validateProducerMethod(method);
                // Create and register ProducerBean for this producer method
                createAndRegisterProducerBean(clazz, method, null);
            }

            if (disposes) {
                valid &= validateDisposerMethod(method);
            }

            if (inject && produces) {
                knowledgeBase.addDefinitionError(fmtMethod(method) + ": may not declare both @Inject and @Produces");
                valid = false;
            }
        }

        // 5) Constructor rules (only if it is a bean OR it has injection points / producers)
        boolean relevantClass = hasInjectionPoints || hasAnyProducer(clazz);
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
        boolean isInterceptor = hasInterceptorAnnotation(clazz);
        boolean isDecorator = hasDecoratorAnnotation(clazz);

        if (isInterceptor) {
            knowledgeBase.addInterceptor(clazz);
            // Validate and register interceptor metadata
            validateAndRegisterInterceptor(clazz);
            // Interceptors are not managed beans - return null (no bean to register)
            return null;
        }

        if (isDecorator) {
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
        boolean alternative = hasAlternativeAnnotation(clazz);
        boolean alternativeEnabled = isAlternativeEnabled(clazz, alternative);

        // Build and register Bean (even if invalid, to track all beans)
        // Mark alternative based on annotation; enablement affects resolution elsewhere.
        BeanImpl<T> bean = new BeanImpl<>(clazz, alternative);

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
        bean.setScope(extractBeanScope(clazz, beanScope));
        bean.setTypes(extractBeanTypes(clazz));
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

    private Annotation[] annotationsOf(Class<?> clazz) {
        return overrideAnnotations != null ? overrideAnnotations : clazz.getAnnotations();
    }

    private String extractBeanName(Class<?> clazz) {
        // CDI: @Named without value defaults to decapitalized simple name.
        for (Annotation a : annotationsOf(clazz)) {
            if (a.annotationType().equals(Named.class)) {
                try {
                    Method value = a.annotationType().getMethod("value");
                    Object v = value.invoke(a);
                    String s = (v == null) ? "" : v.toString();
                    if (!s.trim().isEmpty()) {
                        return s.trim();
                    }
                } catch (ReflectiveOperationException ignored) {
                    // fall through to defaulting
                }
                return decapitalize(clazz.getSimpleName());
            }
        }
        return "";
    }

    private Set<Annotation> extractBeanQualifiers(Class<?> clazz) {
        // CDI 4.1 approach:
        // - Collect qualifier annotations from the class
        // - Inherit qualifiers from stereotypes
        // - If no qualifiers exist (other than @Named), add @Default
        // - @Named is a special qualifier that doesn't replace @Default
        // - Always add @Any (CDI built-in)
        Set<Annotation> result = new HashSet<>();
        boolean hasNonNamedQualifier = false;

        // First, collect qualifiers directly on the class
        for (Annotation a : annotationsOf(clazz)) {
            if (isQualifierAnnotationType(a.annotationType())) {
                result.add(a);
                // Check if this is a qualifier other than @Named
                if (!a.annotationType().equals(Named.class)) {
                    hasNonNamedQualifier = true;
                }
            }
        }

        // Then, inherit qualifiers from stereotypes
        for (Annotation a : annotationsOf(clazz)) {
            if (hasMetaAnnotation(a.annotationType(), Stereotype.class)) {
                Set<Annotation> stereotypeQualifiers = extractQualifiersFromStereotype(a.annotationType());
                result.addAll(stereotypeQualifiers);
                if (!stereotypeQualifiers.isEmpty()) {
                    // Check if stereotype defines non-@Named qualifiers
                    for (Annotation sq : stereotypeQualifiers) {
                        if (!sq.annotationType().equals(Named.class)) {
                            hasNonNamedQualifier = true;
                        }
                    }
                }
            }
        }

        // Add @Default if no qualifiers (other than @Named) exist
        if (!hasNonNamedQualifier) {
            result.add(new DefaultLiteral());
        }

        // Always add @Any
        result.add(new AnyLiteral());

        return result;
    }

    private Class<? extends Annotation> extractBeanScope(Class<?> clazz) {
        // CDI 4.1: Check for scope directly on the class first
        // If a scope exists, it's already validated as at-most-one in validateScopeAnnotations.
        for (Annotation a : annotationsOf(clazz)) {
            Class<? extends Annotation> at = a.annotationType();
            if (isScopeAnnotationType(at)) {
                return at;
            }
        }

        // If no direct scope, inherit from stereotypes
        // If multiple stereotypes define different scopes, CDI requires explicit scope on the bean
        // For now, we take the first stereotype's scope (CDI validation would catch conflicts)
        for (Annotation a : annotationsOf(clazz)) {
            if (hasMetaAnnotation(a.annotationType(), Stereotype.class)) {
                Class<? extends Annotation> stereotypeScope = extractScopeFromStereotype(a.annotationType());
                if (stereotypeScope != null) {
                    return stereotypeScope;
                }
            }
        }

        // Default scope for managed beans is @Dependent.
        return Dependent.class;
    }

    private Set<Type> extractBeanTypes(Class<?> clazz) {
        // CDI 4.1 Section 2.2 - Bean types
        // Check for @Typed annotation which restricts the bean types
        // Using AnnotationsEnum for javax/jakarta compatibility
        if (hasTypedAnnotation(clazz)) {
            Annotation typedAnnotation = getTypedAnnotation(clazz);
            if (typedAnnotation != null) {
                // @Typed present: Use only the types specified in the annotation
                Set<Type> types = new LinkedHashSet<>();

                try {
                    // Use reflection to extract value() from the annotation (works with both javax and jakarta)
                    Method valueMethod = typedAnnotation.getClass().getMethod("value");
                    Class<?>[] typedClasses = (Class<?>[]) valueMethod.invoke(typedAnnotation);

                    if (typedClasses.length == 0) {
                        // @Typed with empty value means only Object.class
                        types.add(Object.class);
                    } else {
                        // Add all types specified in @Typed
                        for (Class<?> typedClass : typedClasses) {
                            // Validate that the bean class is assignable to the typed class
                            if (!typedClass.isAssignableFrom(clazz)) {
                                addValidationError(clazz,
                                    "@Typed specifies type " + typedClass.getName() +
                                    " which is not a type of bean class " + clazz.getName());
                                continue;
                            }
                            types.add(typedClass);
                        }
                        // Object.class is always present per CDI 4.1 spec
                        types.add(Object.class);
                    }
                    return types;
                } catch (ReflectiveOperationException e) {
                    // If reflection fails, fall through to default behavior
                    knowledgeBase.addDefinitionError(clazz.getName() +
                        ": Failed to extract @Typed annotation values: " + e.getMessage());
                }
            }
        }

        // No @Typed annotation: Conservative "bean types" computation
        // Include class, all superclasses (up to Object), and all directly implemented interfaces across the hierarchy.
        Set<Type> types = new LinkedHashSet<>();
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            types.add(c);
            types.addAll(Arrays.asList(c.getGenericInterfaces()));
            c = c.getSuperclass();
        }
        types.add(Object.class);
        return types;
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
        if (s.length() > 1 && Character.isUpperCase(s.charAt(0)) && Character.isUpperCase(s.charAt(1))) {
            // "URLService" stays "URLService" (matches common CDI behavior expectations)
            return s;
        }
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
        Set<Annotation> qualifiers = new HashSet<>();

        // Collect qualifiers directly on the stereotype
        for (Annotation a : stereotypeClass.getAnnotations()) {
            if (isQualifierAnnotationType(a.annotationType())) {
                qualifiers.add(a);
            }
        }

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

            if (!hasValidInjectionParameters(c.getParameters(), fmtConstructor(c))) {
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

    // -----------------------
    // Validation: @Inject
    // -----------------------

    private boolean validateInjectField(Field field) {
        boolean valid = true;

        if (Modifier.isFinal(field.getModifiers())) {
            knowledgeBase.addInjectionError(fmtField(field) + ": final fields are not valid injection points");
            valid = false;
        }

        // CDI is stricter than JSR-330 about certain injection points; keep conservative:
        if (Modifier.isStatic(field.getModifiers())) {
            knowledgeBase.addInjectionError(fmtField(field) + ": static field injection is not a valid CDI injection point");
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

        return valid;
    }

    private boolean validateInitializerMethod(Method method) {
        boolean valid = true;

        // Initializer method constraints (conservative CDI 4.1)
        if (Modifier.isAbstract(method.getModifiers())) {
            knowledgeBase.addInjectionError(fmtMethod(method) + ": cannot inject into an abstract initializer method");
            valid = false;
        }

        if (Modifier.isStatic(method.getModifiers())) {
            knowledgeBase.addInjectionError(fmtMethod(method) + ": static initializer methods are not valid CDI injection points");
            valid = false;
        }

        if (method.getTypeParameters().length > 0) {
            knowledgeBase.addInjectionError(fmtMethod(method) + ": generic methods are not valid CDI initializer methods");
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
        }
        return valid;
    }

    // -----------------------
    // Validation: producers
    // -----------------------

    private boolean validateProducerField(Field field) {
        boolean valid = true;

        // A producer field must not be final (container needs to read it; final often implies constant semantics)
        if (Modifier.isFinal(field.getModifiers())) {
            knowledgeBase.addDefinitionError(fmtField(field) + ": producer field must not be final");
            valid = false;
        }

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

        // Important rule you explicitly asked for:
        if (hasInjectAnnotation(method)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": producer method must not be annotated @Inject");
            valid = false;
        }

        // Validate return type (no type variables/wildcards)
        try {
            checkProducerTypeValidity(method.getGenericReturnType());
        } catch (DefinitionException e) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": " + e.getMessage());
            valid = false;
        }

        // Parameters: at most one @Disposes, and only valid within producer methods.
        int disposesCount = 0;
        for (Parameter p : method.getParameters()) {
            if (hasDisposesAnnotation(p)) {
                disposesCount++;
            } else {
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
            }
        }

        if (disposesCount > 1) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": producer method may declare at most one @Disposes parameter");
            valid = false;
        }

        return valid;
    }

    /**
     * Validates a disposer method according to CDI 4.1 Section 3.4.
     * <p>
     * CDI 4.1 Disposer Method Rules:
     * <ul>
     *   <li>Must have exactly one parameter annotated with @Disposes</li>
     *   <li>Must not be annotated with @Produces or @Inject</li>
     *   <li>Must not be abstract or static</li>
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

        // Rule 2: Must not be static
        if (Modifier.isStatic(method.getModifiers())) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method must not be static");
            valid = false;
        }

        // Rule 3: Must not be generic
        if (method.getTypeParameters().length > 0) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method must not be generic");
            valid = false;
        }

        // Rule 4: Must not be annotated with @Produces
        if (hasProducesAnnotation(method)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method may not be annotated @Produces");
            valid = false;
        }

        // Rule 5: Must not be annotated with @Inject
        if (hasInjectAnnotation(method)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": disposer method may not be annotated @Inject");
            valid = false;
        }

        // Rule 6: Must have exactly one @Disposes parameter
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

        // Rule 7: Validate @Disposes parameter type
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
            }
        }

        // Rule 9: Validate that a matching producer exists
        // Note: This is checked during producer-disposer linking phase, not here

        return valid;
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
     * CDI 4.1 alternative enabling helper usable for bean classes and producer members.
     *
     * <p>CDI 4.1 Section 5.1.2: Alternatives can be enabled via:
     * <ul>
     *   <li>@Priority annotation on the class (preferred in CDI 4.1)</li>
     *   <li>beans.xml &lt;alternatives&gt; section (traditional method)</li>
     * </ul>
     *
     * <p>For stereotype-based alternatives, the stereotype itself (not the class)
     * must be listed in beans.xml or annotated with @Priority.
     */
    private boolean isAlternativeEnabled(AnnotatedElement element, boolean annotatedAlternative) {
        if (!annotatedAlternative) {
            return false;
        }

        // Method 1: Check for @Priority annotation
        Priority priority = element.getAnnotation(Priority.class);
        if (priority != null) {
            return true; // enabled by @Priority
        }

        // Method 2: Check beans.xml alternatives section
        if (element instanceof Class) {
            Class<?> clazz = (Class<?>) element;

            // Check if class is directly listed as alternative in beans.xml
            if (knowledgeBase.isAlternativeEnabledInBeansXml(clazz.getName())) {
                return true;
            }

            // Check if class has a stereotype that's enabled as alternative in beans.xml
            for (Annotation annotation : annotationsOf(clazz)) {
                Class<? extends Annotation> annotationType = annotation.annotationType();

                // Check if this is a stereotype annotation
                if (AnnotationsEnum.STEREOTYPE.isPresent(annotationType)) {
                    // Check if the stereotype is listed in beans.xml alternatives
                    if (knowledgeBase.isAlternativeEnabledInBeansXml(annotationType.getName())) {
                        return true;
                    }
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
        List<Class<? extends Annotation>> scopes = Arrays.stream(annotationsOf(clazz))
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

        // Default scope for managed beans is @Dependent.
        return Dependent.class;
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

    private boolean isQualifierAnnotationType(Class<? extends Annotation> at) {
        return hasMetaAnnotation(at, Qualifier.class);
    }

    /**
     * Validates that a producer type is legal according to CDI 4.1 Section 3.3.2.
     * <p>
     * Rules:
     * <ul>
     *   <li>The type itself cannot be a wildcard or type variable</li>
     *   <li>Parameterized types CAN contain wildcards (e.g., List&lt;?&gt; is valid)</li>
     *   <li>Wildcards in parameterized types are allowed for producers (but not for injection points)</li>
     * </ul>
     *
     * @param type the producer return/field type to validate
     * @throws DefinitionException if the type is invalid
     */
    private void checkProducerTypeValidity(Type type) {
        if (type instanceof WildcardType) {
            throw new DefinitionException("type may not be a wildcard (" + type.getTypeName() + ")");
        }
        if (type instanceof TypeVariable) {
            throw new DefinitionException("type may not be a type variable (" + type.getTypeName() + ")");
        }

        // Note: Parameterized types containing wildcards (e.g., List<? extends Number>) are ALLOWED
        // for producers (unlike injection points). The wildcards are handled during type extraction
        // and resolution according to CDI 4.1 typesafe resolution rules.
    }

    private void checkInjectionTypeValidity(Type type) {
        if (type instanceof WildcardType) {
            throw new DefinitionException("injection point may not contain a wildcard (" + type.getTypeName() + ")");
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
        if (raw.isPrimitive()) {
            throw new IllegalArgumentException("cannot inject a primitive");
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
                if (arg instanceof WildcardType) {
                    throw new DefinitionException("injection point may not contain wildcard type arguments (" + arg.getTypeName() + ")");
                }
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

        // Bean-defining annotations per CDI spec
        if (hasApplicationScopedAnnotation(clazz) ||
                hasSessionScopedAnnotation(clazz) ||
                hasRequestScopedAnnotation(clazz) ||
                hasConversationScopedAnnotation(clazz) ||
                hasDependentAnnotation(clazz) ||
                hasSingletonAnnotation(clazz) ||
                hasInterceptorAnnotation(clazz) ||
                hasDecoratorAnnotation(clazz) ||
                hasAlternativeAnnotation(clazz) ||
                hasStereotypeAnnotation(clazz)) {
            return true;
        }

        // A scope meta-annotated type also counts
        for (Annotation a : annotationsOf(clazz)) {
            if (isScopeAnnotationType(a.annotationType())) {
                return true;
            }
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
        boolean annotatedAlternative = hasAlternativeAnnotation(declaringClass) || hasAlternativeAnnotation(element);
        boolean alternativeEnabled = isAlternativeEnabled(element, annotatedAlternative);

        // Create ProducerBean
        ProducerBean<?> producerBean;
        if (producerMethod != null) {
            producerBean = new ProducerBean<>(declaringClass, producerMethod, annotatedAlternative);

            // Set bean attributes from producer method annotations
            producerBean.setName(extractProducerName(producerMethod));
            producerBean.setQualifiers(extractQualifiers(producerMethod));
            producerBean.setScope(extractScope(producerMethod, Dependent.class));
            producerBean.setTypes(extractProducerTypes(producerMethod.getGenericReturnType()));

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

            // Set bean attributes from producer field annotations
            producerBean.setName(extractProducerName(producerField));
            producerBean.setQualifiers(extractQualifiers(producerField));
            producerBean.setScope(extractScope(producerField, Dependent.class));
            producerBean.setTypes(extractProducerTypes(producerField.getGenericType()));
        } else {
            throw new IllegalArgumentException("Either producerMethod or producerField must be non-null");
        }

        // Find and set the disposer method if present
        if (producerMethod != null) {
            Method disposer = findDisposerForProducer(declaringClass, producerMethod);
            if (disposer != null) {
                producerBean.setDisposerMethod(disposer);
            }
        }

        // Mark producer bean as vetoed if the declaring class was vetoed by an extension
        if (knowledgeBase.isTypeVetoed(declaringClass)) {
            producerBean.setVetoed(true);
            System.out.println("[CDI41BeanValidator] Producer bean marked as vetoed (declaring class vetoed): " +
                declaringClass.getName() + " -> " +
                (producerMethod != null ? producerMethod.getName() : producerField.getName()));
        }

        // Capture @Priority for enabled alternatives (used during resolution ordering)
        Priority priority = element.getAnnotation(Priority.class);
        if (priority != null) {
            producerBean.setPriority(priority.value());
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
        Set<Annotation> qualifiers = new HashSet<>();
        for (Annotation ann : element.getAnnotations()) {
            if (hasMetaAnnotation(ann.annotationType(), Qualifier.class)) {
                qualifiers.add(ann);
            }
        }
        // Add @Default if no qualifiers present
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
        // Always add @Any
        qualifiers.add(new AnyLiteral());
        return qualifiers;
    }

    /**
     * Extracts scope from an annotated element, or returns default scope.
     */
    private Class<? extends Annotation> extractScope(AnnotatedElement element, Class<? extends Annotation> defaultScope) {
        for (Annotation ann : element.getAnnotations()) {
            if (hasMetaAnnotation(ann.annotationType(), Scope.class) ||
                hasMetaAnnotation(ann.annotationType(), jakarta.inject.Scope.class)) {
                return ann.annotationType();
            }
        }
        return defaultScope;
    }

    /**
     * Extracts bean types for a producer (its return/field type and supertypes).
     * <p>
     * According to CDI 4.1 Section 3.3 - Bean types of a producer method/field:
     * <ul>
     *   <li>The producer type itself (with its parameterization, if any)</li>
     *   <li>All supertypes and superinterfaces of the raw type</li>
     *   <li>Object.class</li>
     *   <li>Wildcards in parameterized types are preserved for typesafe resolution</li>
     * </ul>
     * <p>
     * <b>CDI 4.1 Wildcard Handling:</b>
     * When a producer method returns a parameterized type containing wildcards
     * (e.g., {@code List<? extends Number>}), the wildcard is part of the bean type.
     * During typesafe resolution:
     * <ul>
     *   <li>{@code List<? extends Number>} can satisfy injection point {@code List<Integer>}
     *       if Integer extends Number</li>
     *   <li>{@code List<?>} (unbounded) can satisfy any {@code List<T>} injection point</li>
     *   <li>Resolution follows Java's wildcard subtyping rules</li>
     * </ul>
     *
     * @param producerType the type returned by producer method or field
     * @return set of bean types for this producer
     */
    private Set<Type> extractProducerTypes(Type producerType) {
        // Build full bean types per CDI: raw type, all superclasses, all interfaces
        Set<Type> types = new LinkedHashSet<>();

        // Add the declared generic type itself first (keeps parameterization including wildcards)
        // This is crucial: List<? extends Number> must be in the bean types with its wildcard
        types.add(producerType);

        // Add raw type hierarchy (superclasses and interfaces)
        Class<?> c = RawTypeExtractor.getRawType(producerType);
        while (c != null && c != Object.class) {
            types.add(c);
            // Add all interfaces implemented by this class (raw types)
            types.addAll(Arrays.asList(c.getGenericInterfaces()));
            c = c.getSuperclass();
        }

        // Always include Object.class as per CDI spec
        types.add(Object.class);

        return types;
    }

    /**
     * Finds the disposer method for a given producer method.
     */
    private Method findDisposerForProducer(Class<?> clazz, Method producerMethod) {
        Class<?> producedType = producerMethod.getReturnType();

        for (Method method : clazz.getDeclaredMethods()) {
            if (hasDisposesParameter(method)) {
                // Check if disposer parameter type matches producer return type
                for (Parameter param : method.getParameters()) {
                    if (hasDisposesAnnotation(param)) {
                        if (param.getType().equals(producedType)) {
                            return method;
                        }
                    }
                }
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

        // Track which method signatures we've seen (to detect overrides)
        Set<String> seenSignatures = new HashSet<>();

        // Process in superclass → subclass order
        for (Class<?> currentClass : hierarchy) {
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
                // Create method signature for override detection
                String signature = getMethodSignature(foundMethod);

                // If this signature was already seen, it means a subclass overrides it
                // In that case, skip this method (only the overriding method should be called)
                if (!seenSignatures.contains(signature)) {
                    seenSignatures.add(signature);

                    // Add to bean's lifecycle method list
                    if (isPostConstruct) {
                        bean.addPostConstructMethod(foundMethod);
                    } else {
                        bean.addPreDestroyMethod(foundMethod);
                    }
                }
            }
        }
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
        if (postConstructMethod != null && !isValidLifecycleVoidNoArgs(postConstructMethod)) {
            knowledgeBase.addDefinitionError(fmtMethod(postConstructMethod) +
                    ": @PostConstruct interceptor method must be non-static, void, and take no parameters");
            valid = false;
        }
        if (preDestroyMethod != null && !isValidLifecycleVoidNoArgs(preDestroyMethod)) {
            knowledgeBase.addDefinitionError(fmtMethod(preDestroyMethod) +
                    ": @PreDestroy interceptor method must be non-static, void, and take no parameters");
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
        return !Modifier.isStatic(m.getModifiers())
                && m.getReturnType().equals(Object.class)
                && m.getParameterCount() == 1
                && jakarta.interceptor.InvocationContext.class.isAssignableFrom(m.getParameterTypes()[0]);
    }

    private boolean isValidLifecycleVoidNoArgs(Method m) {
        return !Modifier.isStatic(m.getModifiers())
                && m.getReturnType().equals(void.class)
                && m.getParameterCount() == 0;
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
        for (Method method : clazz.getDeclaredMethods()) {
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
        for (Method method : clazz.getDeclaredMethods()) {
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
        for (Method method : clazz.getDeclaredMethods()) {
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
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PreDestroy.class)) {
                return method;
            }
        }
        return null;
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
