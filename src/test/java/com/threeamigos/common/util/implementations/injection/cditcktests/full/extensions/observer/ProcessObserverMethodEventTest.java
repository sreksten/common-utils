package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.observer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.ObserverMethod;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcessObserverMethodEventTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        ProcessObserverMethodObserver.getEventtypes().clear();

        syringe = new Syringe(
                new InMemoryMessageHandler(),
                EventA.class,
                EventB.class,
                EventC.class,
                EventD.class,
                EventObserver.class,
                EventBObserverMethod.class,
                ProcessObserverMethodObserver.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProcessObserverMethodObserver.class.getName());
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
    void testProcessObserverMethodEventsSent() {
        assertTrue(ProcessObserverMethodObserver.getEventtypes().contains(EventA.class));
    }

    @Test
    void testGetAnnotatedMethod() {
        assertEquals(
                EventA.class,
                ProcessObserverMethodObserver.getAnnotatedMethod().getParameters().iterator().next().getBaseType()
        );
    }

    @Test
    void testGetObserverMethod() {
        assertEquals(EventA.class, ProcessObserverMethodObserver.getObserverMethod().getObservedType());
    }

    @Test
    void replaceWithSetObserverMethod() {
        Set<ObserverMethod<? super EventC>> observerMethods = beanManager.resolveObserverMethods(
                new EventC(),
                Any.Literal.INSTANCE
        );
        assertEquals(1, observerMethods.size());
    }

    @Test
    void vetoEventD() {
        Set<ObserverMethod<? super EventD>> observerMethods = beanManager.resolveObserverMethods(
                new EventD(),
                Any.Literal.INSTANCE
        );
        assertEquals(0, observerMethods.size());
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
