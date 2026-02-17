package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves dependencies by finding matching beans from the KnowledgeBase.
 * This is the core dependency resolution engine for the CDI container.
 *
 * @author Stefano Reksten
 */
class BeanResolver implements ProducerBean.DependencyResolver {

    private final KnowledgeBase knowledgeBase;
    private final ContextManager contextManager;
    private final TypeChecker typeChecker;

    BeanResolver(KnowledgeBase knowledgeBase, ContextManager contextManager) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.contextManager = Objects.requireNonNull(contextManager, "contextManager cannot be null");
        this.typeChecker = new TypeChecker();
    }

    @Override
    public Object resolve(Type requiredType, Annotation[] qualifiers) {
        // Find matching beans
        Collection<Bean<?>> candidates = findMatchingBeans(requiredType, qualifiers);

        if (candidates.isEmpty()) {
            throw new RuntimeException(
                "No bean found for type: " + requiredType +
                " with qualifiers: " + Arrays.toString(qualifiers)
            );
        }

        if (candidates.size() > 1) {
            throw new RuntimeException(
                "Ambiguous dependency: multiple beans found for type: " + requiredType +
                " with qualifiers: " + Arrays.toString(qualifiers) +
                ". Matching beans: " + candidates.stream()
                    .map(b -> b.getBeanClass().getName())
                    .collect(Collectors.joining(", "))
            );
        }

        Bean<?> bean = candidates.iterator().next();

        // Get or create instance from appropriate scope
        return getInstanceFromScope(bean);
    }

    @Override
    public Object resolveDeclaringBeanInstance(Class<?> declaringClass) {
        // Find the bean for the declaring class
        for (Bean<?> bean : knowledgeBase.getValidBeans()) {
            if (bean.getBeanClass().equals(declaringClass)) {
                return getInstanceFromScope(bean);
            }
        }

        throw new RuntimeException(
            "No bean found for declaring class: " + declaringClass.getName()
        );
    }

    /**
     * Finds all beans matching the required type and qualifiers.
     */
    private Collection<Bean<?>> findMatchingBeans(Type requiredType, Annotation[] qualifiers) {
        List<Bean<?>> matches = new ArrayList<>();

        // Extract qualifier annotations (ignore non-qualifiers)
        Set<Annotation> requiredQualifiers = extractQualifiers(qualifiers);

        // Search through all valid beans
        for (Bean<?> bean : knowledgeBase.getValidBeans()) {
            // Skip beans with validation errors
            if (bean instanceof BeanImpl && ((BeanImpl<?>) bean).hasValidationErrors()) {
                continue;
            }
            if (bean instanceof ProducerBean && ((ProducerBean<?>) bean).hasValidationErrors()) {
                continue;
            }

            // Check type match
            boolean typeMatches = false;
            for (Type beanType : bean.getTypes()) {
                if (typeChecker.isAssignable(requiredType, beanType)) {
                    typeMatches = true;
                    break;
                }
            }

            if (!typeMatches) {
                continue;
            }

            // Check qualifier match
            if (qualifiersMatch(requiredQualifiers, bean.getQualifiers())) {
                matches.add(bean);
            }
        }

        return matches;
    }

    /**
     * Extracts qualifier annotations from an array of annotations.
     */
    private Set<Annotation> extractQualifiers(Annotation[] annotations) {
        Set<Annotation> qualifiers = new HashSet<>();
        for (Annotation ann : annotations) {
            if (ann.annotationType().isAnnotationPresent(Qualifier.class) ||
                ann.annotationType().isAnnotationPresent(jakarta.inject.Qualifier.class)) {
                qualifiers.add(ann);
            }
        }

        // If no qualifiers specified, use @Default
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }

        return qualifiers;
    }

    /**
     * Checks if bean qualifiers match the required qualifiers.
     * A bean matches if it has all the required qualifiers.
     */
    private boolean qualifiersMatch(Set<Annotation> requiredQualifiers, Set<Annotation> beanQualifiers) {
        // Special case: @Named requires exact match
        Annotation requiredNamed = findAnnotation(requiredQualifiers, Named.class);
        Annotation beanNamed = findAnnotation(beanQualifiers, Named.class);

        if (requiredNamed != null) {
            if (beanNamed == null) {
                return false;
            }
            // Compare @Named values
            String requiredName = getNamedValue(requiredNamed);
            String beanName = getNamedValue(beanNamed);
            if (!requiredName.equals(beanName)) {
                return false;
            }
        }

        // Check all other qualifiers (ignoring @Any which is always present)
        for (Annotation required : requiredQualifiers) {
            if (required.annotationType().equals(jakarta.enterprise.inject.Any.class)) {
                continue; // @Any matches everything
            }
            if (required instanceof Named) {
                continue; // Already handled above
            }

            // Find matching qualifier in bean
            boolean found = false;
            for (Annotation beanQual : beanQualifiers) {
                if (qualifiersEqual(required, beanQual)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    /**
     * Finds an annotation of a specific type in a set.
     */
    private Annotation findAnnotation(Set<Annotation> annotations, Class<? extends Annotation> annotationType) {
        for (Annotation ann : annotations) {
            if (ann.annotationType().equals(annotationType)) {
                return ann;
            }
        }
        return null;
    }

    /**
     * Gets the value from a @Named annotation.
     */
    private String getNamedValue(Annotation namedAnnotation) {
        try {
            return (String) namedAnnotation.annotationType().getMethod("value").invoke(namedAnnotation);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Checks if two qualifiers are equal.
     */
    private boolean qualifiersEqual(Annotation q1, Annotation q2) {
        if (!q1.annotationType().equals(q2.annotationType())) {
            return false;
        }

        // For now, use simple equality (this should compare all annotation members)
        return q1.equals(q2);
    }

    /**
     * Gets or creates an instance from the appropriate scope.
     */
    @SuppressWarnings("unchecked")
    private <T> T getInstanceFromScope(Bean<T> bean) {
        Class<? extends Annotation> scope = bean.getScope();

        // Get context for this scope
        ScopeContext context = contextManager.getContext(scope);

        // Get or create instance (context handles caching for scoped beans)
        CreationalContext<T> creationalContext = new CreationalContextImpl<>();
        return context.get(bean, creationalContext);
    }

    /**
     * Simple implementation of CreationalContext.
     * This tracks dependent instances for cleanup.
     */
    private static class CreationalContextImpl<T> implements CreationalContext<T> {
        private final List<Object> dependentInstances = new ArrayList<>();

        @Override
        public void push(T incompleteInstance) {
            // Not needed for our simple implementation
        }

        @Override
        public void release() {
            // Cleanup dependent instances
            dependentInstances.clear();
        }

        public void addDependentInstance(Object instance) {
            dependentInstances.add(instance);
        }
    }
}
