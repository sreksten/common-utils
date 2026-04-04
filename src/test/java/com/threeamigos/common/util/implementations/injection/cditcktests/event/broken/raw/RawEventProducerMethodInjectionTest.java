package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.raw;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RawEventProducerMethodInjectionTest {

    @Test
    void testDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Foo.class, DisposerMethodInjectionBar.class);
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
    static class DisposerMethodInjectionBar {
        @Produces
        Foo produceFoo() {
            return new Foo();
        }

        @SuppressWarnings("rawtypes")
        void disposeFoo(@Disposes Foo foo, Event event) {
        }
    }
}
