package com.threeamigos.common.util.implementations.injection.cditcktests.full.inheritance.specialization.producer.method;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProducerMethodSpecializationTest {

    private static final Annotation EXPENSIVE_LITERAL = new Expensive.Literal();
    private static final Annotation SPARKLY_LITERAL = new Sparkly.Literal();

    @Test
    void testSpecializingProducerMethod() {
        Syringe syringe = newSyringe();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            @SuppressWarnings({"unchecked", "rawtypes"})
            Set<Bean<Necklace>> expensiveNecklaceBeans = (Set) beanManager.getBeans(Necklace.class, EXPENSIVE_LITERAL);
            assertEquals(1, expensiveNecklaceBeans.size());

            Bean<Necklace> expensiveNecklaceBean = expensiveNecklaceBeans.iterator().next();
            Set<Type> expensiveNecklaceBeanTypes = expensiveNecklaceBean.getTypes();
            assertEquals(3, expensiveNecklaceBeanTypes.size());
            assertTrue(typeSetMatches(expensiveNecklaceBeanTypes, Object.class, Product.class, Necklace.class));

            Set<Annotation> expensiveNecklaceQualifiers = expensiveNecklaceBean.getQualifiers();
            assertEquals(4, expensiveNecklaceQualifiers.size());
            assertTrue(annotationSetMatches(expensiveNecklaceQualifiers, Expensive.class, Sparkly.class, jakarta.enterprise.inject.Any.class, Named.class));

            @SuppressWarnings({"unchecked", "rawtypes"})
            Set<Bean<Necklace>> sparklyNecklaceBeans = (Set) beanManager.getBeans(Necklace.class, SPARKLY_LITERAL);
            assertEquals(1, sparklyNecklaceBeans.size());
            Bean<Necklace> sparklyBean = sparklyNecklaceBeans.iterator().next();
            assertEquals("expensiveGift", sparklyBean.getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSpecializingBeanInjection() {
        Syringe syringe = newSyringe();
        try {
            Product product = getContextualReference(syringe.getBeanManager(), ProductInjectionProbe.class).getProduct();
            assertTrue(product instanceof Necklace);
            assertEquals(10, product.getPrice());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Expensive.class,
                JewelryShop.class,
                Necklace.class,
                Product.class,
                ProductInjectionProbe.class,
                Shop.class,
                Sparkly.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private static boolean typeSetMatches(Set<Type> actual, Class<?>... expectedTypes) {
        if (actual.size() != expectedTypes.length) {
            return false;
        }
        for (Class<?> expectedType : expectedTypes) {
            if (!actual.contains(expectedType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean annotationSetMatches(Set<Annotation> actual, Class<? extends Annotation>... expectedAnnotationTypes) {
        if (actual.size() != expectedAnnotationTypes.length) {
            return false;
        }
        for (Class<? extends Annotation> expectedAnnotationType : expectedAnnotationTypes) {
            boolean matched = false;
            for (Annotation annotation : actual) {
                if (annotation.annotationType().equals(expectedAnnotationType)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type) {
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beanManager.getBeans(type));
        return (T) beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
    }
}
