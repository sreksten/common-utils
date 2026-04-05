package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.beanAttributes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.beanAttributes.support.annotated.AnnotatedTypeWrapper;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateBeanAttributesTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Animal.class,
                Fish.class,
                InvalidBeanType.class,
                Landmark.class,
                Mountain.class,
                Natural.class,
                TundraStereotype.class,
                WaterBody.class,
                Wild.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanAttributesForManagedBean() {
        AnnotatedType<Mountain> type = beanManager.createAnnotatedType(Mountain.class);
        BeanAttributes<Mountain> attributes = beanManager.createBeanAttributes(type);

        assertTrue(typeSetMatches(attributes.getTypes(), Landmark.class, Object.class));
        assertTrue(typeSetMatches(attributes.getStereotypes(), TundraStereotype.class));
        assertTrue(annotationSetMatches(attributes.getQualifiers(), Natural.class, Any.class));
        assertEquals(ApplicationScoped.class, attributes.getScope());
        assertEquals("mountain", attributes.getName());
        assertTrue(attributes.isAlternative());
    }

    @Test
    void testBeanAttributesForManagedBeanWithModifiedAnnotatedType() {
        AnnotatedType<Mountain> type = beanManager.createAnnotatedType(Mountain.class);
        AnnotatedType<Mountain> wrappedType = new AnnotatedTypeWrapper<Mountain>(type, false, NamedLiteral.of("Mount Blanc"));
        BeanAttributes<Mountain> attributes = beanManager.createBeanAttributes(wrappedType);

        assertTrue(typeSetMatches(attributes.getTypes(), Mountain.class, Landmark.class, Object.class));
        assertTrue(attributes.getStereotypes().isEmpty());
        assertTrue(annotationSetMatches(attributes.getQualifiers(), Named.class, Any.class, Default.class));
        assertEquals(Dependent.class, attributes.getScope());
        assertEquals("Mount Blanc", attributes.getName());
        assertFalse(attributes.isAlternative());
    }

    @SuppressWarnings("unchecked")
    private void verifyLakeFish(BeanAttributes<?> attributes) {
        assertTrue(typeSetMatches(attributes.getTypes(), Fish.class, Object.class));
        assertTrue(typeSetMatches(attributes.getStereotypes(), TundraStereotype.class));
        assertTrue(annotationSetMatches(attributes.getQualifiers(), Natural.class, Any.class, Named.class));
        assertEquals(ApplicationScoped.class, attributes.getScope());
        assertEquals("fish", attributes.getName());
        assertTrue(attributes.isAlternative());
    }

    @SuppressWarnings("unchecked")
    private void verifyDamFish(BeanAttributes<?> attributes) {
        assertTrue(typeSetMatches(attributes.getTypes(), Fish.class, Animal.class, Object.class));
        assertTrue(annotationSetMatches(attributes.getQualifiers(), Wild.class, Any.class));
        assertTrue(attributes.getStereotypes().isEmpty());
        assertEquals(Dependent.class, attributes.getScope());
        assertNull(attributes.getName());
        assertFalse(attributes.isAlternative());
    }

    @SuppressWarnings("unchecked")
    private void verifyVolume(BeanAttributes<?> attributes) {
        assertTrue(typeSetMatches(attributes.getTypes(), long.class, Object.class));
        assertTrue(annotationSetMatches(attributes.getQualifiers(), Any.class, Default.class, Named.class));
        assertTrue(attributes.getStereotypes().isEmpty());
        assertEquals(Dependent.class, attributes.getScope());
        assertEquals("volume", attributes.getName());
        assertFalse(attributes.isAlternative());
    }

    @Test
    void testInvalidMember() {
        AnnotatedConstructor<?> constructor = beanManager.createAnnotatedType(InvalidBeanType.class).getConstructors().iterator().next();
        assertThrows(IllegalArgumentException.class, () -> beanManager.createBeanAttributes(constructor));
    }

    private static boolean typeSetMatches(Set<?> types, Object... requiredTypes) {
        return types.size() == requiredTypes.length && types.containsAll(Arrays.asList(requiredTypes));
    }

    private static boolean annotationSetMatches(Set<Annotation> annotations,
                                                Class<? extends Annotation>... expectedTypes) {
        if (annotations.size() != expectedTypes.length) {
            return false;
        }
        Set<Class<? extends Annotation>> actualTypes = new HashSet<Class<? extends Annotation>>();
        for (Annotation annotation : annotations) {
            actualTypes.add(annotation.annotationType());
        }
        return actualTypes.containsAll(Arrays.asList(expectedTypes));
    }
}
