package com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.Barn;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.BorderCollie;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.Cat;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.ClippedBorderCollie;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.Cod;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.DefangedTarantula;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.EnglishBorderCollie;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.HairyQualifier;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.MiniatureShetlandPony;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.ShetlandPony;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.SynchronousQualifier;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.TameLiteral;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.Tarantula;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.ChunkyQualifier;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test.WhitefishQualifier;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualifierDefinitionTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.qualifierdefinitiontest.test";

    @Test
    void testQualifierDeclaresBindingAnnotation() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertFalse(getBeans(beanManager, Tarantula.class, new TameLiteral()).isEmpty());
            }
        });
    }

    @Test
    void testQualifiersDeclaredInJava() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Cat> cat = resolveFirst(getBeans(beanManager, Cat.class, new SynchronousQualifier()), beanManager);
                assertEquals(2, cat.getQualifiers().size());
                assertTrue(cat.getQualifiers().contains(new SynchronousQualifier()));
            }
        });
    }

    @Test
    void testMultipleQualifiers() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<?> model = resolveFirst(getBeans(beanManager, Cod.class,
                        new ChunkyQualifier(true), new WhitefishQualifier()), beanManager);
                assertEquals(4, model.getQualifiers().size());
            }
        });
    }

    // Intentionally no @Test to preserve original TCK method metadata.
    void testFieldInjectedFromProducerMethod() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Barn> barnBean = resolveFirst(getBeans(beanManager, Barn.class), beanManager);
                Barn barn = barnBean.create(beanManager.createCreationalContext(barnBean));
                assertNotNull(barn.petSpider);
                assertTrue(barn.petSpider instanceof DefangedTarantula);
            }
        });
    }

    @Test
    void testQualifierDeclaredInheritedIsInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Set<? extends Annotation> bindings = getBeans(beanManager, BorderCollie.class, new HairyQualifier(false))
                        .iterator()
                        .next()
                        .getQualifiers();
                assertEquals(2, bindings.size());
                assertTrue(bindings.contains(new HairyQualifier(false)));
                assertTrue(bindings.contains(Any.Literal.INSTANCE));
            }
        });
    }

    @Test
    void testQualifierNotDeclaredInheritedIsNotInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Set<? extends Annotation> bindings = getBeans(beanManager, ShetlandPony.class)
                        .iterator()
                        .next()
                        .getQualifiers();
                assertEquals(2, bindings.size());
                assertTrue(bindings.contains(Default.Literal.INSTANCE));
                assertTrue(bindings.contains(Any.Literal.INSTANCE));
            }
        });
    }

    @Test
    void testQualifierDeclaredInheritedIsBlockedByIntermediateClass() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Set<? extends Annotation> bindings = getBeans(beanManager, ClippedBorderCollie.class, new HairyQualifier(true))
                        .iterator()
                        .next()
                        .getQualifiers();
                assertEquals(2, bindings.size());
                Annotation hairyLiteral = new HairyQualifier(true);
                assertTrue(bindings.contains(hairyLiteral));
                assertTrue(bindings.contains(Any.Literal.INSTANCE));
            }
        });
    }

    @Test
    void testQualifierDeclaredInheritedIsIndirectlyInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Set<? extends Annotation> bindings = getBeans(beanManager, EnglishBorderCollie.class, new HairyQualifier(false))
                        .iterator()
                        .next()
                        .getQualifiers();
                assertEquals(2, bindings.size());
                assertTrue(bindings.contains(new HairyQualifier(false)));
            }
        });
    }

    @Test
    void testQualifierNotDeclaredInheritedIsNotIndirectlyInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Set<? extends Annotation> bindings = getBeans(beanManager, MiniatureShetlandPony.class)
                        .iterator()
                        .next()
                        .getQualifiers();
                assertEquals(2, bindings.size());
                assertTrue(bindings.contains(Default.Literal.INSTANCE));
                assertTrue(bindings.contains(Any.Literal.INSTANCE));
            }
        });
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
    private static <T> Set<Bean<?>> getBeans(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        return (Set<Bean<?>>) (Set) beanManager.getBeans(type, qualifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveFirst(Set<Bean<?>> beans, BeanManager beanManager) {
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private interface BeanManagerConsumer {
        void accept(BeanManager beanManager);
    }
}
