package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.injectionPoint;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateInjectionPointTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Book.class,
                Fictional.class,
                Library.class,
                Magazine.class,
                Monograph.class,
                NotABean.class
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
    void testField() {
        AnnotatedType<?> type = beanManager.createAnnotatedType(Library.class);
        assertEquals(1, type.getFields().size());
        AnnotatedField<?> field = type.getFields().iterator().next();

        InjectionPoint ip = beanManager.createInjectionPoint(field);
        validateParameterizedType(ip.getType(), Book.class, String.class);
        annotationSetMatches(ip.getQualifiers(), Monograph.class, Fictional.class);
        assertNull(ip.getBean());
        assertEquals(field.getJavaMember(), ip.getMember());
        assertNotNull(ip.getAnnotated());
        assertFalse(ip.isDelegate());
        assertTrue(ip.isTransient());
    }

    @Test
    void testConstructorParameter() {
        AnnotatedType<?> type = beanManager.createAnnotatedType(Library.class);
        assertEquals(1, type.getConstructors().size());
        AnnotatedConstructor<?> constructor = type.getConstructors().iterator().next();
        AnnotatedParameter<?> parameter = constructor.getParameters().get(1);

        InjectionPoint ip = beanManager.createInjectionPoint(parameter);
        validateParameterizedType(ip.getType(), Book.class, String.class);
        annotationSetMatches(ip.getQualifiers(), Fictional.class);
        assertNull(ip.getBean());
        assertEquals(constructor.getJavaMember(), ip.getMember());
        assertNotNull(ip.getAnnotated());
        assertFalse(ip.isDelegate());
        assertFalse(ip.isTransient());
    }

    @Test
    void testMethodParameter() {
        AnnotatedType<?> type = beanManager.createAnnotatedType(Library.class);
        assertEquals(1, type.getMethods().size());
        AnnotatedMethod<?> method = type.getMethods().iterator().next();
        AnnotatedParameter<?> parameter = method.getParameters().get(2);

        InjectionPoint ip = beanManager.createInjectionPoint(parameter);
        validateParameterizedType(ip.getType(), Book.class, Integer.class);
        annotationSetMatches(ip.getQualifiers(), Default.class);
        assertNull(ip.getBean());
        assertEquals(method.getJavaMember(), ip.getMember());
        assertNotNull(ip.getAnnotated());
        assertFalse(ip.isDelegate());
        assertFalse(ip.isTransient());
    }

    @Test
    void testInvalidField() {
        AnnotatedField<Magazine> invalidField = new AnnotatedField<Magazine>() {

            @Override
            public boolean isStatic() {
                return false;
            }

            @Override
            public AnnotatedType<Magazine> getDeclaringType() {
                return null;
            }

            @Override
            public Type getBaseType() {
                return null;
            }

            @Override
            public Set<Type> getTypeClosure() {
                return null;
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
                return null;
            }

            @Override
            public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
                return null;
            }

            @Override
            public Set<Annotation> getAnnotations() {
                return null;
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
                return false;
            }

            @Override
            public Field getJavaMember() {
                return null;
            }
        };

        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                beanManager.createInjectionPoint(invalidField);
            }
        });
    }

    @Test
    void testInvalidParameter() {
        AnnotatedType<?> type = beanManager.createAnnotatedType(NotABean.class);
        assertEquals(1, type.getMethods().size());
        AnnotatedMethod<?> method = type.getMethods().iterator().next();
        AnnotatedParameter<?> parameter = method.getParameters().get(0);

        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                beanManager.createInjectionPoint(parameter);
            }
        });
    }

    private void validateParameterizedType(Type type, Class<?> rawType, Type... types) {
        assertTrue(type instanceof ParameterizedType);
        ParameterizedType parameterized = (ParameterizedType) type;
        assertEquals(rawType, parameterized.getRawType());
        assertTrue(Arrays.equals(types, parameterized.getActualTypeArguments()));
    }

    private static boolean annotationSetMatches(Set<Annotation> annotations,
                                                Class<? extends Annotation>... expectedTypes) {
        Set<Class<? extends Annotation>> actualTypes = new HashSet<Class<? extends Annotation>>();
        for (Annotation annotation : annotations) {
            actualTypes.add(annotation.annotationType());
        }
        return expectedTypes.length == annotations.size() && actualTypes.containsAll(Arrays.asList(expectedTypes));
    }
}
