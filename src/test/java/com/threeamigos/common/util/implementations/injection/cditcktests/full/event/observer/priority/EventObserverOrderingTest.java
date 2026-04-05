package com.threeamigos.common.util.implementations.injection.cditcktests.full.event.observer.priority;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.ObserverMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventObserverOrderingTest {

    private Syringe syringe;

    @AfterEach
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
            syringe = null;
        }
    }

    @Test
    void testDefaultPriority() {
        setup();
        ObserverExtension extension = syringe.getBeanManager().getExtension(ObserverExtension.class);
        assertEquals(ObserverMethod.DEFAULT_PRIORITY,
                extension.getObserverMethodPriority("Observer2.observeMoon").intValue());
    }

    @Test
    void testProcessObserverMethodPriority() {
        setup();
        ObserverExtension extension = syringe.getBeanManager().getExtension(ObserverExtension.class);
        assertEquals(ObserverMethod.DEFAULT_PRIORITY + 400,
                extension.getObserverMethodPriority("Observer3.observeMoon").intValue());
    }

    private void setup() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                MoonActivity.class,
                Moonrise.class,
                MoonObservers.class,
                MoonObservers.Observer2.class,
                MoonObservers.Observer3.class,
                ObserverExtension.class,
                ActionSequence.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ObserverExtension.class.getName());
        syringe.setup();
    }
}
