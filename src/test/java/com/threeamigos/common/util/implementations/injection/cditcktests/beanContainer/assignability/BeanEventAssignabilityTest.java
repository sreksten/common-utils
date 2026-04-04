package com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.assignability;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanEventAssignabilityTest {

    private Syringe syringe;
    private BeanContainer beanContainer;

    @BeforeEach
    void setUp() {
        syringe = new Syringe("com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.assignability");
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        beanContainer = syringe.getBeanManager();
    }

    @AfterEach
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanMatching() {
        Set<Type> beanTypes = setOf(MyBean.class, MyBeanInterface.class, Object.class);

        assertTrue(beanContainer.isMatchingBean(beanTypes, setOf(), MyBean.class, setOf()));
        assertFalse(beanContainer.isMatchingBean(beanTypes, setOf(), MyBean.class, setOf(Qualifier1.Literal.INSTANCE)));
        assertTrue(beanContainer.isMatchingBean(beanTypes, setOf(Qualifier1.Literal.INSTANCE), MyBean.class,
                setOf(Qualifier1.Literal.INSTANCE)));
        assertTrue(beanContainer.isMatchingBean(beanTypes, setOf(Qualifier1.Literal.INSTANCE, Qualifier2.Literal.INSTANCE),
                MyBean.class, setOf(Qualifier1.Literal.INSTANCE)));

        Set<Type> reducedBeanTypes = setOf(MyBean.class);
        assertTrue(beanContainer.isMatchingBean(reducedBeanTypes, setOf(), MyBean.class, setOf()));
        assertFalse(beanContainer.isMatchingBean(reducedBeanTypes, setOf(), MyBeanInterface.class, setOf()));
        assertTrue(beanContainer.isMatchingBean(reducedBeanTypes, setOf(), Object.class, setOf()));

        assertTrue(beanContainer.isMatchingBean(setOf(MyQualifiedBean.class), setOf(), MyQualifiedBean.class, setOf()));
        assertFalse(beanContainer.isMatchingBean(setOf(MyQualifiedBean.class), setOf(), MyQualifiedBean.class,
                setOf(Qualifier1.Literal.INSTANCE)));
    }

    @Test
    void testBeanMatchingDefaultQualifiers() {
        Set<Type> beanTypes = setOf(MyBean.class, MyBeanInterface.class, Object.class);

        assertTrue(beanContainer.isMatchingBean(beanTypes, setOf(Default.Literal.INSTANCE), MyBean.class, setOf()));
        assertTrue(beanContainer.isMatchingBean(beanTypes, setOf(), MyBean.class, setOf(Default.Literal.INSTANCE)));
        assertTrue(beanContainer.isMatchingBean(beanTypes, setOf(Any.Literal.INSTANCE), MyBean.class, setOf()));
        assertTrue(beanContainer.isMatchingBean(beanTypes, setOf(NamedLiteral.of("foo")), MyBean.class, setOf()));
        assertFalse(beanContainer.isMatchingBean(beanTypes, setOf(Qualifier1.Literal.INSTANCE), MyBean.class, setOf()));
        assertTrue(beanContainer.isMatchingBean(beanTypes, setOf(), MyBean.class, setOf(Any.Literal.INSTANCE)));
        assertTrue(beanContainer.isMatchingBean(beanTypes, setOf(Qualifier1.Literal.INSTANCE), MyBean.class,
                setOf(Any.Literal.INSTANCE)));
    }

    @Test
    void testBeanMatchingNullException() {
        Set<Type> beanTypes = setOf(MyBean.class, MyBeanInterface.class, Object.class);

        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingBean(null, setOf(), MyBean.class, setOf()));
        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingBean(beanTypes, null, MyBean.class, setOf()));
        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingBean(beanTypes, setOf(), null, setOf()));
        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingBean(beanTypes, setOf(), MyBean.class, null));
    }

    @Test
    void testBeanMatchingNonQualifiersException() {
        Set<Type> beanTypes = setOf(MyBean.class, MyBeanInterface.class, Object.class);

        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingBean(beanTypes, setOf(Qualifier1.Literal.INSTANCE, NonQualifier.Literal.INSTANCE),
                        MyBean.class, setOf()));

        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingBean(beanTypes, setOf(), MyBean.class,
                        setOf(Qualifier1.Literal.INSTANCE, NonQualifier.Literal.INSTANCE)));
    }

    @Test
    void testNonLegalBeanTypesIgnored() {
        TypeLiteral<List<?>> listOfWildcard = new TypeLiteral<List<?>>() {
        };
        Set<Type> beanTypes = setOf(MyBean.class, MyBeanInterface.class, listOfWildcard.getType(), Object.class);

        assertTrue(beanContainer.isMatchingBean(beanTypes, setOf(), MyBean.class, setOf()));
        assertFalse(beanContainer.isMatchingBean(beanTypes, setOf(), listOfWildcard.getType(), setOf()));
    }

    @Test
    void testEventMatching() {
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(), MyEvent.class, setOf()));
        assertFalse(beanContainer.isMatchingEvent(MyEvent.class, setOf(), MyEvent.class, setOf(Qualifier1.Literal.INSTANCE)));
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(Qualifier1.Literal.INSTANCE), MyEvent.class,
                setOf(Qualifier1.Literal.INSTANCE)));
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(Qualifier1.Literal.INSTANCE, Qualifier2.Literal.INSTANCE),
                MyEvent.class, setOf(Qualifier1.Literal.INSTANCE)));
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(), MyEventInterface.class, setOf()));
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(), Object.class, setOf()));
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(Default.Literal.INSTANCE), MyEvent.class, setOf()));
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(Any.Literal.INSTANCE), MyEvent.class, setOf()));
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(Qualifier1.Literal.INSTANCE), MyEvent.class, setOf()));
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(NamedLiteral.of("foo")), MyEvent.class, setOf()));
    }

    @Test
    void testEventMatchingDefaultQualifier() {
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(), MyEvent.class, setOf(Default.Literal.INSTANCE)));
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(Default.Literal.INSTANCE), MyEvent.class,
                setOf(Default.Literal.INSTANCE)));
        assertFalse(beanContainer.isMatchingEvent(MyEvent.class, setOf(Qualifier1.Literal.INSTANCE), MyEvent.class,
                setOf(Default.Literal.INSTANCE)));
    }

    @Test
    void testEventMatchingParameterized() {
        TypeLiteral<List<String>> listOfString = new TypeLiteral<List<String>>() {
        };
        TypeLiteral<List<?>> listOfWildcard = new TypeLiteral<List<?>>() {
        };
        TypeLiteral<List<Integer>> listOfInteger = new TypeLiteral<List<Integer>>() {
        };

        assertTrue(beanContainer.isMatchingEvent(listOfString.getType(), setOf(), listOfString.getType(), setOf()));
        assertTrue(beanContainer.isMatchingEvent(listOfString.getType(), setOf(), listOfWildcard.getType(), setOf()));
        assertFalse(beanContainer.isMatchingEvent(listOfString.getType(), setOf(), listOfInteger.getType(), setOf()));
    }

    @Test
    void testEventMatchingAnyQualifier() {
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(), MyEvent.class, setOf(Any.Literal.INSTANCE)));
        assertTrue(beanContainer.isMatchingEvent(MyEvent.class, setOf(Qualifier1.Literal.INSTANCE), MyEvent.class,
                setOf(Any.Literal.INSTANCE)));
    }

    @Test
    void testEventMatchingNullException() {
        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingEvent(null, setOf(), MyEvent.class, setOf()));
        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingEvent(MyEvent.class, null, MyEvent.class, setOf()));
        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingEvent(MyEvent.class, setOf(), null, setOf()));
        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingEvent(MyEvent.class, setOf(), MyEvent.class, null));
    }

    @Test
    <X> void testEventMatchingTypeVarException() {
        TypeLiteral<List<X>> varEventType = new TypeLiteral<List<X>>() {
        };

        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingEvent(varEventType.getType(), setOf(), MyBean.class, setOf()));
    }

    @Test
    void testEventMatchingNonQualifiersException() {
        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingEvent(MyEvent.class,
                        setOf(Qualifier1.Literal.INSTANCE, NonQualifier.Literal.INSTANCE), MyEvent.class, setOf()));

        assertThrows(IllegalArgumentException.class,
                () -> beanContainer.isMatchingEvent(MyEvent.class, setOf(), MyEvent.class,
                        setOf(Qualifier1.Literal.INSTANCE, NonQualifier.Literal.INSTANCE)));
    }

    @SafeVarargs
    private static <T> Set<T> setOf(T... values) {
        return new LinkedHashSet<T>(Arrays.asList(values));
    }
}
