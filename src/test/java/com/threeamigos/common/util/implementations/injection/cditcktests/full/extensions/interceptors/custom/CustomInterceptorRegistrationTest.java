package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.interceptors.custom;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CustomInterceptorRegistrationTest {

    @Test
    void testCustomInterceptorRegistration() {
        Syringe syringe = createSyringe();
        try {
            assertDoesNotThrow(syringe::setup);
        } finally {
            safeShutdown(syringe);
        }
    }

    private Syringe createSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), CustomInterceptorRegistrationTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(CustomInterceptorExtension.class.getName());
        addBeansXmlConfiguration(syringe);
        return syringe;
    }

    private void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "<interceptors><class>" + FooInterceptor.class.getName() + "</class></interceptors>" +
                "</beans>";

        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    private void safeShutdown(Syringe syringe) {
        if (syringe == null) {
            return;
        }
        try {
            syringe.shutdown();
        } catch (Exception ignored) {
            // Container may not have completed startup.
        }
    }
}
