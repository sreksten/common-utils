package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.broken.decorator.field.CityDecorator;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.broken.decorator.field.CityInterface;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.broken.decorator.field.NonPassivating;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.broken.decorator.field.UnderwaterCity;
import com.threeamigos.common.util.implementations.injection.scopes.ClientProxyGenerator;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.util.AnnotationLiteral;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.enterprise.inject.Stereotype;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SyringeBootstrapTest {

    @jakarta.enterprise.context.Dependent
    public static class MyBean {
        public String hello() {
            return "hello";
        }
    }

    @Stereotype
    @ApplicationScoped
    @RequestScoped
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InvalidScopeStereotype {
    }

    @InvalidScopeStereotype
    public static class InvalidScopedBean {
    }

    public static class LegacyStyleInterceptor {
        @AroundInvoke
        public Object aroundInvoke(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @jakarta.enterprise.context.Dependent
    @Interceptors(LegacyStyleInterceptor.class)
    public static class LegacyInterceptedBean {
        public String ping() {
            return "pong";
        }
    }

    public static class PlainManagedBean {
        public String value() {
            return "plain";
        }
    }

    @ApplicationScoped
    public static class ManagedArchiveModeNoneBean {
        public String value() {
            return "none";
        }
    }

    @Dependent
    public static class ServiceLoadedInvalidBean {
        @Inject
        ServiceLoadedMissingDependency missing;
    }

    public static class ServiceLoadedMissingDependency {
    }

    public static class ServiceLoadedVetoExtension implements Extension {
        public void vetoInvalidBean(@Observes ProcessAnnotatedType<ServiceLoadedInvalidBean> event) {
            event.veto();
        }
    }

    public static class TrimmedBike {
    }

    public static class TrimmedBikeProducer {
        @jakarta.enterprise.inject.Produces
        public TrimmedBike produceBike() {
            return new TrimmedBike();
        }
    }

    @ApplicationScoped
    public static class TrimmedBus {
    }

    public static class TrimmedPatRecorderExtension implements Extension {
        static final AtomicBoolean bikeProducerPatFired = new AtomicBoolean(false);
        static final AtomicBoolean bikeProducerPbaFired = new AtomicBoolean(false);

        static void reset() {
            bikeProducerPatFired.set(false);
            bikeProducerPbaFired.set(false);
        }

        public void observeBikeProducerPat(@Observes ProcessAnnotatedType<TrimmedBikeProducer> event) {
            bikeProducerPatFired.set(true);
        }

        public void observeBikeProducerPba(@Observes ProcessBeanAttributes<TrimmedBikeProducer> event) {
            bikeProducerPbaFired.set(true);
        }
    }

    @Qualifier
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ManagedCheap {
    }

    @Qualifier
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ManagedExpensive {
    }

    public static final class ManagedCheapLiteral extends AnnotationLiteral<ManagedCheap> implements ManagedCheap {
        private static final long serialVersionUID = 1L;
    }

    public static final class ManagedExpensiveLiteral extends AnnotationLiteral<ManagedExpensive> implements ManagedExpensive {
        private static final long serialVersionUID = 1L;
    }

    @Dependent
    public static class ManagedAlternativeSausage {
        @Produces
        @ManagedCheap
        ManagedAlternativeSausage cheap = new ManagedAlternativeSausage();

        @Produces
        @ManagedExpensive
        ManagedAlternativeSausage grill() {
            return new ManagedAlternativeSausage();
        }
    }

    @Dependent
    public static class AsyncPriorityObserverBean {
        void observes(@ObservesAsync @Priority(2000) AsyncPriorityEvent event) {
            // no-op
        }
    }

    public static class AsyncPriorityEvent {
    }

    @Decorator
    @Priority(10)
    public static class ManagedSerializableOnlyDecorator implements Serializable {
        @Inject
        @Delegate
        @Any
        Serializable delegate;
    }

    public static class ManagedAlternativeMetadataExtension implements Extension {
        public void observeSausagePat(@Observes ProcessAnnotatedType<ManagedAlternativeSausage> event) {
            final AnnotatedConstructor<ManagedAlternativeSausage> originalConstructor =
                    event.getAnnotatedType().getConstructors().iterator().next();

            event.setAnnotatedType(new jakarta.enterprise.inject.spi.AnnotatedType<ManagedAlternativeSausage>() {
                @Override
                public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
                    return Collections.emptySet();
                }

                @Override
                public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
                    return false;
                }

                @Override
                public Set<Type> getTypeClosure() {
                    Set<Type> typeClosure = new HashSet<Type>();
                    typeClosure.add(ManagedAlternativeSausage.class);
                    typeClosure.add(Object.class);
                    return typeClosure;
                }

                @Override
                public Type getBaseType() {
                    return ManagedAlternativeSausage.class;
                }

                @Override
                public Set<Annotation> getAnnotations() {
                    return Collections.emptySet();
                }

                @Override
                public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
                    return null;
                }

                @Override
                public Set<jakarta.enterprise.inject.spi.AnnotatedMethod<? super ManagedAlternativeSausage>> getMethods() {
                    return Collections.emptySet();
                }

                @Override
                public Class<ManagedAlternativeSausage> getJavaClass() {
                    return ManagedAlternativeSausage.class;
                }

                @Override
                public Set<jakarta.enterprise.inject.spi.AnnotatedField<? super ManagedAlternativeSausage>> getFields() {
                    return Collections.emptySet();
                }

                @Override
                public Set<AnnotatedConstructor<ManagedAlternativeSausage>> getConstructors() {
                    return Collections.singleton(originalConstructor);
                }
            });
        }
    }

    @Test
    public void testManagedBootstrap() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(MyBean.class);

        SyringeBootstrap bootstrap = new SyringeBootstrap(classes, Thread.currentThread().getContextClassLoader());
        Syringe syringe = bootstrap.bootstrap();

        assertNotNull(syringe);
        BeanManager bm = syringe.getBeanManager();
        assertNotNull(bm);

        MyBean bean = syringe.getBeanManager().createInstance().select(MyBean.class).get();
        assertNotNull(bean);
        assertEquals("hello", bean.hello());

        bootstrap.shutdown();
    }

    @Test
    public void testStandaloneSECompatibility() {
        // Test that Syringe still works in SE mode with explicit discovered classes.
        Syringe syringe = new Syringe();
        syringe.initialize();
        syringe.addDiscoveredClass(MyBean.class);
        syringe.start();

        MyBean bean = syringe.getBeanManager().createInstance().select(MyBean.class).get();
        assertNotNull(bean);
        assertEquals("hello", bean.hello());

        syringe.shutdown();
    }

    @Test
    public void testBootstrapFailureCleansGlobalRegistries() throws Exception {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(InvalidScopeStereotype.class);
        classes.add(InvalidScopedBean.class);

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        URLClassLoader isolatedLoader = new URLClassLoader(new URL[0], original);
        Thread.currentThread().setContextClassLoader(isolatedLoader);
        try {
            SyringeBootstrap bootstrap = new SyringeBootstrap(classes, isolatedLoader);

            // Bootstrap should fail because of invalid scope stereotype.
            assertThrows(DefinitionException.class, bootstrap::bootstrap);

            // Regression for failed bootstrap leak: BeanManager registry must not retain classloader.
            assertNull(BeanManagerImpl.getRegisteredBeanManager(isolatedLoader));

            // Regression for failed bootstrap leak: proxy container registry must not retain classloader.
            Field registryField = ClientProxyGenerator.class.getDeclaredField("containerRegistry");
            registryField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ClassLoader, ?> registry = (Map<ClassLoader, ?>) registryField.get(null);
            assertFalse(registry.containsKey(isolatedLoader));
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            isolatedLoader.close();
        }
    }

    @Test
    public void testManagedBootstrapForcesCdiFullLegacyInterception() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(LegacyStyleInterceptor.class);
        classes.add(LegacyInterceptedBean.class);

        SyringeBootstrap bootstrap = new SyringeBootstrap(classes, Thread.currentThread().getContextClassLoader());
        Syringe syringe = assertDoesNotThrow(bootstrap::bootstrap);
        assertNotNull(syringe);
        bootstrap.shutdown();
    }

    @Test
    public void testManagedBootstrapHonorsBeansXmlAllDiscoveryMode() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(PlainManagedBean.class);
        String beansXmlContent = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\"></beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(beansXmlContent.getBytes(StandardCharsets.UTF_8)));

        SyringeBootstrap bootstrap = new SyringeBootstrap(
                classes,
                Thread.currentThread().getContextClassLoader(),
                Collections.singletonList(beansXml));
        Syringe syringe = bootstrap.bootstrap();
        try {
            PlainManagedBean bean = syringe.getBeanManager().createInstance().select(PlainManagedBean.class).get();
            assertEquals("plain", bean.value());
        } finally {
            bootstrap.shutdown();
        }
    }

    @Test
    public void testManagedBootstrapTreatsLegacyBeansXmlWithoutDiscoveryModeAsAll() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(PlainManagedBean.class);
        String beansXmlContent = "<beans xmlns=\"http://java.sun.com/xml/ns/javaee\"></beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(beansXmlContent.getBytes(StandardCharsets.UTF_8)));

        SyringeBootstrap bootstrap = new SyringeBootstrap(
                classes,
                Thread.currentThread().getContextClassLoader(),
                Collections.singletonList(beansXml));
        Syringe syringe = bootstrap.bootstrap();
        try {
            PlainManagedBean bean = syringe.getBeanManager().createInstance().select(PlainManagedBean.class).get();
            assertEquals("plain", bean.value());
        } finally {
            bootstrap.shutdown();
        }
    }

    @Test
    public void testManagedBootstrapUsesPreDiscoveredClassArchiveModes() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(PlainManagedBean.class);
        classes.add(ManagedArchiveModeNoneBean.class);

        Map<String, BeanArchiveMode> classArchiveModes = new java.util.HashMap<String, BeanArchiveMode>();
        classArchiveModes.put(PlainManagedBean.class.getName(), BeanArchiveMode.EXPLICIT);
        classArchiveModes.put(ManagedArchiveModeNoneBean.class.getName(), BeanArchiveMode.NONE);

        SyringeBootstrap bootstrap = new SyringeBootstrap(
                classes,
                Thread.currentThread().getContextClassLoader(),
                null,
                "ManagedArchiveModesTest1234567890abcdef1234567890abcdef12345678.war",
                classArchiveModes);
        Syringe syringe = bootstrap.bootstrap();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, beanManager.getBeans(PlainManagedBean.class).size());
            assertTrue(beanManager.getBeans(ManagedArchiveModeNoneBean.class).isEmpty());
        } finally {
            bootstrap.shutdown();
        }
    }

    @Test
    public void testManagedBootstrapLoadsExtensionServicesWhenScopedResourcePathDoesNotContainDeploymentName() throws Exception {
        Path tempRoot = Files.createTempDirectory("syringe-bootstrap-extension");
        Path serviceDir = tempRoot.resolve("META-INF/services");
        Files.createDirectories(serviceDir);
        Path serviceFile = serviceDir.resolve("jakarta.enterprise.inject.spi.Extension");
        Files.write(serviceFile,
                Collections.singletonList(ServiceLoadedVetoExtension.class.getName()),
                StandardCharsets.UTF_8);

        URLClassLoader serviceLoader = new URLClassLoader(
                new URL[]{tempRoot.toUri().toURL()},
                Thread.currentThread().getContextClassLoader());
        try {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            classes.add(ServiceLoadedInvalidBean.class);
            SyringeBootstrap bootstrap = new SyringeBootstrap(
                    classes,
                    serviceLoader,
                    null,
                    "CustomDecoratorTestfd9d4c94cdd2798a5138811e31f24b6be667beb.war");
            Syringe syringe = assertDoesNotThrow(bootstrap::bootstrap);
            assertNotNull(syringe);
            bootstrap.shutdown();
        } finally {
            serviceLoader.close();
        }
    }

    @Test
    public void testManagedBootstrapTrimmedArchiveKeepsPatForNonBeanDefiningTypes() throws Exception {
        TrimmedPatRecorderExtension.reset();

        Path tempRoot = Files.createTempDirectory("syringe-bootstrap-trimmed");
        Path serviceDir = tempRoot.resolve("META-INF/services");
        Files.createDirectories(serviceDir);
        Path serviceFile = serviceDir.resolve("jakarta.enterprise.inject.spi.Extension");
        Files.write(serviceFile,
                Collections.singletonList(TrimmedPatRecorderExtension.class.getName()),
                StandardCharsets.UTF_8);

        URLClassLoader serviceLoader = new URLClassLoader(
                new URL[]{tempRoot.toUri().toURL()},
                Thread.currentThread().getContextClassLoader());
        try {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            classes.add(TrimmedBikeProducer.class);
            classes.add(TrimmedBike.class);
            classes.add(TrimmedBus.class);

            String beansXmlContent = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                    "version=\"3.0\" bean-discovery-mode=\"all\"><trim/></beans>";
            BeansXml beansXml = new BeansXmlParser().parse(
                    new ByteArrayInputStream(beansXmlContent.getBytes(StandardCharsets.UTF_8)));

            SyringeBootstrap bootstrap = new SyringeBootstrap(
                    classes,
                    serviceLoader,
                    Collections.singletonList(beansXml),
                    "TrimmedBeanArchiveTest1234567890abcdef1234567890abcdef12345678.war");

            Syringe syringe = bootstrap.bootstrap();
            try {
                assertTrue(TrimmedPatRecorderExtension.bikeProducerPatFired.get());
                assertFalse(TrimmedPatRecorderExtension.bikeProducerPbaFired.get());
                assertEquals(0, syringe.getBeanManager().getBeans(TrimmedBike.class).size());
                assertEquals(1, syringe.getBeanManager().getBeans(TrimmedBus.class).size());
            } finally {
                bootstrap.shutdown();
            }
        } finally {
            serviceLoader.close();
        }
    }

    @Test
    public void testManagedBootstrapAnnotatedArchiveRetainsOriginallyBeanDefiningTypeAfterPatReplacement() throws Exception {
        Path tempRoot = Files.createTempDirectory("syringe-bootstrap-managed-alternative-metadata");
        Path serviceDir = tempRoot.resolve("META-INF/services");
        Files.createDirectories(serviceDir);
        Path serviceFile = serviceDir.resolve("jakarta.enterprise.inject.spi.Extension");
        Files.write(serviceFile,
                Collections.singletonList(ManagedAlternativeMetadataExtension.class.getName()),
                StandardCharsets.UTF_8);

        URLClassLoader serviceLoader = new URLClassLoader(
                new URL[]{tempRoot.toUri().toURL()},
                Thread.currentThread().getContextClassLoader());
        try {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            classes.add(ManagedAlternativeSausage.class);

            String beansXmlContent = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                    "version=\"3.0\" bean-discovery-mode=\"annotated\"></beans>";
            BeansXml beansXml = new BeansXmlParser().parse(
                    new ByteArrayInputStream(beansXmlContent.getBytes(StandardCharsets.UTF_8)));

            SyringeBootstrap bootstrap = new SyringeBootstrap(
                    classes,
                    serviceLoader,
                    Collections.singletonList(beansXml),
                    "AlternativeMetadataTest1234567890abcdef1234567890abcdef12345678.war");

            Syringe syringe = bootstrap.bootstrap();
            try {
                BeanManager bm = syringe.getBeanManager();
                assertEquals(1, bm.getBeans(ManagedAlternativeSausage.class, Any.Literal.INSTANCE).size());
                assertTrue(bm.getBeans(ManagedAlternativeSausage.class, new ManagedCheapLiteral()).isEmpty());
                assertTrue(bm.getBeans(ManagedAlternativeSausage.class, new ManagedExpensiveLiteral()).isEmpty());
            } finally {
                bootstrap.shutdown();
            }
        } finally {
            serviceLoader.close();
        }
    }

    @Test
    public void testManagedBootstrapRejectsAsyncObserverPriorityByDefault() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(AsyncPriorityObserverBean.class);
        classes.add(AsyncPriorityEvent.class);

        SyringeBootstrap bootstrap = new SyringeBootstrap(
                classes,
                Thread.currentThread().getContextClassLoader(),
                null,
                "ObserverNotificationsStrictTest1234567890abcdef1234567890abcdef12345678.war");

        assertThrows(DefinitionException.class, bootstrap::bootstrap);
    }

    @Test
    public void testManagedBootstrapAllowsLegacyMixedObserversCompatibilityMode() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(AsyncPriorityObserverBean.class);
        classes.add(AsyncPriorityEvent.class);

        SyringeBootstrap bootstrap = new SyringeBootstrap(
                classes,
                Thread.currentThread().getContextClassLoader(),
                null,
                "MixedObserversTest1234567890abcdef1234567890abcdef12345678.war");

        Syringe syringe = assertDoesNotThrow(bootstrap::bootstrap);
        assertNotNull(syringe);
        bootstrap.shutdown();
    }

    @Test
    public void testManagedBootstrapRejectsDecoratorWithNonPassivatingInjectedField() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(CityDecorator.class);
        classes.add(CityInterface.class);
        classes.add(NonPassivating.class);
        classes.add(UnderwaterCity.class);

        String beansXmlContent = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"annotated\">" +
                "<decorators><class>" + CityDecorator.class.getName() + "</class></decorators>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(beansXmlContent.getBytes(StandardCharsets.UTF_8)));

        SyringeBootstrap bootstrap = new SyringeBootstrap(
                classes,
                Thread.currentThread().getContextClassLoader(),
                Collections.singletonList(beansXml),
                "DecoratorWithNonPassivatingInjectedFieldTest1234567890abcdef1234567890abcdef12345678.war");

        assertThrows(DeploymentException.class, bootstrap::bootstrap);
    }

    @Test
    public void testManagedBootstrapRejectsDecoratorWithNoDecoratedTypesAsDefinitionError() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(ManagedSerializableOnlyDecorator.class);

        SyringeBootstrap bootstrap = new SyringeBootstrap(
                classes,
                Thread.currentThread().getContextClassLoader(),
                null,
                "DecoratorWithNoDecoratedTypesManaged1234567890abcdef1234567890abcdef12345678.war");

        assertThrows(DefinitionException.class, bootstrap::bootstrap);
    }

    @Test
    public void testManagedBootstrapRejectsProgrammaticDecoratorWithNoDecoratedTypesAsDefinitionError() throws Exception {
        Path tempRoot = Files.createTempDirectory("syringe-bootstrap-programmatic-decorator-nodecoratedtypes");
        Path serviceDir = tempRoot.resolve("META-INF/services");
        Files.createDirectories(serviceDir);
        Path serviceFile = serviceDir.resolve("jakarta.enterprise.inject.spi.Extension");
        Files.write(
                serviceFile,
                Collections.singletonList(
                        com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.definition.broken.nodecoratedtypes.GlueDecoratorExtension.class.getName()),
                StandardCharsets.UTF_8);

        URLClassLoader serviceLoader = new URLClassLoader(
                new URL[]{tempRoot.toUri().toURL()},
                Thread.currentThread().getContextClassLoader());
        try {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            classes.add(
                    com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.definition.broken.nodecoratedtypes.Glue.class);
            classes.add(
                    com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.definition.broken.nodecoratedtypes.GlueDecorator.class);

            SyringeBootstrap bootstrap = new SyringeBootstrap(
                    classes,
                    serviceLoader,
                    null,
                    "DecoratorWithNoDecoratedTypes1Test1234567890abcdef1234567890abcdef12345678.war");

            assertThrows(DefinitionException.class, bootstrap::bootstrap);
        } finally {
            serviceLoader.close();
        }
    }
}
