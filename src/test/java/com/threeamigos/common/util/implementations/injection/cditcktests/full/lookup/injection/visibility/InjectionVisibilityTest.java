package com.threeamigos.common.util.implementations.injection.cditcktests.full.lookup.injection.visibility;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class InjectionVisibilityTest {

    @Test
    void testPackagePrivateSetMethodInjection() {
        AbstractBean.fooSetCalled = false;

        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                SimpleSessionBean.class,
                Foo.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);

        BeanManagerImpl beanManager = null;
        try {
            syringe.setup();
            beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateSession("injection-visibility-session");

            Bean<SimpleSessionBean> bean = getBean(beanManager, SimpleSessionBean.class);
            assertNotNull(bean.getInjectionPoints());
            assertEquals(1, bean.getInjectionPoints().size());

            SimpleSessionBean simpleSessionBean = getContextualReference(beanManager, SimpleSessionBean.class);
            simpleSessionBean.simpleMethod();
            assertTrue(AbstractBean.fooSetCalled);
        } finally {
            if (beanManager != null) {
                beanManager.getContextManager().deactivateSession();
            }
            syringe.shutdown();
        }
    }

    private <T> Bean<T> getBean(BeanManager beanManager, Class<T> type) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Set<Bean<?>> beans = (Set) beanManager.getBeans(type);
        @SuppressWarnings("unchecked")
        Bean<T> bean = (Bean<T>) beanManager.resolve(beans);
        return bean;
    }

    private <T> T getContextualReference(BeanManager beanManager, Class<T> type) {
        Bean<T> bean = getBean(beanManager, type);
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        return type.cast(beanManager.getReference(bean, type, creationalContext));
    }
}
