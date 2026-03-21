package com.threeamigos.common.util.implementations.injection.discovery;

import com.threeamigos.common.util.implementations.injection.*;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.events.ObserverMethodInfo;
import com.threeamigos.common.util.implementations.injection.resolution.BeanImpl;
import com.threeamigos.common.util.implementations.injection.resolution.ProducerBean;
import com.threeamigos.common.util.implementations.injection.resolution.TypeChecker;
import com.threeamigos.common.util.implementations.injection.util.AnnotationComparator;
import com.threeamigos.common.util.implementations.injection.util.AnyLiteral;
import com.threeamigos.common.util.implementations.injection.util.GenericTypeResolver;
import com.threeamigos.common.util.implementations.injection.util.RawTypeExtractor;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
public class CDI41InjectionValidator {

    private final KnowledgeBase knowledgeBase;
    private final TypeChecker typeChecker;

    /**
     * Tracks beans currently being resolved to detect circular dependencies.
     * Thread-local to support concurrent validation if needed in the future.
     */
    private final ThreadLocal<Deque<Bean<?>>> resolutionStack = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * Deduplicates circular dependency errors in the current validation thread.
     */
    private final ThreadLocal<Set<String>> reportedCircularDependencies =
            ThreadLocal.withInitial(HashSet::new);

    public CDI41InjectionValidator(KnowledgeBase knowledgeBase) {
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
    public boolean validateAllInjectionPoints() {
        boolean allValid = true;

        // Only validate injection points of valid beans
        Collection<Bean<?>> validBeans = knowledgeBase.getValidBeans().stream()
                .filter(this::isBeanEnabledForResolution)
                .collect(Collectors.toList());

        // Enhancement 3: Validate alternative beans (only one alternative per type)
        allValid &= validateAlternatives(validBeans);
        // CDI 4.1 §5.3.1: validate ambiguous bean names at initialization.
        allValid &= validateNameResolution(validBeans);

        // Enhancement 4: Validate passivation capability for beans in passivating scopes
        allValid &= validatePassivation(validBeans);

        // Enhancement 5: Scan and validate observer methods
        allValid &= scanAndValidateObserverMethods(validBeans);

        Set<Bean<?>> globallyVisited = Collections.newSetFromMap(new IdentityHashMap<>());

        // Validate each bean's dependency graph (including circular dependency detection)
        for (Bean<?> bean : validBeans) {
            boolean valid = validateBeanWithCircularCheck(bean, globallyVisited);
            allValid &= valid;
        }

        // Clean up thread-local storage
        resolutionStack.remove();
        reportedCircularDependencies.remove();

        return allValid;
    }

    private boolean validateNameResolution(Collection<Bean<?>> validBeans) {
        boolean allValid = true;

        Map<String, Set<Bean<?>>> beansByName = new HashMap<>();
        for (Bean<?> bean : validBeans) {
            String name = bean.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            beansByName.computeIfAbsent(name, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(bean);
        }

        for (Map.Entry<String, Set<Bean<?>>> entry : beansByName.entrySet()) {
            String beanName = entry.getKey();
            Set<Bean<?>> candidates = entry.getValue();
            if (candidates.size() <= 1) {
                continue;
            }

            Bean<?> resolved = resolveAmbiguousName(candidates);
            if (resolved == null) {
                String beans = candidates.stream()
                        .map(b -> b.getBeanClass().getName())
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));
                knowledgeBase.addDefinitionError(
                        "Ambiguous bean name '" + beanName + "': [" + beans + "]"
                );
                allValid = false;
            }
        }

        // CDI 4.1 §5.3.1: deployment problem when one name is x and another is x.y (y valid bean name).
        Set<String> names = new HashSet<>(beansByName.keySet());
        for (String name : names) {
            int dotIndex = name.indexOf('.');
            if (dotIndex <= 0 || dotIndex >= name.length() - 1) {
                continue;
            }

            String x = name.substring(0, dotIndex);
            String y = name.substring(dotIndex + 1);
            if (names.contains(x) && isValidSimpleBeanName(y)) {
                knowledgeBase.addDefinitionError(
                        "Bean name conflict detected: '" + x + "' and '" + name + "'"
                );
                allValid = false;
            }
        }

        return allValid;
    }

