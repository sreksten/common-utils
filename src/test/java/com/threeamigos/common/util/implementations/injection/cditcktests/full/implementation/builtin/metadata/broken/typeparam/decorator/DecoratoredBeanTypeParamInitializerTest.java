package com.threeamigos.common.util.implementations.injection.cditcktests.full.implementation.builtin.metadata.broken.typeparam.decorator;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam.Cream;
import com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam.Milk;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DecoratoredBeanTypeParamInitializerTest {

    @Test
    void testDeploymentFails() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Cream.class, MilkDecoratedBeanInitializer.class, Milk.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
