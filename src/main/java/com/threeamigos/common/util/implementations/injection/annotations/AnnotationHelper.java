package com.threeamigos.common.util.implementations.injection.annotations;

import jakarta.annotation.Nonnull;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationHelper {

    @Nonnull
    public static String toList(Collection<Annotation> annotationDef) {
        String metaAnnotationList;
        if (annotationDef != null && !annotationDef.isEmpty()) {
            metaAnnotationList = toList(annotationDef.stream());
        } else {
            metaAnnotationList = "[]";
        }
        return metaAnnotationList;
    }

    @Nonnull
    public static String toList(Annotation[] annotationDef) {
        String metaAnnotationList;
        if (annotationDef != null && annotationDef.length > 0) {
            metaAnnotationList = toList(Stream.of(annotationDef));
        } else {
            metaAnnotationList = "[]";
        }
        return metaAnnotationList;
    }

    private static String toList(Stream<Annotation> annotationDef) {
        return "[" +
                annotationDef
                        .map(def -> "@" + def.annotationType().getSimpleName())
                        .collect(Collectors.joining(", "))
                 + "]";
    }
}
