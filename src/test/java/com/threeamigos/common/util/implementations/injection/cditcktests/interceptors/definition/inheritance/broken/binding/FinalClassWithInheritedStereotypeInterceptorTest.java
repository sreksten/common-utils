package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.definition.inheritance.broken.binding;

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
class FinalClassWithInheritedStereotypeInterceptorTest {

    @Test
    void testFinalMethodWithInheritedStereotypeInterceptor() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Fighter.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FighterStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Messerschmitt.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(LandingBinding.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(LandingInterceptor.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DeploymentException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
