package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.context.dependent;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependentContextTest {

    @Test
    void testDependentScopedDecoratorsAreDependentObjectsOfBean() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Interior.class,
                InteriorDecorator.class,
                InteriorRoom.class,
                Room.class,
                RoomBinding.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addDecoratorBeansXml(syringe, InteriorDecorator.class);
        syringe.setup();

        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<Interior> roomBean = getUniqueBean(beanManager, Interior.class, new RoomBinding());

            CreationalContext<Interior> roomCreationalContext = beanManager.createCreationalContext(roomBean);
            Interior room = (Interior) beanManager.getReference(roomBean, Interior.class, roomCreationalContext);

            InteriorDecorator.reset();
            room.foo();

            assertEquals(1, InteriorDecorator.getInstances().size());
            roomCreationalContext.release();
            assertTrue(InteriorDecorator.isDestroyed());
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
    private static <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> beanType, java.lang.annotation.Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }
}
