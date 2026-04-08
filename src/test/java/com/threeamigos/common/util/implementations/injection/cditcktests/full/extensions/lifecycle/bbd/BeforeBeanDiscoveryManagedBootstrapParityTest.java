package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Bar;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Baz;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Boss;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Foo;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.lib.Pro;
import com.threeamigos.common.util.implementations.injection.wildfly.SyringeBootstrap;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeforeBeanDiscoveryManagedBootstrapParityTest {

    @Test
    void testManagedBootstrapSupportsAddAnnotatedTypeAndQualifierAnnotatedType() throws Exception {
        BeforeBeanDiscoveryObserver.setObserved(false);

        Path tempRoot = Files.createTempDirectory("syringe-bootstrap-bbd");
        Path serviceDir = tempRoot.resolve("META-INF/services");
        Files.createDirectories(serviceDir);
        Path serviceFile = serviceDir.resolve("jakarta.enterprise.inject.spi.Extension");
        Files.write(serviceFile,
                Collections.singletonList(BeforeBeanDiscoveryObserver.class.getName()),
                StandardCharsets.UTF_8);

        URLClassLoader serviceLoader = new URLClassLoader(
                new URL[]{tempRoot.toUri().toURL()},
                Thread.currentThread().getContextClassLoader());
        try {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            classes.add(BeforeBeanDiscoveryObserver.class);
            classes.add(Programmer.class);
            classes.add(Skill.class);

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
                    "BeforeBeanDiscoveryTest1234567890abcdef1234567890abcdef12345678.war");

            Syringe syringe = bootstrap.bootstrap();
            RequestContextController requestContextController = null;
            try {
                BeanManager beanManager = syringe.getBeanManager();
                requestContextController = beanManager.createInstance().select(RequestContextController.class).get();
                requestContextController.activate();

                assertTrue(BeforeBeanDiscoveryObserver.isObserved());
                assertEquals(1, beanManager.getBeans(Programmer.class, new SkillLiteral() {
                    @Override
                    public String language() {
                        return "Java";
                    }

                    @Override
                    public String level() {
                        return "whatever";
                    }
                }).size());
                assertEquals(0, beanManager.getBeans(Programmer.class, new SkillLiteral() {
                    @Override
                    public String language() {
                        return "C++";
                    }

                    @Override
                    public String level() {
                        return "guru";
                    }
                }).size());

                assertEquals(1, beanManager.getBeans(Boss.class).size());
                assertEquals(0, beanManager.getBeans(Bar.class).size());
                assertEquals(1, beanManager.getBeans(Bar.class, Pro.ProLiteral.INSTANCE).size());
                assertEquals(0, beanManager.getBeans(Foo.class).size());
                assertEquals(1, beanManager.getBeans(Foo.class, Pro.ProLiteral.INSTANCE).size());

                @SuppressWarnings("unchecked")
                Bean<Baz> bazBean = (Bean<Baz>) beanManager.getBeans(Baz.class, Pro.ProLiteral.INSTANCE).iterator().next();
                assertEquals(RequestScoped.class, bazBean.getScope());
                CreationalContext<Baz> cc = beanManager.createCreationalContext(bazBean);
                Baz baz = (Baz) beanManager.getReference(bazBean, Baz.class, cc);
                assertFalse(baz.getBarInstance().isUnsatisfied());
            } finally {
                if (requestContextController != null) {
                    try {
                        requestContextController.deactivate();
                    } catch (Exception ignored) {
                        // context already inactive
                    }
                }
                bootstrap.shutdown();
            }
        } finally {
            serviceLoader.close();
        }
    }
}
