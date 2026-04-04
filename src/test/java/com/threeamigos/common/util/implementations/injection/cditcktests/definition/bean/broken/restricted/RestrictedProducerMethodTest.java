package com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted.restrictedproducermethodtest.test.Animal;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted.restrictedproducermethodtest.test.Boulder;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted.restrictedproducermethodtest.test.BoulderMethodProducer;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RestrictedProducerMethodTest {

    @Test
    void testInvalidTypedValueOnProducerField() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Boulder.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BoulderMethodProducer.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
