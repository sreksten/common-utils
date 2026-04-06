package com.threeamigos.common.util.implementations.injection.cditcktests.full.lookup.typesafe.resolution.decorator;

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

class DecoratorNotInjectedTest {

    @Test
    void testDecoratorNotResolved() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Animal.class,
                AnimalDecorator.class,
                Cat.class,
                House.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBeansXmlConfiguration(beansXmlWithDecorators(AnimalDecorator.class));

        try {
            assertThrows(DeploymentException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    private BeansXml beansXmlWithDecorators(Class<?>... decorators) {
        StringBuilder xml = new StringBuilder();
        xml.append("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" bean-discovery-mode=\"all\" version=\"4.0\">");
        xml.append("<decorators>");
        for (Class<?> decorator : decorators) {
            xml.append("<class>").append(decorator.getName()).append("</class>");
        }
        xml.append("</decorators>");
        xml.append("</beans>");
        BeansXmlParser parser = new BeansXmlParser();
        return parser.parse(new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8)));
    }
}
