package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.builtin.event;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinEventDecoratorTest {

    @Test
    void testDecoratorIsResolved() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            @SuppressWarnings("serial")
            TypeLiteral<Event<Foo>> eventFooLiteral = new TypeLiteral<Event<Foo>>() {
            };
            Decorator<?> decorator = resolveUniqueDecorator(
                    syringe.getBeanManager(),
                    Collections.<Type>singleton(eventFooLiteral.getType())
            );
            checkDecorator(
                    decorator,
                    FooEventDecorator.class,
                    Collections.<Type>singleton(eventFooLiteral.getType()),
                    eventFooLiteral.getType()
            );
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDecoratorIsInvoked() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                Probe probe = getContextualReference(syringe.getBeanManager(), Probe.class);
                Foo payload = new Foo(false);
                probe.getFooEvent().fire(payload);

                assertTrue(probe.getObserver().isObserved());
                assertTrue(payload.isDecorated());
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMultipleDecorators() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                Probe probe = getContextualReference(syringe.getBeanManager(), Probe.class);
                probe.getStringEvent().fire("TCK");
                assertEquals("DecoratedCharSequenceDecoratedStringTCK", probe.getObserver().getString());
                assertThrows(Exception.class, probe.getStringEvent()::select);
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Foo.class,
                Observer.class,
                FooEventDecorator.class,
                StringEventDecorator.class,
                CharSequenceEventDecorator.class,
                Probe.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addDecoratorBeansXml(syringe, FooEventDecorator.class, StringEventDecorator.class, CharSequenceEventDecorator.class);
        return syringe;
    }

    private void addDecoratorBeansXml(Syringe syringe, Class<?>... decoratorClasses) {
        StringBuilder xml = new StringBuilder();
        xml.append("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" ")
                .append("version=\"3.0\" bean-discovery-mode=\"all\">")
                .append("<decorators>");
        for (Class<?> decoratorClass : decoratorClasses) {
            xml.append("<class>").append(decoratorClass.getName()).append("</class>");
        }
        xml.append("</decorators></beans>");

        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    private static Decorator<?> resolveUniqueDecorator(BeanManager beanManager, Set<Type> types, Annotation... qualifiers) {
        List<Decorator<?>> decorators = beanManager.resolveDecorators(types, qualifiers);
        assertNotNull(decorators);
        assertEquals(1, decorators.size());
        return decorators.get(0);
    }

    private static void checkDecorator(Decorator<?> decorator, Class<?> beanClass, Set<Type> decoratedTypes, Type delegateType) {
        assertEquals(beanClass, decorator.getBeanClass());
        assertEquals(decoratedTypes, decorator.getDecoratedTypes());
        assertEquals(delegateType, decorator.getDelegateType());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> beanType) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return (T) beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean));
    }

    @RequestScoped
    public static class Probe {

        @Inject
        private Observer observer;

        @Inject
        private Event<Foo> fooEvent;

        @Inject
        private Event<String> stringEvent;

        public Observer getObserver() {
            return observer;
        }

        public Event<Foo> getFooEvent() {
            return fooEvent;
        }

        public Event<String> getStringEvent() {
            return stringEvent;
        }
    }
}
