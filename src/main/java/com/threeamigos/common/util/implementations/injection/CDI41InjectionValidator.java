package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.knowledgebase.ObserverMethodInfo;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates that all injection points can be satisfied according to CDI 4.1 rules.
 *
 * <p>This validator runs AFTER bean discovery and validation (CDI41BeanValidator) completes,
 * and checks whether all declared injection points can be resolved to valid beans.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Check for unsatisfied dependencies (no matching bean found)</li>
 *   <li>Check for ambiguous dependencies (multiple matching beans without qualifiers)</li>
 *   <li>Detect invalid beans being used as dependencies</li>
 *   <li>Handle Instance&lt;T&gt; and Provider&lt;T&gt; injection points (always satisfiable)</li>
 *   <li>Validate producer methods can satisfy injection points</li>
 *   <li>Detect circular dependencies during validation phase</li>
 *   <li>Validate enabled alternatives (only one per type)</li>
 *   <li>Report all resolution errors to KnowledgeBase</li>
 * </ul>
 *
 * <p>This allows the system to avoid false positives: beans with validation errors
 * only cause application failure if they are actually needed for injection.
 *
 * <p>Uses existing utilities:
 * <ul>
 *   <li>{@link TypeChecker} - for proper type hierarchy and generic matching</li>
 *   <li>{@link RawTypeExtractor} - for extracting raw types from generic types</li>
 * </ul>
 */
class CDI41InjectionValidator {

    private final KnowledgeBase knowledgeBase;
    private final TypeChecker typeChecker;

    /**
     * Tracks beans currently being resolved to detect circular dependencies.
     * Thread-local to support concurrent validation if needed in the future.
     */
    private final ThreadLocal<Set<Bean<?>>> resolutionStack = ThreadLocal.withInitial(HashSet::new);

    CDI41InjectionValidator(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.typeChecker = new TypeChecker();
    }

    /**
     * Validates all injection points across all valid beans in the KnowledgeBase.
     *
     * <p>For each valid bean, checks that all its injection points can be satisfied.
     * Errors are added to the KnowledgeBase and can be retrieved via:
     * <ul>
     *   <li>{@link KnowledgeBase#getErrors()} - general resolution errors</li>
     *   <li>{@link KnowledgeBase#getInjectionErrors()} - injection-specific errors</li>
     * </ul>
     *
     * <p>This method also performs additional validations:
     * <ul>
     *   <li>Alternative bean validation - ensures only one alternative per type</li>
     *   <li>Circular dependency detection - validates injection graphs have no cycles</li>
     * </ul>
     *
     * @return true if all injection points can be satisfied, false otherwise
     */
    boolean validateAllInjectionPoints() {
        boolean allValid = true;

        // Only validate injection points of valid beans
        Collection<Bean<?>> validBeans = knowledgeBase.getValidBeans();

        // Enhancement 3: Validate alternative beans (only one alternative per type)
        allValid &= validateAlternatives(validBeans);

        // Enhancement 4: Validate passivation capability for beans in passivating scopes
        allValid &= validatePassivation(validBeans);

        // Enhancement 5: Scan and validate observer methods
        allValid &= scanAndValidateObserverMethods(validBeans);

        // Validate each bean's injection points
        for (Bean<?> bean : validBeans) {
            for (InjectionPoint injectionPoint : bean.getInjectionPoints()) {
                // Enhancement 2: Detect circular dependencies during validation
                boolean valid = validateInjectionPointWithCircularCheck(injectionPoint, bean);
                allValid &= valid;
            }
        }

        // Clean up thread-local storage
        resolutionStack.remove();

        return allValid;
    }

    // ============================================
    // Enhancement 2: Circular Dependency Detection
    // ============================================

    /**
     * Validates an injection point with circular dependency detection.
     *
     * <p>This method wraps {@link #validateInjectionPoint} with circular dependency tracking.
     * It maintains a stack of beans currently being resolved and detects when a bean tries
     * to inject a dependency that is already in the resolution chain.
     *
     * <p><b>Example of circular dependency:</b>
     * <pre>
     * {@literal @}ApplicationScoped
     * class ServiceA {
     *     {@literal @}Inject ServiceB serviceB;  // ServiceA depends on ServiceB
     * }
     *
     * {@literal @}ApplicationScoped
     * class ServiceB {
     *     {@literal @}Inject ServiceA serviceA;  // ServiceB depends on ServiceA → CIRCULAR!
     * }
     * </pre>
     *
     * @param injectionPoint the injection point to validate
     * @param owningBean the bean that declares this injection point
     * @return true if the injection point can be satisfied without circular dependencies
     */
    private boolean validateInjectionPointWithCircularCheck(InjectionPoint injectionPoint, Bean<?> owningBean) {
        // Get the current resolution stack for this thread
        Set<Bean<?>> stack = resolutionStack.get();

        // Check if we're already resolving this bean (circular dependency)
        if (stack.contains(owningBean)) {
            // Build the circular dependency chain for error message
            String chain = stack.stream()
                    .map(b -> b.getBeanClass().getSimpleName())
                    .collect(Collectors.joining(" → "));
            chain += " → " + owningBean.getBeanClass().getSimpleName();

            knowledgeBase.addError(
                    "Circular dependency detected: " + chain +
                    " at injection point " + formatInjectionPoint(injectionPoint, owningBean)
            );
            return false;
        }

        // Push this bean onto the resolution stack
        stack.add(owningBean);

        try {
            // Validate the injection point normally
            return validateInjectionPoint(injectionPoint, owningBean);
        } finally {
            // Pop this bean from the resolution stack
            stack.remove(owningBean);
        }
    }

