package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.validation;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.validation.validationtest.test.ValidationExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.validation.validationtest.test";

    @Test
    void trigger() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(ValidationExtension.class.getName());
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
