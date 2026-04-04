package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.validation;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.InterceptionType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterceptorWithNonPassivationCapableDependenciesTest {

    @Test
    void test() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Engine.class,
                EnginePowered.class,
                EnginePoweredInterceptor.class,
                Ferry.class,
                Vessel.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addInterceptorBeansXml(syringe, EnginePoweredInterceptor.class);
        syringe.setup();
        try {
            assertEquals(
                    1,
                    syringe.getBeanManager().resolveInterceptors(
                            InterceptionType.AROUND_INVOKE,
                            new EnginePowered.EnginePoweredLiteral())
                            .size()
            );
        } finally {
            syringe.shutdown();
        }
    }

    private void addInterceptorBeansXml(Syringe syringe, Class<?> interceptorClass) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "<interceptors><class>" + interceptorClass.getName() + "</class></interceptors>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
