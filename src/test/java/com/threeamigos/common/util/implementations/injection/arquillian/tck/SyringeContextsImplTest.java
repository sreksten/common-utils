package com.threeamigos.common.util.implementations.injection.arquillian.tck;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyringeContextsImplTest {

    @Test
    void shouldDeactivateRequestContextWhenSetInactiveCalled() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), TestRequestBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            syringe.activateRequestContextIfNeeded();
            Context requestContext = beanManager.getContext(RequestScoped.class);
            assertTrue(requestContext.isActive());

            Bean<TestRequestBean> bean = resolveBean(beanManager, TestRequestBean.class);
            SyringeContextsImpl contexts = new SyringeContextsImpl();
            contexts.setInactive(requestContext);

            assertThrows(ContextNotActiveException.class, () -> requestContext.get(bean));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void shouldSuspendAndRestoreSessionContextAcrossInactiveActiveTransitions() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), TestSessionBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateSession("tck-session-contexts-spi");
            Context sessionContext = beanManager.getContext(SessionScoped.class);
            assertTrue(sessionContext.isActive());

            Bean<TestSessionBean> bean = resolveBean(beanManager, TestSessionBean.class);
            CreationalContext<TestSessionBean> creationalContext = beanManager.createCreationalContext(bean);
            TestSessionBean initial = sessionContext.get(bean, creationalContext);
            assertNotNull(initial);
            initial.setId(42);

            SyringeContextsImpl contexts = new SyringeContextsImpl();
            contexts.setInactive(sessionContext);
            assertThrows(ContextNotActiveException.class, () -> sessionContext.get(bean));

            contexts.setActive(sessionContext);
            assertTrue(sessionContext.isActive());
            TestSessionBean restored = sessionContext.get(bean);
            assertNotNull(restored);
            assertEquals(42, restored.getId());

            contexts.destroyContext(sessionContext);
            contexts.setActive(sessionContext);
            assertNull(sessionContext.get(bean));
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    @RequestScoped
    static class TestRequestBean implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    @SessionScoped
    static class TestSessionBean implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;

        int getId() {
            return id;
        }

        void setId(int id) {
            this.id = id;
        }
    }
}
