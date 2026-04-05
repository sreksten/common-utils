package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.interceptor;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Isolated
class InterceptorProcessBeanAttributesTest {

    private static Syringe syringe;

    @BeforeAll
    static void setUp() {
        VerifyingExtension.reset();

        syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(VerifyingExtension.class.getName());
        syringe.initialize();
        syringe.addDiscoveredClass(AlphaInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AlphaInterceptorBinding.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BravoInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BravoInterceptorBinding.class, BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);
        syringe.start();
    }

    @AfterAll
    static void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testAlphaInterceptorObserved() {
        assertEquals(1, VerifyingExtension.aplhaInterceptorObserved.get());
    }

    @Test
    void testBravoInterceptorObserved() {
        assertEquals(1, VerifyingExtension.bravoInterceptorObserved.get());
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<interceptors>" +
                "<class>" + AlphaInterceptor.class.getName() + "</class>" +
                "<class>" + BravoInterceptor.class.getName() + "</class>" +
                "</interceptors>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
