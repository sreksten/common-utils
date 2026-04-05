package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.observer.priority;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

class ExtensionObserverOrderingTest {

    private static final String[] NUMBERS = new String[]{"1", "2", "3", "4", "5", "6", "7"};

    @Test
    void testEventOrdering() {
        ActionSequence.reset();

        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                TestBean.class,
                TestExtension01.class,
                TestExtension02.class,
                TestExtension03.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(TestExtension01.class.getName());
        syringe.addExtension(TestExtension02.class.getName());
        syringe.addExtension(TestExtension03.class.getName());
        addBeansXmlConfiguration(syringe);

        try {
            syringe.setup();
            ActionSequence.assertSequenceDataEquals(NUMBERS);
        } finally {
            syringe.shutdown();
        }
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" "
                + "version=\"3.0\" bean-discovery-mode=\"all\"></beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
