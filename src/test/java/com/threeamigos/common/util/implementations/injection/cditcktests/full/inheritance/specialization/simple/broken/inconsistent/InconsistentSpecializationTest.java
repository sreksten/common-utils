package com.threeamigos.common.util.implementations.injection.cditcktests.full.inheritance.specialization.simple.broken.inconsistent;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InconsistentSpecializationTest {

    @Test
    void testInconsistentSpecialization() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Employee.class, Maid.class, Manager.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DeploymentException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
