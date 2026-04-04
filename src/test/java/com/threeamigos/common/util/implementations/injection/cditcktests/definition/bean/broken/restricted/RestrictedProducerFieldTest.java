package com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted.restrictedproducerfieldtest.test.Animal;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted.restrictedproducerfieldtest.test.Boulder;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted.restrictedproducerfieldtest.test.BoulderFieldProducer;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RestrictedProducerFieldTest {

    @Test
    void testInvalidTypedValueOnProducerField() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Boulder.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BoulderFieldProducer.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
