package com.threeamigos.common.util.implementations.injection.annotations;

import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;

import java.lang.annotation.Annotation;
import java.util.Collection;

import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates.hasAlternativeAnnotation;
import static com.threeamigos.common.util.implementations.injection.annotations.AnnotationPredicates.hasStereotypeAnnotation;

/**
 * Helper for alternative-related annotation decisions.
 */
public class AlternativesHelper {

    private final StereotypesHelper stereotypesHelper;

    public AlternativesHelper(StereotypesHelper stereotypesHelper) {
        this.stereotypesHelper = stereotypesHelper;
    }

    public boolean isAlternativeDeclaration(Class<?> beanClass) {
        if (beanClass == null) {
            return false;
        }

        if (hasAlternativeAnnotation(beanClass)) {
            return true;
        }

        for (Annotation annotation : beanClass.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (hasStereotypeAnnotation(annotationType)
                    && stereotypesHelper.declaresAlternative(annotationType)) {
                return true;
            }
        }

        return false;
    }

    public boolean isAlternativeEnabledInBeansXml(String className, Collection<BeansXml> beansXmlConfigurations) {
        if (className == null || className.isEmpty() || beansXmlConfigurations == null) {
            return false;
        }

        for (BeansXml beansXml : beansXmlConfigurations) {
            if (beansXml.getAlternatives() != null) {
                if (beansXml.getAlternatives().getClasses().contains(className)) {
                    return true;
                }
                if (beansXml.getAlternatives().getStereotypes().contains(className)) {
                    return true;
                }
            }
        }

        return false;
    }
}
