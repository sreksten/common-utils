package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.builtin.injectionpoint;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.InjectionPoint;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinInjectionPointDecoratorTest {

    @Test
    void testDecoratorIsResolved() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            Decorator<?> decorator = resolveUniqueDecorator(
                    syringe.getBeanManager(),
                    Collections.<Type>singleton(InjectionPoint.class)
            );
            checkDecorator(
                    decorator,
                    InjectionPointDecorator.class,
                    Collections.<Type>singleton(InjectionPoint.class),
                    InjectionPoint.class
            );
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDecoratorInvoked() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                BeanManager beanManager = syringe.getBeanManager();
                Company company = getContextualReference(beanManager, Company.class);
                assertTrue(company.getFuse().getInjectionPoint().isTransient());
                assertEquals(getUniqueBean(beanManager, Company.class), company.getFuse().getInjectionPoint().getBean());
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
                Company.class,
                Fuse.class,
                InjectionPointDecorator.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addDecoratorBeansXml(syringe, InjectionPointDecorator.class);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> beanType) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }
}