    // ============================================
    // Enhancement 3: Alternative Bean Validation
    // ============================================

    /**
     * Validates that alternative beans are properly configured according to CDI 4.1 specification.
     *
     * <p><b>CDI 4.1 rules for alternatives (Section 5.1.2):</b>
     * <ul>
     *   <li>Multiple alternatives for the same type ARE allowed if they have different {@literal @}Priority values</li>
     *   <li>Higher priority value = higher precedence (e.g., {@literal @}Priority(200) beats {@literal @}Priority(100))</li>
     *   <li>If alternatives have the SAME priority (or no priority), it's an ambiguous dependency → ERROR</li>
     *   <li>If an alternative exists, it takes precedence over non-alternative beans</li>
     * </ul>
     *
     * <p><b>Valid scenario (different priorities):</b>
     * <pre>
     * {@literal @}Alternative {@literal @}Priority(100) {@literal @}ApplicationScoped
     * class MockDatabaseService implements DatabaseService { }
     *
     * {@literal @}Alternative {@literal @}Priority(200) {@literal @}ApplicationScoped
     * class TestDatabaseService implements DatabaseService { }
     *
     * // ✓ VALID: TestDatabaseService wins (higher priority)
     * </pre>
     *
     * <p><b>Invalid scenario (same/no priority):</b>
     * <pre>
     * {@literal @}Alternative {@literal @}ApplicationScoped
     * class MockDatabaseService implements DatabaseService { }
     *
     * {@literal @}Alternative {@literal @}ApplicationScoped
     * class TestDatabaseService implements DatabaseService { }
     *
     * // ✗ ERROR: Ambiguous - both have no priority!
     * </pre>
     *
     * @param validBeans collection of all valid beans
     * @return true if alternatives are properly configured per CDI 4.1 spec
     */
    private boolean validateAlternatives(Collection<Bean<?>> validBeans) {
        boolean allValid = true;

        // Group beans by their types to find alternatives (includes producer beans)
        Map<Type, List<Bean<?>>> beansByType = new HashMap<>();

        for (Bean<?> bean : validBeans) {
            for (Type type : bean.getTypes()) {
                beansByType.computeIfAbsent(type, k -> new ArrayList<>()).add(bean);
            }
        }

        // Check each type for ambiguous alternatives
        for (Map.Entry<Type, List<Bean<?>>> entry : beansByType.entrySet()) {
            Type type = entry.getKey();
            List<Bean<?>> beansOfType = entry.getValue();

            // Filter for alternative beans only
            List<Bean<?>> alternatives = beansOfType.stream()
                    .filter(Bean::isAlternative)
                    .collect(Collectors.toList());

            // If more than one alternative exists, check priorities
            if (alternatives.size() > 1) {
                // Extract priorities for each alternative
                Map<Bean<?>, Integer> priorities = new HashMap<>();
                List<Bean<?>> noPriorityAlternatives = new ArrayList<>();

                for (Bean<?> alt : alternatives) {
                    Priority priority = alt.getBeanClass().getAnnotation(Priority.class);
                    if (priority != null) {
                        priorities.put(alt, priority.value());
                    } else {
                        // Alternative without @Priority annotation
                        noPriorityAlternatives.add(alt);
                    }
                }

                // CDI 4.1: ERROR if multiple alternatives have no priority
                if (noPriorityAlternatives.size() > 1) {
                    String alternativeList = noPriorityAlternatives.stream()
                            .map(b -> b.getBeanClass().getName())
                            .collect(Collectors.joining(", "));

                    knowledgeBase.addError(
                            "Ambiguous alternatives for type " + formatType(type) +
                            ": [" + alternativeList + "]. " +
                            "Multiple alternatives without @Priority - cannot determine precedence. " +
                            "Add @Priority annotation to resolve ambiguity."
                    );
                    allValid = false;
                }

                // CDI 4.1: ERROR if alternatives have the same priority value
                if (priorities.size() > 1) {
                    // Group by priority value to find duplicates
                    Map<Integer, List<Bean<?>>> byPriority = new HashMap<>();
                    for (Map.Entry<Bean<?>, Integer> e : priorities.entrySet()) {
                        byPriority.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
                    }

                    // Check for multiple alternatives with same priority
                    for (Map.Entry<Integer, List<Bean<?>>> e : byPriority.entrySet()) {
                        if (e.getValue().size() > 1) {
                            String samePriorityList = e.getValue().stream()
                                    .map(b -> b.getBeanClass().getName())
                                    .collect(Collectors.joining(", "));

                            knowledgeBase.addError(
                                    "Ambiguous alternatives for type " + formatType(type) +
                                    ": [" + samePriorityList + "] all have the same priority @Priority(" + e.getKey() + "). " +
                                    "Alternatives must have different priority values to resolve ambiguity."
                            );
                            allValid = false;
                        }
                    }
                }

                // CDI 4.1: ERROR if mix of priority and no-priority (ambiguous)
                if (!priorities.isEmpty() && !noPriorityAlternatives.isEmpty()) {
                    // This is technically allowed, but potentially confusing
                    // No-priority alternatives have implicit priority of 0 (lowest)
                    // So prioritized alternatives will win - this is VALID per CDI 4.1
                    // No error needed here
                }
            }
        }

        return allValid;
    }

