package com.threeamigos.common.util.implementations.injection.cditcktests.full.deployment.exclude;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.deployment.exclude.food.Meat;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.deployment.exclude.haircut.Chonmage;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.deployment.exclude.mustache.Mustache;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.deployment.exclude.mustache.beard.Beard;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcludeFiltersTest {

    private static final String EXCLUDE_DUMMY_KEY = "cdiTckExcludeDummy";
    private static final String EXCLUDE_DUMMY_VALUE = "true";

    @Test
    void testTypeFcqnMatchesExcludeFilterName() {
        Fixture fixture = startFixture();
        try {
            assertTypeIsExcluded(fixture, Stubble.class);
            assertTypeIsNotExcluded(fixture, Golf.class);
        } finally {
            fixture.syringe.shutdown();
        }
    }

    @Test
    void testTypePackageMatchesExcludeFilterName() {
        Fixture fixture = startFixture();
        try {
            assertTypeIsExcluded(fixture, Mustache.class);
            assertTypeIsExcluded(fixture, Beard.class);
            assertTypeIsExcluded(fixture, Chonmage.class);
        } finally {
            fixture.syringe.shutdown();
        }
    }

    @Test
    void testExcludeClassActivators() {
        Fixture fixture = startFixture();
        try {
            assertTypeIsExcluded(fixture, Alpha.class);
            assertTypeIsNotExcluded(fixture, Foxtrot.class);
            assertTypeIsExcluded(fixture, Bravo.class);
            assertTypeIsNotExcluded(fixture, Echo.class);
            assertTypeIsNotExcluded(fixture, Meat.class);
        } finally {
            fixture.syringe.shutdown();
        }
    }

    @Test
    void testExcludeSystemPropertyActivator() {
        String previous = System.getProperty(EXCLUDE_DUMMY_KEY);
        System.setProperty(EXCLUDE_DUMMY_KEY, EXCLUDE_DUMMY_VALUE);

        Fixture fixture = startFixture();
        try {
            assertTypeIsExcluded(fixture, Charlie.class);
            assertTypeIsExcluded(fixture, Delta.class);
        } finally {
            fixture.syringe.shutdown();
            restoreProperty(EXCLUDE_DUMMY_KEY, previous);
        }
    }

    private Fixture startFixture() {
        VerifyingExtension extension = new VerifyingExtension();
        Syringe syringe = new Syringe(
                "com.threeamigos.common.util.implementations.injection.cditcktests.full.deployment.exclude"
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(extension);
        syringe.addBeansXmlConfiguration(createExcludeBeansXml());
        syringe.setup();

        return new Fixture(syringe, extension);
    }

    private BeansXml createExcludeBeansXml() {
        String meatPackage = Meat.class.getPackage().getName();
        String mustachePackage = Mustache.class.getPackage().getName();
        String chonmagePackage = Chonmage.class.getPackage().getName();

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "<scan>" +
                "<exclude name=\"" + chonmagePackage + ".*\"/>" +
                "<exclude name=\"" + mustachePackage + ".**\"/>" +
                "<exclude name=\"" + meatPackage + ".*\"><if-class-available name=\"com.some.unreal.class.Name\"/></exclude>" +
                "<exclude name=\"" + meatPackage + ".*\"><if-class-not-available name=\"" + ExcludeFiltersTest.class.getName() + "\"/></exclude>" +
                "<exclude name=\"" + Alpha.class.getName() + "\"><if-class-available name=\"" + Stubble.class.getName() + "\"/></exclude>" +
                "<exclude name=\"" + Stubble.class.getName() + "\"/>" +
                "<exclude name=\"" + Foxtrot.class.getName() + "\"><if-class-available name=\"com.some.unreal.class.Name\"/></exclude>" +
                "<exclude name=\"" + Bravo.class.getName() + "\"><if-class-not-available name=\"com.some.unreal.class.Name\"/></exclude>" +
                "<exclude name=\"" + Echo.class.getName() + "\"><if-class-not-available name=\"" + ExcludeFiltersTest.class.getName() + "\"/></exclude>" +
                "<exclude name=\"" + Charlie.class.getName() + "\"><if-system-property name=\"" + EXCLUDE_DUMMY_KEY + "\"/></exclude>" +
                "<exclude name=\"" + Delta.class.getName() + "\"><if-system-property name=\"" + EXCLUDE_DUMMY_KEY + "\" value=\"" + EXCLUDE_DUMMY_VALUE + "\"/></exclude>" +
                "</scan>" +
                "</beans>";

        return new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private void assertTypeIsExcluded(Fixture fixture, Class<?> type) {
        BeanManager beanManager = fixture.syringe.getBeanManager();
        assertTrue(beanManager.getBeans(type).isEmpty());
        assertFalse(fixture.extension.getObservedAnnotatedTypes().contains(type));
    }

    private void assertTypeIsNotExcluded(Fixture fixture, Class<?> type) {
        BeanManager beanManager = fixture.syringe.getBeanManager();
        assertFalse(beanManager.getBeans(type).isEmpty());
        assertTrue(fixture.extension.getObservedAnnotatedTypes().contains(type));
    }

    private void restoreProperty(String key, String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }

    private static class Fixture {
        private final Syringe syringe;
        private final VerifyingExtension extension;

        private Fixture(Syringe syringe, VerifyingExtension extension) {
            this.syringe = syringe;
            this.extension = extension;
        }
    }
}
