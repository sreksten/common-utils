package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.bindings.overriding;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterceptorBindingOverridingTest {

    @Test
    void testInterceptorBindingOverriden() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Pony pony = getContextualReference(syringe, Pony.class);
            assertEquals(-4, pony.getAge());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Aging.class,
                FastAgingInterceptor.class,
                Negating.class,
                NegatingInterceptor.class,
                Pony.class,
                SlowAgingInterceptor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }
}