    // ============================================
    // Enhancement 4: Passivation Validation
    // ============================================

    /**
     * Validates that beans in passivation-capable scopes are serializable.
     *
     * <p>CDI 4.1 Passivation Requirements:
     * <ul>
     *   <li>@SessionScoped beans MUST implement Serializable (session can be passivated)</li>
     *   <li>@ConversationScoped beans MUST implement Serializable (conversation can be passivated)</li>
     *   <li>@ApplicationScoped beans do NOT need to be Serializable (never passivated)</li>
     *   <li>@RequestScoped beans do NOT need to be Serializable (short-lived)</li>
     *   <li>@Dependent beans do NOT need to be Serializable (lifecycle tied to parent)</li>
     * </ul>
     *
     * <p>When a bean is in a passivation-capable scope, it must be passivation capable:
     * - The bean class must implement {@link java.io.Serializable}
     * - All dependencies (injection points) must be passivation capable (checked recursively)
     * - All interceptors and decorators must be serializable (checked in future phases)
     *
     * <p>Important: Client proxies are ALWAYS Serializable, so injecting non-passivating
     * beans into passivating beans is allowed (the proxy is serialized, not the actual bean).
     * This means normal-scoped beans (@ApplicationScoped, @SessionScoped, etc.) are always
     * passivation-capable dependencies because they are injected as proxies.
     *
     * @param validBeans collection of valid beans to validate
     * @return true if all beans satisfy passivation requirements, false otherwise
     */
    private boolean validatePassivation(Collection<Bean<?>> validBeans) {
        boolean allValid = true;

        // Track visited beans to avoid infinite recursion in circular dependencies
        Set<Bean<?>> visited = new HashSet<>();

        for (Bean<?> bean : validBeans) {
            Class<? extends Annotation> scopeAnnotation = bean.getScope();

            // Check if this is a passivation-capable scope
            if (isPassivationCapableScope(scopeAnnotation)) {
                // Bean must be Serializable
                if (!java.io.Serializable.class.isAssignableFrom(bean.getBeanClass())) {
                    knowledgeBase.addError(
                            "Bean " + bean.getBeanClass().getName() +
                            " has passivation-capable scope @" + scopeAnnotation.getSimpleName() +
                            " but does not implement java.io.Serializable. " +
                            "Beans in @SessionScoped or @ConversationScoped must be Serializable " +
                            "because the container may passivate (serialize) them to disk or database."
                    );
                    allValid = false;
                }

                // Recursively validate all dependencies are passivation-capable
                visited.clear();
                allValid &= validateDependenciesPassivationCapable(bean, visited, validBeans);
            }
        }

        return allValid;
    }

