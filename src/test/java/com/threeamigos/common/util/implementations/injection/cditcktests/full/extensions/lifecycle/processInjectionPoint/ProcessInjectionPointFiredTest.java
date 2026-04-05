package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processInjectionPoint;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessInjectionPointFiredTest {

    private Syringe syringe;
    private VerifyingExtension extension;

    @BeforeEach
    void setUp() {
        extension = new VerifyingExtension();

        syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(extension);
        syringe.initialize();
        syringe.addDiscoveredClass(Alpha.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bravo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BravoObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Charlie.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Delta.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(InjectingBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PlainAnnotation.class, BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);
        syringe.start();
    }

    @AfterEach
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testFieldInjectionPoint() {
        InjectionPoint ip = extension.getAlpha();
        assertNotNull(ip);
        assertTrue(annotationTypeSetMatches(ip.getQualifiers(), Foo.class));
        assertNotNull(ip.getBean());
        assertEquals(extension.getInjectingBean(), ip.getBean());
        verifyType(ip, Alpha.class, String.class);
        verifyAnnotated(ip);
        verifyMember(ip, InjectingBean.class);
        assertFalse(ip.isDelegate());
        assertTrue(ip.isTransient());
    }

    @Test
    void testConstructorInjectionPoint() {
        InjectionPoint ip = extension.getBravo();
        assertNotNull(ip);
        assertTrue(annotationTypeSetMatches(ip.getQualifiers(), Bar.class));
        assertNotNull(ip.getBean());
        assertEquals(extension.getInjectingBean(), ip.getBean());
        verifyType(ip, Bravo.class, String.class);
        verifyAnnotated(ip);
        verifyMember(ip, InjectingBean.class);
        assertFalse(ip.isDelegate());
        assertFalse(ip.isTransient());
    }

    @Test
    void testInitializerInjectionPoint() {
        InjectionPoint ip = extension.getCharlie();
        assertNotNull(ip);
        assertTrue(annotationTypeSetMatches(ip.getQualifiers(), Default.class));
        assertNotNull(ip.getBean());
        assertEquals(extension.getInjectingBean(), ip.getBean());
        verifyType(ip, Charlie.class);
        verifyAnnotated(ip);
        verifyMember(ip, InjectingBean.class);
        assertFalse(ip.isDelegate());
        assertFalse(ip.isTransient());
    }

    @Test
    void testProducerMethodInjectionPoint1() {
        InjectionPoint ip = extension.getProducerAlpha();
        assertNotNull(ip);
        assertTrue(annotationTypeSetMatches(ip.getQualifiers(), Foo.class));
        assertNotNull(ip.getBean());
        assertEquals(extension.getProducingBean(), ip.getBean());
        verifyType(ip, Alpha.class, Integer.class);
        verifyAnnotated(ip);
        verifyMember(ip, InjectingBean.class);
        assertFalse(ip.isDelegate());
        assertFalse(ip.isTransient());
    }

    @Test
    void testProducerMethodInjectionPoint2() {
        InjectionPoint ip = extension.getProducerBravo();
        assertNotNull(ip);
        assertTrue(annotationTypeSetMatches(ip.getQualifiers(), Bar.class));
        assertNotNull(ip.getBean());
        assertEquals(extension.getProducingBean(), ip.getBean());
        verifyType(ip, Bravo.class, Integer.class);
        verifyAnnotated(ip);
        verifyMember(ip, InjectingBean.class);
        assertFalse(ip.isDelegate());
        assertFalse(ip.isTransient());
    }

    @Test
    void testObserverMethodInjectionPoint() {
        InjectionPoint charlieIp = extension.getObserverCharliIp();
        assertNotNull(charlieIp);
        verifyType(charlieIp, Charlie.class);

        InjectionPoint deltaIp = extension.getObserverDeltaIp();
        assertNotNull(deltaIp);
        verifyType(deltaIp, Delta.class);
    }

    @Test
    void testDisposerMethodInjectionPoint() {
        InjectionPoint deltaIp = extension.getDisposerDeltaIp();
        assertNotNull(deltaIp);
        verifyType(deltaIp, Delta.class);
    }

    private static void verifyType(InjectionPoint ip, Class<?> rawType, Class<?>... typeParameters) {
        assertEquals(rawType, getRawType(ip.getType()));
        if (typeParameters.length > 0) {
            assertTrue(ip.getType() instanceof ParameterizedType);
            assertTrue(Arrays.equals(typeParameters, getActualTypeArguments(ip.getType())));
        }
    }

    private static void verifyAnnotated(InjectionPoint ip) {
        assertNotNull(ip.getAnnotated());
        assertTrue(ip.getAnnotated().isAnnotationPresent(PlainAnnotation.class));
    }

    private static void verifyMember(InjectionPoint ip, Class<?> declaringClass) {
        assertNotNull(ip.getMember());
        assertEquals(declaringClass, ip.getMember().getDeclaringClass());
    }

    private static boolean annotationTypeSetMatches(Set<? extends Annotation> actual, Class<? extends Annotation>... expected) {
        Set<Class<? extends Annotation>> actualTypes = new HashSet<Class<? extends Annotation>>();
        for (Annotation annotation : actual) {
            actualTypes.add(annotation.annotationType());
        }
        return new HashSet<Class<? extends Annotation>>(actualTypes)
                .equals(new HashSet<Class<? extends Annotation>>(Arrays.asList(expected)));
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        }
        if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() instanceof Class<?>) {
            return (Class<T>) ((ParameterizedType) type).getRawType();
        }
        return null;
    }

    private static Type[] getActualTypeArguments(Type type) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments();
        }
        return new Type[]{};
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
