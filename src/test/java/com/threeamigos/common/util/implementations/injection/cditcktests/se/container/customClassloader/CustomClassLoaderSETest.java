package com.threeamigos.common.util.implementations.injection.cditcktests.se.container.customClassloader;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class CustomClassLoaderSETest {

    @Test
    void testCustomClassLoader() throws IOException {
        ClassLoader classLoader = new URLClassLoader(new URL[]{}, Alpha.class.getClassLoader()) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                if ("META-INF/services/jakarta.enterprise.inject.spi.Extension".equals(name)) {
                    return super.getResources("META-INF/services/" + MyExtension.class.getName());
                }
                return super.getResources(name);
            }
        };

        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        try (SeContainer container = seContainerInitializer
                .setClassLoader(classLoader)
                .disableDiscovery()
                .addBeanClasses(Alpha.class, Bravo.class)
                .initialize()) {
            container.select(Alpha.class, ProcessedByExtension.ProcessedByExtensionLiteral.INSTANCE).get().ping();
            assertTrue(container.select(Bravo.class, ProcessedByExtension.ProcessedByExtensionLiteral.INSTANCE).isUnsatisfied());
        }
    }
}
