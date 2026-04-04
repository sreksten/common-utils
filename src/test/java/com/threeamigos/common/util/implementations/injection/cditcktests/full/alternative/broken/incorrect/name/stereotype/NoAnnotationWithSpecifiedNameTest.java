package com.threeamigos.common.util.implementations.injection.cditcktests.full.alternative.broken.incorrect.name.stereotype;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NoAnnotationWithSpecifiedNameTest {

    @Test
    void test() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), DummyBean.class, DummyAnnotation.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addInvalidBeansXml(syringe);

        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    private void addInvalidBeansXml(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "<alternatives>" +
                "<stereotype>org.jboss.cdi.tck.tests.policy.broken.incorrect.name.stereotype.Mock</stereotype>" +
                "</alternatives>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @Dependent
    static class DummyBean {
    }

    @interface DummyAnnotation {
    }
}
