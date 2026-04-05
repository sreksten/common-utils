package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.unmanaged.broken;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Unmanaged;
import jakarta.enterprise.inject.spi.Unmanaged.UnmanagedInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnamangedInstanceIllegalStatesTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                House.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void produceCalledOnAlreadyProducedInstance() {
        assertThrows(IllegalStateException.class, () -> {
            UnmanagedInstance<House> unmanagedHouseInstance = createUnmanagedInstance();
            unmanagedHouseInstance.produce().get();
            unmanagedHouseInstance.produce();
        });
    }

    @Test
    void produceCalledOnAlreadyDisposedInstance() {
        assertThrows(IllegalStateException.class, () -> {
            UnmanagedInstance<House> unmanagedHouseInstance = createUnmanagedInstance();
            House house = unmanagedHouseInstance.produce().get();
            house.build();
            unmanagedHouseInstance.dispose();
            unmanagedHouseInstance.produce();
        });
    }

    @Test
    void injectCallBeforeProduce() {
        assertThrows(IllegalStateException.class, () -> {
            UnmanagedInstance<House> unmanagedHouseInstance = createUnmanagedInstance();
            unmanagedHouseInstance.inject();
        });
    }

    @Test
    void injectCalledOnAlreadyDisposedInstance() {
        assertThrows(IllegalStateException.class, () -> {
            UnmanagedInstance<House> unmanagedHouseInstance = createUnmanagedInstance();
            House house = unmanagedHouseInstance.produce().get();
            house.build();
            unmanagedHouseInstance.dispose();
            unmanagedHouseInstance.inject();
        });
    }

    @Test
    void posConstructCallBeforeProduce() {
        assertThrows(IllegalStateException.class, () -> {
            UnmanagedInstance<House> unmanagedHouseInstance = createUnmanagedInstance();
            unmanagedHouseInstance.postConstruct();
        });
    }

    @Test
    void postConstructCalledOnAlreadyDisposedInstance() {
        assertThrows(IllegalStateException.class, () -> {
            UnmanagedInstance<House> unmanagedHouseInstance = createUnmanagedInstance();
            House house = unmanagedHouseInstance.produce().get();
            house.build();
            unmanagedHouseInstance.dispose();
            unmanagedHouseInstance.postConstruct();
        });
    }

    @Test
    void preDestroyCallBeforeProduce() {
        assertThrows(IllegalStateException.class, () -> {
            UnmanagedInstance<House> unmanagedHouseInstance = createUnmanagedInstance();
            unmanagedHouseInstance.preDestroy();
        });
    }

    @Test
    void preDeStroyCalledOnAlreadyDisposedInstance() {
        assertThrows(IllegalStateException.class, () -> {
            UnmanagedInstance<House> unmanagedHouseInstance = createUnmanagedInstance();
            House house = unmanagedHouseInstance.produce().get();
            house.build();
            unmanagedHouseInstance.dispose();
            unmanagedHouseInstance.preDestroy();
        });
    }

    @Test
    void disposeCallBeforeProduce() {
        assertThrows(IllegalStateException.class, () -> {
            UnmanagedInstance<House> unmanagedHouseInstance = createUnmanagedInstance();
            unmanagedHouseInstance.dispose();
        });
    }

    @Test
    void disposeCalledOnAlreadyDisposedInstance() {
        assertThrows(IllegalStateException.class, () -> {
            UnmanagedInstance<House> unmanagedHouseInstance = createUnmanagedInstance();
            House house = unmanagedHouseInstance.produce().get();
            house.build();
            unmanagedHouseInstance.dispose();
            unmanagedHouseInstance.dispose();
        });
    }

    private UnmanagedInstance<House> createUnmanagedInstance() {
        Unmanaged<House> unmanagedHouse = new Unmanaged<House>(beanManager, House.class);
        return unmanagedHouse.newInstance();
    }
}
