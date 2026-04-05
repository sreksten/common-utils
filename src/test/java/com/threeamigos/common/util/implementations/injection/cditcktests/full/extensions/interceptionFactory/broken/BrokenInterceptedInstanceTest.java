package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.interceptionFactory.broken;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrokenInterceptedInstanceTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(new InMemoryMessageHandler(), BrokenInterceptedInstanceTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.exclude(BeanWithInvalidInjectionPoint.class, InvalidInterceptionFactoryInjectionPointTest.class);
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
    void unproxyableExceptionIsThrown() {
        Probe probe = beanManager.createInstance().select(Probe.class).get();
        assertNull(probe.getUnproxyableTypeInstance().get());
    }

    @Test
    void illegalExceptionIsThrownForSubsequentCall() {
        Probe probe = beanManager.createInstance().select(Probe.class).get();
        assertNull(probe.getFooInstance().get());
    }
}
