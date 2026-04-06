package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.validation.ambiguous;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DisposerMethodParameterInjectionValidationTest {

    @Test
    void test() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Product.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Cow.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Producer.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DeploymentException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
