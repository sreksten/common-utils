package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injectionpoint.dynamic;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicInjectionPointTest {

    @Test
    void testInjectionPointGetBean() {
        Syringe syringe = newSyringe();
        try {
            Bar bar = getBar(syringe);
            assertEquals(getUniqueBean(syringe, Bar.class), bar.getFoo().getInjectionPoint().getBean());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionPointGetType() {
        Syringe syringe = newSyringe();
        try {
            Bar bar = getBar(syringe);
            assertEquals(Foo.class, bar.getFoo().getInjectionPoint().getType());
            assertEquals(NiceFoo.class, bar.getTypeNiceFoo().getInjectionPoint().getType());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionPointGetQualifiers() {
        Syringe syringe = newSyringe();
        try {
            Bar bar = getBar(syringe);
            Set<Annotation> fooQualifiers = bar.getFoo().getInjectionPoint().getQualifiers();
            Set<Annotation> niceFooQualifiers = bar.getQualifierNiceFoo().getInjectionPoint().getQualifiers();

            assertAnnotationTypes(fooQualifiers, Any.class, Default.class);
            assertAnnotationTypes(niceFooQualifiers, Any.class, Nice.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionPointGetMember() {
        Syringe syringe = newSyringe();
        try {
            Bar bar = getBar(syringe);

            Member fieldMember = bar.getFoo().getInjectionPoint().getMember();
            assertNotNull(fieldMember);
            assertTrue(fieldMember instanceof Field);
            Field field = (Field) fieldMember;
            assertEquals("fooInstance", field.getName());
            assertEquals(Instance.class, field.getType());
            assertEquals(Bar.class, field.getDeclaringClass());

            Member methodMember = bar.getInitializerFoo().getInjectionPoint().getMember();
            assertNotNull(methodMember);
            assertTrue(methodMember instanceof Method);
            Method method = (Method) methodMember;
            assertEquals("setInitializerInjectionFooInstance", method.getName());
            assertEquals(1, method.getParameterTypes().length);
            assertEquals(Bar.class, method.getDeclaringClass());

            Member constructorMember = bar.getConstructorInjectionFoo().getInjectionPoint().getMember();
            assertNotNull(constructorMember);
            assertTrue(constructorMember instanceof Constructor);
            Constructor<?> constructor = (Constructor<?>) constructorMember;
            assertTrue(Bar.class.getName().equals(constructor.getName()) || "Bar".equals(constructor.getName()));
            assertNotNull(constructor.getAnnotation(Inject.class));
            assertEquals(Bar.class, constructor.getDeclaringClass());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void testInjectionPointGetAnnotated() {
        Syringe syringe = newSyringe();
        try {
            Bar bar = getBar(syringe);

            Annotated fooFieldAnnotated = bar.getFoo().getInjectionPoint().getAnnotated();
            assertTrue(fooFieldAnnotated instanceof AnnotatedField);
            assertEquals("fooInstance", ((AnnotatedField) fooFieldAnnotated).getJavaMember().getName());
            assertTrue(fooFieldAnnotated.isAnnotationPresent(Any.class));

            Annotated fooInitializerAnnotated = bar.getInitializerFoo().getInjectionPoint().getAnnotated();
            assertTrue(fooInitializerAnnotated instanceof AnnotatedParameter);
            assertEquals(0, ((AnnotatedParameter) fooInitializerAnnotated).getPosition());

            Annotated fooConstructorAnnotated = bar.getConstructorInjectionFoo().getInjectionPoint().getAnnotated();
            assertTrue(fooConstructorAnnotated instanceof AnnotatedParameter);
            assertEquals(0, ((AnnotatedParameter) fooConstructorAnnotated).getPosition());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionPointIsDelegate() {
        Syringe syringe = newSyringe();
        try {
            Bar bar = getBar(syringe);
            assertFalse(bar.getFoo().getInjectionPoint().isDelegate());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionPointIsTransient() {
        Syringe syringe = newSyringe();
        try {
            Bar bar = getBar(syringe);
            assertTrue(bar.getTransientFoo().getInjectionPoint().isTransient());
            assertFalse(bar.getFoo().getInjectionPoint().isTransient());
        } finally {
            syringe.shutdown();
        }
    }

    private Bar getBar(Syringe syringe) {
        return syringe.getBeanManager().createInstance().select(Bar.class).get();
    }

    private <T> Bean<?> getUniqueBean(Syringe syringe, Class<T> type) {
        return syringe.getBeanManager().resolve(syringe.getBeanManager().getBeans(type));
    }

    private void assertAnnotationTypes(Set<Annotation> annotations, Class<? extends Annotation>... expectedTypes) {
        assertEquals(expectedTypes.length, annotations.size());
        for (Class<? extends Annotation> expectedType : expectedTypes) {
            boolean found = false;
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(expectedType)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Missing annotation type: " + expectedType.getName());
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Bar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Nice.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NiceFoo.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
