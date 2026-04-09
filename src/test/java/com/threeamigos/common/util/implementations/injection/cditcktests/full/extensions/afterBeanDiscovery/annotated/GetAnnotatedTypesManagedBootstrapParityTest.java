package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.wildfly.SyringeBootstrap;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetAnnotatedTypesManagedBootstrapParityTest {

    @Test
    void testManagedBootstrapAnnotatedTypesIdentityParity() throws Exception {
        Path tempRoot = Files.createTempDirectory("syringe-bootstrap-abd-annotated-types");
        Path serviceDir = tempRoot.resolve("META-INF/services");
        Files.createDirectories(serviceDir);
        Path serviceFile = serviceDir.resolve("jakarta.enterprise.inject.spi.Extension");
        Files.write(serviceFile,
                Collections.singletonList(ModifyingExtension.class.getName()),
                StandardCharsets.UTF_8);

        URLClassLoader serviceLoader = new URLClassLoader(
                new URL[]{tempRoot.toUri().toURL()},
                Thread.currentThread().getContextClassLoader());
        try {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            classes.add(Foo.class);
            classes.add(Bar.class);
            classes.add(Alpha.class);
            classes.add(Bravo.class);
            classes.add(Charlie.class);
            classes.add(ModifyingExtension.class);

            String beansXmlContent = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                    "version=\"3.0\" bean-discovery-mode=\"all\"></beans>";
            BeansXml beansXml = new BeansXmlParser().parse(
                    new ByteArrayInputStream(beansXmlContent.getBytes(StandardCharsets.UTF_8)));

            SyringeBootstrap bootstrap = new SyringeBootstrap(
                    classes,
                    serviceLoader,
                    Collections.singletonList(beansXml),
                    "GetAnnotatedTypesTest1234567890abcdef1234567890abcdef12345678.war");
            Syringe syringe = bootstrap.bootstrap();
            try {
                BeanManager beanManager = syringe.getBeanManager();
                ModifyingExtension extension = beanManager.createInstance().select(ModifyingExtension.class).get();
                assertNotNull(extension);

                List<AnnotatedType<Foo>> allFoo = extension.getAllFoo();
                assertEquals(3, allFoo.size());
                assertTrue(allFoo.contains(extension.getAplha()));
                assertTrue(allFoo.contains(extension.getBravo()));
                assertTrue(allFoo.contains(extension.getCharlie()));
            } finally {
                bootstrap.shutdown();
            }
        } finally {
            serviceLoader.close();
        }
    }
}

