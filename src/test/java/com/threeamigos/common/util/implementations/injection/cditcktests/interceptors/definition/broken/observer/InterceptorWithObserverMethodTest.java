package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.definition.broken.observer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InterceptorWithObserverMethodTest {

    @Test
    void testInterceptorWithObserverMethodNotOk() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Transactional.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TransactionalInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FooPayload.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TransactionalService.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
