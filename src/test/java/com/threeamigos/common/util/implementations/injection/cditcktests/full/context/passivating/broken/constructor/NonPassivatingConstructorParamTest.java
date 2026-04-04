package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.broken.constructor;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NonPassivatingConstructorParamTest {

    @Test
    void testBeanWithNonSerializableImplementationInjectedIntoNonTransientFieldOfBeanWithPassivatingScopeFails() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Vantaa_Broken.class, Violation.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DeploymentException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
