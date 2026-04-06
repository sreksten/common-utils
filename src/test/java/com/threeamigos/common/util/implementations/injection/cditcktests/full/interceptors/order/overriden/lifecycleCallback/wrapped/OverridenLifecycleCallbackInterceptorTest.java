package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.order.overriden.lifecycleCallback.wrapped;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class OverridenLifecycleCallbackInterceptorTest {

    @Test
    void testCallbackOverridenByCallback() {
        Bird.reset();
        Eagle.reset();

        Syringe syringe = newSyringe();
        try {
            syringe.setup();

            BeanManager beanManager = syringe.getBeanManager();
            Bean<Eagle> eagleBean = getUniqueBean(beanManager, Eagle.class);
            CreationalContext<Eagle> ctx = beanManager.createCreationalContext(eagleBean);
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

            BeanManager beanManager = syringe.getBeanManager();
            Bean<Falcon> falconBean = getUniqueBean(beanManager, Falcon.class);
            CreationalContext<Falcon> ctx = beanManager.createCreationalContext(falconBean);
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

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bird.class,
                Eagle.class,
                Falcon.class,
                WrappingExtension.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(WrappingExtension.class.getName());
        return syringe;
    }

    @SuppressWarnings("unchecked")
    private static <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        assertEquals(1, beans.size());
        return (Bean<T>) beans.iterator().next();
    }
}