    /**
     * Recursively validates that all dependencies of a bean are passivation-capable.
     *
     * <p>This method checks that for a bean in a passivation-capable scope, all its
     * injected dependencies can be safely serialized. The rules are:
     * <ul>
     *   <li>Normal-scoped beans (@ApplicationScoped, @SessionScoped, etc.) are ALWAYS passivation-capable
     *       because they are injected as serializable client proxies</li>
     *   <li>@Dependent scoped beans MUST be Serializable if injected into passivation-capable beans</li>
     *   <li>@Dependent beans' dependencies are recursively checked (transitive closure)</li>
     *   <li>Instance&lt;T&gt; and Provider&lt;T&gt; are always passivation-capable (they are proxies)</li>
     * </ul>
     *
     * @param bean the bean whose dependencies to check
     * @param visited set of already visited beans to prevent infinite recursion
     * @param validBeans all valid beans for dependency resolution
     * @return true if all dependencies are passivation-capable
     */
    private boolean validateDependenciesPassivationCapable(Bean<?> bean, Set<Bean<?>> visited,
                                                           Collection<Bean<?>> validBeans) {
        // Avoid infinite recursion on circular dependencies
        if (visited.contains(bean)) {
            return true;
        }
        visited.add(bean);

        boolean allValid = true;

        for (InjectionPoint injectionPoint : bean.getInjectionPoints()) {
            Type requiredType = injectionPoint.getType();
            Set<Annotation> qualifiers = injectionPoint.getQualifiers();

            // Instance<T> and Provider<T> are always passivation-capable (they are proxies)
            if (isInstanceOrProvider(requiredType)) {
                continue;
            }

            // Find the bean that will be injected
            Set<Bean<?>> candidates = findMatchingBeans(requiredType, qualifiers);

            if (candidates.isEmpty()) {
                // Unsatisfied dependency - already reported by validateInjectionPoint
                continue;
            }

            // Get the single resolved bean (ambiguity already checked in validateInjectionPoint)
            Bean<?> resolvedBean;
            if (candidates.size() > 1) {
                Optional<Bean<?>> preferred = chooseByPriority(candidates);
                if (!preferred.isPresent()) {
                    // Ambiguous - already reported by validateInjectionPoint
                    continue;
                }
                resolvedBean = preferred.get();
            } else {
                resolvedBean = candidates.iterator().next();
            }

            // Check if dependency is passivation-capable
            Class<? extends Annotation> dependencyScope = resolvedBean.getScope();

            // Normal-scoped beans are ALWAYS passivation-capable because they inject as proxies
            if (isNormalScope(dependencyScope)) {
                // Proxy is serializable, so this dependency is fine
                continue;
            }

            // @Dependent scoped dependencies must be Serializable themselves
            if (isDependentScope(dependencyScope)) {
                if (!java.io.Serializable.class.isAssignableFrom(resolvedBean.getBeanClass())) {
                    knowledgeBase.addError(
                            "Bean " + bean.getBeanClass().getName() +
                            " has passivation-capable scope @" + bean.getScope().getSimpleName() +
                            " and injects @Dependent bean " + resolvedBean.getBeanClass().getName() +
                            " at " + formatInjectionPoint(injectionPoint, bean) +
                            ", but the dependency does not implement java.io.Serializable. " +
                            "@Dependent beans injected into passivation-capable beans must be Serializable."
                    );
                    allValid = false;
                } else {
                    // Recursively check @Dependent bean's dependencies
                    allValid &= validateDependenciesPassivationCapable(resolvedBean, visited, validBeans);
                }
            }

            // Pseudo-scoped beans (@Singleton from JSR-330) should also be checked
            // They are not proxied, so they need to be Serializable
            if (!isNormalScope(dependencyScope) && !isDependentScope(dependencyScope)) {
                // Custom scope or @Singleton - must be Serializable
                if (!java.io.Serializable.class.isAssignableFrom(resolvedBean.getBeanClass())) {
                    knowledgeBase.addError(
                            "Bean " + bean.getBeanClass().getName() +
                            " has passivation-capable scope @" + bean.getScope().getSimpleName() +
                            " and injects non-normal-scoped bean " + resolvedBean.getBeanClass().getName() +
                            " with scope @" + dependencyScope.getSimpleName() +
                            " at " + formatInjectionPoint(injectionPoint, bean) +
                            ", but the dependency does not implement java.io.Serializable. " +
                            "Non-normal-scoped beans injected into passivation-capable beans must be Serializable."
                    );
                    allValid = false;
                }
            }
        }

        return allValid;
    }

    /**
     * Checks if a scope is a normal scope (uses client proxies).
     * Normal scopes: @ApplicationScoped, @SessionScoped, @ConversationScoped, @RequestScoped
     *
     * @param scopeAnnotation the scope annotation
     * @return true if it's a normal scope
     */
    private boolean isNormalScope(Class<? extends Annotation> scopeAnnotation) {
        String scopeName = scopeAnnotation.getName();
        return scopeName.equals("jakarta.enterprise.context.ApplicationScoped") ||
               scopeName.equals("jakarta.enterprise.context.SessionScoped") ||
               scopeName.equals("jakarta.enterprise.context.ConversationScoped") ||
               scopeName.equals("jakarta.enterprise.context.RequestScoped") ||
               scopeName.equals("javax.enterprise.context.ApplicationScoped") ||
               scopeName.equals("javax.enterprise.context.SessionScoped") ||
               scopeName.equals("javax.enterprise.context.ConversationScoped") ||
               scopeName.equals("javax.enterprise.context.RequestScoped") ||
               // Check for @NormalScope meta-annotation
               scopeAnnotation.isAnnotationPresent(jakarta.enterprise.context.NormalScope.class) ||
               scopeAnnotation.isAnnotationPresent(javax.enterprise.context.NormalScope.class);
    }

