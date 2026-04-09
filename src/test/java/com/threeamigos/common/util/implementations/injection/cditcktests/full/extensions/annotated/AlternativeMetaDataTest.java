package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.annotated;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlternativeMetaDataTest {

    private Syringe syringe;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(new InMemoryMessageHandler());
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(AbstractC.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(WildCat.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testBaseType() {
        AnnotatedType<?> annotatedType = syringe.getBeanManager().createAnnotatedType(DogHouse.class);
        assertEquals(DogHouse.class, annotatedType.getBaseType());
    }

    @Test
    void testTypeClosure() {
        AnnotatedType<?> annotatedType = syringe.getBeanManager().createAnnotatedType(ClassD.class);
        assertTrue(annotatedType.getTypeClosure().contains(Object.class));
        assertTrue(annotatedType.getTypeClosure().contains(InterfaceA.class));
        assertTrue(annotatedType.getTypeClosure().contains(InterfaceB.class));
        assertTrue(annotatedType.getTypeClosure().contains(AbstractC.class));
        assertTrue(annotatedType.getTypeClosure().contains(ClassD.class));
    }

    @Test
    void testGetAnnotation() {
        AnnotatedType<?> annotatedType = syringe.getBeanManager().createAnnotatedType(ClassD.class);
        assertTrue(annotatedType.getAnnotation(RequestScoped.class) != null);
        assertTrue(annotatedType.getAnnotation(ApplicationScoped.class) == null);
    }

    @Test
    void testGetAnnotations() {
        AnnotatedType<?> annotatedType = syringe.getBeanManager().createAnnotatedType(ClassD.class);
        assertEquals(2, annotatedType.getAnnotations().size());
        assertTrue(annotationSetMatches(annotatedType.getAnnotations(), RequestScoped.class, Tame.class));

        AnnotatedType<WildCat> annotatedWildCatType = syringe.getBeanManager().createAnnotatedType(WildCat.class);
        assertTrue(annotationSetMatches(annotatedWildCatType.getAnnotations(), RequestScoped.class));
    }

    @Test
    void testIsAnnotationPresent() {
        AnnotatedType<?> annotatedType = syringe.getBeanManager().createAnnotatedType(ClassD.class);
        assertTrue(annotatedType.isAnnotationPresent(RequestScoped.class));
        assertFalse(annotatedType.isAnnotationPresent(ApplicationScoped.class));
    }

    @Test
    void testConstructors() {
        AnnotatedType<WildCat> annotatedType = syringe.getBeanManager().createAnnotatedType(WildCat.class);
        Set<AnnotatedConstructor<WildCat>> constructors = annotatedType.getConstructors();
        assertEquals(4, constructors.size());
        for (AnnotatedConstructor<WildCat> constructor : constructors) {
            if (java.lang.reflect.Modifier.isPrivate(constructor.getJavaMember().getModifiers())) {
                verifyConstructor(constructor, Integer.class);
            } else if (java.lang.reflect.Modifier.isPublic(constructor.getJavaMember().getModifiers())) {
                verifyConstructor(constructor, String.class);
            } else if (java.lang.reflect.Modifier.isProtected(constructor.getJavaMember().getModifiers())) {
                verifyConstructor(constructor, Cat.class);
            } else {
                verifyConstructor(constructor, Date.class);
            }
        }
    }

    @Test
    void testMethods() {
        AnnotatedType<WildCat> annotatedType = syringe.getBeanManager().createAnnotatedType(WildCat.class);
        Set<AnnotatedMethod<? super WildCat>> methods = annotatedType.getMethods();
        String[] names = new String[]{"yowl", "jump", "bite", "getName"};
        assertEquals(4, methods.size());
        for (AnnotatedMethod<? super WildCat> method : methods) {
            assertTrue(arrayContains(names, method.getJavaMember().getName()));
        }
    }

    @Test
    void testFields() {
        AnnotatedType<WildCat> annotatedType = syringe.getBeanManager().createAnnotatedType(WildCat.class);
        Set<AnnotatedField<? super WildCat>> fields = annotatedType.getFields();
        String[] names = new String[]{"age", "name", "publicName", "isOld"};
        assertEquals(4, fields.size());
        for (AnnotatedField<? super WildCat> field : fields) {
            assertTrue(arrayContains(names, field.getJavaMember().getName()));
        }
    }

    private static void verifyConstructor(AnnotatedConstructor<WildCat> constructor, Class<?> paramClass) {
        Class<?>[] params = constructor.getJavaMember().getParameterTypes();
        assertEquals(1, params.length);
        assertEquals(paramClass, params[0]);
    }

    private static boolean arrayContains(Object[] array, Object objectToFind) {
        if (array == null || objectToFind == null) {
            return false;
        }
        for (Object element : array) {
            if (objectToFind.equals(element)) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    private static boolean annotationSetMatches(Set<? extends Annotation> annotations,
                                                Class<? extends Annotation>... expectedAnnotationTypes) {
        if (annotations == null) {
            return false;
        }
        if (annotations.size() != expectedAnnotationTypes.length) {
            return false;
        }
        Set<Class<? extends Annotation>> required = new HashSet<Class<? extends Annotation>>(Arrays.asList(expectedAnnotationTypes));
        for (Annotation annotation : annotations) {
            if (!required.contains(annotation.annotationType())) {
                return false;
            }
        }
        return true;
    }
}
