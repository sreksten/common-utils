package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lite.coexistence;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildCompatibleExtensionSmokeTest {

    @Test
    void testExtensionsCanCoexist() {
        resetState();

        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                DummyBean.class,
                StandardPortableExtension.class,
                OverridingPortableExtension.class,
                StandardBuildCompatibleExtension.class,
                OverridenBuildCompatibleExtension.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(OverridingPortableExtension.class.getName());
        syringe.addExtension(StandardPortableExtension.class.getName());
        syringe.addBuildCompatibleExtension(StandardBuildCompatibleExtension.class.getName());
        syringe.addBuildCompatibleExtension(OverridenBuildCompatibleExtension.class.getName());

        try {
            syringe.setup();

            assertTrue(syringe.getBeanManager().createInstance().select(DummyBean.class).isResolvable());
            // Registration phase runs twice, but non-synthetic components are not re-delivered.
            assertEquals(5, StandardBuildCompatibleExtension.TIMES_INVOKED);
            assertEquals(0, OverridenBuildCompatibleExtension.TIMES_INVOKED);
            assertEquals(3, OverridingPortableExtension.TIMES_INVOKED);
            assertTrue(StandardPortableExtension.INVOKED);
        } finally {
            syringe.shutdown();
            resetState();
        }
    }

    private static void resetState() {
        StandardBuildCompatibleExtension.TIMES_INVOKED = 0;
        OverridenBuildCompatibleExtension.TIMES_INVOKED = 0;
        OverridingPortableExtension.TIMES_INVOKED = 0;
        StandardPortableExtension.INVOKED = false;
    }
}
