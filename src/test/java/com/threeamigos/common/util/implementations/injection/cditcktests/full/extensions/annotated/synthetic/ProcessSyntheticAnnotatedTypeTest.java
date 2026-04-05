package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.annotated.synthetic;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcessSyntheticAnnotatedTypeTest {

    private Syringe syringe;
    private BeanManager beanManager;
    private VerifyingExtension verifyingExtension;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Apple.class,
                Fresh.class,
                Fruit.class,
                Juicy.class,
                ModifyingExtension.class,
                Orange.class,
                Pear.class,
                Plants.class,
                RegisteringAnnotationExtension.class,
                RegisteringExtension1.class,
                RegisteringExtension2.class,
                RegisteringExtension3.class,
                TestAnnotation.class,
                Vegetables.class,
                VerifyingExtension.class
        );
        addAllDiscoveryBeansXml(syringe);
        syringe.addExtension(RegisteringAnnotationExtension.class.getName());
        syringe.addExtension(RegisteringExtension1.class.getName());
        syringe.addExtension(RegisteringExtension2.class.getName());
        syringe.addExtension(RegisteringExtension3.class.getName());
        syringe.addExtension(ModifyingExtension.class.getName());
        syringe.addExtension(VerifyingExtension.class.getName());
        syringe.setup();
        beanManager = syringe.getBeanManager();
        verifyingExtension = beanManager.getExtension(VerifyingExtension.class);
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testEventsFired() {
        Set<Class<?>> patClasses = verifyingExtension.getPatClasses();
        Set<Class<?>> psatClasses = verifyingExtension.getPsatClasses();

        assertTrue(psatClasses.contains(Orange.class));
        assertTrue(psatClasses.contains(Apple.class));
        assertTrue(psatClasses.contains(Pear.class));
        assertTrue(psatClasses.contains(Vegetables.class));
        // Also verify that PAT is fired for classes in a BDA
        assertTrue(patClasses.contains(Orange.class));
        assertTrue(patClasses.contains(Apple.class));
        assertTrue(patClasses.contains(Pear.class));

        // Verify that PAT is not fired for annotation type
        assertFalse(psatClasses.contains(TestAnnotation.class));
        assertFalse(psatClasses.contains(Juicy.class));

        // Test changes applied
        Set<Bean<?>> oranges = beanManager.getBeans(Orange.class, Any.Literal.INSTANCE);
        assertEquals(1, oranges.size());
        assertFalse(oranges.iterator().next().getQualifiers().contains(Juicy.Literal.INSTANCE));
        Set<Bean<?>> apples = beanManager.getBeans(Apple.class, Any.Literal.INSTANCE);
        assertEquals(2, apples.size());
        Set<Bean<?>> juicyApples = beanManager.getBeans(Apple.class, Juicy.Literal.INSTANCE);
        assertEquals(1, juicyApples.size());
        assertTrue(juicyApples.iterator().next().getQualifiers().contains(Fresh.Literal.INSTANCE));
        assertEquals(2, beanManager.getBeans(Pear.class, Any.Literal.INSTANCE).size());
        Set<Bean<?>> juicyPears = beanManager.getBeans(Pear.class, Juicy.Literal.INSTANCE);
        assertEquals(1, juicyPears.size());
        Set<Bean<?>> annotation = beanManager.getBeans(TestAnnotation.class, Any.Literal.INSTANCE);
        assertEquals(0, annotation.size());

    }

    @Test
    void testEventsSources() {
        Map<Class<?>, Extension> sources = verifyingExtension.getSources();
        assertTrue(sources.get(Apple.class) instanceof RegisteringExtension1);
        assertTrue(sources.get(Orange.class) instanceof RegisteringExtension1);
        assertTrue(sources.get(Pear.class) instanceof RegisteringExtension2);
    }

    private static void addAllDiscoveryBeansXml(Syringe syringe) {
        String beansXmlText = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\"></beans>";
        BeansXml beansXml = new BeansXmlParser()
                .parse(new ByteArrayInputStream(beansXmlText.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
