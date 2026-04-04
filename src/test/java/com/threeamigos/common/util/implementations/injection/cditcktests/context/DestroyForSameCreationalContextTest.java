package com.threeamigos.common.util.implementations.injection.cditcktests.context;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.destroyforsamecreationalcontexttest.testdestroyforsamecreationalcontextonly.AnotherRequestBean;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DestroyForSameCreationalContextTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.context.destroyforsamecreationalcontexttest.testdestroyforsamecreationalcontextonly";

    @Test
    void testDestroyForSameCreationalContextOnly() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Context requestContext = beanManager.getContext(RequestScoped.class);
            Bean<AnotherRequestBean> requestBean = resolveBean(beanManager, AnotherRequestBean.class);

            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            InspectableCreationalContext<AnotherRequestBean> creationalContext =
                    new InspectableCreationalContext<AnotherRequestBean>(beanManager.createCreationalContext(requestBean));
            try {
                AnotherRequestBean instance = requestContext.get(requestBean, creationalContext);
                instance.ping();
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
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
}
