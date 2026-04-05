package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.synthetic;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessBeanAttributesNotFiredForSyntheticBeanTest {

    @Test
    void testProcessBeanAttributesNotFired() {
        BicycleExtension bicycleExtension = new BicycleExtension();
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(bicycleExtension);
        syringe.initialize();
        syringe.addDiscoveredClass(Bicycle.class, BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);

        try {
            syringe.start();

            assertTrue(bicycleExtension.isVetoed());

            BeanAttributes<Bicycle> attributesBeforeRegistering = bicycleExtension.getBicycleAttributesBeforeRegistering();
            assertEquals(ApplicationScoped.class, attributesBeforeRegistering.getScope());
            assertTrue(typeSetMatches(attributesBeforeRegistering.getTypes(), Object.class, Vehicle.class, Bicycle.class));
            assertFalse(attributesBeforeRegistering.isAlternative());

            assertNull(bicycleExtension.getBicycleAttributesBeforeModifying());

            @SuppressWarnings({"unchecked", "rawtypes"})
            Set<Bean<Bicycle>> beans = (Set) syringe.getBeanManager().getBeans(Bicycle.class, Any.Literal.INSTANCE);
            assertEquals(1, beans.size());
            Bean<Bicycle> bean = beans.iterator().next();
            assertEquals(ApplicationScoped.class, bean.getScope());
            assertTrue(typeSetMatches(bean.getTypes(), Object.class, Vehicle.class, Bicycle.class));
            assertFalse(bean.isAlternative());
        } finally {
            syringe.shutdown();
        }
    }

    private static boolean typeSetMatches(Set<?> actual, Object... expected) {
        return new HashSet<Object>(actual).equals(new HashSet<Object>(Arrays.asList(expected)));
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
