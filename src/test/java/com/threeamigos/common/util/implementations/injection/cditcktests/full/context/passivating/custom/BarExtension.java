package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.custom;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

public class BarExtension implements Extension {

    void registerBar(@Observes AfterBeanDiscovery event, BeanManager manager) {
        AnnotatedType<Bar> annotatedType = manager.createAnnotatedType(Bar.class);
        final BeanAttributes<Bar> attributes = manager.createBeanAttributes(annotatedType);
        Bean<Bar> bean = new AbstractPassivationCapableBean<Bar>() {
            @Override
            public Class<?> getBeanClass() {
                return Bar.class;
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return Collections.emptySet();
            }

            @Override
            public Bar create(CreationalContext<Bar> creationalContext) {
                return new Bar();
            }

            @Override
            public void destroy(Bar instance, CreationalContext<Bar> creationalContext) {
                creationalContext.release();
            }

            @Override
            public String getId() {
                return getBeanClass().getCanonicalName();
            }

            @Override
            protected BeanAttributes<Bar> attributes() {
                return attributes;
            }
        };
        event.addBean(bean);
    }

    private static abstract class AbstractPassivationCapableBean<T> implements Bean<T>, PassivationCapable {

        protected abstract BeanAttributes<T> attributes();

        @Override
        public Set<Type> getTypes() {
            return attributes().getTypes();
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return attributes().getQualifiers();
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return attributes().getScope();
        }

        @Override
        public String getName() {
            return attributes().getName();
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return attributes().getStereotypes();
        }

        @Override
        public boolean isAlternative() {
            return attributes().isAlternative();
        }
    }
}
