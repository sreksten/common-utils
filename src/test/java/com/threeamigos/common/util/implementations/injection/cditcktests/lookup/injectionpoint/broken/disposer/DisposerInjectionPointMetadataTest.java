package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injectionpoint.broken.disposer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DisposerInjectionPointMetadataTest {

    @Test
    void testDisposerWithInjectionPointMetadata() {
        assertThrows(DefinitionException.class, this::startSyringe);
    }

    private void startSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Disposer_Broken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Nice.class, BeanArchiveMode.EXPLICIT);
        try {
            syringe.start();
        } finally {
            syringe.shutdown();
        }
    }
}
