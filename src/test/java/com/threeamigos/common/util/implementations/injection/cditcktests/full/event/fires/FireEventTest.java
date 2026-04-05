package com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

class FireEventTest {

    @Test
    void testFireContainerLifecycleEvent() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                ContainerLifecycleEventDispatcher.class,
                ContainerLifecycleEvents.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            syringe.activateRequestContextIfNeeded();
            syringe.inject(ContainerLifecycleEventDispatcher.class).fireContainerLifecycleEvents();
        } finally {
            try {
                syringe.deactivateRequestContextIfActive();
            } catch (RuntimeException ignored) {
                // Best effort cleanup.
            }
            syringe.shutdown();
        }
    }
}
