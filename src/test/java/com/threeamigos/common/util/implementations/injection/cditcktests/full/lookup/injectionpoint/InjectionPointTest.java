package com.threeamigos.common.util.implementations.injection.cditcktests.full.lookup.injectionpoint;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class InjectionPointTest {

    @Test
    void testIsDelegate() {
        Syringe syringe = newSyringeWithDecorators(AnimalDecorator1.class, AnimalDecorator2.class, AnimalDecorator3.class);
        BeanManagerImpl beanManager = null;
        try {
            syringe.setup();
            beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateRequest();

            assertFalse(getContextualReference(beanManager, FieldInjectionPointBean.class)
                    .getInjectedBean().getInjectedMetadata().isDelegate());

            Cat cat = getContextualReference(beanManager, Cattery.class).getCat();
            assertEquals("hello!!!", cat.hello());
            assertNotNull(cat.getBeanManager());
            assertNotNull(cat.getInjectionPoint());
            assertFalse(cat.getInjectionPoint().isDelegate());

            List<Decorator<?>> animalDecorators = beanManager.resolveDecorators(Collections.<Type>singleton(Animal.class));
            assertEquals(3, animalDecorators.size());
            for (Decorator<?> animalDecorator : animalDecorators) {
                assertEquals(2, animalDecorator.getInjectionPoints().size());

                for (InjectionPoint injectionPoint : animalDecorator.getInjectionPoints()) {
                    if (injectionPoint.getType().equals(InjectionPoint.class)) {
                        assertFalse(injectionPoint.isDelegate());
                    } else if (injectionPoint.getType().equals(Animal.class)) {
                        assertTrue(injectionPoint.isDelegate());
                    } else if (injectionPoint.getType().equals(Toy.class)) {
                        assertFalse(injectionPoint.isDelegate());
                    } else {
                        throw new AssertionError("Unexpected injection point type: " + injectionPoint.getType());
                    }
                }
            }

            Toy toy = cat.getToy();
            assertNotNull(toy.getInjectionPoint());
            assertEquals(AnimalDecorator2.class, toy.getInjectionPoint().getBean().getBeanClass());
        } finally {
            if (beanManager != null) {
                beanManager.getContextManager().deactivateRequest();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testPassivationCapability() throws Exception {
        Syringe syringe = newSyringeWithDecorators(AnimalDecorator1.class, AnimalDecorator2.class, AnimalDecorator3.class);
        BeanManagerImpl beanManager = null;
        try {
            syringe.setup();
            beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateRequest();

            InjectionPoint ip1 = getContextualReference(beanManager, FieldInjectionPointBean.class).getInjectedBean().getInjectedMetadata();
            InjectionPoint ip2 = getContextualReference(beanManager, MethodInjectionPointBean.class).getInjectedBean().getInjectedMetadata();
            InjectionPoint ip3 = getContextualReference(beanManager, ConstructorInjectionPointBean.class).getInjectedBean().getInjectedMetadata();

            ip1 = deserialize(serialize(ip1), InjectionPoint.class);
            ip2 = deserialize(serialize(ip2), InjectionPoint.class);
            ip3 = deserialize(serialize(ip3), InjectionPoint.class);

            assertEquals(BeanWithInjectionPointMetadata.class, ip1.getType());
            assertEquals(BeanWithInjectionPointMetadata.class, ip2.getType());
            assertEquals(BeanWithInjectionPointMetadata.class, ip3.getType());
        } finally {
            if (beanManager != null) {
                beanManager.getContextManager().deactivateRequest();
            }
            syringe.shutdown();
        }
    }

    private Syringe newSyringeWithDecorators(Class<?>... decorators) {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Animal.class,
                AnimalDecorator1.class,
                AnimalDecorator2.class,
                AnimalDecorator3.class,
                AnimalStereotype.class,
                BeanWithInjectionPointMetadata.class,
                Cat.class,
                Cattery.class,
                ConstructorInjectionPointBean.class,
                FieldInjectionPointBean.class,
                MethodInjectionPointBean.class,
                Toy.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBeansXmlConfiguration(beansXmlWithDecorators(decorators));
        return syringe;
    }

    private BeansXml beansXmlWithDecorators(Class<?>... decorators) {
        StringBuilder xml = new StringBuilder();
        xml.append("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" bean-discovery-mode=\"all\" version=\"4.0\">");
        xml.append("<decorators>");
        for (Class<?> decorator : decorators) {
            xml.append("<class>").append(decorator.getName()).append("</class>");
        }
        xml.append("</decorators>");
        xml.append("</beans>");
        BeansXmlParser parser = new BeansXmlParser();
        return parser.parse(new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] serialize(Object value) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = new ObjectOutputStream(output);
        objectOutput.writeObject(value);
        objectOutput.flush();
        return output.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(byte[] bytes, Class<T> type) throws Exception {
        ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object deserialized = objectInput.readObject();
        return (T) type.cast(deserialized);
    }

    private <T> Bean<T> getBean(BeanManagerImpl beanManager, Class<T> type) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Set<Bean<?>> beans = (Set) beanManager.getBeans(type);
        @SuppressWarnings("unchecked")
        Bean<T> bean = (Bean<T>) beanManager.resolve(beans);
        return bean;
    }

    private <T> T getContextualReference(BeanManagerImpl beanManager, Class<T> type) {
        Bean<T> bean = getBean(beanManager, type);
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        return type.cast(beanManager.getReference(bean, type, creationalContext));
    }
}
