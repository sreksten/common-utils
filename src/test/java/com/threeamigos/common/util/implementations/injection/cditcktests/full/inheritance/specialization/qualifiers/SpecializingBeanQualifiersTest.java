package com.threeamigos.common.util.implementations.injection.cditcktests.full.inheritance.specialization.qualifiers;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecializingBeanQualifiersTest {

    @Test
    void testQuailifiersOfSpecializingdNestedClass() {
        Syringe syringe = newSyringe();
        try {
            testQualifiersOfSpecializedBean(
                    syringe.getBeanManager(),
                    StaticNestedClassesParent.StaticSpecializationBean.class,
                    StaticNestedClassesParent.StaticMockSpecializationBean.class
            );
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testQuailifiersOfSpecializingBean() {
        Syringe syringe = newSyringe();
        try {
            testQualifiersOfSpecializedBean(
                    syringe.getBeanManager(),
                    SpecializationBean.class,
                    MockSpecializationBean.class
            );
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testQualifiersOfProducedSpecializingBean() {
        Syringe syringe = newSyringe();
        try {
            testAndReturnSpecializedBeanWithQualifiers(syringe.getBeanManager(), DataProvider.class);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                DataProvider.class,
                DataProviderProducer.class,
                Mock.class,
                MockDataProviderProducer.class,
                MockSpecializationBean.class,
                SpecializationBean.class,
                StaticNestedClassesParent.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private void testQualifiersOfSpecializedBean(BeanManager beanManager, Class<?> specializedClass, Class<?> specializingClass) {
        Bean<?> bean = testAndReturnSpecializedBeanWithQualifiers(beanManager, specializedClass);
        assertTrue(bean.getTypes().contains(specializingClass));
    }

    private Bean<?> testAndReturnSpecializedBeanWithQualifiers(BeanManager beanManager, Class<?> specializedClass) {
        Set<Bean<?>> specializationBeans = beanManager.getBeans(specializedClass, new Mock.MockLiteral());
        assertEquals(1, specializationBeans.size());

        Bean<?> bean = specializationBeans.iterator().next();
        Set<Annotation> qualifiers = bean.getQualifiers();
        assertEquals(2, qualifiers.size());
        assertTrue(qualifiers.contains(Any.Literal.INSTANCE));
        assertTrue(qualifiers.contains(new Mock.MockLiteral()));
        return bean;
    }
}
