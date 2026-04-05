package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.stereotype;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StereotypeExtensionTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Chair.class,
                StereotypeCandidate.class,
                StereotypeExtension.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(StereotypeExtension.class.getName());
        addBeansXmlConfiguration(syringe);
        syringe.setup();

        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testStereotypeWorks() {
        Bean<Chair> chairBean = getUniqueBean(Chair.class);
        assertEquals("chair", chairBean.getName());

        Chair instance = getContextualReference("chair", Chair.class);
        assertNotNull(instance);
        assertEquals(5, instance.breakUpToPieces());
    }

    @SuppressWarnings("unchecked")
    private <T> Bean<T> getUniqueBean(Class<T> type, java.lang.annotation.Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        Bean<?> resolved = beanManager.resolve(beans);
        assertNotNull(resolved, "No bean resolved for type " + type.getName());
        return (Bean<T>) resolved;
    }

    @SuppressWarnings("unchecked")
    private <T> T getContextualReference(String name, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(name));
        assertNotNull(bean, "No bean resolved for name " + name);
        return (T) beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
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
