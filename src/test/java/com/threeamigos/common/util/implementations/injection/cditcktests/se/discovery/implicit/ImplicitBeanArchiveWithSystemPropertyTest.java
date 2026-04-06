package com.threeamigos.common.util.implementations.injection.cditcktests.se.discovery.implicit;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Isolated
class ImplicitBeanArchiveWithSystemPropertyTest {

    private static final String IMPLICIT_SCAN_KEY = "jakarta.enterprise.inject.scan.implicit";

    @Test
    void testImplicitArchiveDiscovered() throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(IMPLICIT_SCAN_KEY, true);

        URLClassLoader isolatedClassLoader = isolatedImplicitClassLoader();
        Class<?> barClass = isolatedClassLoader.loadClass(Bar.class.getName());

        try (URLClassLoader ignored = isolatedClassLoader;
             SeContainer seContainer = SeContainerInitializer.newInstance()
                     .setClassLoader(isolatedClassLoader)
                     .setProperties(properties)
                     .initialize()) {
            Object bar = seContainer.select((Class) barClass).get();
            assertNotNull(bar);
            bar.getClass().getMethod("ping").invoke(bar);
        }
    }

    private URLClassLoader isolatedImplicitClassLoader() throws IOException {
        Path root = Files.createTempDirectory("syringe-implicit-system-property");
        copyClassBytes(Bar.class, root);
        return new IsolatedDiscoveryClassLoader(
                new URL[]{root.toUri().toURL()},
                Bar.class.getClassLoader()
        );
    }

    private void copyClassBytes(Class<?> clazz, Path root) throws IOException {
        String resource = clazz.getName().replace('.', '/') + ".class";
        try (InputStream input = clazz.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Could not load class bytes for " + clazz.getName());
            }
            Path target = root.resolve(resource);
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static class IsolatedDiscoveryClassLoader extends URLClassLoader {
        private IsolatedDiscoveryClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return findResources(name);
        }
    }
}
