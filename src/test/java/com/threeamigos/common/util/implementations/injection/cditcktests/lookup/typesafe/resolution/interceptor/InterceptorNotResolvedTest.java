package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.typesafe.resolution.interceptor;

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
class InterceptorNotResolvedTest {

    @Test
    void testInterceptorNotAvailableForInjection() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Cat.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(CatInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(CatInterceptorBinding.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);

        try {
            assertThrows(DeploymentException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
