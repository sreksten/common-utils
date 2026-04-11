package com.threeamigos.common.util.implementations.injection.discovery.validation.bean;

import com.threeamigos.common.util.implementations.injection.annotations.QualifiersHelper;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.context.Dependent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationExtractors.getNamedAnnotation;
import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates.hasInheritedAnnotation;
import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates.hasNamedAnnotation;
import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates.normalizeSingletonToApplicationScoped;

/**
 * Extracted bean-attributes extraction rules for CDI41BeanValidator.
 */
public class BeanAttributesExtractor {

    public interface Ops {
        Annotation[] annotationsOf(Class<?> clazz);
        Annotation[] declaredAnnotationsOf(Class<?> clazz);
        boolean isStereotypeAnnotationType(Class<? extends Annotation> annotationType);
        boolean isScopeAnnotationType(Class<? extends Annotation> annotationType);
        boolean isQualifierAnnotationType(Class<? extends Annotation> annotationType);
    }

    private final KnowledgeBase knowledgeBase;
    private final Ops ops;

    public BeanAttributesExtractor(KnowledgeBase knowledgeBase, Ops ops) {
        this.knowledgeBase = knowledgeBase;
        this.ops = ops;
    }

    public String extractBeanName(Class<?> clazz) {
        for (Annotation annotation : ops.annotationsOf(clazz)) {
            if (hasNamedAnnotation(annotation.annotationType())) {
                return defaultedBeanName(readNamedValue(annotation), clazz);
            }
        }

        Set<Class<? extends Annotation>> visited = new HashSet<>();
        for (Annotation annotation : ops.annotationsOf(clazz)) {
            Class<? extends Annotation> at = annotation.annotationType();
            if (ops.isStereotypeAnnotationType(at)) {
                String stereotypeName = extractNameFromStereotype(at, clazz, visited);
                if (stereotypeName != null) {
                    return stereotypeName;
                }
            }
        }

        return "";
    }

    public Set<Annotation> extractBeanQualifiers(Class<?> clazz) {
        Set<Annotation> result = QualifiersHelper.extractQualifierAnnotations(ops.annotationsOf(clazz));
        for (Annotation annotation : ops.annotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (ops.isStereotypeAnnotationType(annotationType)) {
                result.addAll(extractQualifiersFromStereotype(annotationType));
            }
            if (ops.isQualifierAnnotationType(annotationType)) {
                result.add(annotation);
            }
        }
        return QualifiersHelper.normalizeBeanQualifiers(result);
    }

    public Class<? extends Annotation> extractBeanScope(Class<?> clazz) {
        for (Annotation annotation : ops.declaredAnnotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (ops.isScopeAnnotationType(annotationType)) {
                return normalizeSingletonToApplicationScoped(annotationType);
            }
        }

        Class<? extends Annotation> inheritedClassScope = resolveInheritedScopeByCdiRules(clazz);
        if (inheritedClassScope != null) {
            return inheritedClassScope;
        }

        Class<? extends Annotation> inheritedScope = null;
        for (Annotation annotation : ops.annotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (!ops.isStereotypeAnnotationType(annotationType)) {
                continue;
            }
            Class<? extends Annotation> stereotypeScope = extractScopeFromStereotype(annotationType);
            if (stereotypeScope == null) {
                continue;
            }
            if (inheritedScope == null) {
                inheritedScope = stereotypeScope;
                continue;
            }
            if (!inheritedScope.equals(stereotypeScope)) {
                knowledgeBase.addDefinitionError(clazz.getName() +
                        ": conflicting scopes inherited from stereotypes (" +
                        inheritedScope.getName() + " vs " + stereotypeScope.getName() +
                        "). Declare an explicit scope on the bean to resolve.");
            }
        }

        if (inheritedScope != null) {
            return inheritedScope;
        }

        return Dependent.class;
    }

    public Set<Class<? extends Annotation>> extractBeanStereotypes(Class<?> clazz) {
        Set<Class<? extends Annotation>> stereotypes = new HashSet<>();
        for (Annotation annotation : ops.annotationsOf(clazz)) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (ops.isStereotypeAnnotationType(annotationType)) {
                stereotypes.add(annotationType);
            }
        }
        return stereotypes;
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

