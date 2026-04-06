package com.threeamigos.common.util.implementations.injection.cditcktests.se.discovery.trimmed;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class TrimmedBeanArchiveSETest {

    @Test
    void discoveredTypes() throws Exception {
        URLClassLoader isolatedClassLoader = isolatedTrimmedClassLoader();
        Class<?> barClass = isolatedClassLoader.loadClass(Bar.class.getName());
        Class<?> fooClass = isolatedClassLoader.loadClass(Foo.class.getName());
        Class<?> extensionClass = isolatedClassLoader.loadClass(TestExtension.class.getName());

        try (URLClassLoader ignored = isolatedClassLoader;
             SeContainer seContainer = SeContainerInitializer.newInstance()
                     .setClassLoader(isolatedClassLoader)
                     .initialize()) {
            Object bar = seContainer.select((Class) barClass).get();
            Object foo = seContainer.select((Class) fooClass).get();
            assertNotNull(foo);
            assertNotNull(bar);
            assertEquals("Intercepted Bar", bar.getClass().getMethod("ping").invoke(bar));

            Object extension = seContainer.select((Class) extensionClass).get();
            assertTrue((Boolean) extension.getClass().getMethod("getBarPATFired").invoke(extension));
            assertTrue((Boolean) extension.getClass().getMethod("getBarProducerPBAFired").invoke(extension));
            assertFalse((Boolean) extension.getClass().getMethod("getBarPBFired").invoke(extension));
        }
    }

    private URLClassLoader isolatedTrimmedClassLoader() throws IOException {
        Path root = Files.createTempDirectory("syringe-trimmed");

        copyClassAndNestedBytes(Bar.class, root);
        copyClassAndNestedBytes(Foo.class, root);
        copyClassAndNestedBytes(BarProducer.class, root);
        copyClassAndNestedBytes(FooProducer.class, root);
        copyClassAndNestedBytes(TestStereotype.class, root);
        copyClassAndNestedBytes(TestExtension.class, root);
        copyClassAndNestedBytes(BarInterceptor.class, root);
        copyClassAndNestedBytes(BarInterceptorBinding.class, root);

        copyResource(
                "com/threeamigos/common/util/implementations/injection/cditcktests/se/discovery/trimmed/beans.xml",
                root.resolve("META-INF/beans.xml")
        );

        Path serviceFile = root.resolve("META-INF/services/jakarta.enterprise.inject.spi.Extension");
        Files.createDirectories(serviceFile.getParent());
        Files.write(serviceFile,
                (TestExtension.class.getName() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));

        return new IsolatedDiscoveryClassLoader(
                new URL[]{root.toUri().toURL()},
                TrimmedBeanArchiveSETest.class.getClassLoader()
        );
    }

    private void copyClassAndNestedBytes(Class<?> rootClass, Path root) throws IOException {
        Deque<Class<?>> stack = new ArrayDeque<Class<?>>();
        stack.push(rootClass);

        while (!stack.isEmpty()) {
            Class<?> current = stack.pop();
            copyClassBytes(current, root);
            Class<?>[] nested = current.getDeclaredClasses();
            if (nested != null) {
                for (Class<?> nestedClass : nested) {
                    stack.push(nestedClass);
                }
            }
        }
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

    private void copyResource(String resource, Path target) throws IOException {
        try (InputStream input = TrimmedBeanArchiveSETest.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Could not load resource " + resource);
            }
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
