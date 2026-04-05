package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.producer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Producer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyntheticProducerTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                AnotherFactory.class,
                Factory.class,
                SpaceSuit.class,
                Toy.class
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
    void testStaticProducerField() {
        AnnotatedField<? super Factory> field = this.<Factory, AnnotatedField<Factory>>getAnnotatedMember(Factory.class, "WOODY");
        Producer<Toy> producer = cast(beanManager.getProducerFactory(field, null).createProducer(null));
        assertNotNull(producer);
        assertTrue(producer.getInjectionPoints().isEmpty());
        Toy woody = producer.produce(beanManager.<Toy>createCreationalContext(null));
        assertEquals("Woody", woody.getName());
    }

    @Test
    void testNonStaticProducerField() {
        AnnotatedField<? super AnotherFactory> field =
                this.<AnotherFactory, AnnotatedField<AnotherFactory>>getAnnotatedMember(AnotherFactory.class, "jessie");
        Bean<AnotherFactory> declaringBean = cast(beanManager.resolve(beanManager.getBeans(AnotherFactory.class)));
        Producer<Toy> producer = cast(beanManager.getProducerFactory(field, declaringBean).createProducer(null));
        assertNotNull(producer);
        assertTrue(producer.getInjectionPoints().isEmpty());
        Toy jessie = producer.produce(beanManager.<Toy>createCreationalContext(null));
        assertEquals("Jessie", jessie.getName());
    }

    @Test
    void testStaticProducerMethod() {
        AnnotatedMethod<? super Factory> method = this.<Factory, AnnotatedMethod<Factory>>getAnnotatedMember(Factory.class, "getBuzz");
        Producer<Toy> producer = cast(beanManager.getProducerFactory(method, null).createProducer(null));
        assertNotNull(producer);
        validateInjectionPoints(producer.getInjectionPoints());
        Toy buzz = producer.produce(beanManager.<Toy>createCreationalContext(null));
        assertEquals("Buzz Lightyear", buzz.getName());
    }

    @Test
    void testNonStaticProducerMethod() {
        AnnotatedMethod<? super AnotherFactory> method =
                this.<AnotherFactory, AnnotatedMethod<AnotherFactory>>getAnnotatedMember(AnotherFactory.class, "getRex");
        Bean<AnotherFactory> declaringBean = cast(beanManager.resolve(beanManager.getBeans(AnotherFactory.class)));
        Producer<Toy> producer = cast(beanManager.getProducerFactory(method, declaringBean).createProducer(null));
        assertNotNull(producer);
        validateInjectionPoints(producer.getInjectionPoints());
        Toy rex = producer.produce(beanManager.<Toy>createCreationalContext(null));
        assertEquals("Rex", rex.getName());
    }

    @Test
    void testInvalidProducerMethod1() {
        AnnotatedMethod<? super Factory> method = this.<Factory, AnnotatedMethod<Factory>>getAnnotatedMember(Factory.class, "invalidProducerMethod1");
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                beanManager.getProducerFactory(method, null).createProducer(null);
            }
        });
    }

    @Test
    void testInvalidProducerMethod2() {
        AnnotatedMethod<? super Factory> method = this.<Factory, AnnotatedMethod<Factory>>getAnnotatedMember(Factory.class, "invalidProducerMethod2");
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                beanManager.getProducerFactory(method, null).createProducer(null);
            }
        });
    }

    @Test
    void testInvalidProducerField1() {
        AnnotatedField<? super Factory> field = this.<Factory, AnnotatedField<Factory>>getAnnotatedMember(Factory.class, "INVALID_FIELD1");
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                beanManager.getProducerFactory(field, null).createProducer(null);
            }
        });
    }

    @Test
    void testInvalidProducerField2() {
        AnnotatedField<? super Factory> field = this.<Factory, AnnotatedField<Factory>>getAnnotatedMember(Factory.class, "INVALID_FIELD2");
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                beanManager.getProducerFactory(field, null).createProducer(null);
            }
        });
    }

    @Test
    void testInvalidProducerField3() {
        AnnotatedField<? super Factory> field = this.<Factory, AnnotatedField<Factory>>getAnnotatedMember(Factory.class, "INVALID_FIELD3");
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                beanManager.getProducerFactory(field, null).createProducer(null);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <X, A extends AnnotatedMember<? super X>> A getAnnotatedMember(Class<X> javaClass, String memberName) {
        AnnotatedType<X> type = beanManager.createAnnotatedType(javaClass);
        for (AnnotatedField<? super X> field : type.getFields()) {
            if (field.getJavaMember().getName().equals(memberName)) {
                return (A) field;
            }
        }
        for (AnnotatedMethod<? super X> method : type.getMethods()) {
            if (method.getJavaMember().getName().equals(memberName)) {
                return (A) method;
            }
        }
        throw new IllegalArgumentException("Member " + memberName + " not found on " + javaClass);
    }

    private void validateInjectionPoints(Set<InjectionPoint> injectionPoints) {
        assertEquals(2, injectionPoints.size());
        for (InjectionPoint ip : injectionPoints) {
            AnnotatedParameter<Factory> parameter = this.<AnnotatedParameter<Factory>>cast(ip.getAnnotated());
            if (parameter.getPosition() == 0) {
                assertEquals(BeanManager.class, parameter.getBaseType());
            } else if (parameter.getPosition() == 1) {
                Type baseType = parameter.getBaseType();
                if (baseType instanceof ParameterizedType && ((ParameterizedType) baseType).getRawType() instanceof Class<?>) {
                    assertEquals(SpaceSuit.class, ((ParameterizedType) baseType).getRawType());
                } else {
                    fail();
                }
            } else {
                fail("Unexpected injection point " + ip);
            }
            assertFalse(ip.isDelegate());
            assertFalse(ip.isTransient());
            assertNull(ip.getBean());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object obj) {
        return (T) obj;
    }
}
