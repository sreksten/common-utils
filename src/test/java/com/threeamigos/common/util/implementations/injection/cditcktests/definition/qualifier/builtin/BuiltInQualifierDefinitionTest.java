package com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.builtin;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.builtin.builtinqualifierdefinitiontest.test.AnyBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.builtin.builtinqualifierdefinitiontest.test.NamedAnyBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.builtin.builtinqualifierdefinitiontest.test.NamedBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.builtin.builtinqualifierdefinitiontest.test.Order;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.builtin.builtinqualifierdefinitiontest.test.ProducedAnyBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.builtin.builtinqualifierdefinitiontest.test.ProducedNamedAnyBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.builtin.builtinqualifierdefinitiontest.test.ProducedNamedBean;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInQualifierDefinitionTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.builtin.builtinqualifierdefinitiontest.test";

    @Test
    void testDefaultQualifierDeclaredInJava() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Order> order = resolveFirst(beanManager.getBeans(Order.class), beanManager);
                assertEquals(2, order.getQualifiers().size());
                assertTrue(order.getQualifiers().contains(Default.Literal.INSTANCE));
                assertTrue(order.getQualifiers().contains(Any.Literal.INSTANCE));
            }
        });
    }

    @Test
    void testDefaultQualifierForInjectionPoint() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Order> order = resolveFirst(beanManager.getBeans(Order.class), beanManager);
                assertEquals(1, order.getInjectionPoints().size());
                InjectionPoint injectionPoint = order.getInjectionPoints().iterator().next();
                assertTrue(injectionPoint.getQualifiers().contains(Default.Literal.INSTANCE));
            }
        });
    }

    @Test
    void testNamedAndAnyBeanHasDefaultQualifier() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<NamedAnyBean> bean = getUniqueBean(beanManager, NamedAnyBean.class, Any.Literal.INSTANCE);
                assertEquals(3, bean.getQualifiers().size());
                checkSetContainsAllQuallifiers(bean.getQualifiers(),
                        Default.Literal.INSTANCE,
                        NamedLiteral.of("namedAnyBean"),
                        Any.Literal.INSTANCE);
            }
        });
    }

    @Test
    void testNamedBeanHasDefaultQualifier() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<NamedBean> bean = getUniqueBean(beanManager, NamedBean.class);
                assertEquals(3, bean.getQualifiers().size());
                checkSetContainsAllQuallifiers(bean.getQualifiers(),
                        Default.Literal.INSTANCE,
                        NamedLiteral.of("namedBean"),
                        Any.Literal.INSTANCE);
            }
        });
    }

    @Test
    void testAnyBeanHasDefaultQualifier() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<AnyBean> bean = getUniqueBean(beanManager, AnyBean.class, Any.Literal.INSTANCE);
                assertEquals(2, bean.getQualifiers().size());
                checkSetContainsAllQuallifiers(bean.getQualifiers(),
                        Default.Literal.INSTANCE,
                        Any.Literal.INSTANCE);
            }
        });
    }

    @Test
    void testProducedNamedAndAnyBeanHasDefaultQualifier() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<ProducedNamedAnyBean> bean = getUniqueBean(beanManager, ProducedNamedAnyBean.class);
                assertEquals(3, bean.getQualifiers().size());
                checkSetContainsAllQuallifiers(bean.getQualifiers(),
                        Default.Literal.INSTANCE,
                        NamedLiteral.of("producedNamedAnyBean"),
                        Any.Literal.INSTANCE);
            }
        });
    }

    @Test
    void testProducedNamedBeanHasDefaultQualifier() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<ProducedNamedBean> bean = getUniqueBean(beanManager, ProducedNamedBean.class);
                assertEquals(3, bean.getQualifiers().size());
                checkSetContainsAllQuallifiers(bean.getQualifiers(),
                        Default.Literal.INSTANCE,
                        NamedLiteral.of("producedNamedBean"),
                        Any.Literal.INSTANCE);
            }
        });
    }

    @Test
    void testProducedAnyBeanHasDefaultQualifier() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<ProducedAnyBean> bean = getUniqueBean(beanManager, ProducedAnyBean.class);
                assertEquals(2, bean.getQualifiers().size());
                checkSetContainsAllQuallifiers(bean.getQualifiers(),
                        Default.Literal.INSTANCE,
                        Any.Literal.INSTANCE);
            }
        });
    }

    private static void checkSetContainsAllQuallifiers(Set<Annotation> qualifiersSet, Annotation... literals) {
        for (Annotation literal : literals) {
            assertTrue(qualifiersSet.contains(literal));
        }
    }

    private static void runInContainer(BeanManagerConsumer assertions) {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            assertions.accept(syringe.getBeanManager());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        assertEquals(1, beans.size(), "Expected a unique bean for type " + type.getName());
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveFirst(Set<Bean<?>> beans, BeanManager beanManager) {
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private interface BeanManagerConsumer {
        void accept(BeanManager beanManager);
    }
}
