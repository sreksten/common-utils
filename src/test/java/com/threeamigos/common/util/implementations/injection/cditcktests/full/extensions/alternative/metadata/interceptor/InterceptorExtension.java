package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.alternative.metadata.interceptor;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InterceptorExtension implements Extension {

    public void registerAdditionalLoginAnnotatedType(@Observes BeforeBeanDiscovery event, BeanManager manager) {
        AnnotatedType<Login> interceptedLogin = manager.createAnnotatedType(Login.class);
        AnnotatedType<Login> modifiedInterceptedLogin = new ModifiedLoginAnnotatedType(interceptedLogin);
        event.addAnnotatedType(modifiedInterceptedLogin, buildId(Login.class));
    }

    private static String buildId(Class<?> javaClass) {
        return InterceptorExtension.class.getName() + "_" + javaClass.getName();
    }

    private static final class ModifiedLoginAnnotatedType implements AnnotatedType<Login> {

        private final AnnotatedType<Login> delegate;
        private final LoginInterceptorBinding.Literal interceptorBinding = new LoginInterceptorBinding.Literal();
        private final Secured.Literal secured = new Secured.Literal();
        private final Set<Annotation> annotations;

        private ModifiedLoginAnnotatedType(AnnotatedType<Login> delegate) {
            this.delegate = delegate;
            this.annotations = Collections.unmodifiableSet(
                    new HashSet<Annotation>(Arrays.<Annotation>asList(interceptorBinding, secured)));
        }

        @Override
        public Class<Login> getJavaClass() {
            return delegate.getJavaClass();
        }

        @Override
        public Set<AnnotatedConstructor<Login>> getConstructors() {
            return delegate.getConstructors();
        }

        @Override
        public Set<AnnotatedMethod<? super Login>> getMethods() {
            return delegate.getMethods();
        }

        @Override
        public Set<AnnotatedField<? super Login>> getFields() {
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
            if (LoginInterceptorBinding.class.equals(annotationType)) {
                return annotationType.cast(interceptorBinding);
            }
            if (Secured.class.equals(annotationType)) {
                return annotationType.cast(secured);
            }
            return null;
        }

        @Override
        public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
            Set<T> result = new HashSet<T>();
            if (annotationType == null) {
                return result;
            }
            T annotation = getAnnotation(annotationType);
            if (annotation != null) {
                result.add(annotation);
            }
            return result;
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return annotations;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return LoginInterceptorBinding.class.equals(annotationType) || Secured.class.equals(annotationType);
        }
    }
}
