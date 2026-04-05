package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.veto;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VetoTest {

    @Test
    void testBeanVetoed() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(VetoingExtension.class.getName());
        syringe.initialize();
        syringe.addDiscoveredClass(Field.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Wheat.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Flower.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Factory.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Car.class, BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);

        try {
            syringe.start();
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(0, beanManager.getBeans(Field.class).size());
            assertEquals(0, beanManager.getBeans(Wheat.class).size());
            assertEquals(1, beanManager.getBeans(Flower.class).size());
            assertEquals(1, beanManager.getBeans(Factory.class).size());
            assertEquals(0, beanManager.getBeans(Car.class).size());
        } finally {
            syringe.shutdown();
        }
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
