package com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.bytype;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.bytype.beanbytypetest.fixtures.AlternativeConnector;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.bytype.beanbytypetest.fixtures.Connector;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.bytype.beanbytypetest.fixtures.NonBindingTypeLiteral;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.bytype.beanbytypetest.fixtures.SimpleBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.bytype.beanbytypetest.fixtures.TameLiteral;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanByTypeTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.bytype.beanbytypetest.fixtures";

    @Test
    void testGetBeans() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            Set<Bean<?>> beans = beanContainer.getBeans(SimpleBean.class);
            assertEquals(1, beans.size());
            assertEquals(SimpleBean.class, beans.iterator().next().getBeanClass());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNoBindingImpliesCurrent() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            Set<Bean<?>> beans = beanContainer.getBeans(SimpleBean.class);
            assertEquals(1, beans.size());
            assertTrue(beans.iterator().next().getQualifiers().contains(Default.Literal.INSTANCE));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetBeansDoesNotResolveAlternatives() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            Set<Bean<?>> beans = beanContainer.getBeans(Connector.class);
            assertEquals(2, beans.size());
            for (Bean<?> bean : beans) {
                assertTrue(typeSetMatches(bean.getTypes(), Object.class, Connector.class)
                        || typeSetMatches(bean.getTypes(), Object.class, Connector.class, AlternativeConnector.class));
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    <T> void testTypeVariable() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            TypeVariable<?> t = (TypeVariable<?>) new TypeLiteral<T>() {
            }.getType();
            assertThrows(IllegalArgumentException.class, () -> beanContainer.getBeans(t));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSameBindingTwice() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            assertThrows(IllegalArgumentException.class,
                    () -> beanContainer.getBeans(SimpleBean.class, new TameLiteral("a"), new TameLiteral("b")));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonBindingType() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            assertThrows(IllegalArgumentException.class,
                    () -> beanContainer.getBeans(SimpleBean.class, new NonBindingTypeLiteral()));
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private static boolean typeSetMatches(Set<Type> actualTypes, Type... expectedTypes) {
        Set<Type> expected = new HashSet<Type>();
        Collections.addAll(expected, expectedTypes);
        return actualTypes.equals(expected);
    }
}
