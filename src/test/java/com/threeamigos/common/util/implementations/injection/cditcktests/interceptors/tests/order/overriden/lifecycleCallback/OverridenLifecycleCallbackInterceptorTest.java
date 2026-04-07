package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.order.overriden.lifecycleCallback;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class OverridenLifecycleCallbackInterceptorTest {

    @Test
    void testCallbackOverridenByCallback() {
        Bird.reset();
        Eagle.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Bean<Eagle> eagleBean = getUniqueBean(syringe, Eagle.class);
            CreationalContext<Eagle> ctx = syringe.getBeanManager().createCreationalContext(eagleBean);
            Eagle eagle = eagleBean.create(ctx);

            eagle.ping();
            eagleBean.destroy(eagle, ctx);

            assertEquals(0, Bird.getInitBirdCalled().get());
            assertEquals(1, Eagle.getInitEagleCalled().get());
            assertEquals(0, Bird.getDestroyBirdCalled().get());
            assertEquals(1, Eagle.getDestroyEagleCalled().get());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testCallbackOverridenByNonCallback() {
        Bird.reset();
        Falcon.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Bean<Falcon> falconBean = getUniqueBean(syringe, Falcon.class);
            CreationalContext<Falcon> ctx = syringe.getBeanManager().createCreationalContext(falconBean);
            Falcon falcon = falconBean.create(ctx);

            falcon.ping();
            falconBean.destroy(falcon, ctx);

            assertEquals(0, Bird.getInitBirdCalled().get());
            assertEquals(0, Falcon.getInitFalconCalled().get());
            assertEquals(0, Bird.getDestroyBirdCalled().get());
            assertEquals(0, Falcon.getDestroyFalconCalled().get());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bird.class,
                Eagle.class,
                Falcon.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    @SuppressWarnings("unchecked")
    private <T> Bean<T> getUniqueBean(Syringe syringe, Class<T> beanClass) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(beanClass);
        assertEquals(1, beans.size());
        return (Bean<T>) beans.iterator().next();
    }
}