    private Bean<?> resolveAmbiguousName(Set<Bean<?>> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.iterator().next();
        }

        List<Bean<?>> alternatives = candidates.stream()
                .filter(Bean::isAlternative)
                .collect(Collectors.toList());

        // Eliminate non-alternatives first (CDI 4.1 §5.3.1).
        List<Bean<?>> remaining = alternatives.isEmpty()
                ? new ArrayList<>(candidates)
                : alternatives;

        if (remaining.size() == 1) {
            return remaining.get(0);
        }

        // If all remaining beans are alternatives with priority, keep only highest priority.
        boolean allHavePriority = remaining.stream().allMatch(this::hasPriorityValue);
        if (allHavePriority) {
            int highest = remaining.stream()
                    .mapToInt(this::extractPriorityValue)
                    .max()
                    .orElse(Integer.MIN_VALUE);
            List<Bean<?>> highestPriorityBeans = remaining.stream()
                    .filter(bean -> extractPriorityValue(bean) == highest)
                    .collect(Collectors.toList());
            if (highestPriorityBeans.size() == 1) {
                return highestPriorityBeans.get(0);
            }
            return null;
        }

        return null;
    }

    private boolean hasPriorityValue(Bean<?> bean) {
        return extractPriorityValue(bean) != Integer.MIN_VALUE;
    }

    private int extractPriorityValue(Bean<?> bean) {
        if (bean == null) {
            return Integer.MIN_VALUE;
        }

        if (bean instanceof ProducerBean) {
            ProducerBean<?> producerBean = (ProducerBean<?>) bean;
            Method producerMethod = producerBean.getProducerMethod();
            if (producerMethod != null) {
                Priority priority = producerMethod.getAnnotation(Priority.class);
                if (priority != null) {
                    return priority.value();
                }
            }
            Field producerField = producerBean.getProducerField();
            if (producerField != null) {
                Priority priority = producerField.getAnnotation(Priority.class);
                if (priority != null) {
                    return priority.value();
                }
            }

            Integer explicitProducerPriority = producerBean.getPriority();
            if (explicitProducerPriority != null) {
                return explicitProducerPriority;
            }

            Priority declaringPriority = producerBean.getDeclaringClass().getAnnotation(Priority.class);
            if (declaringPriority != null) {
                return declaringPriority.value();
            }
        }

        if (bean instanceof BeanImpl) {
            Integer priority = ((BeanImpl<?>) bean).getPriority();
            if (priority != null) {
                return priority;
            }
        }

        Priority classPriority = bean.getBeanClass().getAnnotation(Priority.class);
        return classPriority == null ? Integer.MIN_VALUE : classPriority.value();
    }

    private boolean isValidSimpleBeanName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // ============================================
    // Enhancement 2: Circular Dependency Detection
    // ============================================

    private boolean validateBeanWithCircularCheck(Bean<?> rootBean, Set<Bean<?>> globallyVisited) {
        Deque<Bean<?>> stack = resolutionStack.get();
        stack.clear();
        try {
            return validateBeanDependencies(rootBean, stack, globallyVisited);
        } finally {
            stack.clear();
        }
    }

    private boolean validateBeanDependencies(Bean<?> owningBean, Deque<Bean<?>> stack,
                                             Set<Bean<?>> globallyVisited) {
        if (containsByIdentity(stack, owningBean)) {
            reportCircularDependency(formatCircularDependencyChain(stack, owningBean), null, null);
            return false;
        }

        if (globallyVisited.contains(owningBean)) {
            return true;
        }

        stack.addLast(owningBean);
        globallyVisited.add(owningBean);
        boolean allValid = true;
        try {
            for (InjectionPoint injectionPoint : owningBean.getInjectionPoints()) {
                boolean valid = validateInjectionPoint(injectionPoint, owningBean);
                allValid &= valid;
                if (!valid) {
                    continue;
                }

                Optional<Bean<?>> resolvedDependency = resolveInjectionPointTargetBean(injectionPoint);
                if (!resolvedDependency.isPresent()) {
                    continue;
                }

                Bean<?> dependency = resolvedDependency.get();
                if (containsByIdentity(stack, dependency)) {
                    String chain = formatCircularDependencyChain(stack, dependency);
                    reportCircularDependency(chain, injectionPoint, owningBean);
                    allValid = false;
                    continue;
                }

                allValid &= validateBeanDependencies(dependency, stack, globallyVisited);
            }
        } finally {
            stack.removeLast();
        }

        return allValid;
    }

    private Optional<Bean<?>> resolveInjectionPointTargetBean(InjectionPoint injectionPoint) {
        Type requiredType = injectionPoint.getType();
        if (isInstanceOrProvider(requiredType)) {
            return Optional.empty();
        }

        Set<Bean<?>> candidates = findMatchingBeans(requiredType, injectionPoint.getQualifiers());
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        if (candidates.size() > 1) {
            return resolveByAlternativePrecedence(candidates);
        }

        return Optional.of(candidates.iterator().next());
    }

    private void reportCircularDependency(String chain, InjectionPoint injectionPoint, Bean<?> owningBean) {
        StringBuilder message = new StringBuilder("Circular dependency detected: ").append(chain);
        if (injectionPoint != null && owningBean != null) {
            message.append(" at ").append(formatInjectionPoint(injectionPoint, owningBean));
        }

        String rendered = message.toString();
        Set<String> reported = reportedCircularDependencies.get();
        if (reported.add(rendered)) {
            knowledgeBase.addInjectionError(rendered);
        }
    }

    private boolean containsByIdentity(Deque<Bean<?>> stack, Bean<?> target) {
        for (Bean<?> bean : stack) {
            if (bean == target) {
                return true;
            }
        }
        return false;
    }

    private int indexByIdentity(List<Bean<?>> beans, Bean<?> target) {
        for (int i = 0; i < beans.size(); i++) {
            if (beans.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private String formatCircularDependencyChain(Deque<Bean<?>> stack, Bean<?> repeatedBean) {
        List<Bean<?>> path = new ArrayList<>(stack);
        int start = indexByIdentity(path, repeatedBean);
        if (start < 0) {
            String name = beanDisplayName(repeatedBean);
            return name + " -> " + name;
        }

        List<Bean<?>> cycle = new ArrayList<>(path.subList(start, path.size()));
        if (cycle.isEmpty()) {
            String name = beanDisplayName(repeatedBean);
            return name + " -> " + name;
        }

        int canonicalStart = 0;
        String canonicalName = beanDisplayName(cycle.get(0));
        for (int i = 1; i < cycle.size(); i++) {
            String candidate = beanDisplayName(cycle.get(i));
            if (candidate.compareTo(canonicalName) < 0) {
                canonicalStart = i;
                canonicalName = candidate;
            }
        }

        List<String> names = new ArrayList<>(cycle.size() + 1);
        for (int i = 0; i < cycle.size(); i++) {
            names.add(beanDisplayName(cycle.get((canonicalStart + i) % cycle.size())));
        }
        names.add(names.get(0));
        return String.join(" -> ", names);
    }

    private String beanDisplayName(Bean<?> bean) {
        String simpleName = bean.getBeanClass().getSimpleName();
        if (simpleName != null && !simpleName.isEmpty()) {
            return simpleName;
        }
        return bean.getBeanClass().getName();
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

        // Group by type AND effective qualifiers to detect ambiguity only within the same qualifier set.
        Map<Type, Map<Set<Annotation>, Set<Bean<?>>>> byTypeAndQuals = new HashMap<>();

        for (Bean<?> bean : validBeans) {
            Set<Annotation> qKey = qualifierKey(bean.getQualifiers());
            for (Type type : bean.getTypes()) {
                if (isJavaLangObject(type)) {
                    // Skip java.lang.Object to avoid spurious ambiguity: every bean has Object
                    // as a type, but injection points of Object are exceedingly rare and would
                    // be validated during normal resolution if present.
                    continue;
                }
                byTypeAndQuals
                        .computeIfAbsent(type, t -> new HashMap<>())
                        .computeIfAbsent(qKey, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
                        .add(bean);
            }
        }

        // Check each (type, qualifier set) bucket for ambiguous alternatives
        for (Map.Entry<Type, Map<Set<Annotation>, Set<Bean<?>>>> typeEntry : byTypeAndQuals.entrySet()) {
            Type type = typeEntry.getKey();
            for (Map.Entry<Set<Annotation>, Set<Bean<?>>> qualEntry : typeEntry.getValue().entrySet()) {
                List<Bean<?>> alternatives = qualEntry.getValue().stream()
                        .filter(Bean::isAlternative)
                        .collect(Collectors.toList());

                if (alternatives.size() > 1) {
                    // Deduplicate logically equivalent beans (same bean class) within this qualifier bucket
                    Map<String, Bean<?>> uniqueByClass = new LinkedHashMap<>();
                    for (Bean<?> alt : alternatives) {
                        uniqueByClass.put(alt.getBeanClass().getName(), alt);
                    }
                    alternatives = new ArrayList<>(uniqueByClass.values());

                    Map<Bean<?>, Integer> priorities = new HashMap<>();
                    List<Bean<?>> noPriorityAlternatives = new ArrayList<>();

                    for (Bean<?> alt : alternatives) {
                        Priority priority = alt.getBeanClass().getAnnotation(Priority.class);
                        if (priority != null) {
                            priorities.put(alt, priority.value());
                        } else {
                            noPriorityAlternatives.add(alt);
                        }
                    }

                    if (noPriorityAlternatives.size() > 1) {
                        String alternativeList = noPriorityAlternatives.stream()
                                .map(b -> b.getBeanClass().getName())
                                .collect(Collectors.joining(", "));

                        knowledgeBase.addError(
                                "Ambiguous alternatives for type " + formatType(type) +
                                " with qualifiers " + formatQualifiers(qualEntry.getKey()) +
                                ": [" + alternativeList + "]. " +
                                "Multiple alternatives without @Priority - cannot determine precedence. " +
                                "Add @Priority to resolve ambiguity."
                        );
                        allValid = false;
                    }

                    if (!priorities.isEmpty()) {
                        Map<Integer, List<Bean<?>>> byPriority = new HashMap<>();
                        for (Map.Entry<Bean<?>, Integer> e : priorities.entrySet()) {
                            byPriority.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
                        }
                        int max = byPriority.keySet().stream().max(Integer::compareTo).orElse(Integer.MIN_VALUE);
                        List<Bean<?>> top = byPriority.get(max);
                        if (top != null && top.size() > 1) {
                            String samePriorityList = top.stream()
                                    .map(b -> b.getBeanClass().getName())
                                    .collect(Collectors.joining(", "));

                            knowledgeBase.addError(
                                    "Ambiguous alternatives for type " + formatType(type) +
                                    " with qualifiers " + formatQualifiers(qualEntry.getKey()) +
                                    ": [" + samePriorityList + "] all have the same (highest) priority @Priority(" + max + "). " +
                                    "Alternatives must have different priority values to resolve ambiguity."
                            );
                            allValid = false;
                        }
                    }
                }
            }
        }

        return allValid;
    }

    private boolean isJavaLangObject(Type type) {
        if (type instanceof Class) {
            return Object.class.equals(type);
        }
        if (type != null && "java.lang.Object".equals(type.getTypeName())) {
            return true;
        }
        return false;
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
                Optional<Bean<?>> preferred = resolveByAlternativePrecedence(candidates);
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

        // Check for ambiguous dependency (apply CDI alternative precedence rules)
        if (candidates.size() > 1) {
            Optional<Bean<?>> preferred = resolveByAlternativePrecedence(candidates);
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

        if (!validateProxyableBeanTypesIfRequired(injectionPoint, owningBean, resolvedBean)) {
            return false;
        }

        // Injection point is valid
        return true;
    }

    /**
     * CDI 4.1 §3.10 - unproxyable bean types:
     * if an injection point resolves to a bean that requires a client proxy (normal scope)
     * or has bound interceptors, every bean type of that bean must be proxyable.
     */
    private boolean validateProxyableBeanTypesIfRequired(InjectionPoint injectionPoint,
                                                         Bean<?> owningBean,
                                                         Bean<?> resolvedBean) {
        if (!requiresProxyableBeanTypes(resolvedBean)) {
            return true;
        }

        List<String> unproxyableTypes = findUnproxyableBeanTypes(resolvedBean.getTypes());
        if (unproxyableTypes.isEmpty()) {
            return true;
        }

        knowledgeBase.addError(
                formatInjectionPoint(injectionPoint, owningBean) +
                ": resolved bean " + resolvedBean.getBeanClass().getName() +
                " requires proxying but declares unproxyable bean type(s): " +
                String.join(", ", unproxyableTypes)
        );
        return false;
    }

    private boolean requiresProxyableBeanTypes(Bean<?> bean) {
        if (isNormalScope(bean.getScope())) {
            return true;
        }

        return hasBoundInterceptor(bean);
    }

    private boolean hasBoundInterceptor(Bean<?> bean) {
        if (!(bean instanceof BeanImpl)) {
            return false;
        }

        Class<?> beanClass = bean.getBeanClass();
        Set<Annotation> classBindings = extractInterceptorBindingAnnotations(beanClass.getAnnotations());
        if (!classBindings.isEmpty() && !knowledgeBase.getInterceptorsByBindings(classBindings).isEmpty()) {
            return true;
        }

        for (Method method : beanClass.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)) {
                continue;
            }

            Set<Annotation> effectiveBindings = new HashSet<>(classBindings);
            effectiveBindings.addAll(extractInterceptorBindingAnnotations(method.getAnnotations()));
            if (!effectiveBindings.isEmpty() &&
                    !knowledgeBase.getInterceptorsByBindings(effectiveBindings).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private Set<Annotation> extractInterceptorBindingAnnotations(Annotation[] annotations) {
        Set<Annotation> bindings = new HashSet<>();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(jakarta.interceptor.InterceptorBinding.class)) {
                bindings.add(annotation);
            }
        }
        return bindings;
    }

    private List<String> findUnproxyableBeanTypes(Set<Type> beanTypes) {
        List<String> unproxyable = new ArrayList<>();

        for (Type beanType : beanTypes) {
            String reason = unproxyableReason(beanType);
            if (reason != null) {
                unproxyable.add(beanType.getTypeName() + " (" + reason + ")");
            }
        }

        return unproxyable;
    }

    private String unproxyableReason(Type beanType) {
        Class<?> rawType = RawTypeExtractor.getRawType(beanType);
        if (rawType == null) {
            return null;
        }

        if (rawType.equals(Object.class)) {
            return null;
        }

        if (rawType.isPrimitive()) {
            return "primitive type";
        }

        if (rawType.isArray()) {
            return "array type";
        }

        if (isSealed(rawType)) {
            return "sealed class or interface";
        }

        if (rawType.isInterface()) {
            return null;
        }

        if (Modifier.isFinal(rawType.getModifiers())) {
            return "final class";
        }

        if (!hasNonPrivateNoArgConstructor(rawType)) {
            return "missing non-private no-arg constructor";
        }

        Method finalBusinessMethod = findNonStaticFinalNonPrivateMethod(rawType);
        if (finalBusinessMethod != null) {
            return "has non-static final method with non-private visibility: " + finalBusinessMethod.getName();
        }

        return null;
    }

    private boolean hasNonPrivateNoArgConstructor(Class<?> type) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0 && !Modifier.isPrivate(constructor.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    private Method findNonStaticFinalNonPrivateMethod(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) || Modifier.isPrivate(modifiers)) {
                continue;
            }
            return method;
        }
        return null;
    }

    private boolean isSealed(Class<?> type) {
        try {
            Method isSealedMethod = Class.class.getMethod("isSealed");
            Object value = isSealedMethod.invoke(type);
            return value instanceof Boolean && (Boolean) value;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    /**
     * Resolves candidates using CDI alternative precedence:
     * if enabled alternatives are present they are preferred over non-alternatives,
     * and the highest-priority alternative wins; equal top priority remains ambiguous.
     */
    private Optional<Bean<?>> resolveByAlternativePrecedence(Collection<Bean<?>> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() == 1) {
            return Optional.of(candidates.iterator().next());
        }

        List<Bean<?>> alternatives = candidates.stream()
                .filter(Bean::isAlternative)
                .collect(Collectors.toList());

        if (alternatives.isEmpty()) {
            return Optional.empty();
        }

        Bean<?> winner = null;
        int winnerPriority = Integer.MIN_VALUE;
        boolean tie = false;

        for (Bean<?> alternative : alternatives) {
            int priority = getAlternativePriority(alternative);
            if (winner == null || priority > winnerPriority) {
                winner = alternative;
                winnerPriority = priority;
                tie = false;
            } else if (priority == winnerPriority) {
                tie = true;
            }
        }

        if (tie || winner == null) {
            return Optional.empty();
        }

        return Optional.of(winner);
    }

    private int getAlternativePriority(Bean<?> bean) {
        if (bean instanceof ProducerBean) {
            ProducerBean<?> producerBean = (ProducerBean<?>) bean;
            Integer memberPriority = extractPriorityFromProducerMember(producerBean);
            if (memberPriority != null) {
                return memberPriority;
            }

            Integer producerPriority = producerBean.getPriority();
            if (producerPriority != null) {
                return producerPriority;
            }

            Integer declaringPriority = extractPriorityFromClass(producerBean.getDeclaringClass());
            if (declaringPriority != null) {
                return declaringPriority;
            }
        }

        if (bean instanceof BeanImpl) {
            Integer beanPriority = ((BeanImpl<?>) bean).getPriority();
            if (beanPriority != null) {
                return beanPriority;
            }
        }

        Integer classPriority = extractPriorityFromClass(bean.getBeanClass());
        if (classPriority != null) {
            return classPriority;
        }

        return jakarta.interceptor.Interceptor.Priority.APPLICATION;
    }

    private Integer extractPriorityFromProducerMember(ProducerBean<?> producerBean) {
        if (producerBean.getProducerMethod() != null) {
            return extractPriorityFromAnnotations(producerBean.getProducerMethod().getAnnotations());
        }
        if (producerBean.getProducerField() != null) {
            return extractPriorityFromAnnotations(producerBean.getProducerField().getAnnotations());
        }
        return null;
    }

    private Integer extractPriorityFromClass(Class<?> beanClass) {
        if (beanClass == null) {
            return null;
        }
        return extractPriorityFromAnnotations(beanClass.getAnnotations());
    }

    private Integer extractPriorityFromAnnotations(Annotation[] annotations) {
        if (annotations == null) {
            return null;
        }
        for (Annotation annotation : annotations) {
            String annotationTypeName = annotation.annotationType().getName();
            if (Priority.class.getName().equals(annotationTypeName) ||
                    "javax.annotation.Priority".equals(annotationTypeName)) {
                try {
                    Method valueMethod = annotation.annotationType().getMethod("value");
                    Object value = valueMethod.invoke(annotation);
                    if (value instanceof Integer) {
                        return (Integer) value;
                    }
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
        }
        return null;
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
            if (!isBeanEnabledForResolution(bean)) {
                continue;
            }
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

        return applySpecializationFiltering(matches);
    }

    private boolean isBeanEnabledForResolution(Bean<?> bean) {
        if (bean == null) {
            return false;
        }
        if (!bean.isAlternative()) {
            return true;
        }
        if (bean instanceof BeanImpl) {
            return ((BeanImpl<?>) bean).isAlternativeEnabled();
        }
        if (bean instanceof ProducerBean) {
            return ((ProducerBean<?>) bean).isAlternativeEnabled();
        }
        return true;
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
        if (required.isEmpty()) {
            return hasDefault(provided);
        }

        // Explicitly requesting only @Any means "any qualifier set is acceptable"
        if (hasOnlyAny(required)) {
            return true;
        }

        // @Default (with optional @Any) matches beans with @Default
        if (hasOnlyDefault(required)) {
            return hasDefault(provided);
        }

        // All required qualifiers must be present in provided
        for (Annotation reqQualifier : required) {
            // @Any is implicit and should not constrain matching when other qualifiers are present
            if (reqQualifier.annotationType().equals(Any.class)) {
                continue;
            }
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

        // CDI qualifier equality must honor @Nonbinding members.
        return AnnotationComparator.equals(a, b);
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
     * Checks if the set contains only @Any.
     */
    private boolean hasOnlyAny(Set<Annotation> qualifiers) {
        boolean hasAtLeastOneQualifier = false;
        for (Annotation qualifier : qualifiers) {
            if (!isQualifier(qualifier)) {
                continue;
            }
            hasAtLeastOneQualifier = true;
            if (!qualifier.annotationType().equals(Any.class)) {
                return false;
            }
        }
        return hasAtLeastOneQualifier;
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

    private Set<Annotation> qualifierKey(Set<Annotation> qualifiers) {
        return qualifiers.stream()
                .filter(q -> !q.annotationType().equals(javax.enterprise.inject.Any.class)
                        && !q.annotationType().equals(javax.enterprise.inject.Default.class)
                        && !q.annotationType().equals(jakarta.enterprise.inject.Any.class)
                        && !q.annotationType().equals(jakarta.enterprise.inject.Default.class))
                .collect(Collectors.toSet());
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
            for (java.lang.reflect.Method method : collectObserverCandidateMethods(beanClass)) {
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
        Type eventType = GenericTypeResolver.resolve(
                observedParameter.getParameterizedType(),
                declaringBean.getBeanClass(),
                method.getDeclaringClass()
        );
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
        // Observer methods with no explicit qualifiers observe events with @Any.
        if (qualifiers.isEmpty()) {
            qualifiers.add(new AnyLiteral());
        }
        return qualifiers;
    }

    /**
     * Collects methods from superclass to subclass and keeps overriding methods from subclasses.
     */
    private List<Method> collectObserverCandidateMethods(Class<?> beanClass) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = beanClass;
        while (current != null && !Object.class.equals(current)) {
            hierarchy.add(0, current);
            current = current.getSuperclass();
        }

        Map<String, Method> bySignature = new LinkedHashMap<>();
        for (Class<?> type : hierarchy) {
            for (Method method : type.getDeclaredMethods()) {
                bySignature.put(methodSignature(method), method);
            }
        }
        return new ArrayList<>(bySignature.values());
    }

    private String methodSignature(Method method) {
        StringBuilder builder = new StringBuilder(method.getName()).append("(");
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(parameterTypes[i].getName());
        }
        builder.append(")");
        return builder.toString();
    }

    /**
     * Basic specialization filtering: if a bean specializes its direct superclass, remove the
     * specialized superclass from candidates.
     */
    private Set<Bean<?>> applySpecializationFiltering(Set<Bean<?>> candidates) {
        if (candidates == null || candidates.size() < 2) {
            return candidates;
        }

        Set<Class<?>> specializedSuperclasses = new HashSet<>();
        for (Bean<?> candidate : candidates) {
            Class<?> beanClass = candidate.getBeanClass();
            if (beanClass != null && hasSpecializesAnnotation(beanClass)) {
                Class<?> superclass = beanClass.getSuperclass();
                if (superclass != null && !Object.class.equals(superclass)) {
                    specializedSuperclasses.add(superclass);
                }
            }
        }

        if (specializedSuperclasses.isEmpty()) {
            return candidates;
        }

        return candidates.stream()
                .filter(candidate -> !specializedSuperclasses.contains(candidate.getBeanClass()))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean hasSpecializesAnnotation(Class<?> beanClass) {
        for (Annotation annotation : beanClass.getAnnotations()) {
            String name = annotation.annotationType().getName();
            if ("jakarta.enterprise.inject.Specializes".equals(name) ||
                    "javax.enterprise.inject.Specializes".equals(name)) {
                return true;
            }
        }
        return false;
    }
}
