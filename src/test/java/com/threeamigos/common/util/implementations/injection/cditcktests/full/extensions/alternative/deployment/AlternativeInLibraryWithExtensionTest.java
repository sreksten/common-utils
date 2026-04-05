package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.alternative.deployment;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlternativeInLibraryWithExtensionTest {

    @Test
    void testAlternative() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Foo.class,
                Bar.class,
                BarAlternative.class,
                NoopExtension.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(NoopExtension.class.getName());
        addBeansXmlSelectingAlternative(syringe);

        try {
            syringe.setup();
            Foo foo = syringe.inject(Foo.class);
            assertEquals("barAlternative", foo.getBar().ping());
        } finally {
            syringe.shutdown();
        }
    }

    private static void addBeansXmlSelectingAlternative(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<alternatives>" +
                "<class>" + BarAlternative.class.getName() + "</class>" +
                "</alternatives>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
