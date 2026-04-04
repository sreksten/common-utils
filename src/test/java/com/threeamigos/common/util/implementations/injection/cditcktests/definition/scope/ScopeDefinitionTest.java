package com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.AnotherScopeType;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.BorderCollie;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.EnglishBorderCollie;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.GoldenLabrador;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.GoldenRetriever;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.Grayling;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.MiniatureClydesdale;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.Minnow;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.Mullet;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.Order;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.Pollock;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.RedSnapper;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.SeaBass;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test.ShetlandPony;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopeDefinitionTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.scopedefinitiontest.test";

    @Test
    void testScopeTypesAreExtensible() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, Mullet.class).size());
                Bean<Mullet> bean = getBeans(beanManager, Mullet.class).iterator().next();
                assertEquals(AnotherScopeType.class, bean.getScope());
            }
        });
    }

    @Test
    void testScopeTypeHasCorrectTarget() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, Mullet.class).size());
                Bean<Mullet> bean = getBeans(beanManager, Mullet.class).iterator().next();
                Target target = bean.getScope().getAnnotation(Target.class);
                assertNotNull(target);
                List<ElementType> elements = Arrays.asList(target.value());
                assertTrue(elements.contains(ElementType.TYPE));
                assertTrue(elements.contains(ElementType.METHOD));
                assertTrue(elements.contains(ElementType.FIELD));
            }
        });
    }

    @Test
    void testScopeTypeDeclaresScopeTypeAnnotation() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, Mullet.class).size());
                Bean<Mullet> bean = getBeans(beanManager, Mullet.class).iterator().next();
                assertNotNull(bean.getScope().getAnnotation(NormalScope.class));
            }
        });
    }

    @Test
    void testScopeDeclaredInJava() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, SeaBass.class).size());
                Bean<SeaBass> bean = getBeans(beanManager, SeaBass.class).iterator().next();
                assertEquals(RequestScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testDefaultScope() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, Order.class).size());
                Bean<Order> bean = getBeans(beanManager, Order.class).iterator().next();
                assertEquals(Dependent.class, bean.getScope());
            }
        });
    }

    @Test
    void testScopeSpecifiedAndStereotyped() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, Minnow.class).size());
                Bean<Minnow> bean = getBeans(beanManager, Minnow.class).iterator().next();
                assertEquals(RequestScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testMultipleIncompatibleScopeStereotypesWithScopeSpecified() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, Pollock.class).size());
                Bean<Pollock> bean = getBeans(beanManager, Pollock.class).iterator().next();
                assertEquals(Dependent.class, bean.getScope());
            }
        });
    }

    @Test
    void testMultipleCompatibleScopeStereotypes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, Grayling.class).size());
                Bean<Grayling> bean = getBeans(beanManager, Grayling.class).iterator().next();
                assertEquals(ApplicationScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testWebBeanScopeTypeOverridesStereotype() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, RedSnapper.class).size());
                Bean<RedSnapper> bean = getBeans(beanManager, RedSnapper.class).iterator().next();
                assertEquals(RequestScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testScopeTypeDeclaredInheritedIsInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<BorderCollie> bean = getBeans(beanManager, BorderCollie.class).iterator().next();
                assertEquals(RequestScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testScopeTypeNotDeclaredInheritedIsNotInherited() {
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
    void testScopeTypeDeclaredInheritedIsBlockedByIntermediateScopeTypeMarkedInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, GoldenRetriever.class).size());
            }
        });
    }

    @Test
    void testScopeTypeDeclaredInheritedIsBlockedByIntermediateScopeTypeNotMarkedInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, GoldenLabrador.class).size());
                Bean<GoldenLabrador> bean = getBeans(beanManager, GoldenLabrador.class).iterator().next();
                assertEquals(Dependent.class, bean.getScope());
            }
        });
    }

    @Test
    void testScopeTypeDeclaredInheritedIsIndirectlyInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<EnglishBorderCollie> bean = getBeans(beanManager, EnglishBorderCollie.class).iterator().next();
                assertEquals(RequestScoped.class, bean.getScope());
            }
        });
    }

    @Test
    void testScopeTypeNotDeclaredInheritedIsNotIndirectlyInherited() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, getBeans(beanManager, MiniatureClydesdale.class).size());
                Bean<MiniatureClydesdale> bean = getBeans(beanManager, MiniatureClydesdale.class).iterator().next();
                assertEquals(Dependent.class, bean.getScope());
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
    private static <T> Set<Bean<T>> getBeans(BeanManager beanManager, Class<T> type) {
        return (Set<Bean<T>>) (Set) beanManager.getBeans(type);
    }

    private interface BeanManagerConsumer {
        void accept(BeanManager beanManager);
    }
}
