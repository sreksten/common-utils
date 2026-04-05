package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.decorator.Decorator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.inject.Named;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Execution(ExecutionMode.SAME_THREAD)
class VerifyValuesTest {

    private Syringe syringe;
    private VerifyingExtension extension;

    @BeforeEach
    void setUp() {
        VerifyingExtension.reset();

        syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(VerifyingExtension.class.getName());
        syringe.initialize();

        Class<?>[] fixtureClasses = {
                Alpha.class,
                AlphaQualifier.class,
                AlphaStereotype.class,
                Bravo.class,
                BravoDecorator.class,
                BravoInterceptor.class,
                BravoInterceptorBinding.class,
                BravoInterface.class,
                BravoProducer.class,
                BravoQualifier.class,
                Charlie.class,
                CharlieInterface.class,
                CharlieProducer.class,
                CharlieQualifier.class,
                Mike.class
        };
        for (Class<?> fixtureClass : fixtureClasses) {
            syringe.addDiscoveredClass(fixtureClass, BeanArchiveMode.EXPLICIT);
        }

        addBeansXmlConfiguration(syringe);
        syringe.start();

        extension = VerifyingExtension.getInstance();
        assertNotNull(extension, "Extension should have been instantiated and observed lifecycle events");
    }

    @AfterEach
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testManagedBeanAnnotated() {
        Annotated alphaAnnotated = extension.getAnnotatedMap().get(Alpha.class);
        assertNotNull(alphaAnnotated);
        assertTrue(alphaAnnotated instanceof AnnotatedType);

        @SuppressWarnings("unchecked")
        AnnotatedType<Alpha> alphaAnnotatedType = (AnnotatedType<Alpha>) alphaAnnotated;
        assertEquals(Alpha.class, alphaAnnotatedType.getJavaClass());
        assertEquals(0, alphaAnnotatedType.getMethods().size());
    }

    @Test
    void testProducerMethodAnnotated() {
        Annotated bravoAnnotated = extension.getAnnotatedMap().get(Bravo.class);
        assertNotNull(bravoAnnotated);
        assertTrue(bravoAnnotated instanceof AnnotatedMethod);

        @SuppressWarnings("unchecked")
        AnnotatedMethod<Bravo> bravoAnnotatedMethod = (AnnotatedMethod<Bravo>) bravoAnnotated;
        assertEquals("createBravo", bravoAnnotatedMethod.getJavaMember().getName());
    }

    @Test
    void testProducerMethodBeanAttributes() {
        BeanAttributes<Bravo> attributes = extension.getProducedBravoAttributes();
        assertNotNull(attributes);
        assertEquals(RequestScoped.class, attributes.getScope());
        verifyName(attributes, "createBravo");
        assertTrue(attributes.isAlternative());

        assertTrue(typeSetMatches(attributes.getTypes(), BravoInterface.class, Object.class));
        assertTrue(typeSetMatches(attributes.getStereotypes(), AlphaStereotype.class));
        assertTrue(annotationSetMatches(attributes.getQualifiers(), BravoQualifier.class, Named.class, Any.class));
    }

    @Test
    void testProducerFieldAnnotated() {
        Annotated charlieAnnotated = extension.getAnnotatedMap().get(Charlie.class);
        assertNotNull(charlieAnnotated);
        assertTrue(charlieAnnotated instanceof AnnotatedField);

        @SuppressWarnings("unchecked")
        AnnotatedField<Charlie> charlieAnnotatedField = (AnnotatedField<Charlie>) charlieAnnotated;
        assertEquals("charlie", charlieAnnotatedField.getJavaMember().getName());
    }

    @Test
    void testProducerFieldBeanAttributes() {
        BeanAttributes<Charlie> attributes = extension.getProducedCharlieAttributes();
        assertNotNull(attributes);
        assertEquals(ApplicationScoped.class, attributes.getScope());
        verifyName(attributes, "charlie");
        assertFalse(attributes.isAlternative());

        assertTrue(typeSetMatches(attributes.getTypes(), Object.class, Charlie.class, CharlieInterface.class));
        assertTrue(typeSetMatches(attributes.getStereotypes(), AlphaStereotype.class));
        assertTrue(annotationSetMatches(attributes.getQualifiers(), CharlieQualifier.class, Named.class, Any.class));
    }

    @Test
    void testInterceptorBeanAttributes() {
        BeanAttributes<BravoInterceptor> attributes = extension.getBravoInterceptorAttributes();
        assertNotNull(attributes);
        assertEquals(Dependent.class, attributes.getScope());
        assertFalse(attributes.isAlternative());

        assertTrue(typeSetMatches(attributes.getTypes(), Object.class, BravoInterceptor.class));
        assertTrue(attributes.getStereotypes().isEmpty());
    }

    @Test
    void testDecoratorBeanAttributes() {
        BeanAttributes<BravoDecorator> attributes = extension.getBravoDecoratorAttributes();
        assertNotNull(attributes);
        assertEquals(Dependent.class, attributes.getScope());
        assertFalse(attributes.isAlternative());

        assertTrue(typeSetMatches(attributes.getTypes(), Object.class, BravoDecorator.class, BravoInterface.class));
        assertEquals(1, attributes.getStereotypes().size());
        assertTrue(attributes.getStereotypes().contains(Decorator.class));
    }

    private void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<alternatives>" +
                "<class>" + Alpha.class.getName() + "</class>" +
                "<class>" + BravoProducer.class.getName() + "</class>" +
                "<class>" + CharlieProducer.class.getName() + "</class>" +
                "</alternatives>" +
                "<interceptors><class>" + BravoInterceptor.class.getName() + "</class></interceptors>" +
                "<decorators><class>" + BravoDecorator.class.getName() + "</class></decorators>" +
                "</beans>";

        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    private void verifyName(BeanAttributes<?> attributes, String name) {
        assertEquals(name, attributes.getName());
        for (Annotation qualifier : attributes.getQualifiers()) {
            if (Named.class.equals(qualifier.annotationType())) {
                assertEquals(name, ((Named) qualifier).value());
                return;
            }
        }
        fail("@Named qualifier not found.");
    }

    private boolean typeSetMatches(Set<?> actual, Object... expected) {
        Set<Object> expectedSet = new HashSet<Object>();
        for (Object item : expected) {
            expectedSet.add(item);
        }
        return expectedSet.equals(new HashSet<Object>(actual));
    }

    private boolean annotationSetMatches(Set<Annotation> actual,
                                         Class<? extends Annotation>... expectedTypes) {
        Set<Class<? extends Annotation>> actualTypes = new HashSet<Class<? extends Annotation>>();
        for (Annotation annotation : actual) {
            actualTypes.add(annotation.annotationType());
        }

        Set<Class<? extends Annotation>> expectedSet = new HashSet<Class<? extends Annotation>>();
        for (Class<? extends Annotation> expectedType : expectedTypes) {
            expectedSet.add(expectedType);
        }
        return expectedSet.equals(actualTypes);
    }
}
