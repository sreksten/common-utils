package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.builtin.conversation;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Conversation;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BuiltinConversationDecoratorTest {

    @Test
    void testDecoratorIsResolved() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Decorator<?> decorator = resolveUniqueDecorator(beanManager, Collections.<Type>singleton(Conversation.class));
            checkDecorator(decorator, ConversationDecorator.class, Collections.<Type>singleton(Conversation.class), Conversation.class);
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
                BeanManager beanManager = syringe.getBeanManager();
                ConversationObserver conversationObserver = getContextualReference(beanManager, ConversationObserver.class);
                Conversation conversation = getContextualReference(beanManager, Conversation.class);

                String cid = "foo";
                conversation.begin(cid);
                assertFalse(conversation.isTransient());
                assertEquals(cid, conversationObserver.getDecoratedConversationId());
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
                ConversationDecorator.class,
                ConversationObserver.class,
                Duck.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addDecoratorBeansXml(syringe, ConversationDecorator.class);
        return syringe;
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
}
