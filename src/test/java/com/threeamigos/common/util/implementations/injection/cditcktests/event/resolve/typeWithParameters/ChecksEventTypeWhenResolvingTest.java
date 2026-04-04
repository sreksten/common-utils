package com.threeamigos.common.util.implementations.injection.cditcktests.event.resolve.typeWithParameters;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChecksEventTypeWhenResolvingTest {

    @Test
    void testResolvingChecksEventType() {
        Syringe syringe = newSyringe(AnObserver.class);
        try {
            assertFalse(syringe.getBeanManager().resolveObserverMethods(new AnEventType()).isEmpty());
            assertTrue(syringe.getBeanManager().resolveObserverMethods(new UnusedEventType("name")).isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe(Class<?>... discoveredClasses) {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        for (Class<?> discoveredClass : discoveredClasses) {
            syringe.addDiscoveredClass(discoveredClass, BeanArchiveMode.EXPLICIT);
        }
        syringe.start();
        return syringe;
    }

    static class AnEventType {
    }

    static class UnusedEventType {
        UnusedEventType(String name) {
        }
    }

    @Dependent
    static class AnObserver {
        boolean wasNotified = false;

        void observes(@Observes AnEventType event) {
            wasNotified = true;
        }
    }
}
