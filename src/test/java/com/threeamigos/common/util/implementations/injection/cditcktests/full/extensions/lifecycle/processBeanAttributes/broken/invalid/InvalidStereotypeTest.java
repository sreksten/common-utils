package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.broken.invalid;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InvalidStereotypeTest {

    @Test
    void test() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(InvalidStereotypeExtension.class.getName());
        syringe.initialize();
        syringe.addDiscoveredClass(Telephone.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PlainOldAnnotation.class, BeanArchiveMode.EXPLICIT);

        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            safeShutdown(syringe);
        }
    }

    private static void safeShutdown(Syringe syringe) {
        try {
            syringe.shutdown();
        } catch (Exception ignored) {
            // Startup failed as expected.
        }
    }
}
