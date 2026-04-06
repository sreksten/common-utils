package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.definition.broken.finalClassInterceptor;

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
class DependentBeanFinalMethodInterceptorTest {

    @Test
    void testFinalMethodWithClassLevelInterceptor() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(FooBinding.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(MissileInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NuclearMissileFinalMethod.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NuclearMissileIPBean.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DeploymentException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
