package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AfterBeanDiscoveryTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                AfterBeanDiscoveryObserver.class,
                Cage.class,
                SuperScoped.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterBeanDiscoveryObserver.class.getName());
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanIsAdded() {
        assertEquals(1, beanManager.getBeans(Cockatoo.class).size());
        assertEquals("Billy", getContextualReference(Cockatoo.class).getName());
    }

    @Test
    void testCustomDependentBeanInjectionPointIsAvailable() {
        Cage cage = getContextualReference(Cage.class);
        assertNotNull(cage);
        assertNotNull(cage.getCockatoo());
        assertNotNull(cage.getCockatoo().getInjectionPoint());
        assertEquals(Cage.class, cage.getCockatoo().getInjectionPoint().getBean().getBeanClass());
    }

    @Test
    void testProcessBeanIsFired() {
        AfterBeanDiscoveryObserver extension = beanManager.getExtension(AfterBeanDiscoveryObserver.class);
        assertEquals(1, extension.getCockatooPBObservedCount().get());
        assertEquals(1, extension.getCockatooPSBObservedCount().get());
    }

    @Test
    void testProcessObserverMethodFiredWhileAddingObserverMethod() {
        AfterBeanDiscoveryObserver extension = beanManager.getExtension(AfterBeanDiscoveryObserver.class);
        assertEquals(1, extension.getTalkPOMObservedCount().get());
        assertEquals(1, extension.getTalkPSOMObservedCount().get());
    }

    @Test
    void testObserverMethodRegistered() {
        beanManager.getEvent().select(Talk.class).fire(new Talk("Hello"));
        assertTrue(AfterBeanDiscoveryObserver.addedObserverMethod.isObserved());
    }

    @Test
    void testAddContext() {
        Context context = beanManager.getContext(SuperScoped.class);
        assertNotNull(context);
        assertTrue(context.isActive());
        assertTrue(context instanceof SuperContext);
    }

    @SuppressWarnings("unchecked")
    private <T> T getContextualReference(Class<T> type, Annotation... qualifiers) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type, qualifiers));
        assertNotNull(bean, "No bean resolved for type " + type.getName());
        return (T) beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
    }
}