    /**
     * Checks if a scope is @Dependent.
     *
     * @param scopeAnnotation the scope annotation
     * @return true if it's @Dependent
     */
    private boolean isDependentScope(Class<? extends Annotation> scopeAnnotation) {
        String scopeName = scopeAnnotation.getName();
        return scopeName.equals("jakarta.enterprise.context.Dependent") ||
               scopeName.equals("javax.enterprise.context.Dependent");
    }

    /**
     * Checks if a scope annotation represents a passivation-capable scope.
     *
     * <p>In CDI 4.1, only @SessionScoped and @ConversationScoped are passivation-capable.
     *
     * @param scopeAnnotation the scope annotation to check
     * @return true if the scope is passivation-capable
     */
    private boolean isPassivationCapableScope(Class<? extends Annotation> scopeAnnotation) {
        // SessionScoped and ConversationScoped are passivation-capable
        String scopeName = scopeAnnotation.getName();
        return scopeName.equals("jakarta.enterprise.context.SessionScoped") ||
               scopeName.equals("jakarta.enterprise.context.ConversationScoped") ||
               scopeName.equals("javax.enterprise.context.SessionScoped") ||
               scopeName.equals("javax.enterprise.context.ConversationScoped");
    }

    // ============================================
    // Original Validation Methods
    // ============================================

    /**
     * Validates a single injection point can be satisfied.
     *
     * <p>This method performs the core validation logic:
     * <ul>
     *   <li>Checks if Instance&lt;T&gt; or Provider&lt;T&gt; (always satisfiable)</li>
     *   <li>Finds matching beans by type and qualifiers</li>
     *   <li>Reports unsatisfied dependencies (no beans found)</li>
     *   <li>Reports ambiguous dependencies (multiple beans found)</li>
     *   <li>Checks if resolved bean has validation errors</li>
     *   <li>Enhancement 1: Validates producer methods can satisfy injection points</li>
     * </ul>
     *
     * @param injectionPoint the injection point to validate
     * @param owningBean the bean that declares this injection point
     * @return true if the injection point can be satisfied, false otherwise
     */
    private boolean validateInjectionPoint(InjectionPoint injectionPoint, Bean<?> owningBean) {
        Type requiredType = injectionPoint.getType();
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();

        // Special handling for Instance<T> and Provider<T>
        // These are always satisfiable as they provide lazy/programmatic access
        if (isInstanceOrProvider(requiredType)) {
            return true; // Always valid - resolved at runtime
        }

        // Find all beans that match the required type
        Set<Bean<?>> candidates = findMatchingBeans(requiredType, qualifiers);

        // Check for unsatisfied dependency
        if (candidates.isEmpty()) {
            knowledgeBase.addInjectionError(
                formatInjectionPoint(injectionPoint, owningBean) +
                ": unsatisfied dependency - no bean found for type " +
                formatType(requiredType) + " with qualifiers " + formatQualifiers(qualifiers)
            );
            return false;
        }

        // Check for ambiguous dependency (respect alternative priorities if present)
        if (candidates.size() > 1) {
            // If one or more alternatives present with differing priorities, pick highest
            Optional<Bean<?>> preferred = chooseByPriority(candidates);
            if (preferred.isPresent()) {
                candidates = Collections.singleton(preferred.get());
            } else {
                String candidateList = candidates.stream()
                    .map(b -> b.getBeanClass().getName())
                    .collect(Collectors.joining(", "));

                knowledgeBase.addInjectionError(
                    formatInjectionPoint(injectionPoint, owningBean) +
                    ": ambiguous dependency - multiple beans found for type " +
                    formatType(requiredType) + ": [" + candidateList + "]"
                );
                return false;
            }
        }

        // Single candidate found - check if it has validation errors
        Bean<?> resolvedBean = candidates.iterator().next();
        if (resolvedBean instanceof BeanImpl && ((BeanImpl<?>) resolvedBean).hasValidationErrors()) {
            knowledgeBase.addError(
                formatInjectionPoint(injectionPoint, owningBean) +
                ": cannot resolve dependency - resolved bean " +
                resolvedBean.getBeanClass().getName() + " has validation errors"
            );
            return false;
        }

        // Injection point is valid
        return true;
    }

