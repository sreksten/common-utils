package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.order.aroundInvoke;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class AroundInvokeOrderTest {

    @Test
    void testInvocationOrder() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertEquals(8, getContextualReference(syringe, Tram.class).getId());
            assertFalse(OverridenInterceptor.isOverridenMethodCalled());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Interceptor1.class,
                Interceptor3.class,
                Interceptor4.class,
                Interceptor5.class,
                Tram.class,
                TramClassBinding.class,
                TramMethodBinding.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }
}
