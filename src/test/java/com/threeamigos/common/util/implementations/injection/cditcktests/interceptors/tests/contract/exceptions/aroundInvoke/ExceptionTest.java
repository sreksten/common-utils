package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.exceptions.aroundInvoke;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class ExceptionTest {

    @Test
    void testExceptions1() throws Exception {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertTrue(getContextualReference(syringe, SimpleBean.class).foo());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testExceptions2() throws Exception {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertTrue(getContextualReference(syringe, ExceptionBean.class).bar());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                SimpleBinding.class,
                ExceptionBinding.class,
                SimpleBean.class,
                ExceptionBean.class,
                Interceptor1.class,
                Interceptor2.class,
                Interceptor3.class,
                Interceptor4.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }
}
