package com.threeamigos.common.util.implementations.injection.cditcktests.full.inheritance.specialization.simple.broken.noextend2;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SpecializingBeanExtendsNothingTest {

    @Test
    void testSpecializingClassDirectlyExtendsNothing() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Cow_Broken.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
