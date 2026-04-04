package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.broken.decorator.field;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DecoratorWithNonPassivatingInjectedFieldTest {

    @Test
    void testPassivationCapableBeanWithNonPassivatingDecoratorInjectedFieldFails() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                CityDecorator.class,
                CityInterface.class,
                NonPassivating.class,
                UnderwaterCity.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addDecoratorBeansXml(syringe, CityDecorator.class);
        try {
            assertThrows(DeploymentException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    private void addDecoratorBeansXml(Syringe syringe, Class<?> decoratorClass) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "<decorators><class>" + decoratorClass.getName() + "</class></decorators>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
