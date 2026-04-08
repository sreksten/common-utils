package com.threeamigos.common.util.implementations.injection.cditcktests.deployment.discovery;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.wildfly.SyringeBootstrap;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    @Test
    void testManagedBootstrapWithEmptyModernBeansXmlUsesAnnotatedDiscovery() {
        Set<Class<?>> discoveredClasses = new HashSet<Class<?>>();
        discoveredClasses.add(SomeAnnotatedBean.class);
        discoveredClasses.add(SomeUnannotatedBean.class);

        String beansXmlContent = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"></beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(beansXmlContent.getBytes(StandardCharsets.UTF_8)));

        SyringeBootstrap bootstrap = new SyringeBootstrap(
                discoveredClasses,
                Thread.currentThread().getContextClassLoader(),
                Collections.singletonList(beansXml));

        Syringe syringe = bootstrap.bootstrap();
        try {
            Instance<Object> instance = syringe.getBeanManager().createInstance();
            assertTrue(instance.select(SomeAnnotatedBean.class).isResolvable());
            assertFalse(instance.select(SomeUnannotatedBean.class).isResolvable());
        } finally {
            bootstrap.shutdown();
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
