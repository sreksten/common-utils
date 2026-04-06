package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.typesafe.resolution.broken.disabled;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class DisabledBeanNotAvailableForInjectionTest {

    @Test
    void testDeployment() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(CrabSpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Sea.class, BeanArchiveMode.EXPLICIT);

        try {
            assertThrows(DeploymentException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