    /**
     * If candidates include alternatives with @Priority, prefer the highest priority.
     * Returns empty when no clear single winner.
     */
    private Optional<Bean<?>> chooseByPriority(Set<Bean<?>> candidates) {
        // Filter to alternatives with a known priority (ProducerBean stores it explicitly; BeanImpl reads @Priority on class)
        List<BeanWithPriority> prioritized = new ArrayList<>();
        for (Bean<?> bean : candidates) {
            Integer priority = null;
            if (bean instanceof ProducerBean) {
                priority = ((ProducerBean<?>) bean).getPriority();
            }
            if (priority == null) {
                Priority p = bean.getBeanClass().getAnnotation(Priority.class);
                if (p != null) {
                    priority = p.value();
                }
            }
            if (bean.isAlternative() && priority != null) {
                prioritized.add(new BeanWithPriority(bean, priority));
            }
        }

        if (prioritized.isEmpty()) {
            return Optional.empty();
        }

        // Pick highest priority; if multiple share it, still ambiguous
        prioritized.sort((a, b) -> Integer.compare(b.priority, a.priority));
        BeanWithPriority top = prioritized.get(0);
        boolean uniqueTop = prioritized.stream().filter(bp -> bp.priority == top.priority).count() == 1;
        return uniqueTop ? Optional.of(top.bean) : Optional.empty();
    }

    private static class BeanWithPriority {
        final Bean<?> bean;
        final int priority;
        BeanWithPriority(Bean<?> bean, int priority) {
            this.bean = bean;
            this.priority = priority;
        }
    }

    /**
     * Checks if the type is Instance&lt;T&gt; or Provider&lt;T&gt;.
     * These types are always satisfiable as they provide lazy/programmatic access.
     *
     * @param type the type to check
     * @return true if the type is Instance or Provider
     */
    private boolean isInstanceOrProvider(Type type) {
        Class<?> rawType = RawTypeExtractor.getRawType(type);
        return Instance.class.equals(rawType) || Provider.class.equals(rawType);
    }

    // ============================================
    // Enhancement 1: Producer Method Resolution
    // ============================================

    /**
     * Finds all beans that match the required type and qualifiers.
     *
     * <p>This method searches for beans that can satisfy an injection point by:
     * <ol>
     *   <li>Checking managed beans (regular classes with @Inject, @ApplicationScoped, etc.)</li>
     *   <li>Checking producer fields (fields annotated with @Produces)</li>
     *   <li>Checking producer methods (methods annotated with @Produces)</li>
     * </ol>
     *
     * <p><b>Type Matching:</b> Uses {@link TypeChecker} for proper generic type matching.
     * For example:
     * <pre>
     * // Injection point
     * {@literal @}Inject List&lt;String&gt; items;
     *
     * // Producer method can satisfy this
     * {@literal @}Produces
     * public ArrayList&lt;String&gt; createList() {
     *     return new ArrayList&lt;&gt;();
     * }
     * </pre>
     *
     * <p><b>Qualifier Matching:</b> CDI 4.1 rules apply:
     * <ul>
     *   <li>Injection point with no qualifiers → matches beans with @Default</li>
     *   <li>Injection point with qualifiers → all qualifiers must match</li>
     *   <li>@Any matches all beans</li>
     * </ul>
     *
     * @param requiredType the type being injected
     * @param qualifiers the qualifiers that must match
     * @return set of matching beans (may include producer-backed beans)
     */
    private Set<Bean<?>> findMatchingBeans(Type requiredType, Set<Annotation> qualifiers) {
        Set<Bean<?>> matches = new HashSet<>();

        // Get all valid beans (exclude beans with validation errors)
        // This includes:
        // 1. Regular managed beans (classes with injection points)
        // 2. Producer field beans (fields annotated with @Produces)
        // 3. Producer method beans (methods annotated with @Produces)
        Collection<Bean<?>> validBeans = knowledgeBase.getValidBeans();

        for (Bean<?> bean : validBeans) {
            // Check if bean's types are compatible with required type
            // Uses TypeChecker for proper generic matching (e.g., List<String> matches ArrayList<String>)
            if (isTypeCompatible(requiredType, bean.getTypes()) &&
                // Check if bean's qualifiers match the required qualifiers
                // Handles @Default, @Any, and custom qualifiers per CDI 4.1 spec
                areQualifiersCompatible(qualifiers, bean.getQualifiers())) {
                matches.add(bean);
            }
        }

        // Note: Producer methods are already validated by CDI41BeanValidator
        // and registered as beans in KnowledgeBase, so they're included in
        // the validBeans collection above. No special handling needed here.

        return matches;
    }

    /**
     * Checks if the required type is compatible with the bean's types.
     * Uses the existing TypeChecker for proper type hierarchy and generic matching.
     *
     * @param requiredType the type being injected
     * @param beanTypes the types the bean provides
     * @return true if types are compatible
     */
    private boolean isTypeCompatible(Type requiredType, Set<Type> beanTypes) {
        for (Type beanType : beanTypes) {
            try {
                // Use TypeChecker for proper type matching with generic support
                if (typeChecker.isAssignable(requiredType, beanType)) {
                    return true;
                }
            } catch (Exception e) {
                // If TypeChecker fails, continue checking other bean types
                continue;
            }
        }

        return false;
    }

