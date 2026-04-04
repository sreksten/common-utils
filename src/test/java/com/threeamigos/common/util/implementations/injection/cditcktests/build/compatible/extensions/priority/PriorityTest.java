package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.priority;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.priority.prioritytest.test.PriorityExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;

class PriorityTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.priority.prioritytest.test";

    @Test
    void trigger() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(PriorityExtension.class.getName());
        syringe.setup();
        try {
            // Validation logic in the extension determines test outcome.
        } finally {
            syringe.shutdown();
        }
    }
}
