package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.raw;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RawEventInitMethodInjectionTest {

    @Test
    void testDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Foo.class, InitMethodInjectionBar.class);
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

    @SuppressWarnings("rawtypes")
    @Dependent
    static class InitMethodInjectionBar {
        Event event;

        @Inject
        void setInstance(Event event) {
            this.event = event;
        }
    }
}
