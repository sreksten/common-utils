package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.registration;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.registration.registrationtest.test.RegistrationExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;

class RegistrationTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.registration.registrationtest.test";

    @Test
    void trigger() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.IMPLICIT);
        syringe.addBuildCompatibleExtension(RegistrationExtension.class.getName());
        syringe.setup();
        try {
            // Validation logic in the extension determines test outcome.
        } finally {
            syringe.shutdown();
        }
    }
}
