package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.bean;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.enterprise.inject.spi.ProducerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BeanExtension implements Extension {

    private Bean<Zoo> zooBean;

    @SuppressWarnings("unchecked")
    void registerBeans(@Observes AfterBeanDiscovery event, BeanManager manager) {
        // create a synthetic class bean
        {
            AnnotatedType<Office> oat = manager.createAnnotatedType(Office.class);
            BeanAttributes<Office> oa = manager.createBeanAttributes(oat);
            InjectionTargetFactory<Office> factory = manager.getInjectionTargetFactory(oat);
            Bean<?> bean = manager.createBean(oa, Office.class, factory);
            event.addBean(bean);
        }
        // create a serializable synthetic class bean
        {
            AnnotatedType<SerializableOffice> oat = manager.createAnnotatedType(SerializableOffice.class);
            BeanAttributes<SerializableOffice> oa = manager.createBeanAttributes(oat);
            InjectionTargetFactory<SerializableOffice> factory = manager.getInjectionTargetFactory(oat);
            Bean<?> bean = manager.createBean(oa, SerializableOffice.class, factory);
            event.addBean(bean);
        }
        // create a synthetic decorator
        {
            AnnotatedType<VehicleDecorator> oat = manager.createAnnotatedType(VehicleDecorator.class);
            BeanAttributes<VehicleDecorator> oa = addDecoratorStereotype(manager.createBeanAttributes(oat));
            InjectionTargetFactory<VehicleDecorator> factory = manager.getInjectionTargetFactory(oat);
            Bean<?> bean = manager.createBean(oa, VehicleDecorator.class, factory);
            assertCondition(bean instanceof Decorator<?>, "Expected synthetic bean to be a decorator");
            event.addBean(bean);
        }

        assertNotNull(zooBean, "Expected Zoo bean to be observed before synthetic producer registration");

        // create synthetic producer field
        {
            AnnotatedType<Zoo> zoo = manager.createAnnotatedType(Zoo.class);
            assertEquals(1, zoo.getFields().size(), "Expected exactly one producer field");
            AnnotatedField<? super Zoo> field = zoo.getFields().iterator().next();
            BeanAttributes<Lion> attributes = (BeanAttributes<Lion>) starveOut(manager.createBeanAttributes(field));
            ProducerFactory<Zoo> factory = manager.getProducerFactory(field, zooBean);
            event.addBean(manager.createBean(attributes, Zoo.class, factory));
        }
        // create synthetic producer method
        {
            AnnotatedType<Zoo> zoo = manager.createAnnotatedType(Zoo.class);
            AnnotatedMethod<? super Zoo> method = null;
            for (AnnotatedMethod<? super Zoo> _method : zoo.getMethods()) {
                if (_method.getBaseType().equals(Tiger.class)) {
                    method = _method;
                }
            }
            assertNotNull(method, "Expected tiger producer method");
            BeanAttributes<Tiger> attributes = (BeanAttributes<Tiger>) starveOut(manager.createBeanAttributes(method));
            ProducerFactory<Zoo> factory = manager.getProducerFactory(method, zooBean);
            event.addBean(manager.createBean(attributes, Zoo.class, factory));
        }
    }

    void observeZooBean(@Observes ProcessManagedBean<Zoo> event) {
        this.zooBean = event.getBean();
    }

    private <T> BeanAttributes<T> starveOut(final BeanAttributes<T> attributes) {
        return new DelegatingBeanAttributes<T>(attributes) {

            @Override
            public Set<Annotation> getQualifiers() {
                Set<Annotation> qualifiers = new HashSet<Annotation>(attributes.getQualifiers());
                qualifiers.add(Hungry.Literal.INSTANCE);
                qualifiers.remove(Default.Literal.INSTANCE);
                return Collections.unmodifiableSet(qualifiers);
            }
        };
    }

    private <T> BeanAttributes<T> addDecoratorStereotype(final BeanAttributes<T> attributes) {
        return new DelegatingBeanAttributes<T>(attributes) {

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return Collections.<Class<? extends Annotation>>singleton(jakarta.decorator.Decorator.class);
            }
        };
    }

    private static class DelegatingBeanAttributes<T> implements BeanAttributes<T> {

        private final BeanAttributes<T> delegate;

        private DelegatingBeanAttributes(BeanAttributes<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Set<Type> getTypes() {
            return delegate.getTypes();
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return delegate.getQualifiers();
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return delegate.getScope();
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return delegate.getStereotypes();
        }

        @Override
        public boolean isAlternative() {
            return delegate.isAlternative();
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " - expected: " + expected + ", actual: " + actual);
        }
    }

    private static <T> T assertNotNull(T value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
        return value;
    }
}
