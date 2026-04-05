package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.alternative.metadata.interceptor;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlternativeMetadataInterceptorInjectionTargetTest {

    @Test
    void testInterceptorInterceptsOnlyBindedClass() {
        withBootedSyringe(new Scenario() {
            @Override
            public void run(Syringe syringe, BeanManager manager) {
                AnnotatedType<Login> interceptedLogin = manager.createAnnotatedType(Login.class);
                AnnotatedType<Login> modifiedInterceptedLogin = new ModifiedLoginAnnotatedType(interceptedLogin);

                BeanAttributes<Login> beanAttributes = manager.createBeanAttributes(modifiedInterceptedLogin);
                InjectionTargetFactory<Login> factory = manager.getInjectionTargetFactory(modifiedInterceptedLogin);
                Bean<Login> bean = manager.createBean(beanAttributes, Login.class, factory);
                InjectionTarget<Login> injectionTarget = factory.createInjectionTarget(null);
                CreationalContext<Login> ctx = manager.createCreationalContext(bean);
                Login login = injectionTarget.produce(ctx);

                assertEquals("intercepted", login.login());
            }
        });
    }

    private void withBootedSyringe(Scenario scenario) {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Login.class,
                LoginInterceptor.class,
                LoginInterceptorBinding.class,
                Secured.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addBeansXmlInterceptors(syringe, LoginInterceptor.class.getName());
        try {
            syringe.setup();
            scenario.run(syringe, syringe.getBeanManager());
        } finally {
            syringe.shutdown();
        }
    }

    private static void addBeansXmlInterceptors(Syringe syringe, String... interceptorClassNames) {
        StringBuilder classes = new StringBuilder();
        for (String interceptorClassName : interceptorClassNames) {
            classes.append("<class>").append(interceptorClassName).append("</class>");
        }
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<interceptors>" + classes + "</interceptors>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    private interface Scenario {
        void run(Syringe syringe, BeanManager beanManager);
    }

    private static final class ModifiedLoginAnnotatedType implements AnnotatedType<Login> {

        private final AnnotatedType<Login> delegate;
        private final LoginInterceptorBinding.Literal interceptorBinding = new LoginInterceptorBinding.Literal();
        private final Secured.Literal secured = new Secured.Literal();
        private final Set<Annotation> annotations;

        private ModifiedLoginAnnotatedType(AnnotatedType<Login> delegate) {
            this.delegate = delegate;
            this.annotations = Collections.unmodifiableSet(new HashSet<Annotation>(
                    Arrays.<Annotation>asList(interceptorBinding, secured)));
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
