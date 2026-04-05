package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PassivationIdTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                CowBean.class,
                DummyContext.class,
                AfterBeanDiscoveryObserver.class,
                Cow.class,
                DummyScoped.class
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
    void testGetPassivationCapableBeanById() {
        Bean<?> passivatingBean = beanManager.getPassivationCapableBean(CowBean.PASSIVATION_ID);
        assertEquals(Cow.class, passivatingBean.getBeanClass());
    }
}
