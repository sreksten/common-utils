package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.aroundInvoke.bindings;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class AroundInvokeInterceptorTest {

    @Test
    void testBusinessMethodIntercepted() {
        ActionSequence.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Foo foo = getContextualReference(syringe, Foo.class);
            foo.ping();

            ActionSequence.assertSequenceDataContainsAll(
                    SuperInterceptor1.class,
                    MiddleInterceptor1.class,
                    Interceptor1.class,
                    SuperInterceptor2.class,
                    Interceptor2.class,
                    SuperFoo.class,
                    MiddleFoo.class,
                    Foo.class
            );

            ActionSequence.assertSequenceDataEquals(
                    SuperInterceptor1.class,
                    MiddleInterceptor1.class,
                    Interceptor1.class,
                    SuperInterceptor2.class,
                    Interceptor2.class,
                    SuperFoo.class,
                    MiddleFoo.class,
                    Foo.class
            );
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Binding.class,
                Foo.class,
                Interceptor1.class,
                Interceptor2.class,
                MiddleFoo.class,
                MiddleInterceptor1.class,
                SuperFoo.class,
                SuperInterceptor1.class,
                SuperInterceptor2.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }
}
