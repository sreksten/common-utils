package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.decorator;

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
class DecoratorProcessBeanAttributesTest {

    private static Syringe syringe;

    @BeforeAll
    static void setUp() {
        VerifyingExtension.reset();

        syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(VerifyingExtension.class.getName());
        syringe.initialize();
        syringe.addDiscoveredClass(AlphaDecorator.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BravoDecorator.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Charlie.class, BeanArchiveMode.EXPLICIT);
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
    void testAlphaDecoratorObserved() {
        assertEquals(1, VerifyingExtension.aplhaDecoratorObserved.get());
    }

    @Test
    void testBravoDecoratorObserved() {
        assertEquals(1, VerifyingExtension.bravoDecoratorObserved.get());
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<decorators>" +
                "<class>" + AlphaDecorator.class.getName() + "</class>" +
                "<class>" + BravoDecorator.class.getName() + "</class>" +
                "</decorators>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
