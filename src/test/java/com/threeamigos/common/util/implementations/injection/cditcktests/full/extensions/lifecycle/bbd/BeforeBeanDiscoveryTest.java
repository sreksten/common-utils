package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Bar;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Baz;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Boss;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Foo;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Pro;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeforeBeanDiscoveryTest {

    private Syringe syringe;
    private BeanManager beanManager;
    private RequestContextController requestContextController;

    @BeforeAll
    void setUp() {
        BeforeBeanDiscoveryObserver.setObserved(false);

        syringe = new Syringe(new InMemoryMessageHandler(), BeforeBeanDiscoveryTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(BeforeBeanDiscoveryObserver.class.getName());
        syringe.setup();

        beanManager = syringe.getBeanManager();
        requestContextController = beanManager.createInstance().select(RequestContextController.class).get();
        requestContextController.activate();
    }

    @AfterAll
    void tearDown() {
        if (requestContextController != null) {
            try {
                requestContextController.deactivate();
            } catch (Exception ignored) {
                // Context may already be inactive.
            }
        }
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testBeforeBeanDiscoveryEventIsCalled() {
        assertTrue(BeforeBeanDiscoveryObserver.isObserved());
    }

    @Test
    void testAddingScopeType() {
        assertTrue(BeforeBeanDiscoveryObserver.isObserved());
        assertEquals(1, getBeans(RomanEmpire.class).size());

        Bean<RomanEmpire> bean = getBeans(RomanEmpire.class).iterator().next();
        assertEquals(EpochScoped.class, bean.getScope());
    }

    @Test
    void testAddingQualifierByClass() {
        assertTrue(BeforeBeanDiscoveryObserver.isObserved());
        assertEquals(0, getBeans(Alligator.class).size());
        assertEquals(1, getBeans(Alligator.class, new Tame.Literal()).size());
        assertTrue(beanManager.isQualifier(Tame.class));
    }

    @Test
    void testAddingQualifierByAnnotatedType() {
        assertTrue(BeforeBeanDiscoveryObserver.isObserved());

        assertEquals(1, beanManager.getBeans(Programmer.class, new SkillLiteral() {
            @Override
            public String language() {
                return "Java";
            }

            @Override
            public String level() {
                return "whatever";
            }
        }).size());

        assertEquals(0, beanManager.getBeans(Programmer.class, new SkillLiteral() {
            @Override
            public String language() {
                return "C++";
            }

            @Override
            public String level() {
                return "guru";
            }
        }).size());
    }

    @Test
    void testAddAnnotatedType() {
        getUniqueBean(Boss.class);

        assertEquals(0, getBeans(Bar.class).size());
        assertEquals(1, getBeans(Bar.class, Pro.ProLiteral.INSTANCE).size());

        assertEquals(0, getBeans(Foo.class).size());
        assertEquals(1, getBeans(Foo.class, Pro.ProLiteral.INSTANCE).size());
    }

    @Test
    void testAddAnnotatedTypeWithConfigurator() {
        Bean<Baz> bazBean = getUniqueBean(Baz.class, Pro.ProLiteral.INSTANCE);
        assertEquals(RequestScoped.class, bazBean.getScope());

        Baz baz = (Baz) beanManager.getReference(bazBean, Baz.class,
                beanManager.createCreationalContext(bazBean));
        assertFalse(baz.getBarInstance().isUnsatisfied());
    }

    @SuppressWarnings("unchecked")
    private <T> Set<Bean<T>> getBeans(Class<T> type, Annotation... qualifiers) {
        return (Set<Bean<T>>) (Set<?>) beanManager.getBeans(type, qualifiers);
    }

    private <T> Bean<T> getUniqueBean(Class<T> type, Annotation... qualifiers) {
        Set<Bean<T>> beans = getBeans(type, qualifiers);
        assertEquals(1, beans.size());
        return beans.iterator().next();
    }
}
