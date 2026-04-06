package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.definition.interceptorCalledBeforeDecorator;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class InterceptorCalledBeforeDecoratorTest {

    @Test
    void testInterceptorCalledBeforeDecorator() {
        FooImpl.interceptorCalledFirst = false;
        FooImpl.decoratorCalledFirst = false;

        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Foo.class,
                FooDecorator.class,
                FooImpl.class,
                TransactionInterceptor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);

        try {
            syringe.setup();

            Foo foo = syringe.getBeanManager().createInstance().select(Foo.class).get();
            foo.bar();

            assertTrue(FooImpl.interceptorCalledFirst);
            assertFalse(FooImpl.decoratorCalledFirst);
        } finally {
            syringe.shutdown();
        }
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" "
                + "version=\"3.0\" bean-discovery-mode=\"all\">"
                + "<decorators>"
                + "<class>" + FooDecorator.class.getName() + "</class>"
                + "</decorators>"
                + "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
