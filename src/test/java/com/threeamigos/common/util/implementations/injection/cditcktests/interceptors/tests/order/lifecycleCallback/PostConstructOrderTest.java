package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.order.lifecycleCallback;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class PostConstructOrderTest {

    @Test
    void testInvocationOrder() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            LakeCargoShip.setSequence(0);
            getContextualReference(syringe, LakeCargoShip.class);
            assertEquals(7, LakeCargoShip.getSequence());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Interceptor1.class,
                Interceptor4.class,
                LakeCargoShip.class,
                LakeCargoShipClassBinding.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }
}
