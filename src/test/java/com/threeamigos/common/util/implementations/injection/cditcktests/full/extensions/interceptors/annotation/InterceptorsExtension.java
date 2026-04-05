package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.interceptors.annotation;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.interceptor.Interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class InterceptorsExtension implements Extension {

    void registerCustomInterceptor(@Observes ProcessAnnotatedType<SimpleBean> pat) {
        final AnnotatedType<SimpleBean> oldAnnotatedType = pat.getAnnotatedType();
        final Interceptors interceptorsAnnotation = new InterceptorsLiteral();

        AnnotatedType<SimpleBean> modifiedSimpleAnnotatedType = new AnnotatedType<SimpleBean>() {
            @Override
            public Class<SimpleBean> getJavaClass() {
                return oldAnnotatedType.getJavaClass();
            }

            @Override
            public Set<AnnotatedConstructor<SimpleBean>> getConstructors() {
                return oldAnnotatedType.getConstructors();
            }

            @Override
            public Set<AnnotatedMethod<? super SimpleBean>> getMethods() {
                return oldAnnotatedType.getMethods();
            }

            @Override
            public Set<AnnotatedField<? super SimpleBean>> getFields() {
                return oldAnnotatedType.getFields();
            }

            @Override
            public Type getBaseType() {
                return oldAnnotatedType.getBaseType();
            }

            @Override
            public Set<Type> getTypeClosure() {
                return oldAnnotatedType.getTypeClosure();
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annType) {
                if (Interceptors.class.equals(annType)) {
                    return annType.cast(interceptorsAnnotation);
                }
                return oldAnnotatedType.getAnnotation(annType);
            }

            @Override
            public Set<Annotation> getAnnotations() {
                Set<Annotation> annotations = new HashSet<Annotation>(oldAnnotatedType.getAnnotations());
                annotations.add(interceptorsAnnotation);
                return annotations;
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
                return Interceptors.class.equals(annotationType) || oldAnnotatedType.isAnnotationPresent(annotationType);
            }
        };
        pat.setAnnotatedType(modifiedSimpleAnnotatedType);
    }
}
