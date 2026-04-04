package com.threeamigos.common.util.implementations.injection.cditcktests.context;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.getoninactivecontexttest.testinvokinggetoninactivecontextfails.MyRequestBean;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetOnInactiveContextTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.context.getoninactivecontexttest.testinvokinggetoninactivecontextfails";

    @Test
    void testInvokingGetOnInactiveContextFails() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            syringe.activateRequestContextIfNeeded();
            Context requestContext = beanManager.getContext(RequestScoped.class);
            assertTrue(requestContext.isActive());

            syringe.deactivateRequestContextIfActive();
            Contextual<MyRequestBean> myRequestBean = resolveBean(beanManager, MyRequestBean.class);
            assertThrows(ContextNotActiveException.class, () -> requestContext.get(myRequestBean));
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
