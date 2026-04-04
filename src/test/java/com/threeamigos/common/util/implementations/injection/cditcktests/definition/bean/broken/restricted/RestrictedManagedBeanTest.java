package com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted.restrictedmanagedbeantest.test.Animal;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted.restrictedmanagedbeantest.test.Stone;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RestrictedManagedBeanTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.broken.restricted.restrictedmanagedbeantest.test";

    @Test
    void testInvalidTypedValueOnManagedBean() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Stone.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
