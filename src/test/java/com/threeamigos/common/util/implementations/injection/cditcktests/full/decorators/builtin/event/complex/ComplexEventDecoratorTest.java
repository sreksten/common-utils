package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.builtin.event.complex;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComplexEventDecoratorTest {

    @Test
    void testOrderedEvents() {
        InMemoryMessageHandler messageHandler = new InMemoryMessageHandler();
        Syringe syringe = new Syringe(
                messageHandler,
                Bar.class,
                Baz.class,
                Payload.class,
                Observers.class,
                Ordered.class,
                OrderedEventDeliveryDecorator.class,
                OrderedEventDeliveryExtension.class,
                ObserverMethodComparator.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.exclude(ObserverMethodComparator.class);
        syringe.addExtension(OrderedEventDeliveryExtension.class.getName());
        addDecoratorBeansXml(syringe, OrderedEventDeliveryDecorator.class);

        setupOrThrow(syringe, messageHandler);
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Event<Payload> payloadEvent = beanManager.getEvent().select(Payload.class);
            payloadEvent.select(Bar.Literal.INSTANCE).select(Baz.Literal.INSTANCE).fire(new Payload());

            List<String> expectedResult = new ArrayList<String>();
            expectedResult.add("first");
            expectedResult.add("second");
            expectedResult.add("third");

            Observers observers = getContextualReference(beanManager, Observers.class);
            List<String> result = observers.getSequence().getData();
            assertEquals(expectedResult, result);
        } finally {
            syringe.shutdown();
        }
    }

    private void addDecoratorBeansXml(Syringe syringe, Class<?> decoratorClass) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "<decorators><class>" + decoratorClass.getName() + "</class></decorators>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> beanType) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return (T) beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean));
    }

    private static void setupOrThrow(Syringe syringe, InMemoryMessageHandler messageHandler) {
        try {
            syringe.setup();
        } catch (DefinitionException e) {
            StringBuilder details = new StringBuilder("Syringe setup failed.");
            if (!messageHandler.getAllErrorMessages().isEmpty()) {
                details.append(" Errors: ");
                for (String error : messageHandler.getAllErrorMessages()) {
                    details.append('[').append(error).append("] ");
                }
            }
            if (!messageHandler.getAllExceptionMessages().isEmpty()) {
                details.append(" Exceptions: ");
                for (String exception : messageHandler.getAllExceptionMessages()) {
                    details.append('[').append(exception).append("] ");
                }
            }
            throw new AssertionError(details.toString(), e);
        }
    }
}
