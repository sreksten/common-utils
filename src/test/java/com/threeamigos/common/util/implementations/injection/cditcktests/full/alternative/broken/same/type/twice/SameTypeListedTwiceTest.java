package com.threeamigos.common.util.implementations.injection.cditcktests.full.alternative.broken.same.type.twice;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SameTypeListedTwiceTest {

    @Test
    void test() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Dog.class, Cat.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addBeansXmlWithDuplicateAlternativeClass(syringe);

        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    private void addBeansXmlWithDuplicateAlternativeClass(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<alternatives>" +
                "<class>" + Dog.class.getName() + "</class>" +
                "<class>" + Cat.class.getName() + "</class>" +
                "<class>" + Cat.class.getName() + "</class>" +
                "</alternatives>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @RequestScoped
    @Alternative
    static class Cat {
    }

    @RequestScoped
    @Named
    @Alternative
    static class Dog {
    }
}
