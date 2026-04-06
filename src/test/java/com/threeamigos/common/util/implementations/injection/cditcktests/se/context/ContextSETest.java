package com.threeamigos.common.util.implementations.injection.cditcktests.se.context;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class ContextSETest {

    @Test
    void applicationContextSharedBetweenAllBeansWithinContainer() {
        try (SeContainer seContainer = newInitializer().initialize()) {
            seContainer.select(Foo.class).get().ping();
            seContainer.select(Bar.class).get().ping();
            seContainer.select(Baz.class).get().ping();

            ApplicationScopedCounter applicationScopedCounter = seContainer.select(ApplicationScopedCounter.class).get();
            assertEquals(3, applicationScopedCounter.getCount());
        }
    }

    @Test
    void testEventIsFiredWhenAplicationContextInitialized() {
        ApplicationScopedObserver.reset();
        try (SeContainer seContainer = newInitializer().initialize()) {
            assertTrue(ApplicationScopedObserver.isInitialized);
            assertNotNull(ApplicationScopedObserver.initializedEventPayload);
        }
    }

    @Test
    void testEventIsFiredWhenAplicationContextDestroyed() {
        ApplicationScopedObserver.reset();
        try (SeContainer seContainer = newInitializer().initialize()) {
            // no-op
        }
        assertTrue(ApplicationScopedObserver.isBeforeDestroyed);
        assertNotNull(ApplicationScopedObserver.beforeDestroyedEventPayload);
        assertTrue(ApplicationScopedObserver.isDestroyed);
        assertNotNull(ApplicationScopedObserver.destroyedEventPayload);
    }

    private SeContainerInitializer newInitializer() {
        return SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(
                        ApplicationScopedCounter.class,
                        ApplicationScopedObserver.class,
                        Foo.class,
                        Bar.class,
                        Baz.class
                );
    }
}
