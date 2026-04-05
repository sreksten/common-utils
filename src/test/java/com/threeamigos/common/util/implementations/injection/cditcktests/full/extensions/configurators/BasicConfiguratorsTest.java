package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BasicConfiguratorsTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bar.class,
                DummyConfiguringExtension.class,
                Foo.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(DummyConfiguringExtension.class.getName());
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
    void configuratorReturnsAlwaysSameAnnotatedTypeConfigurator() {
        assertTrue(getDummyConfiguringExtension().isSameATConfiguratorReturned().get());
    }

    @Test
    void configuratorReturnsAlwaysSameInjectionPointConfigurator() {
        assertTrue(getDummyConfiguringExtension().isSameIPConfiguratorReturned().get());
    }

    @Test
    void configuratorReturnsAlwaysSameBeanAttributesConfigurator() {
        assertTrue(getDummyConfiguringExtension().isSameBAConfiguratorReturned().get());
    }

    @Test
    void configuratorReturnsAlwaysSameObserverMethodConfigurator() {
        assertTrue(getDummyConfiguringExtension().isSameOMConfiguratorReturned().get());
    }

    private DummyConfiguringExtension getDummyConfiguringExtension() {
        return beanManager.getExtension(DummyConfiguringExtension.class);
    }
}
