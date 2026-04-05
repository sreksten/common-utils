package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.alternative.metadata.interceptor;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlternativeMetadataInterceptorTest {

    @Test
    void testInterceptorInterceptsOnlyBindedClass() {
        withBootedSyringe(new Scenario() {
            @Override
            public void run(Syringe syringe, BeanManager beanManager) {
                Login login = getContextualReference(beanManager, Login.class);
                Login securedLogin = getContextualReference(beanManager, Login.class, new Secured.Literal());

                assertTrue(login instanceof Login);
                assertTrue(securedLogin instanceof Login);
                assertEquals("logged", login.login());
                assertEquals("intercepted", securedLogin.login());
            }
        });
    }

    private void withBootedSyringe(Scenario scenario) {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                InterceptorExtension.class,
                Login.class,
                LoginInterceptor.class,
                LoginInterceptorBinding.class,
                Secured.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(InterceptorExtension.class.getName());
        addBeansXmlInterceptors(syringe, LoginInterceptor.class.getName());
        try {
            syringe.setup();
            scenario.run(syringe, syringe.getBeanManager());
        } finally {
            syringe.shutdown();
        }
    }

    private static void addBeansXmlInterceptors(Syringe syringe, String... interceptorClassNames) {
        StringBuilder classes = new StringBuilder();
        for (String interceptorClassName : interceptorClassNames) {
            classes.append("<class>").append(interceptorClassName).append("</class>");
        }
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<interceptors>" + classes + "</interceptors>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, java.lang.annotation.Annotation... qualifiers) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type, qualifiers));
        assertNotNull(bean, "No bean resolved for type " + type.getName());
        CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
        return type.cast(beanManager.getReference(bean, type, ctx));
    }

    private interface Scenario {
        void run(Syringe syringe, BeanManager beanManager);
    }
}
