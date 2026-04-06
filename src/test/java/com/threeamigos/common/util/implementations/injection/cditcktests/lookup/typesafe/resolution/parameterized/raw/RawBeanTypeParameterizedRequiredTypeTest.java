package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.typesafe.resolution.parameterized.raw;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("serial")
class RawBeanTypeParameterizedRequiredTypeTest<T, X extends Number> {

    private final TypeLiteral<Foo<T>> fooUnboundedTypeVariableLiteral = new TypeLiteral<Foo<T>>() {
    };

    private final TypeLiteral<Foo<X>> fooBoundedTypeVariableLiteral = new TypeLiteral<Foo<X>>() {
    };

    private final TypeLiteral<Foo<Object>> fooObjectLiteral = new TypeLiteral<Foo<Object>>() {
    };

    private final TypeLiteral<Foo<Integer>> fooIntegerLiteral = new TypeLiteral<Foo<Integer>>() {
    };

    private final TypeLiteral<Bar<String, T>> barStringUnboundedTypeVariableLiteral = new TypeLiteral<Bar<String, T>>() {
    };

    private final TypeLiteral<Bar<String, X>> barStringBoundedTypeVariableLiteral = new TypeLiteral<Bar<String, X>>() {
    };

    private final TypeLiteral<Bar<Object, X>> barObjectBoundedTypeVariableLiteral = new TypeLiteral<Bar<Object, X>>() {
    };

    private final TypeLiteral<Bar<Object, Integer>> barObjectStringLiteral = new TypeLiteral<Bar<Object, Integer>>() {
    };

    private final TypeLiteral<Bar<Object, Object>> barObjectLiteral = new TypeLiteral<Bar<Object, Object>>() {
    };

    @Test
    void testNotAssignableTypeParams() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(0, getBeans(syringe, fooIntegerLiteral).size());
            assertEquals(0, getBeans(syringe, fooBoundedTypeVariableLiteral).size());
            assertEquals(0, getBeans(syringe, barStringUnboundedTypeVariableLiteral).size());
            assertEquals(0, getBeans(syringe, barStringBoundedTypeVariableLiteral).size());
            assertEquals(0, getBeans(syringe, barObjectBoundedTypeVariableLiteral).size());
            assertEquals(0, getBeans(syringe, barObjectStringLiteral).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAssignableTypeParams() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, fooUnboundedTypeVariableLiteral).size());
            assertEquals(1, getBeans(syringe, fooObjectLiteral).size());
            assertEquals(1, getBeans(syringe, barObjectLiteral).size());
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(RawProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(Syringe syringe, TypeLiteral<T> type, Annotation... qualifiers) {
        return (Set) syringe.getBeanManager().getBeans(type.getType(), qualifiers);
    }
}
