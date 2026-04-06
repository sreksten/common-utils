package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.aroundInvoke;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AroundInvokeAccessInterceptorTest {

    @Test
    void testPrivateAroundInvokeInterceptor() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertEquals(1, getContextualReference(syringe, SimpleBean.class).zero());
            assertEquals(1, getContextualReference(syringe, Bean3.class).zero());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProtectedAroundInvokeInterceptor() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertEquals(2, getContextualReference(syringe, SimpleBean.class).one());
            assertEquals(1, getContextualReference(syringe, Bean1.class).zero());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPackagePrivateAroundInvokeInterceptor() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertEquals(3, getContextualReference(syringe, SimpleBean.class).two());
            assertEquals(1, getContextualReference(syringe, Bean2.class).zero());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bean1.class,
                Bean2.class,
                Bean3.class,
                PackagePrivateBinding.class,
                PackagePrivateInterceptor.class,
                PrivateBinding.class,
                PrivateInterceptor.class,
                ProtectedBinding.class,
                ProtectedInterceptor.class,
                SimpleBean.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }
}