        if (knowledgeBase.isRegisteredStereotype(stereotypeAnnotation)) {
            Set<Annotation> definition = knowledgeBase.getStereotypeDefinition(stereotypeAnnotation);
            if (definition != null) {
                for (Annotation meta : definition) {
                    if (meta == null) {
                        continue;
                    }
                    if (hasNamedAnnotation(meta.annotationType())) {
                        return defaultedBeanName(readNamedValue(meta), beanClass);
                    }
                }
            }
        }

        Annotation named = getNamedAnnotation(stereotypeAnnotation);
        if (named != null) {
            return defaultedBeanName(readNamedValue(named), beanClass);
        }

        for (Annotation meta : stereotypeAnnotation.getAnnotations()) {
            Class<? extends Annotation> metaType = meta.annotationType();
            if (ops.isStereotypeAnnotationType(metaType)) {
                String nested = extractNameFromStereotype(metaType, beanClass, visited);
                if (nested != null) {
                    return nested;
                }
            }
        }

        return null;
    }

    private Class<? extends Annotation> resolveInheritedScopeByCdiRules(Class<?> clazz) {
        Class<?> current = clazz.getSuperclass();
        while (current != null && current != Object.class) {
            Class<? extends Annotation> declaredScope = firstDeclaredScope(current);
            if (declaredScope != null) {
                return hasInheritedAnnotation(declaredScope) ? declaredScope : null;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Class<? extends Annotation> firstDeclaredScope(Class<?> clazz) {
        for (Annotation annotation : clazz.getDeclaredAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (ops.isScopeAnnotationType(annotationType)) {
                return normalizeSingletonToApplicationScoped(annotationType);
            }
        }
        return null;
    }

    private String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    public Class<? extends Annotation> extractScopeFromStereotype(Class<? extends Annotation> stereotypeClass) {
        if (knowledgeBase.isRegisteredStereotype(stereotypeClass)) {
            Set<Annotation> definition = knowledgeBase.getStereotypeDefinition(stereotypeClass);
            if (definition != null) {
                for (Annotation annotation : definition) {
                    if (annotation == null) {
                        continue;
                    }
                    Class<? extends Annotation> annotationType = annotation.annotationType();
                    if (ops.isScopeAnnotationType(annotationType)) {
                        return normalizeSingletonToApplicationScoped(annotationType);
                    }
                }
            }
        }

        for (Annotation annotation : stereotypeClass.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (ops.isScopeAnnotationType(annotationType)) {
                return normalizeSingletonToApplicationScoped(annotationType);
            }
        }

        for (Annotation annotation : stereotypeClass.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (ops.isStereotypeAnnotationType(annotationType)) {
                Class<? extends Annotation> nestedScope = extractScopeFromStereotype(annotationType);
                if (nestedScope != null) {
                    return nestedScope;
                }
            }
        }

        return null;
    }

    private Set<Annotation> extractQualifiersFromStereotype(Class<? extends Annotation> stereotypeClass) {
        Set<Annotation> qualifiers = QualifiersHelper.extractQualifierAnnotations(stereotypeClass.getAnnotations())
                .stream()
                .filter(annotation -> !hasNamedAnnotation(annotation.annotationType()))
                .collect(Collectors.toSet());

        if (knowledgeBase.isRegisteredStereotype(stereotypeClass)) {
            Set<Annotation> definition = knowledgeBase.getStereotypeDefinition(stereotypeClass);
            if (definition != null) {
                for (Annotation annotation : definition) {
                    if (annotation == null) {
                        continue;
                    }
                    Class<? extends Annotation> annotationType = annotation.annotationType();
                    if (ops.isQualifierAnnotationType(annotationType) && !hasNamedAnnotation(annotationType)) {
                        qualifiers.add(annotation);
                    }
                }
            }
        }

        for (Annotation annotation : stereotypeClass.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (ops.isStereotypeAnnotationType(annotationType)) {
                qualifiers.addAll(extractQualifiersFromStereotype(annotationType));
            }
        }

        return qualifiers;
    }
}
