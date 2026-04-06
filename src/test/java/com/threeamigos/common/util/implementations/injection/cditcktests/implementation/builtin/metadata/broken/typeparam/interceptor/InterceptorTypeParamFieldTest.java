package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam.interceptor;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam.Cream;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InterceptorTypeParamFieldTest {

    @Test
    void testDeploymentFails() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Cream.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(InterceptorField.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Binding.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
