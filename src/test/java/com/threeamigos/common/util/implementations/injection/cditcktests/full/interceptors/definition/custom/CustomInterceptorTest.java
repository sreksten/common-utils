package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.definition.custom;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InterceptionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static jakarta.enterprise.inject.spi.InterceptionType.AROUND_INVOKE;
import static jakarta.enterprise.inject.spi.InterceptionType.AROUND_TIMEOUT;
import static jakarta.enterprise.inject.spi.InterceptionType.POST_ACTIVATE;
import static jakarta.enterprise.inject.spi.InterceptionType.POST_CONSTRUCT;
import static jakarta.enterprise.inject.spi.InterceptionType.PRE_DESTROY;
import static jakarta.enterprise.inject.spi.InterceptionType.PRE_PASSIVATE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class CustomInterceptorTest {

    @Test
    void testCustomPostConstructInterceptor() {
        assertCustomInterceptorResolved(POST_CONSTRUCT, AfterBeanDiscoveryObserver.POST_CONSTRUCT_INTERCEPTOR);
    }

    @Test
    void testCustomPreDestroyInterceptor() {
        assertCustomInterceptorResolved(PRE_DESTROY, AfterBeanDiscoveryObserver.PRE_DESTROY_INTERCEPTOR);
    }

    @Test
    void testCustomPostActivateInterceptor() {
        assertCustomInterceptorResolved(POST_ACTIVATE, AfterBeanDiscoveryObserver.POST_ACTIVATE_INTERCEPTOR);
    }

    @Test
    void testCustomPrePassivateInterceptor() {
        assertCustomInterceptorResolved(PRE_PASSIVATE, AfterBeanDiscoveryObserver.PRE_PASSIVATE_INTERCEPTOR);
    }

    @Test
    void testCustomAroundInvokeInterceptor() {
        assertCustomInterceptorResolved(AROUND_INVOKE, AfterBeanDiscoveryObserver.AROUND_INVOKE_INTERCEPTOR);
    }

    @Test
    void testCustomAroundTimeoutInterceptor() {
        assertCustomInterceptorResolved(AROUND_TIMEOUT, AfterBeanDiscoveryObserver.AROUND_TIMEOUT_INTERCEPTOR);
    }

    private static void assertCustomInterceptorResolved(InterceptionType interceptionType,
                                                        CustomInterceptorImplementation interceptor) {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AfterBeanDiscoveryObserver.class,
                CustomInterceptorImplementation.class,
                Secure.class,
                SecureLiteral.class,
                SimpleInterceptorWithoutAnnotations.class,
                Transactional.class,
                TransactionalLiteral.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterBeanDiscoveryObserver.class.getName());
        addBeansXmlConfiguration(syringe);

        try {
            syringe.setup();

            BeanManager beanManager = syringe.getBeanManager();
            assertFalse(beanManager
                    .resolveInterceptors(interceptionType, new SecureLiteral(), new TransactionalLiteral())
                    .isEmpty());
            assertTrue(interceptor.isGetInterceptorBindingsCalled());
            assertTrue(interceptor.isInterceptsCalled());
        } finally {
            syringe.shutdown();
        }
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" "
                + "version=\"3.0\" bean-discovery-mode=\"all\">"
                + "<interceptors>"
                + "<class>" + SimpleInterceptorWithoutAnnotations.class.getName() + "</class>"
                + "</interceptors>"
                + "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