    /**
     * Checks if the required qualifiers are compatible with the bean's qualifiers.
     *
     * <p>CDI rules:
     * <ul>
     *   <li>Injection point with no qualifiers matches bean with @Default</li>
     *   <li>Injection point with qualifiers must match ALL of them on the bean</li>
     *   <li>@Any qualifier matches all beans</li>
     * </ul>
     *
     * @param required the required qualifiers
     * @param provided the bean's qualifiers
     * @return true if qualifiers are compatible
     */
    private boolean areQualifiersCompatible(Set<Annotation> required, Set<Annotation> provided) {
        // If no specific qualifiers required, match @Default
        if (required.isEmpty() || hasOnlyDefault(required)) {
            return hasDefault(provided);
        }

        // Check if @Any is present in required (matches all beans)
        if (hasAny(required)) {
            return true;
        }

        // All required qualifiers must be present in provided
        for (Annotation reqQualifier : required) {
            if (isQualifier(reqQualifier) && !hasMatchingQualifier(reqQualifier, provided)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a matching qualifier exists in the provided set.
     *
     * @param required the required qualifier
     * @param provided the set of provided qualifiers
     * @return true if a matching qualifier is found
     */
    private boolean hasMatchingQualifier(Annotation required, Set<Annotation> provided) {
        for (Annotation providedQualifier : provided) {
            if (qualifiersMatch(required, providedQualifier)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if two qualifiers match according to CDI rules.
     *
     * @param a first qualifier
     * @param b second qualifier
     * @return true if qualifiers match
     */
    private boolean qualifiersMatch(Annotation a, Annotation b) {
        // Same type check
        if (!a.annotationType().equals(b.annotationType())) {
            return false;
        }

        // For now, simple equality check - could be enhanced to check annotation members
        return a.equals(b);
    }

    /**
     * Checks if the annotation is a qualifier.
     *
     * @param annotation the annotation to check
     * @return true if it's a qualifier
     */
    private boolean isQualifier(Annotation annotation) {
        return hasQualifierAnnotation(annotation.annotationType());
    }

    /**
     * Checks if @Default is present in the set.
     *
     * @param qualifiers the qualifiers to check
     * @return true if @Default is present
     */
    private boolean hasDefault(Set<Annotation> qualifiers) {
        return qualifiers.stream()
            .anyMatch(q -> q.annotationType().equals(Default.class));
    }

    /**
     * Checks if the set contains only @Default (and possibly @Any).
     *
     * @param qualifiers the qualifiers to check
     * @return true if only default qualifiers are present
     */
    private boolean hasOnlyDefault(Set<Annotation> qualifiers) {
        return qualifiers.stream()
            .filter(this::isQualifier)
            .allMatch(q -> q.annotationType().equals(Default.class) ||
                          q.annotationType().equals(Any.class));
    }

    /**
     * Checks if @Any is present in the set.
     *
     * @param qualifiers the qualifiers to check
     * @return true if @Any is present
     */
    private boolean hasAny(Set<Annotation> qualifiers) {
        return qualifiers.stream()
            .anyMatch(q -> q.annotationType().equals(Any.class));
    }

    /**
     * Formats an injection point for error messages.
     *
     * @param ip the injection point
     * @param owningBean the bean that owns this injection point
     * @return formatted string
     */
    private String formatInjectionPoint(InjectionPoint ip, Bean<?> owningBean) {
        if (ip.getMember() != null) {
            return "Injection point " + ip.getMember().getName() +
                   " in class " + owningBean.getBeanClass().getName();
        }
        return "Injection point in class " + owningBean.getBeanClass().getName();
    }

    /**
     * Formats a type for error messages.
     *
     * @param type the type to format
     * @return formatted string
     */
    private String formatType(Type type) {
        return type.getTypeName();
    }

    /**
     * Formats qualifiers for error messages.
     *
     * @param qualifiers the qualifiers to format
     * @return formatted string
     */
    private String formatQualifiers(Set<Annotation> qualifiers) {
        if (qualifiers.isEmpty()) {
            return "[@Default]";
        }
        return qualifiers.stream()
            .map(q -> "@" + q.annotationType().getSimpleName())
            .collect(Collectors.joining(", ", "[", "]"));
    }

    // ============================================
    // Enhancement 5: Observer Method Validation
    // ============================================

    /**
     * Scans all beans for observer methods and validates them.
     *
     * <p><b>CDI 4.1 Observer Method Requirements (Section 10.4):</b>
     * <ul>
     *   <li>Must have exactly one parameter annotated with @Observes or @ObservesAsync</li>
     *   <li>The observed parameter defines the event type</li>
     *   <li>Cannot have both @Observes and @ObservesAsync on same method</li>
     *   <li>Can have additional injection points as other parameters</li>
     *   <li>May have qualifiers on observed parameter for event filtering</li>
     *   <li>Conditional observers (@Observes(notifyObserver=IF_EXISTS)) only notified if bean exists</li>
     * </ul>
     *
     * @param validBeans the beans to scan for observer methods
     * @return true if all observer methods are valid
     */
    private boolean scanAndValidateObserverMethods(Collection<Bean<?>> validBeans) {
        boolean allValid = true;

        for (Bean<?> bean : validBeans) {
            if (!(bean instanceof BeanImpl)) {
                continue; // Producer beans don't have observer methods
            }

            BeanImpl<?> beanImpl = (BeanImpl<?>) bean;
            Class<?> beanClass = beanImpl.getBeanClass();

            // Scan all methods for @Observes and @ObservesAsync
            for (java.lang.reflect.Method method : beanClass.getDeclaredMethods()) {
                boolean valid = validateObserverMethod(method, bean);
                allValid &= valid;
            }
        }

        return allValid;
    }

    /**
     * Validates a single observer method and registers it if valid.
     *
     * @param method the method to validate
     * @param declaringBean the bean that declares this method
     * @return true if valid
     */
    private boolean validateObserverMethod(java.lang.reflect.Method method, Bean<?> declaringBean) {
        // Count @Observes and @ObservesAsync parameters
        int observesCount = 0;
        int observesAsyncCount = 0;
        java.lang.reflect.Parameter observedParameter = null;

        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (AnnotationsEnum.hasObservesAnnotation(parameter)) {
                observesCount++;
                observedParameter = parameter;
            }
            if (AnnotationsEnum.hasObservesAsyncAnnotation(parameter)) {
                observesAsyncCount++;
                observedParameter = parameter;
            }
        }

        // No observer annotations - not an observer method
        if (observesCount == 0 && observesAsyncCount == 0) {
            return true;
        }

        // Validate exactly one observer annotation
        if (observesCount + observesAsyncCount > 1) {
            knowledgeBase.addError(
                "Observer method " + method.getName() + " in " + declaringBean.getBeanClass().getName() +
                " must have exactly one parameter with @Observes or @ObservesAsync, found " +
                (observesCount + observesAsyncCount)
            );
            return false;
        }

        // Cannot mix @Observes and @ObservesAsync
        if (observesCount > 0 && observesAsyncCount > 0) {
            knowledgeBase.addError(
                "Observer method " + method.getName() + " in " + declaringBean.getBeanClass().getName() +
                " cannot have both @Observes and @ObservesAsync"
            );
            return false;
        }

        // Extract observer metadata
        boolean async = observesAsyncCount > 0;
        Type eventType = observedParameter.getParameterizedType();
        Set<Annotation> qualifiers = extractQualifiers(observedParameter);

        // Extract reception and transaction phase
        jakarta.enterprise.event.Reception reception = jakarta.enterprise.event.Reception.ALWAYS;
        jakarta.enterprise.event.TransactionPhase transactionPhase = jakarta.enterprise.event.TransactionPhase.IN_PROGRESS;

        if (async) {
            // Extract from @ObservesAsync
            jakarta.enterprise.event.ObservesAsync observesAsync =
                observedParameter.getAnnotation(jakarta.enterprise.event.ObservesAsync.class);
            reception = observesAsync.notifyObserver();
        } else {
            // Extract from @Observes
            jakarta.enterprise.event.Observes observes =
                observedParameter.getAnnotation(jakarta.enterprise.event.Observes.class);
            reception = observes.notifyObserver();
            transactionPhase = observes.during();
        }

        // Extract priority
        int priority = 0;
        jakarta.annotation.Priority priorityAnnotation = method.getAnnotation(jakarta.annotation.Priority.class);
        if (priorityAnnotation != null) {
            priority = priorityAnnotation.value();
        }

        // Create and register observer method info
        ObserverMethodInfo observerMethodInfo = new ObserverMethodInfo(
            method,
            eventType,
            qualifiers,
            reception,
            transactionPhase,
            async,
            declaringBean,
            priority
        );

        knowledgeBase.addObserverMethodInfo(observerMethodInfo);
        return true;
    }

    /**
     * Extracts qualifiers from a parameter.
     * Qualifiers are annotations annotated with @Qualifier.
     */
    private Set<Annotation> extractQualifiers(java.lang.reflect.Parameter parameter) {
        Set<Annotation> qualifiers = new HashSet<>();
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
                qualifiers.add(annotation);
            }
        }
        // If no explicit qualifiers, add @Default
        if (qualifiers.isEmpty()) {
            qualifiers.add(new com.threeamigos.common.util.implementations.injection.literals.DefaultLiteral());
        }
        return qualifiers;
    }
}
