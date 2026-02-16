package com.threeamigos.common.util.implementations.injection;

import jakarta.annotation.Priority;
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

        if (isVetoed(clazz)) {
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
            boolean inject = hasAnnotation(field, Inject.class);
            boolean produces = hasAnnotation(field, Produces.class);

            if (inject) {
                hasInjectionPoints = true;
                valid &= validateInjectField(field);
            }

            if (produces) {
                valid &= validateProducerField(field);
            }

            // Disallow illegal combos proactively
            if (inject && produces) {
                knowledgeBase.addDefinitionError(fmtField(field) + ": may not declare both @Inject and @Produces");
                valid = false;
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            boolean inject = hasAnnotation(method, Inject.class);
            boolean produces = hasAnnotation(method, Produces.class);

            if (inject) {
                hasInjectionPoints = true;
                valid &= validateInitializerMethod(method);
            }

            if (produces) {
                valid &= validateProducerMethod(method);
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

        // 6) Check if this is an alternative bean and if it's properly enabled
        boolean alternative = hasAnnotation(clazz, Alternative.class);
        boolean alternativeEnabled = false;

        if (alternative) {
            // CDI 4.1 Section 5.1.3: Alternatives must be enabled via beans.xml OR @Priority
            // Since we don't support beans.xml, alternatives MUST have @Priority to be enabled
            Priority priority = clazz.getAnnotation(Priority.class);

            if (priority != null) {
                // Alternative is properly enabled with @Priority
                alternativeEnabled = true;
            } else {
                // Alternative without @Priority is NOT enabled - log warning and skip
                knowledgeBase.addError(
                    "Alternative bean " + clazz.getName() + " is not enabled. " +
                    "Without beans.xml support, alternatives must have @Priority annotation. " +
                    "Add @Priority(value) to enable this alternative, or remove @Alternative if not needed."
                );
                // Return null - this bean should not be registered as it's not enabled
                return null;
            }
        }

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

        // Minimal injection point collection (best-effort, avoids hard dependency on a full CDI SPI implementation)
        // If InjectionPointImpl exists and you want richer metadata later, this is the place to add it.
        for (Field field : clazz.getDeclaredFields()) {
            if (hasAnnotation(field, Inject.class)) {
                InjectionPoint ip = tryCreateInjectionPoint(field);
                if (ip != null) {
                    bean.addInjectionPoint(ip);
                }
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (hasAnnotation(method, Inject.class)) {
                for (Parameter p : method.getParameters()) {
                    InjectionPoint ip = tryCreateInjectionPoint(p);
                    if (ip != null) {
                        bean.addInjectionPoint(ip);
                    }
                }
            }
        }
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (hasAnnotation(c, Inject.class)) {
                for (Parameter p : c.getParameters()) {
                    InjectionPoint ip = tryCreateInjectionPoint(p);
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
        // Minimal, practical approach:
        // - Collect qualifier annotations from the class
        // - If none exist, add @Default
        // - Always add @Any (CDI built-in)
        Set<Annotation> result = new HashSet<>();

        for (Annotation a : clazz.getAnnotations()) {
            if (isQualifierAnnotationType(a.annotationType())) {
                result.add(a);
            }
        }

        if (result.isEmpty()) {
            result.add(new DefaultLiteral());
        }

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
                .filter(c -> hasAnnotation(c, Inject.class))
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
        if (hasAnnotation(method, Produces.class)) {
            knowledgeBase.addDefinitionError(fmtMethod(method) + ": initializer method may not be annotated @Produces");
            valid = false;
        }
        if (hasAnyParameterAnnotated(method, Disposes.class)) {
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
            if (hasAnnotation(p, Disposes.class)) {
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
        if (hasAnnotation(field, Inject.class)) {
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
        if (hasAnnotation(method, Inject.class)) {
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
            if (hasAnnotation(p, Disposes.class)) {
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
            if (hasAnnotation(f, Produces.class)) return true;
        }
        for (Method m : clazz.getDeclaredMethods()) {
            if (hasAnnotation(m, Produces.class)) return true;
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
        // Conservative: treat a class as "candidate" if it has CDI-relevant annotations (scope, inject, produces, named, alternative, stereotype)
        // Otherwise, we return false to avoid logging errors for random classes on the classpath.
        if (clazz == null) return false;

        if (isVetoed(clazz)) return false;

        if (hasAnnotation(clazz, Inject.class)
                || hasAnnotation(clazz, Produces.class)
                || hasAnnotation(clazz, Named.class)
                || hasAnnotation(clazz, Alternative.class)
                || hasAnnotation(clazz, Stereotype.class)
                || hasAnnotation(clazz, Decorator.class)
                || hasAnnotation(clazz, Interceptor.class)) {
            return true;
        }

        // scope presence makes it a candidate
        for (Annotation a : clazz.getAnnotations()) {
            if (isScopeAnnotationType(a.annotationType())) {
                return true;
            }
        }

        // having injection points/producers makes it a candidate
        for (Field f : clazz.getDeclaredFields()) {
            if (hasAnnotation(f, Inject.class) || hasAnnotation(f, Produces.class)) return true;
        }
        for (Method m : clazz.getDeclaredMethods()) {
            if (hasAnnotation(m, Inject.class) || hasAnnotation(m, Produces.class)) return true;
        }

        return false;
    }

    private boolean isVetoed(Class<?> clazz) {
        return hasAnnotation(clazz, Vetoed.class);
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

    private boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return element.isAnnotationPresent(annotationType);
    }

    private boolean hasAnyParameterAnnotated(Method method, Class<? extends Annotation> annotationType) {
        for (Parameter p : method.getParameters()) {
            if (hasAnnotation(p, annotationType)) return true;
        }
        return false;
    }

    private boolean hasMetaAnnotation(Class<? extends Annotation> annotationType, Class<? extends Annotation> metaAnnotationType) {
        return annotationType.isAnnotationPresent(metaAnnotationType);
    }

    // -----------------------
    // InjectionPoint best-effort creation
    // -----------------------

    private InjectionPoint tryCreateInjectionPoint(AnnotatedElement element) {
        // This project has InjectionPointImpl, but we keep this validator decoupled:
        // return null if we can't safely construct it without assuming a constructor signature.
        try {
            Class<?> ipImpl = Class.forName("com.threeamigos.common.util.implementations.injection.InjectionPointImpl");
            // Try common constructors reflectively (Field) or (Parameter)
            if (element instanceof Field) {
                Field f = (Field) element;
                for (Constructor<?> c : ipImpl.getDeclaredConstructors()) {
                    Class<?>[] p = c.getParameterTypes();
                    if (p.length == 1 && p[0].equals(Field.class)) {
                        c.setAccessible(true);
                        return (InjectionPoint) c.newInstance(f);
                    }
                }
            }
            if (element instanceof Parameter) {
                Parameter p0 = (Parameter)element;
                for (Constructor<?> c : ipImpl.getDeclaredConstructors()) {
                    Class<?>[] p = c.getParameterTypes();
                    if (p.length == 1 && p[0].equals(Parameter.class)) {
                        c.setAccessible(true);
                        return (InjectionPoint) c.newInstance(p0);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Best-effort only. Bean can still be registered without concrete injection point objects.
        }
        return null;
    }


}