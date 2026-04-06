package com.threeamigos.common.util.implementations.injection.cditcktests.full.lookup.dynamic.broken.raw;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Isolated
class RawInstanceCustomBeanTest {

    @Test
    void testDefinitionError() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bar.class,
                AfterBeanDiscoveryObserver.class,
                CustomBarBean.class,
                CustomInstanceInjectionPoint.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterBeanDiscoveryObserver.class.getName());

        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
