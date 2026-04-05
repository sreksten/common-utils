package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.bean;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyntheticBeanTest {

    private Syringe syringe;
    private BeanManager beanManager;
    private boolean requestContextActivated;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                BeanExtension.class,
                Building.class,
                Employee.class,
                FireTruck.class,
                Hungry.class,
                Large.class,
                Lion.class,
                Office.class,
                SerializableOffice.class,
                Simple.class,
                SimpleInterceptor.class,
                Tiger.class,
                Vehicle.class,
                VehicleDecorator.class,
                Zoo.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addInterceptorsAndDecoratorsBeansXml(syringe, new Class<?>[]{SimpleInterceptor.class}, new Class<?>[]{VehicleDecorator.class});
        syringe.addExtension(BeanExtension.class.getName());
        syringe.setup();
        beanManager = syringe.getBeanManager();
        requestContextActivated = syringe.activateRequestContextIfNeeded();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            try {
                if (requestContextActivated) {
                    syringe.deactivateRequestContextIfActive();
                }
            } finally {
                syringe.shutdown();
            }
        }
    }

    @Test
    void testRegisteredBean() {
        Bean<Office> bean = getUniqueBean(Office.class, Large.Literal.INSTANCE);
        assertEquals(3, bean.getInjectionPoints().size());
        for (InjectionPoint ip : bean.getInjectionPoints()) {
            assertEquals(bean, ip.getBean());
        }
        testOffice(bean);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSerializableBean() {
        Bean<?> bean = beanManager.resolve((Set) beanManager.getBeans(SerializableOffice.class, Any.Literal.INSTANCE));
        assertTrue(bean instanceof PassivationCapable);
        testOffice(bean);
    }

    @Test
    void testSyntheticBeanIntercepted() {
        Office office = getContextualReference(Office.class, Large.Literal.INSTANCE);
        SerializableOffice serializableOffice = getContextualReference(SerializableOffice.class);
        assertTrue(office.intercepted());
        assertTrue(serializableOffice.intercepted());
    }

    @Test
    void testSyntheticProducerField() {
        Lion lion = getContextualReference(Lion.class, Hungry.Literal.INSTANCE);
        assertNotNull(lion);
        lion.foo();
        Bean<Lion> bean = getUniqueBean(Lion.class, Hungry.Literal.INSTANCE);
        assertTrue(bean.getQualifiers().contains(Hungry.Literal.INSTANCE));
    }

    @Test
    void testSyntheticProducerMethod() {
        Tiger tiger = getContextualReference(Tiger.class, Hungry.Literal.INSTANCE);
        assertNotNull(tiger);
        tiger.foo();
        Bean<Tiger> bean = getUniqueBean(Tiger.class, Hungry.Literal.INSTANCE);
        assertTrue(bean.getQualifiers().contains(Hungry.Literal.INSTANCE));
    }

    @Test
    void testSyntheticDecorator() {
        FireTruck truck = getContextualReference(FireTruck.class);
        assertTrue(truck.decorated());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void testOffice(Bean<?> bean) {
        Office.reset();
        CreationalContext ctx = beanManager.createCreationalContext(bean);
        Object created = ((Bean) bean).create(ctx);
        Office office = (Office) created;
        assertNotNull(office);
        assertNotNull(office.getConstructorEmployee());
        assertNotNull(office.getFieldEmployee());
        assertNotNull(office.getInitializerEmployee());
        assertTrue(office.isPostConstructCalled());
        ((Bean) bean).destroy(created, ctx);
        assertTrue(Office.isPreDestroyCalled());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T getContextualReference(Class<T> beanClass, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return (T) beanManager.getReference(bean, beanClass, beanManager.createCreationalContext(bean));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Class<T> beanClass, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static void addInterceptorsAndDecoratorsBeansXml(Syringe syringe, Class<?>[] interceptorClasses, Class<?>[] decoratorClasses) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" ")
                .append("version=\"3.0\" bean-discovery-mode=\"all\">")
                .append("<interceptors>");
        for (Class<?> interceptorClass : interceptorClasses) {
            xmlBuilder.append("<class>").append(interceptorClass.getName()).append("</class>");
        }
        xmlBuilder.append("</interceptors><decorators>");
        for (Class<?> decoratorClass : decoratorClasses) {
            xmlBuilder.append("<class>").append(decoratorClass.getName()).append("</class>");
        }
        xmlBuilder.append("</decorators></beans>");

        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xmlBuilder.toString().getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
