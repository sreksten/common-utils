package com.threeamigos.common.util.implementations.injection.discovery.validation.bean;

import com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Extension;

/**
 * Extracted bean-class eligibility rules for CDI41BeanValidator.
 */
public class BeanClassEligibilityValidator {

    public interface Ops {
        boolean hasBeanDefiningAnnotation(Class<?> clazz);
        boolean isCurrentValidatedTypeOverridden(Class<?> clazz);
        boolean hasBeanDefiningAnnotationFromReflection(Class<?> clazz);
        boolean hasAlternativeAnnotation(Class<?> clazz);
        boolean hasDecoratorAnnotation(Class<?> clazz);
        boolean hasNoArgsConstructor(Class<?> clazz);
        boolean hasNotInjectConstructor(Class<?> clazz);
        boolean hasResolvableInjectConstructor(Class<?> clazz);
        boolean hasAnyDisposer(Class<?> clazz);
        boolean hasAnyProducer(Class<?> clazz);
        boolean hasOnlyStaticProducersAndDisposers(Class<?> clazz);
    }

    private final Ops ops;

    public BeanClassEligibilityValidator(Ops ops) {
        this.ops = ops;
    }

    public boolean isCandidateBeanClass(Class<?> clazz, BeanArchiveMode beanArchiveMode) {
        if (clazz == null || AnnotationPredicates.hasVetoedAnnotation(clazz) || beanArchiveMode == BeanArchiveMode.NONE) {
            return false;
        }

        if (Extension.class.isAssignableFrom(clazz)) {
            return false;
        }

        if (clazz.isAnnotation() || clazz.isInterface() || clazz.isEnum() || clazz.isPrimitive() || clazz.isArray()) {
            return false;
        }

        boolean beanDefining = ops.hasBeanDefiningAnnotation(clazz);
        if (ops.isCurrentValidatedTypeOverridden(clazz)) {
            beanDefining = beanDefining || ops.hasBeanDefiningAnnotationFromReflection(clazz);
        }
        if (beanDefining
                || ops.hasDecoratorAnnotation(clazz)
                || ops.hasAlternativeAnnotation(clazz)) {
            if (!ops.hasNoArgsConstructor(clazz) && ops.hasNotInjectConstructor(clazz)) {
                if (ops.hasAnyDisposer(clazz) && !ops.hasAnyProducer(clazz)) {
                    return false;
                }
                return !ops.hasOnlyStaticProducersAndDisposers(clazz);
            }
            return true;
        }

        if (beanArchiveMode == BeanArchiveMode.EXPLICIT) {
            return ops.hasNoArgsConstructor(clazz) || ops.hasResolvableInjectConstructor(clazz);
        }

        return false;
    }
}
