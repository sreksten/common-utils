package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.broken.event;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FireContainerLifecycleEventTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                ContainerLifecycleEvents.class,
                FooExtension.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(FooExtension.class.getName());
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
    void testFireContainerLifecycleEvent() {
        for (final Object event : ContainerLifecycleEvents.CONTAINER_LIFECYCLE_EVENTS) {
            assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
                @Override
                public void execute() {
                    beanManager.getEvent().fire(event);
                }
            });
        }
    }
}
