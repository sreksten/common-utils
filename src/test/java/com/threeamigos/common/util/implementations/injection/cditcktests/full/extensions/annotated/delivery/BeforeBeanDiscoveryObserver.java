package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.annotated.delivery;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BeforeBeanDiscoveryObserver implements Extension {

    public void observeBeforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
        AnnotatedType<Phoenix> phoenix = beanManager.createAnnotatedType(Phoenix.class);
        event.addAnnotatedType(phoenix, buildId(Phoenix.class));

        AnnotatedType<Griffin> griffin = beanManager.createAnnotatedType(Griffin.class);
        AnnotatedType<Griffin> wrappedGriffin =
                new AdditionalTypeAnnotationsAnnotatedType<Griffin>(griffin, false, Wanted.WantedLiteral.INSTANCE);
        event.addAnnotatedType(wrappedGriffin, buildId(Griffin.class));
    }

    private static String buildId(Class<?> javaClass) {
        return BeforeBeanDiscoveryObserver.class.getName() + "_" + javaClass.getName();
    }

    private static final class AdditionalTypeAnnotationsAnnotatedType<X> implements AnnotatedType<X> {

        private final AnnotatedType<X> delegate;
        private final Set<Annotation> annotations;

        private AdditionalTypeAnnotationsAnnotatedType(AnnotatedType<X> delegate,
                                                       boolean keepOriginalAnnotations,
                                                       Annotation... additionalAnnotations) {
            this.delegate = delegate;
            Set<Annotation> merged = new HashSet<Annotation>();
            if (keepOriginalAnnotations) {
                merged.addAll(delegate.getAnnotations());
            }
            if (additionalAnnotations != null) {
                for (Annotation annotation : additionalAnnotations) {
                    if (annotation != null) {
                        merged.add(annotation);
                    }
                }
            }
            this.annotations = Collections.unmodifiableSet(merged);
        }

        @Override
        public Class<X> getJavaClass() {
            return delegate.getJavaClass();
        }

        @Override
        public Set<AnnotatedConstructor<X>> getConstructors() {
            return delegate.getConstructors();
        }

        @Override
        public Set<AnnotatedMethod<? super X>> getMethods() {
            return delegate.getMethods();
        }

        @Override
        public Set<AnnotatedField<? super X>> getFields() {
            return delegate.getFields();
        }

        @Override
        public Type getBaseType() {
            return delegate.getBaseType();
        }

        @Override
        public Set<Type> getTypeClosure() {
            return delegate.getTypeClosure();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
            if (annotationType == null) {
                return null;
            }
            for (Annotation annotation : annotations) {
                if (annotationType.equals(annotation.annotationType())) {
                    return annotationType.cast(annotation);
                }
            }
            return null;
        }

        @Override
        public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
            Set<T> result = new HashSet<T>();
            if (annotationType == null) {
                return result;
            }
            for (Annotation annotation : annotations) {
                if (annotationType.equals(annotation.annotationType())) {
                    result.add(annotationType.cast(annotation));
                }
            }
            return result;
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return annotations;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return getAnnotation(annotationType) != null;
        }
    }
}
