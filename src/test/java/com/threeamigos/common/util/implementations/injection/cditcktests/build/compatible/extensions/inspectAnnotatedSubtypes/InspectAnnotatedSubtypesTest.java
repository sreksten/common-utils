package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.inspectAnnotatedSubtypes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.inspectAnnotatedSubtypes.inspectannotatedsubtypestest.test.InspectAnnotatedSubtypesExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;

class InspectAnnotatedSubtypesTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.inspectAnnotatedSubtypes.inspectannotatedsubtypestest.test";

    @Test
    void test() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(InspectAnnotatedSubtypesExtension.class.getName());
        syringe.setup();
        try {
            // Validation logic in the extension determines test outcome.
        } finally {
            syringe.shutdown();
        }
    }
}
