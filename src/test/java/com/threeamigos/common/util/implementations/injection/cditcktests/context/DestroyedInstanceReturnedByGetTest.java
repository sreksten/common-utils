package com.threeamigos.common.util.implementations.injection.cditcktests.context;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.destroyedinstancereturnedbygettest.testdestroyedinstancemustnotbereturnedbyget.MyRequestBean;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DestroyedInstanceReturnedByGetTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.context.destroyedinstancereturnedbygettest.testdestroyedinstancemustnotbereturnedbyget";

    @Test
    void testDestroyedInstanceMustNotBeReturnedByGet() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Context requestContext = beanManager.getContext(RequestScoped.class);
            Bean<MyRequestBean> myRequestBean = resolveBean(beanManager, MyRequestBean.class);

            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                CreationalContext<MyRequestBean> creationalContext = beanManager.createCreationalContext(myRequestBean);
                MyRequestBean beanInstance = myRequestBean.create(creationalContext);
                assertNotNull(beanInstance);
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }

            boolean reactivatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                MyRequestBean beanInstance = requestContext.get(myRequestBean);
                assertNull(beanInstance);
            } finally {
                if (reactivatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
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
}
