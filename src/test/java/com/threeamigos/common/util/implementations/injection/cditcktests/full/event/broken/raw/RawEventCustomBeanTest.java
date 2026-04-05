package com.threeamigos.common.util.implementations.injection.cditcktests.full.event.broken.raw;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RawEventCustomBeanTest {

    @Test
    void testDefinitionError() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bar.class,
                AfterBeanDiscoveryObserver.class,
                CustomBarBean.class,
                CustomEventInjectionPoint.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterBeanDiscoveryObserver.class.getName());

        try {
            assertThrows(DeploymentException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
