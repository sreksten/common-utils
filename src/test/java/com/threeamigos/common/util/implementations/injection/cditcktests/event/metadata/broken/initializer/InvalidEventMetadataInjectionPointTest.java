package com.threeamigos.common.util.implementations.injection.cditcktests.event.metadata.broken.initializer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InvalidEventMetadataInjectionPointTest {

    @Test
    void testDeployment() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Foo.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class Foo {
        @Inject
        void setEventMetadata(EventMetadata metadata) {
        }

        void observeSelf(@Observes Foo foo) {
        }
    }
}
