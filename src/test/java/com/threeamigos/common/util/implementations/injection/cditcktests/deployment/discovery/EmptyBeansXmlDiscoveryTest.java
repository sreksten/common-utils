package com.threeamigos.common.util.implementations.injection.cditcktests.deployment.discovery;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmptyBeansXmlDiscoveryTest {

    @Test
    void testBeanArchiveWithEmptyBeansXml() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), SomeAnnotatedBean.class, SomeUnannotatedBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.IMPLICIT);
        syringe.setup();
        try {
            Instance<Object> instance = syringe.getBeanManager().createInstance();
            Instance<SomeAnnotatedBean> annotatedBeanInstance = instance.select(SomeAnnotatedBean.class);
            assertTrue(annotatedBeanInstance.isResolvable());
            annotatedBeanInstance.get().pong();

            Instance<SomeUnannotatedBean> unannotatedBeanInstance = instance.select(SomeUnannotatedBean.class);
            assertFalse(unannotatedBeanInstance.isResolvable());
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    public static class SomeAnnotatedBean {
        public void pong() {
        }
    }

    public static class SomeUnannotatedBean {
    }
}
