package com.threeamigos.common.util.implementations.injection.cditcktests.se.container;

import com.threeamigos.common.util.implementations.injection.cditcktests.se.container.testPackage.Apple;
import com.threeamigos.common.util.implementations.injection.cditcktests.se.container.testPackage.nestedPackage.Pear;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.InjectLiteral;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class BootstrapSEContainerTest {

    @Test
    void testContainerIsInitialized() {
        SeContainer seContainer = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(Foo.class)
                .initialize();
        assertTrue(seContainer.isRunning());
        Foo foo = seContainer.select(Foo.class).get();
        assertNotNull(foo);
        foo.ping();
        seContainer.close();
        assertFalse(seContainer.isRunning());
    }

    @Test
    void testContainerCloseMethodOnNotInitializedContainer() {
        SeContainer seContainer = initializeAndShutdownContainer();
        assertThrows(IllegalStateException.class, seContainer::close);
    }

    @Test
    void testInvocationOfInitializedMethodReturnsNewSeContainerInstance() {
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(Foo.class);

        SeContainer seContainer1 = seContainerInitializer.initialize();
        assertNotNull(seContainer1);
        seContainer1.close();

        SeContainer seContainer2 = seContainerInitializer.initialize();
        assertNotNull(seContainer2);
        seContainer2.close();
        assertNotEquals(seContainer1, seContainer2);
    }

    @Test
    void testSyntheticArchive() {
        BazObserver.isNotified = false;
        QuxObserver.isNotified = false;
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        try (SeContainer seContainer = seContainerInitializer.disableDiscovery()
                .addBeanClasses(Baz.class, Qux.class, BazObserver.class).initialize()) {
            BeanManager beanManager = seContainer.getBeanManager();
            beanManager.getEvent().select(Baz.class, Any.Literal.INSTANCE).fire(new Baz());
            beanManager.getEvent().select(Qux.class, Any.Literal.INSTANCE).fire(new Qux());
            assertNotNull(seContainer.select(Baz.class).get().ping());
            assertTrue(BazObserver.isNotified);
            assertFalse(QuxObserver.isNotified);
        }
    }

    @Test
    void testAlternativesInSE() {
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        try (SeContainer seContainer = seContainerInitializer.disableDiscovery()
                .addBeanClasses(Square.class, Circle.class, Foo.class, FooProducer.class)
                .selectAlternatives(Circle.class)
                .selectAlternativeStereotypes(AlternativeStereotype.class)
                .initialize()) {
            Shape shape = seContainer.select(Shape.class).get();
            assertEquals(Circle.NAME, shape.name());
            Set<Bean<?>> foos = seContainer.getBeanManager().getBeans(Foo.class);
            Optional<Bean<?>> alternativeFoo = foos.stream().filter(Bean::isAlternative).findAny();
            assertTrue(alternativeFoo.isPresent());
            assertEquals("createFoo", alternativeFoo.get().getName());
        }
    }

    @Test
    void testAddPackageNotRecursively() {
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        try (SeContainer seContainer = seContainerInitializer.disableDiscovery()
                .addPackages(Apple.class.getPackage())
                .initialize()) {
            Instance<Apple> appleInstance = seContainer.select(Apple.class);
            Instance<Pear> pearInstance = seContainer.select(Pear.class);
            assertFalse(appleInstance.isUnsatisfied());
            assertTrue(pearInstance.isUnsatisfied());
            assertNotNull(appleInstance.get().getWorm());
        }
    }

    @Test
    void testAddPackageRecursively() {
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        try (SeContainer seContainer = seContainerInitializer.disableDiscovery()
                .addPackages(true, Apple.class.getPackage())
                .initialize()) {
            Instance<Apple> appleInstance = seContainer.select(Apple.class);
            Instance<Pear> pearInstance = seContainer.select(Pear.class);
            assertFalse(appleInstance.isUnsatisfied());
            assertFalse(pearInstance.isUnsatisfied());
            assertNotNull(appleInstance.get().getWorm());
        }
    }

    @Test
    void testAddExtensionAsExtensionInstance() {
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        TestExtension testExtension = new TestExtension();
        try (SeContainer seContainer = seContainerInitializer.disableDiscovery()
                .addBeanClasses(Foo.class)
                .addExtensions(testExtension)
                .initialize()) {
            TestExtension containerExtension = seContainer.select(TestExtension.class).get();
            assertTrue(containerExtension.getBeforeBeanDiscoveryNotified().get());
            assertTrue(containerExtension.getAfterTypeDiscoveryNotified().get());
            assertTrue(containerExtension.getAfterBeanDiscoveryNotified().get());
            assertTrue(containerExtension.getAfterDeploymentValidationNotified().get());
            assertTrue(containerExtension.getProcessAnnotatedTypeNotified().get());
            assertTrue(containerExtension.getProcessInjectionTargetNotified().get());
            assertTrue(containerExtension.getProcessBeanAttributesNotified().get());
            assertTrue(containerExtension.getProcessBeanNotified().get());
        }
    }

    @Test
    void testAddExtensionAsClass() {
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        try (SeContainer seContainer = seContainerInitializer.disableDiscovery()
                .addBeanClasses(Foo.class)
                .addExtensions(TestExtension.class)
                .initialize()) {
            TestExtension containerExtension = seContainer.select(TestExtension.class).get();
            assertTrue(containerExtension.getBeforeBeanDiscoveryNotified().get());
            assertTrue(containerExtension.getAfterTypeDiscoveryNotified().get());
            assertTrue(containerExtension.getAfterBeanDiscoveryNotified().get());
            assertTrue(containerExtension.getAfterDeploymentValidationNotified().get());
            assertTrue(containerExtension.getProcessAnnotatedTypeNotified().get());
            assertTrue(containerExtension.getProcessInjectionTargetNotified().get());
            assertTrue(containerExtension.getProcessBeanAttributesNotified().get());
            assertTrue(containerExtension.getProcessBeanNotified().get());
        }
    }

    @Test
    void testAddInterceptor() {
        BarInterceptor1.notified = false;
        BarInterceptor2.notified = false;
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        try (SeContainer seContainer = seContainerInitializer.disableDiscovery()
                .addBeanClasses(Bar.class, BarInterceptor1.class, BarInterceptor2.class)
                .enableInterceptors(BarInterceptor1.class, BarInterceptor2.class)
                .initialize()) {
            Bar bar = seContainer.select(Bar.class).get();
            int result = bar.ping();
            assertTrue(BarInterceptor1.notified);
            assertTrue(BarInterceptor2.notified);
            assertEquals(3, result);
        }
    }

    @Test
    void testAddDecorator() {
        CorgeDecorator.notified = false;
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        try (SeContainer seContainer = seContainerInitializer.disableDiscovery()
                .addBeanClasses(Corge.class, CorgeImpl.class, CorgeDecorator.class)
                .enableDecorators(CorgeDecorator.class)
                .initialize()) {
            Corge corge = seContainer.select(Corge.class).get();
            int result = corge.ping();
            assertTrue(CorgeDecorator.notified);
            assertEquals(2, result);
        }
    }

    @Test
    void testSeContainerLookup() {
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        try (SeContainer seContainer = seContainerInitializer.disableDiscovery()
                .addBeanClasses(Garply.class, GarplyProducer.class)
                .initialize()) {
            Instance<Garply> garplyInstance = seContainer.select(Garply.class);
            assertTrue(garplyInstance.isResolvable());
            assertEquals(0, garplyInstance.get().getNumber());
        }
    }

    @Test
    void seContainerThrowsISEWhenAccessingBmAtShutdownContainer() {
        SeContainer seContainer = initializeAndShutdownContainer();
        assertThrows(IllegalStateException.class, seContainer::getBeanManager);
    }

    @Test
    void instanceSelectClassThrowsISEWhenAccessedAfterShutdown() {
        SeContainer seContainer = initializeAndShutdownContainer();
        assertThrows(IllegalStateException.class, () -> seContainer.select(Corge.class));
    }

    @Test
    void instanceSelectAnnotationThrowsISEWhenAccessedAfterShutdown() {
        SeContainer seContainer = initializeAndShutdownContainer();
        assertThrows(IllegalStateException.class, () -> seContainer.select(InjectLiteral.INSTANCE));
    }

    private SeContainer initializeAndShutdownContainer() {
        SeContainer seContainer = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(Foo.class)
                .initialize();
        seContainer.close();
        assertFalse(seContainer.isRunning());
        return seContainer;
    }
}
