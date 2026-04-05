package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.lib.Bar;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.lib.Baz;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.lib.Boss;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.lib.Foo;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.lib.Pro;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AfterTypeDiscoveryTest {

    private Syringe syringe;
    private BeanManager beanManager;
    private AfterTypeDiscoveryObserver extension;
    private RequestContextController requestContextController;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(new InMemoryMessageHandler(), AfterTypeDiscoveryTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterTypeDiscoveryObserver.class.getName());
        addBeansXmlConfiguration(syringe);
        syringe.setup();

        beanManager = syringe.getBeanManager();
        extension = AfterTypeDiscoveryObserver.getInstance();
        assertNotNull(extension);
        requestContextController = beanManager.createInstance().select(RequestContextController.class).get();
        requestContextController.activate();
    }

    @AfterAll
    void tearDown() {
        if (requestContextController != null) {
            try {
                requestContextController.deactivate();
            } catch (Exception ignored) {
                // Context may already be inactive.
            }
        }
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testInitialInterceptors() {
        assertTrue(extension.getInterceptors().contains(BravoInterceptor.class));
        assertTrue(extension.getInterceptors().contains(AlphaInterceptor.class));
        assertTrue(extension.getInterceptors().contains(DeltaInterceptor.class));
        assertTrue(extension.getInterceptors().contains(EchoInterceptor.class));
    }

    @Test
    void testInitialAlternatives() {
        assertTrue(extension.getAlternatives().size() >= 3);
        List<Class<?>> alternatives = extension.getAlternatives();

        Integer alphaAltIndex = null;
        Integer echoAltIndex = null;
        Integer deltaAltIndex = null;

        for (int i = 0; i < alternatives.size(); i++) {
            if (alternatives.get(i).equals(AlphaAlternative.class)) {
                alphaAltIndex = i;
            }
            if (alternatives.get(i).equals(EchoAlternative.class)) {
                echoAltIndex = i;
            }
            if (alternatives.get(i).equals(DeltaAlternative.class)) {
                deltaAltIndex = i;
            }
        }

        assertNotNull(alphaAltIndex);
        assertNotNull(echoAltIndex);
        assertNotNull(deltaAltIndex);
        assertTrue(alphaAltIndex < echoAltIndex);
        assertTrue(echoAltIndex < deltaAltIndex);
    }

    @Test
    void testInitialDecorators() {
        assertTrue(extension.getDecorators().size() >= 4);
        List<Class<?>> decorators = extension.getDecorators();

        Integer alphaDecIndex = null;
        Integer bravoDecIndex = null;
        Integer echoDecIndex = null;
        Integer deltaDecIndex = null;

        for (int i = 0; i < decorators.size(); i++) {
            if (decorators.get(i).equals(AlphaDecorator.class)) {
                alphaDecIndex = i;
            }
            if (decorators.get(i).equals(BravoDecorator.class)) {
                bravoDecIndex = i;
            }
            if (decorators.get(i).equals(EchoDecorator.class)) {
                echoDecIndex = i;
            }
            if (decorators.get(i).equals(DeltaDecorator.class)) {
                deltaDecIndex = i;
            }
        }

        assertNotNull(alphaDecIndex);
        assertNotNull(bravoDecIndex);
        assertNotNull(echoDecIndex);
        assertNotNull(deltaDecIndex);
        assertTrue(alphaDecIndex < bravoDecIndex);
        assertTrue(bravoDecIndex < echoDecIndex);
        assertTrue(echoDecIndex < deltaDecIndex);
    }

    @Test
    void testFinalInterceptors() {
        TransactionLogger logger = beanManager.createInstance().select(TransactionLogger.class).get();

        AlphaInterceptor.reset();
        BravoInterceptor.reset();
        CharlieInterceptor.reset();
        DeltaInterceptor.reset();
        EchoInterceptor.reset();

        logger.ping();

        assertTrue(AlphaInterceptor.isIntercepted());
        assertFalse(BravoInterceptor.isIntercepted());
        assertTrue(CharlieInterceptor.isIntercepted());
        assertTrue(DeltaInterceptor.isIntercepted());
        assertFalse(EchoInterceptor.isIntercepted());
    }

    @Test
    void testFinalDecorators() {
        TransactionLogger logger = beanManager.createInstance().select(TransactionLogger.class).get();
        assertEquals("pingdeltabravoalphacharlie", logger.log("ping"));
    }

    @Test
    void testFinalAlternatives() {
        TransactionLogger logger = beanManager.createInstance().select(TransactionLogger.class).get();
        assertEquals(DeltaAlternative.class, logger.getAlternativeClass());
        assertTrue(getBeans(AlphaAlternative.class).isEmpty());
        assertTrue(getBeans(EchoAlternative.class).isEmpty());
    }

    @Test
    void testAddAnnotatedType() {
        assertTrue(extension.isBossObserved());
        getUniqueBean(Boss.class);

        assertEquals(0, getBeans(Bar.class).size());
        assertEquals(1, getBeans(Bar.class, Pro.ProLiteral.INSTANCE).size());

        assertEquals(0, getBeans(Foo.class).size());
        assertEquals(1, getBeans(Foo.class, Pro.ProLiteral.INSTANCE).size());
    }

    @Test
    void testAddAnnotatedTypeWithConfigurator() {
        Bean<Baz> bazBean = getUniqueBean(Baz.class, Pro.ProLiteral.INSTANCE);
        assertEquals(RequestScoped.class, bazBean.getScope());

        Baz baz = (Baz) beanManager.getReference(bazBean, Baz.class,
                beanManager.createCreationalContext(bazBean));
        assertFalse(baz.getBarInstance().isUnsatisfied());
    }

    @Test
    void testProcessProducerEventFiredForProducerField() {
        assertTrue(extension.isProcessProcuderFieldObserved());
    }

    @Test
    void testProcessProducerEventFiredForProducerMethod() {
        assertTrue(extension.isProcessProcuderMethodObserved());
    }

    private void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<interceptors><class>" + CharlieInterceptor.class.getName() + "</class></interceptors>" +
                "<decorators><class>" + CharlieDecorator.class.getName() + "</class></decorators>" +
                "<alternatives><class>" + CharlieAlternative.class.getName() + "</class></alternatives>" +
                "</beans>";

        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @SuppressWarnings("unchecked")
    private <T> Bean<T> getUniqueBean(Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        assertEquals(1, beans.size());
        return (Bean<T>) beans.iterator().next();
    }

    private Set<Bean<?>> getBeans(Class<?> type, Annotation... qualifiers) {
        return beanManager.getBeans(type, qualifiers);
    }
}
