package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.BorderCollie;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.Chihuahua;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.EnglishBorderCollie;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.HighlandCow;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.LongHairedDog;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.MexicanChihuahua;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.MiniatureClydesdale;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.Moose;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.Reindeer;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.ShetlandPony;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.Springbok;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test.Tame;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StereotypeDefinitionTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.stereotypedefinitiontest.test";

    private static final Annotation TAME_LITERAL = new Tame.Literal();

    @Test
    void testStereotypeWithScopeType() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, Moose.class).size());
                Bean<Moose> bean = getBeans(beanManager, Moose.class).iterator().next();
                assertEquals(RequestScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testStereotypeWithoutScopeType() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, Reindeer.class).size());
                Bean<Reindeer> bean = getBeans(beanManager, Reindeer.class).iterator().next();
                assertEquals(Dependent.class, bean.getScope());
            }
        });
    }

    @Test
    void testOneStereotypeAllowed() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<LongHairedDog> bean = getBeans(beanManager, LongHairedDog.class).iterator().next();
                assertEquals(RequestScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testMultipleStereotypesAllowed() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, HighlandCow.class, TAME_LITERAL).size());
                Bean<HighlandCow> highlandCow = getBeans(beanManager, HighlandCow.class, TAME_LITERAL).iterator().next();
                assertNull(highlandCow.getName());
                assertTrue(highlandCow.getQualifiers().contains(TAME_LITERAL));
                assertEquals(RequestScoped.class, highlandCow.getScope());
            }
        });
    }

    @Test
    void testExplicitScopeOverridesMergedScopesFromMultipleStereotype() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, Springbok.class).size());
                Bean<Springbok> bean = getBeans(beanManager, Springbok.class).iterator().next();
                assertEquals(RequestScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testStereotypeDeclaredInheritedIsInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<BorderCollie> bean = getBeans(beanManager, BorderCollie.class).iterator().next();
                assertEquals(RequestScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testStereotypeNotDeclaredInheritedIsNotInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, ShetlandPony.class).size());
                Bean<ShetlandPony> bean = getBeans(beanManager, ShetlandPony.class).iterator().next();
                assertEquals(Dependent.class, bean.getScope());
            }
        });
    }

    @Test
    void testStereotypeDeclaredInheritedIsIndirectlyInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<EnglishBorderCollie> bean = getBeans(beanManager, EnglishBorderCollie.class).iterator().next();
                assertEquals(RequestScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testStereotypeNotDeclaredInheritedIsNotIndirectlyInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, MiniatureClydesdale.class).size());
                Bean<MiniatureClydesdale> bean = getBeans(beanManager, MiniatureClydesdale.class).iterator().next();
                assertEquals(Dependent.class, bean.getScope());
            }
        });
    }

    @Test
    void testStereotypeScopeIsOverriddenByInheritedScope() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Chihuahua> bean = getBeans(beanManager, Chihuahua.class).iterator().next();
                assertEquals(ApplicationScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testStereotypeScopeIsOverriddenByIndirectlyInheritedScope() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<MexicanChihuahua> bean = getBeans(beanManager, MexicanChihuahua.class).iterator().next();
                assertEquals(ApplicationScoped.class, bean.getScope());
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
    private static <T> Set<Bean<T>> getBeans(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        return (Set<Bean<T>>) (Set) beanManager.getBeans(type, qualifiers);
    }

    private interface BeanManagerConsumer {
        void accept(BeanManager beanManager);
    }
}
