package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test.ChangeBeanQualifierExtension;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test.MyOtherService;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test.MyQualifier;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test.MyService;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test.MyServiceBar;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test.MyServiceBaz;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test.MyServiceFoo;
import com.threeamigos.common.util.implementations.injection.wildfly.SyringeBootstrap;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeBeanQualifierManagedBootstrapParityTest {

    @Test
    void testManagedBootstrapResolvesBeanWithChangedQualifier() throws Exception {
        Path tempRoot = Files.createTempDirectory("syringe-bootstrap-change-bean-qualifier");
        Path serviceDir = tempRoot.resolve("META-INF/services");
        Files.createDirectories(serviceDir);
        Path serviceFile = serviceDir.resolve("jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension");
        Files.write(serviceFile,
                Collections.singletonList(ChangeBeanQualifierExtension.class.getName()),
                StandardCharsets.UTF_8);

        URLClassLoader serviceLoader = new URLClassLoader(
                new URL[]{tempRoot.toUri().toURL()},
                Thread.currentThread().getContextClassLoader());
        try {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            classes.add(ChangeBeanQualifierExtension.class);
            classes.add(MyService.class);
            classes.add(MyQualifier.class);
            classes.add(MyServiceFoo.class);
            classes.add(MyServiceBar.class);
            classes.add(MyServiceBaz.class);
            classes.add(MyOtherService.class);

            String beansXmlContent = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                    "version=\"3.0\"></beans>";
            BeansXml beansXml = new BeansXmlParser().parse(
                    new ByteArrayInputStream(beansXmlContent.getBytes(StandardCharsets.UTF_8)));

            SyringeBootstrap bootstrap = new SyringeBootstrap(
                    classes,
                    serviceLoader,
                    Collections.singletonList(beansXml),
                    "ChangeBeanQualifierTest1234567890abcdef1234567890abcdef12345678.war");

            Syringe syringe = bootstrap.bootstrap();
            try {
                MyOtherService bean = resolveReference(syringe.getBeanManager(), MyOtherService.class);
                assertNotNull(bean);
                assertTrue(bean.getMyService() instanceof MyServiceBar);
            } finally {
                bootstrap.shutdown();
            }
        } finally {
            serviceLoader.close();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
