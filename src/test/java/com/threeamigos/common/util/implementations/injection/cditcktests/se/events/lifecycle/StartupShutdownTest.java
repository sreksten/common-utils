package com.threeamigos.common.util.implementations.injection.cditcktests.se.events.lifecycle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Shutdown;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupShutdownTest {

    @Test
    void testEvents() {
        ObservingBean.OBSERVED_STARTING_EVENTS.clear();
        ObservingBean.OBSERVED_SHUTDOWN_EVENTS.clear();

        assertTrue(ObservingBean.OBSERVED_STARTING_EVENTS.isEmpty());
        assertTrue(ObservingBean.OBSERVED_SHUTDOWN_EVENTS.isEmpty());

        try (SeContainer seContainer = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(ObservingBean.class)
                .initialize()) {
            assertEquals(2, ObservingBean.OBSERVED_STARTING_EVENTS.size());
            assertEquals(ApplicationScoped.class.getSimpleName(), ObservingBean.OBSERVED_STARTING_EVENTS.get(0));
            assertEquals(Startup.class.getSimpleName(), ObservingBean.OBSERVED_STARTING_EVENTS.get(1));
        }

        assertEquals(2, ObservingBean.OBSERVED_SHUTDOWN_EVENTS.size());
        assertEquals(Shutdown.class.getSimpleName(), ObservingBean.OBSERVED_SHUTDOWN_EVENTS.get(0));
        assertEquals(ApplicationScoped.class.getSimpleName(), ObservingBean.OBSERVED_SHUTDOWN_EVENTS.get(1));
    }
}
