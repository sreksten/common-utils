package com.threeamigos.common.util.implementations.injection.cditcktests.full.inheritance.specialization.producer.method.broken.name;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SpecializingAndSpecializedBeanHaveNameTest {

    @Test
    void testSpecializingAndSpecializedBeanHasName() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                HighSchool_Broken.class,
                Pupil.class,
                School.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
