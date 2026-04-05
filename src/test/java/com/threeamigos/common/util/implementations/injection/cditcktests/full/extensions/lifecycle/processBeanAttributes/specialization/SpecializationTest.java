package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.specialization;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.BeanAttributes;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecializationTest {

    @Test
    void testProcessBeanAttributesFiredProperlyForSpecializedBean() {
        VerifyingExtension extension = new VerifyingExtension();
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(extension);
        syringe.initialize();
        syringe.addDiscoveredClass(Alpha.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bravo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Charlie.class, BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);

        try {
            syringe.start();

            assertNull(extension.getAlpha());
            assertNull(extension.getBravo());

            BeanAttributes<Charlie> charlieAttributes = extension.getCharlie();
            assertNotNull(charlieAttributes);
            assertTrue(annotationSetMatches(charlieAttributes.getQualifiers(),
                    Foo.Literal.INSTANCE,
                    Bar.Literal.INSTANCE,
                    Baz.Literal.INSTANCE,
                    Any.Literal.INSTANCE,
                    NamedLiteral.of("alpha")));
            assertEquals("alpha", charlieAttributes.getName());
        } finally {
            syringe.shutdown();
        }
    }

    private static boolean annotationSetMatches(Set<Annotation> actual, Annotation... expected) {
        return new HashSet<Annotation>(actual).equals(new HashSet<Annotation>(Arrays.asList(expected)));
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
