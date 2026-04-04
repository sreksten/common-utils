package com.threeamigos.common.util.implementations.injection.cditcktests.event.lifecycle;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Shutdown;
import jakarta.enterprise.event.Startup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupShutdownTest {

    @Test
    void testEventsObserved() {
        ObservingBean.OBSERVED_STARTING_EVENTS.clear();
        ObservingBean.OBSERVED_SHUTDOWN_EVENTS.clear();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ObservingBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            assertEquals(2, ObservingBean.OBSERVED_STARTING_EVENTS.size());
            assertEquals(ApplicationScoped.class.getSimpleName(), ObservingBean.OBSERVED_STARTING_EVENTS.get(0));
            assertEquals(Startup.class.getSimpleName(), ObservingBean.OBSERVED_STARTING_EVENTS.get(1));
            assertTrue(ObservingBean.OBSERVED_SHUTDOWN_EVENTS.isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @ApplicationScoped
    static class ObservingBean {
        static final List<String> OBSERVED_STARTING_EVENTS = new ArrayList<String>();
        static final List<String> OBSERVED_SHUTDOWN_EVENTS = new ArrayList<String>();

        void startup(@Observes Startup startup) {
            OBSERVED_STARTING_EVENTS.add(Startup.class.getSimpleName());
        }

        void initAppScope(@Observes @Initialized(ApplicationScoped.class) Object init) {
            OBSERVED_STARTING_EVENTS.add(ApplicationScoped.class.getSimpleName());
        }

        void shutdown(@Observes Shutdown shutdown) {
            OBSERVED_SHUTDOWN_EVENTS.add(Shutdown.class.getSimpleName());
        }

        void observeBeforeShutdown(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
            OBSERVED_SHUTDOWN_EVENTS.add(ApplicationScoped.class.getSimpleName());
        }
    }
}
