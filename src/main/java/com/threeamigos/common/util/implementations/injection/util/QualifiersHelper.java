package com.threeamigos.common.util.implementations.injection.util;

import jakarta.inject.Named;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.hasQualifierAnnotation;

/**
 * Shared qualifier utilities used across resolution components.
 */
public final class QualifiersHelper {

    private QualifiersHelper() {}

    /**
     * Extracts qualifiers from an annotation array, defaulting to @Default when empty.
     */
    public static Set<Annotation> extractQualifiers(Annotation[] annotations) {
        Set<Annotation> qualifiers = new HashSet<>();
        if (annotations != null) {
            for (Annotation ann : annotations) {
                if (hasQualifierAnnotation(ann.annotationType())) {
                    qualifiers.add(ann);
                }
            }
        }
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
        return qualifiers;
    }

    /**
     * Normalizes a collection of annotations to a qualifier set (adds @Default if none).
     */
    public static Set<Annotation> normalizeQualifiers(Collection<Annotation> annotations) {
        Set<Annotation> qualifiers = annotations == null ? new HashSet<>() :
                annotations.stream()
                        .filter(ann -> hasQualifierAnnotation(ann.annotationType()))
                        .collect(Collectors.toSet());
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
        return qualifiers;
    }

    /**
     * Returns true if the available set contains all required qualifiers, respecting @Named values
     * and @Nonbinding semantics.
     */
    public static boolean qualifiersMatch(Set<Annotation> requiredQualifiers, Set<Annotation> availableQualifiers) {
        // Special case: @Named requires an exact match
        Annotation requiredNamed = findAnnotation(requiredQualifiers, Named.class);
        Annotation availableNamed = findAnnotation(availableQualifiers, Named.class);

        if (requiredNamed != null) {
            if (availableNamed == null) {
                return false;
            }
            if (!getNamedValue(requiredNamed).equals(getNamedValue(availableNamed))) {
                return false;
            }
        }

        for (Annotation required : requiredQualifiers) {
            if (required.annotationType().equals(jakarta.enterprise.inject.Any.class)) {
                continue;
            }
            if (required instanceof Named) {
                continue;
            }
            boolean found = false;
            for (Annotation avail : availableQualifiers) {
                if (qualifiersEqual(required, avail)) {
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

    public static boolean qualifiersEqual(Annotation q1, Annotation q2) {
        return AnnotationComparator.equals(q1, q2);
    }

    public static Annotation findAnnotation(Set<Annotation> annotations, Class<? extends Annotation> type) {
        if (annotations == null) {
            return null;
        }
        for (Annotation ann : annotations) {
            if (ann.annotationType().equals(type)) {
                return ann;
            }
        }
        return null;
    }

    public static String getNamedValue(Annotation namedAnnotation) {
        try {
            return (String) namedAnnotation.annotationType().getMethod("value").invoke(namedAnnotation);
        } catch (Exception e) {
            return "";
        }
    }
}
