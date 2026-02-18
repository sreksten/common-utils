package com.threeamigos.common.util.implementations.injection;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Disposes;
import jakarta.inject.Named;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.enterprise.context.*;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.Vetoed;
import jakarta.decorator.Decorator;
import jakarta.interceptor.Interceptor;
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

    CDI41BeanValidator(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
    }

    /**
     * Validates the class and, if valid as a CDI bean, registers a corresponding Bean in the KnowledgeBase.
     *
     * @return true if valid and registered, false otherwise.
     */
    <T> boolean isValid(Class<T> clazz) {
        return validateAndRegister(clazz) != null;
    }

    /**
     * Validates the class and returns a Bean if valid, otherwise returns null.
     * If valid, the Bean is registered in the KnowledgeBase.
     */
    <T> BeanImpl<T> validateAndRegister(Class<T> clazz) {
        Objects.requireNonNull(clazz, "Class cannot be null");

        boolean valid = true;

        // 1) Bean class eligibility (managed bean type)
        if (!isCandidateBeanClass(clazz)) {
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
                // Disposers will be linked to ProducerBeans after all beans are created
                // Store disposer info for later processing
                // (handled in a separate pass after all producers are registered)
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
            // Interceptors are not managed beans - return null (no bean to register)
            return null;
        }

        if (isDecorator) {
            knowledgeBase.addDecorator(clazz);
            // Decorators are not managed beans - return null (no bean to register)
            return null;
        }

        // 7) Check if this is an alternative bean and if it's properly enabled
        boolean alternative = hasAlternativeAnnotation(clazz);
        boolean alternativeEnabled = isAlternativeEnabled(clazz, alternative);

        // 7) Build and register Bean (even if invalid, to track all beans)
        // Note: Only alternatives with @Priority reach this point
        BeanImpl<T> bean = new BeanImpl<>(clazz, alternativeEnabled);

        // Mark bean as having validation errors if validation failed
        if (!valid) {
            bean.setHasValidationErrors(true);
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
    }

    private String extractBeanName(Class<?> clazz) {
        // CDI: @Named without value defaults to decapitalized simple name.
        for (Annotation a : clazz.getAnnotations()) {
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
        // - If no qualifiers exist (other than @Named), add @Default
        // - @Named is a special qualifier that doesn't replace @Default
        // - Always add @Any (CDI built-in)
        Set<Annotation> result = new HashSet<>();
        boolean hasNonNamedQualifier = false;

        for (Annotation a : clazz.getAnnotations()) {
            if (isQualifierAnnotationType(a.annotationType())) {
                result.add(a);
                // Check if this is a qualifier other than @Named
                if (!a.annotationType().equals(Named.class)) {
                    hasNonNamedQualifier = true;
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
        // If a scope exists, it's already validated as at-most-one in validateScopeAnnotations.
        for (Annotation a : clazz.getAnnotations()) {
            Class<? extends Annotation> at = a.annotationType();
            if (isScopeAnnotationType(at)) {
                return at;
            }
        }
        // Default scope for managed beans is @Dependent.
        return Dependent.class;
    }

    private Set<Type> extractBeanTypes(Class<?> clazz) {
        // Conservative "bean types" computation:
        // include class, all superclasses (up to Object), and all directly implemented interfaces across the hierarchy.
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
        for (Annotation a : clazz.getAnnotations()) {
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
     */
    private boolean isAlternativeEnabled(AnnotatedElement element, boolean annotatedAlternative) {
        if (!annotatedAlternative) {
            return false;
        }

        Priority priority = element.getAnnotation(Priority.class);
        if (priority != null) {
            return true; // enabled by @Priority
        }

        knowledgeBase.addError(
                "Alternative " + element + " is not enabled. Without beans.xml support, alternatives must declare @Priority."
        );
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
        List<Class<? extends Annotation>> scopes = Arrays.stream(clazz.getAnnotations())
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

    private void checkProducerTypeValidity(Type type) {
        if (type instanceof WildcardType) {
            throw new DefinitionException("type may not be a wildcard (" + type.getTypeName() + ")");
        }
        if (type instanceof TypeVariable) {
            throw new DefinitionException("type may not be a type variable (" + type.getTypeName() + ")");
        }
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

    private boolean isCandidateBeanClass(Class<?> clazz) {
        // CDI 4.1 annotated discovery: a bean-defining annotation is required in an implicit archive.
        // We therefore accept only true bean-defining annotations and skip plain @Inject-only classes.
        if (clazz == null || hasVetoedAnnotation(clazz)) return false;

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
        for (Annotation a : clazz.getAnnotations()) {
            if (isScopeAnnotationType(a.annotationType())) {
                return true;
            }
        }

        return false;
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
     * Creates and registers a ProducerBean for a producer method or field.
     *
     * @param declaringClass the class containing the producer
     * @param producerMethod the producer method (null if this is a field producer)
     * @param producerField the producer field (null if this is a method producer)
     */
    private void createAndRegisterProducerBean(Class<?> declaringClass, Method producerMethod, Field producerField) {
        AnnotatedElement element = (producerMethod != null) ? producerMethod : producerField;

        // Determine alternative status at element level; require @Priority to enable in beans.xml-less mode
        boolean annotatedAlternative = hasAlternativeAnnotation(declaringClass) || hasAlternativeAnnotation(element);
        boolean alternativeEnabled = isAlternativeEnabled(element, annotatedAlternative);

        // Create ProducerBean
        ProducerBean<?> producerBean;
        if (producerMethod != null) {
            producerBean = new ProducerBean<>(declaringClass, producerMethod, alternativeEnabled);

            // Set bean attributes from producer method annotations
            producerBean.setName(extractProducerName(producerMethod));
            producerBean.setQualifiers(extractQualifiers(producerMethod));
            producerBean.setScope(extractScope(producerMethod, Dependent.class));
            producerBean.setTypes(extractProducerTypes(producerMethod.getGenericReturnType()));
        } else if (producerField != null) {
            producerBean = new ProducerBean<>(declaringClass, producerField, alternativeEnabled);

            // Set bean attributes from producer field annotations
            producerBean.setName(extractProducerName(producerField));
            producerBean.setQualifiers(extractQualifiers(producerField));
            producerBean.setScope(extractScope(producerField, Dependent.class));
            producerBean.setTypes(extractProducerTypes(producerField.getGenericType()));
        } else {
            throw new IllegalArgumentException("Either producerMethod or producerField must be non-null");
        }

        // Find and set disposer method if present
        if (producerMethod != null) {
            Method disposer = findDisposerForProducer(declaringClass, producerMethod);
            if (disposer != null) {
                producerBean.setDisposerMethod(disposer);
            }
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
     */
    private Set<Type> extractProducerTypes(Type producerType) {
        // Build full bean types per CDI: raw type, all superclasses, all interfaces
        Set<Type> types = new LinkedHashSet<>();

        // Add hierarchy
        Class<?> c = RawTypeExtractor.getRawType(producerType);
        while (c != null && c != Object.class) {
            types.add(c);
            types.addAll(Arrays.asList(c.getGenericInterfaces()));
            c = c.getSuperclass();
        }

        // Include the declared generic type itself (keeps parameterization)
        types.add(producerType);
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

        // 2. Collect @Inject fields from entire hierarchy (superclass → subclass)
        // Fields are inherited, so we need to collect from all classes in hierarchy
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (hasInjectAnnotation(field)) {
                    bean.addInjectField(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // 3. Collect @Inject methods from entire hierarchy (superclass → subclass)
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

        // 4. Find @PostConstruct method (searches hierarchy automatically)
        // CDI 4.1: At most one @PostConstruct method per class
        Method postConstruct = findLifecycleMethod(clazz, PostConstruct.class);
        if (postConstruct != null) {
            bean.setPostConstructMethod(postConstruct);
        }

        // 5. Find @PreDestroy method (searches hierarchy automatically)
        // CDI 4.1: At most one @PreDestroy method per class
        Method preDestroy = findLifecycleMethod(clazz, PreDestroy.class);
        if (preDestroy != null) {
            bean.setPreDestroyMethod(preDestroy);
        }
    }

    /**
     * Finds a lifecycle method (@PostConstruct or @PreDestroy) in the class hierarchy.
     * Returns the first method found with the specified annotation.
     *
     * @param clazz the bean class
     * @param lifecycleAnnotation the lifecycle annotation class (@PostConstruct or @PreDestroy)
     * @return the lifecycle method, or null if not found
     */
    private Method findLifecycleMethod(Class<?> clazz, Class<? extends Annotation> lifecycleAnnotation) {
        // Determine which static checker to use
        boolean isPostConstruct = lifecycleAnnotation.equals(PostConstruct.class) ||
                                   lifecycleAnnotation.equals(javax.annotation.PostConstruct.class);

        // Search in current class and superclasses
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Method method : currentClass.getDeclaredMethods()) {
                boolean hasLifecycle = isPostConstruct ? hasPostConstructAnnotation(method) : hasPreDestroyAnnotation(method);

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
                        return null;
                    }
                    if (Modifier.isStatic(method.getModifiers())) {
                        knowledgeBase.addDefinitionError(
                            fmtMethod(method) + ": " + lifecycleAnnotation.getSimpleName() +
                            " method must not be static"
                        );
                        return null;
                    }
                    return method;
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
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


}
