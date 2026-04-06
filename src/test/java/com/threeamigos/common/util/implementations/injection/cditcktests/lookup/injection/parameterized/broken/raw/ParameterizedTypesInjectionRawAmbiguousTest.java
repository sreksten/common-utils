package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.parameterized.broken.raw;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.parameterized.ConsumerRaw;
import com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.parameterized.Dao;
import com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.parameterized.ObjectDao;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParameterizedTypesInjectionRawAmbiguousTest {

    @Test
    void testInjection() {
        assertThrows(DeploymentException.class, this::startSyringe);
    }

    private void startSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Dao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ObjectDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ConsumerRaw.class, BeanArchiveMode.EXPLICIT);
        try {
            syringe.start();
        } finally {
            syringe.shutdown();
        }
    }
}
