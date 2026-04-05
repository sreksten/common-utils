package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.invalid;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfiguratorAndSetMethodTest {

    private Syringe syringe;

    @BeforeAll
    void setUp() {
        resetFlags();
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Box.class,
                BoxObserverMethod.class,
                ConfigureAndSetExtension.class,
                Sorted.class,
                TestAnnotatedType.class,
                Warehouse.class,
                Worker.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ConfigureAndSetExtension.class.getName());
        syringe.setup();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testPAT() {
        assertTrue(ConfigureAndSetExtension.PAT_ISE_CAUGHT);
        assertTrue(ConfigureAndSetExtension.PAT_REVERSE_ISE_CAUGHT);
    }

    @Test
    void testPBA() {
        assertTrue(ConfigureAndSetExtension.PBA_ISE_CAUGHT);
        assertTrue(ConfigureAndSetExtension.PBA_REVERSE_ISE_CAUGHT);
    }

    @Test
    void testPIP() {
        assertTrue(ConfigureAndSetExtension.PIP_ISE_CAUGHT);
        assertTrue(ConfigureAndSetExtension.PIP_REVERSE_ISE_CAUGHT);
    }

    @Test
    void testPOM() {
        assertTrue(ConfigureAndSetExtension.POM_ISE_CAUGHT);
        assertTrue(ConfigureAndSetExtension.POM_REVERSE_ISE_CAUGHT);
    }

    private static void resetFlags() {
        ConfigureAndSetExtension.PAT_ISE_CAUGHT = false;
        ConfigureAndSetExtension.PAT_REVERSE_ISE_CAUGHT = false;
        ConfigureAndSetExtension.PBA_ISE_CAUGHT = false;
        ConfigureAndSetExtension.PBA_REVERSE_ISE_CAUGHT = false;
        ConfigureAndSetExtension.PIP_ISE_CAUGHT = false;
        ConfigureAndSetExtension.PIP_REVERSE_ISE_CAUGHT = false;
        ConfigureAndSetExtension.POM_ISE_CAUGHT = false;
        ConfigureAndSetExtension.POM_REVERSE_ISE_CAUGHT = false;
    }
}
