package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.bindings.resolution;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InterceptionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InterceptorBindingResolutionTest {

    @Test
    void testBusinessMethodInterceptorBindings() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();

            assertEquals(
                    1,
                    syringe.getBeanManager().resolveInterceptors(
                            InterceptionType.AROUND_INVOKE,
                            new MessageBinding.Literal(),
                            new LoggedBinding.Literal(),
                            new TransactionalBinding.Literal(),
                            new PingBinding.Literal(),
                            new PongBinding.Literal(),
                            new BallBindingLiteral(true, true)
                    ).size()
            );

            MessageService messageService = getContextualReference(syringe, MessageService.class);
            assertNotNull(messageService);
            ComplicatedInterceptor.reset();
            messageService.ping();
            assertTrue(ComplicatedInterceptor.intercepted);

            MonitorService monitorService = getContextualReference(syringe, MonitorService.class);
            assertNotNull(monitorService);
            ComplicatedInterceptor.reset();
            monitorService.ping();
            assertFalse(ComplicatedInterceptor.intercepted);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testLifecycleInterceptorBindings() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();

            assertEquals(
                    1,
                    syringe.getBeanManager().resolveInterceptors(
                            InterceptionType.POST_CONSTRUCT,
                            new MessageBinding.Literal(),
                            new LoggedBinding.Literal(),
                            new TransactionalBinding.Literal(),
                            new BasketBindingLiteral(true, true)
                    ).size()
            );
            assertEquals(
                    1,
                    syringe.getBeanManager().resolveInterceptors(
                            InterceptionType.PRE_DESTROY,
                            new MessageBinding.Literal(),
                            new LoggedBinding.Literal(),
                            new TransactionalBinding.Literal(),
                            new BasketBindingLiteral(true, true)
                    ).size()
            );

            ComplicatedLifecycleInterceptor.reset();

            Bean<RemoteMessageService> bean = getUniqueBean(syringe, RemoteMessageService.class);
            CreationalContext<RemoteMessageService> ctx = syringe.getBeanManager().createCreationalContext(bean);
            RemoteMessageService remoteMessageService = bean.create(ctx);
            remoteMessageService.ping();
            bean.destroy(remoteMessageService, ctx);

            assertTrue(ComplicatedLifecycleInterceptor.postConstructCalled);
            assertTrue(ComplicatedLifecycleInterceptor.preDestroyCalled);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testConstructorInterceptorBindings() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();

            assertEquals(
                    1,
                    syringe.getBeanManager().resolveInterceptors(
                            InterceptionType.AROUND_CONSTRUCT,
                            new MachineBinding.Literal(),
                            new LoggedBinding.Literal(),
                            new TransactionalBinding.Literal(),
                            new ConstructorBinding.Literal(),
                            new CreativeBinding.Literal()
                    ).size()
            );

            ComplicatedAroundConstructInterceptor.reset();

            Bean<MachineService> bean = getUniqueBean(syringe, MachineService.class);
            CreationalContext<MachineService> ctx = syringe.getBeanManager().createCreationalContext(bean);
            bean.create(ctx);

            assertTrue(ComplicatedAroundConstructInterceptor.aroundConstructCalled);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                BallBinding.class,
                BallBindingLiteral.class,
                BasketBinding.class,
                BasketBindingLiteral.class,
                ComplicatedAroundConstructInterceptor.class,
                ComplicatedInterceptor.class,
                ComplicatedLifecycleInterceptor.class,
                ConstructorBinding.class,
                CreativeBinding.class,
                LoggedBinding.class,
                LoggedService.class,
                MachineBinding.class,
                MachineService.class,
                MessageBinding.class,
                MessageService.class,
                MonitorService.class,
                PingBinding.class,
                PongBinding.class,
                RemoteMessageService.class,
                RemoteService.class,
                Service.class,
                ServiceStereotype.class,
                TransactionalBinding.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }

    private <T> Bean<T> getUniqueBean(Syringe syringe, Class<T> beanType) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(beanType);
        Bean<?> bean = syringe.getBeanManager().resolve(beans);
        assertNotNull(bean);
        @SuppressWarnings("unchecked")
        Bean<T> cast = (Bean<T>) bean;
        return cast;
    }
}
