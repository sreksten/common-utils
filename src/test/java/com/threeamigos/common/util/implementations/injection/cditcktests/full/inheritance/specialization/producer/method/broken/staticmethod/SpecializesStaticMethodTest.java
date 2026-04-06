package com.threeamigos.common.util.implementations.injection.cditcktests.full.inheritance.specialization.producer.method.broken.staticmethod;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SpecializesStaticMethodTest {

    @Test
    void testSpecializedStaticMethod() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Expensive.class,
                FurnitureShop_Broken.class,
                Product.class,
                Shop.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
