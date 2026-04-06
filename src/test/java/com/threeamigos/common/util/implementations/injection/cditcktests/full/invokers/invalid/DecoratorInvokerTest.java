package com.threeamigos.common.util.implementations.injection.cditcktests.full.invokers.invalid;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Isolated
class DecoratorInvokerTest {

    @Test
    void trigger() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                MyService.class,
                MyDecorator.class,
                TestExtension.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(TestExtension.class.getName());

        try {
            assertThrows(DeploymentException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
