package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.communication;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExtensionsCommunicationTest {

    private Syringe syringe;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                ActionSequence.class,
                Bar.class,
                Baz.class,
                EventBase.class,
                ExtensionAlpha.class,
                ExtensionBeta.class,
                Foo.class,
                PatEvent.class,
                PbEvent.class
        );
        addAllDiscoveryBeansXml(syringe);
        syringe.addExtension(ExtensionAlpha.class.getName());
        syringe.addExtension(ExtensionBeta.class.getName());
        syringe.setup();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testEvents() {
        ActionSequence patSeq = ActionSequence.getSequence(EventBase.PAT_SEQ);
        assertNotNull(patSeq);
        patSeq.assertDataContainsAll(
                Foo.class.getName(),
                Bar.class.getName(),
                EventBase.class.getName(),
                PatEvent.class.getName(),
                PbEvent.class.getName(),
                ExtensionAlpha.class.getName(),
                ExtensionBeta.class.getName()
        );

        ActionSequence pbSeq = ActionSequence.getSequence(EventBase.PB_SEQ);
        assertNotNull(pbSeq);
        assertEquals(2, pbSeq.getData().size());
        pbSeq.assertDataContainsAll(Foo.class.getName(), Bar.class.getName());
    }

    private static void addAllDiscoveryBeansXml(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\"></beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
