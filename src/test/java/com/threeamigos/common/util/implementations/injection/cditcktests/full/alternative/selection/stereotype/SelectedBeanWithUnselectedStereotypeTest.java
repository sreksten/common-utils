package com.threeamigos.common.util.implementations.injection.cditcktests.full.alternative.selection.stereotype;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectedBeanWithUnselectedStereotypeTest {

    @Test
    void testSingleAlternativeIsSelected() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Bar.class, Foo.class, UnselectedStereotype.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addBeansXmlSelectingBar(syringe);

        try {
            syringe.setup();
            BeanManager beanManager = syringe.getBeanManager();

            Set<Bean<?>> barBeans = beanManager.getBeans(Bar.class);
            assertEquals(1, barBeans.size());
            assertEquals(RequestScoped.class, barBeans.iterator().next().getScope());

            Set<Bean<?>> fooBeans = beanManager.getBeans(Foo.class);
            assertEquals(0, fooBeans.size());
        } finally {
            syringe.shutdown();
        }
    }

    private void addBeansXmlSelectingBar(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<alternatives>" +
                "<class>" + Bar.class.getName() + "</class>" +
                "</alternatives>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @UnselectedStereotype
    static class Bar {
    }

    @UnselectedStereotype
    static class Foo {
    }

    @RequestScoped
    @Stereotype
    @Alternative
    @Target({TYPE, METHOD, FIELD})
    @Retention(RUNTIME)
    public @interface UnselectedStereotype {
    }
}
