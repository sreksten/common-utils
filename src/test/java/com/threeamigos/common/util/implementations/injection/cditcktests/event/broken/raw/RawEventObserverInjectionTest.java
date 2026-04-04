package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.raw;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RawEventObserverInjectionTest {

    @Test
    void testDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Foo.class, ObserverInjectionBar.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class Foo {
    }

    @Dependent
    static class ObserverInjectionBar {
        @SuppressWarnings("rawtypes")
        void observeSomething(@Observes Foo foo, Event event) {
        }
    }
}
