package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype.customstereotypetest.notdiscovered.NotDiscoveredBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype.customstereotypetest.test.CustomStereotypeExtension;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype.customstereotypetest.test.MyCustomStereotype;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype.customstereotypetest.test.MyService;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.wildfly.SyringeBootstrap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomStereotypeTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype.customstereotypetest.test";

    @Test
    void test() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(CustomStereotypeExtension.class.getName());
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            Set<Bean<?>> serviceBeans = beanManager.getBeans(MyService.class);
            assertEquals(1, serviceBeans.size());
            assertEquals(ApplicationScoped.class, serviceBeans.iterator().next().getScope());
            assertEquals("Hello!", resolveReference(beanManager, MyService.class).hello());

            assertTrue(beanManager.getBeans(NotDiscoveredBean.class).isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testManagedBootstrapNoBeansXmlTreatsBceExtensionArchiveAsNonBeanArchive() throws Exception {
        Path tempRoot = Files.createTempDirectory("syringe-bootstrap-custom-stereotype");
        Path serviceDir = tempRoot.resolve("META-INF/services");
        Files.createDirectories(serviceDir);
        Path serviceFile =
                serviceDir.resolve("jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension");
        Files.write(serviceFile,
                Collections.singletonList(CustomStereotypeExtension.class.getName()),
                StandardCharsets.UTF_8);

        URLClassLoader serviceLoader = new URLClassLoader(
                new URL[]{tempRoot.toUri().toURL()},
                Thread.currentThread().getContextClassLoader());
        try {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            classes.add(CustomStereotypeExtension.class);
            classes.add(MyCustomStereotype.class);
            classes.add(MyService.class);
            classes.add(NotDiscoveredBean.class);

            SyringeBootstrap bootstrap = new SyringeBootstrap(
                    classes,
                    serviceLoader,
                    null,
                    "CustomStereotypeTest1234567890abcdef1234567890abcdef12345678.war");
            Syringe syringe = bootstrap.bootstrap();
            try {
                BeanManager beanManager = syringe.getBeanManager();
                Set<Bean<?>> serviceBeans = beanManager.getBeans(MyService.class);
                assertEquals(1, serviceBeans.size());
                assertEquals(ApplicationScoped.class, serviceBeans.iterator().next().getScope());
                assertEquals("Hello!", resolveReference(beanManager, MyService.class).hello());
                assertTrue(beanManager.getBeans(NotDiscoveredBean.class).isEmpty());
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
