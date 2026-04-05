package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.modify;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetBeanAttributesTest {

    @Test
    void testBeanModified() {
        Bootstrap bootstrap = bootstrap();
        try {
            BeanManager beanManager = bootstrap.syringe.getBeanManager();

            assertEquals(0, beanManager.getBeans(Cat.class, Default.Literal.INSTANCE).size());
            assertEquals(0, beanManager.getBeans(Animal.class, Any.Literal.INSTANCE).size());
            assertEquals(0, beanManager.getBeans(Animal.class, new Wild.Literal(false)).size());

            assertEquals(1, beanManager.getBeans(Cat.class, new Wild.Literal(true)).size());
            assertEquals(1, beanManager.getBeans(Cat.class, new Cute.Literal()).size());
            assertEquals(1, beanManager.getBeans("cat").size());

            Bean<Cat> bean = getUniqueBean(beanManager, Cat.class, new Cute.Literal());

            assertTrue(typeSetMatches(bean.getTypes(), Object.class, Cat.class));
            assertTrue(typeSetMatches(bean.getStereotypes(), PersianStereotype.class));
            assertTrue(annotationSetMatches(bean.getQualifiers(), new Wild.Literal(true), new Cute.Literal(),
                    Any.Literal.INSTANCE));

            assertEquals(ApplicationScoped.class, bean.getScope());
            assertTrue(bean.isAlternative());
        } finally {
            bootstrap.syringe.shutdown();
        }
    }

    @Test
    void testChangesAreNotPropagated() {
        Bootstrap bootstrap = bootstrap();
        try {
            assertNotNull(bootstrap.extension.getCatAnnotatedType());
            assertTrue(bootstrap.extension.getCatAnnotatedType().getAnnotations().isEmpty());
            assertTrue(typeSetMatches(bootstrap.extension.getCatAnnotatedType().getTypeClosure(), Object.class, Cat.class,
                    Animal.class));
        } finally {
            bootstrap.syringe.shutdown();
        }
    }

    private Bootstrap bootstrap() {
        ModifyingExtension extension = new ModifyingExtension();
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(extension);
        syringe.initialize();
        syringe.addDiscoveredClass(Cat.class, BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);
        syringe.start();
        return new Bootstrap(syringe, extension);
    }

    private void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "<alternatives><class>" + Cat.class.getName() + "</class></alternatives>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set beans = beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve(beans);
    }

    private static boolean typeSetMatches(Set<?> actual, Object... expected) {
        return new HashSet<Object>(actual).equals(new HashSet<Object>(Arrays.asList(expected)));
    }

    private static boolean annotationSetMatches(Set<Annotation> actual, Annotation... expected) {
        return new HashSet<Annotation>(actual).equals(new HashSet<Annotation>(Arrays.asList(expected)));
    }

    private static final class Bootstrap {
        private final Syringe syringe;
        private final ModifyingExtension extension;

        private Bootstrap(Syringe syringe, ModifyingExtension extension) {
            this.syringe = syringe;
            this.extension = extension;
        }
    }
}
