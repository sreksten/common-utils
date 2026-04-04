package com.threeamigos.common.util.implementations.injection.cditcktests.full.alternative.broken.not.alternative;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassIsNotAlternativeTest {

    @Test
    void test() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Cat.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addBeansXmlSelectingNonAlternativeClass(syringe);

        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    private void addBeansXmlSelectingNonAlternativeClass(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<alternatives>" +
                "<class>" + Cat.class.getName() + "</class>" +
                "</alternatives>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @RequestScoped
    @Named
    static class Cat {
    }
}
