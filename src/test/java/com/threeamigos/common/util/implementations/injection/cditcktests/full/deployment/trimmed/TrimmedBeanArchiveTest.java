package com.threeamigos.common.util.implementations.injection.cditcktests.full.deployment.trimmed;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrimmedBeanArchiveTest {

    /**
     * Legacy CDI TCK expectation (CDI 1.x): trimmed explicit archives may still fire PAT for
     * non-bean-defining types such as plain producer hosts.
     *
     * CDI 4.1 parity tests in this codebase expect the opposite behavior.
     * Keep this toggle disabled by default so the suite remains CDI 4.1 aligned.
     */
    private static final boolean LEGACY_EXPECT_PAT_FOR_TRIMMED_NON_BEAN_DEFINING_TYPES = false;

    private Syringe syringe;
    private TestExtension extension;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        extension = new TestExtension();

        syringe = new Syringe(new InMemoryMessageHandler());
        syringe.addExtension(extension);

        syringe.initialize();
        syringe.addDiscoveredClass(Bus.class, BeanArchiveMode.TRIMMED);
        syringe.addDiscoveredClass(Car.class, BeanArchiveMode.TRIMMED);
        syringe.addDiscoveredClass(BikeProducer.class, BeanArchiveMode.TRIMMED);
        syringe.addDiscoveredClass(Bike.class, BeanArchiveMode.TRIMMED);
        syringe.addDiscoveredClass(Segway.class, BeanArchiveMode.TRIMMED);
        syringe.addDiscoveredClass(Probe.class, BeanArchiveMode.TRIMMED);
        syringe.start();

        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testDiscoveredBean() {
        assertEquals(1, extension.getVehiclePBAinvocations().get());

        Bean<MotorizedVehicle> vehicleBean = getUniqueBean(MotorizedVehicle.class);
        CreationalContext<MotorizedVehicle> cc = beanManager.createCreationalContext(vehicleBean);
        MotorizedVehicle vehicle = (MotorizedVehicle) beanManager.getReference(vehicleBean, MotorizedVehicle.class, cc);
        assertEquals(Bus.class.getSimpleName(), vehicle.start());
    }

    @Test
    void testProducerNotDsicovered() {
        Probe probe = getContextualReference(Probe.class);

        if (LEGACY_EXPECT_PAT_FOR_TRIMMED_NON_BEAN_DEFINING_TYPES) {
            assertTrue(extension.isBikerProducerPATFired());
        } else {
            assertFalse(extension.isBikerProducerPATFired());
        }
        assertFalse(extension.isBikerProducerPBAFired());
        assertFalse(probe.bikeInstance.isAmbiguous());
    }

    @Test
    void testDiscoveredBeanWithStereoType() {
        Probe probe = getContextualReference(Probe.class);
        assertFalse(probe.segwayInstance.isUnsatisfied());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private <T> T getContextualReference(Class<T> type) {
        Bean<T> bean = getUniqueBean(type);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    @Dependent
    static class Probe {
        @Inject
        Instance<Bike> bikeInstance;

        @Inject
        Instance<Segway> segwayInstance;
    }
}
