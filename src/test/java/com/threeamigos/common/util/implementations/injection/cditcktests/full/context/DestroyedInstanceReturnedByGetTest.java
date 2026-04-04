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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DestroyedInstanceReturnedByGetTest {

    @Test
    void testDestroyedInstanceMustNotBeReturnedByGet() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), MySessionBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            Set<Bean<?>> beans = beanManager.getBeans(MySessionBean.class);
            assertEquals(1, beans.size());

            Bean<MySessionBean> mySessionBean = resolveBean(beanManager, MySessionBean.class);
            CreationalContext<MySessionBean> sessionCreationalContext = beanManager.createCreationalContext(mySessionBean);
            MySessionBean beanInstance = mySessionBean.create(sessionCreationalContext);
            assertNotNull(beanInstance);

            Context sessionContext = beanManager.getContext(SessionScoped.class);
            String sessionId = "full-context-destroyed-instance";
            beanManager.getContextManager().activateSession(sessionId);
            try {
                beanManager.getContextManager().invalidateSession(sessionId);
                beanManager.getContextManager().activateSession(sessionId);
                beanInstance = sessionContext.get(mySessionBean);
                assertNull(beanInstance);
            } finally {
                beanManager.getContextManager().invalidateSession(sessionId);
            }
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    @SessionScoped
    public static class MySessionBean implements Serializable {
        private static final long serialVersionUID = 1L;

        private int id;

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void ping() {
        }
    }
}
