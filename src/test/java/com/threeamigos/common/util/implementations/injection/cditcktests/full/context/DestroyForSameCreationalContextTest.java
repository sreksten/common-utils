package com.threeamigos.common.util.implementations.injection.cditcktests.full.context;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DestroyForSameCreationalContextTest {

    @Test
    void testDestroyForSameCreationalContextOnly() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), AnotherSessionBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            Bean<AnotherSessionBean> sessionBean = resolveBean(beanManager, AnotherSessionBean.class);
            Context sessionContext = beanManager.getContext(SessionScoped.class);
            String sessionId = "full-context-destroy-same-cc";

            InspectableCreationalContext<AnotherSessionBean> creationalContext =
                    new InspectableCreationalContext<AnotherSessionBean>(beanManager.createCreationalContext(sessionBean));

            beanManager.getContextManager().activateSession(sessionId);
            try {
                AnotherSessionBean instance = sessionContext.get(sessionBean, creationalContext);
                instance.ping();
            } finally {
                beanManager.getContextManager().invalidateSession(sessionId);
            }

            assertTrue(creationalContext.isReleaseCalled());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static class InspectableCreationalContext<T> implements CreationalContext<T> {

        private final CreationalContext<T> delegate;
        private boolean releaseCalled;

        private InspectableCreationalContext(CreationalContext<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void push(T incompleteInstance) {
            delegate.push(incompleteInstance);
        }

        @Override
        public void release() {
            releaseCalled = true;
            delegate.release();
        }

        boolean isReleaseCalled() {
            return releaseCalled;
        }
    }

    @SessionScoped
    public static class AnotherSessionBean implements Serializable {
        private static final long serialVersionUID = 1L;

        public void ping() {
        }
    }
}
