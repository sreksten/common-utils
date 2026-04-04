package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.broken.producer.method.managed;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NonPassivationCapableProducerMethodTest {

    @Test
    void testNonPassivationCapableProducerMethodNotOk() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), RecordProducer.class, Broken_Record.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DeploymentException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
