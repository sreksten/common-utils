package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.equivalence.interceptorbinding;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InterceptorBindingEquivalenceTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Level.class,
                Missile.class,
                MissileInterceptor.class,
                MissileInterceptorBinding.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addInterceptorsBeansXml(syringe, MissileInterceptor.class);
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @SuppressWarnings("serial")
    @Test
    void testAreInterceptorBindingsEquivalent() {
        Annotation literal1 = new MissileInterceptorBinding() {
        };
        Annotation literal2 = new MissileInterceptorBinding() {
            @Override
            public int numberOfTargets() {
                return 7;
            }

            @Override
            public Level level() {
                return Level.B;
            }
        };
        Annotation containerProvided = getContainerProvidedInterceptorBinding(literal1);
        assertTrue(beanManager.areInterceptorBindingsEquivalent(literal1, containerProvided));
        assertFalse(beanManager.areInterceptorBindingsEquivalent(literal1, literal2));
        assertFalse(beanManager.areInterceptorBindingsEquivalent(containerProvided, literal2));
    }

    @SuppressWarnings("serial")
    @Test
    void testGetInterceptorBindingHashCode() {
        Annotation literal1 = new MissileInterceptorBinding() {
        };
        Annotation literal2 = new MissileInterceptorBinding() {
            @Override
            public String position() {
                return "hill";
            }
        };
        Annotation containerProvided = getContainerProvidedInterceptorBinding(literal1);
        assertEquals(beanManager.getInterceptorBindingHashCode(literal1),
                beanManager.getInterceptorBindingHashCode(containerProvided));
        assertNotEquals(beanManager.getInterceptorBindingHashCode(literal1),
                beanManager.getInterceptorBindingHashCode(literal2));
        assertNotEquals(beanManager.getInterceptorBindingHashCode(containerProvided),
                beanManager.getInterceptorBindingHashCode(literal2));
    }

    private Annotation getContainerProvidedInterceptorBinding(Annotation literal) {
        List<Interceptor<?>> interceptors = beanManager.resolveInterceptors(InterceptionType.AROUND_INVOKE, literal);
        assertFalse(interceptors.isEmpty());
        return interceptors.get(0).getInterceptorBindings().iterator().next();
    }

    private static void addInterceptorsBeansXml(Syringe syringe, Class<?>... interceptorClasses) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" ")
                .append("version=\"3.0\" bean-discovery-mode=\"all\"><interceptors>");
        for (Class<?> interceptorClass : interceptorClasses) {
            xmlBuilder.append("<class>").append(interceptorClass.getName()).append("</class>");
        }
        xmlBuilder.append("</interceptors></beans>");

        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xmlBuilder.toString().getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
