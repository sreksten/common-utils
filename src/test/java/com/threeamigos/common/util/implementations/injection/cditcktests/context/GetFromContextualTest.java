package com.threeamigos.common.util.implementations.injection.cditcktests.context;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.getfromcontextualtest.testgetmaynotcreatenewinstanceunlesscreationalcontextgiven.MyApplicationBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.getfromcontextualtest.testgetmaynotcreatenewinstanceunlesscreationalcontextgiven.MyRequestBean;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GetFromContextualTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.context.getfromcontextualtest.testgetmaynotcreatenewinstanceunlesscreationalcontextgiven";

    @Test
    void testGetMayNotCreateNewInstanceUnlessCreationalContextGiven() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                Contextual<MyRequestBean> myRequestBean = resolveBean(beanManager, MyRequestBean.class);
                assertNull(beanManager.getContext(RequestScoped.class).get(myRequestBean));

                Contextual<MyApplicationBean> myApplicationBean = resolveBean(beanManager, MyApplicationBean.class);
                assertNull(beanManager.getContext(ApplicationScoped.class).get(myApplicationBean));

                CreationalContext<MyRequestBean> myRequestCreationalContext =
                        beanManager.createCreationalContext(myRequestBean);
                assertNotNull(beanManager.getContext(RequestScoped.class).get(myRequestBean, myRequestCreationalContext));

                CreationalContext<MyApplicationBean> myApplicationCreationalContext =
                        beanManager.createCreationalContext(myApplicationBean);
                assertNotNull(beanManager.getContext(ApplicationScoped.class)
                        .get(myApplicationBean, myApplicationCreationalContext));
            } finally {
                if (activatedRequest) {
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
