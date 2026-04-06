package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.parameterized.multiple.bounds;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ParameterizedTypesWithTypeVariableWithMultipleBoundsTest {

    @Test
    void testInjectionOfBeanWithWildcardWithTypeVariableAsLowerBound() {
        Syringe syringe = newSyringe();
        try {
            ConsumerMultipleBounds<?, ?> consumer = getConsumer(syringe);
            assertNotNull(consumer.getGenericInterfaceSuperBazImpl2());
            assertEquals(GenericInterfaceSuperBazImpl.class.getSimpleName(),
                    consumer.getGenericInterfaceSuperBazImpl2().getId());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionOfBeanWithTypeVariableWithMultipleBoundsToParameterizedTypeWithActualType() {
        Syringe syringe = newSyringe();
        try {
            ConsumerMultipleBounds<?, ?> consumer = getConsumer(syringe);
            assertNotNull(consumer.getGenericInterfaceBarFooImpl2());
            assertEquals(GenericInterfaceBarFooImpl.class.getSimpleName(),
                    consumer.getGenericInterfaceBarFooImpl2().getId());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionOfBeanWithTypeVariableWithMultipleBounds() {
        Syringe syringe = newSyringe();
        try {
            ConsumerMultipleBounds<?, ?> consumer = getConsumer(syringe);
            assertNotNull(consumer.getGenericInterfaceBarBazFooImpl());
            assertEquals(GenericInterfaceBarBazFooImpl.class.getSimpleName(),
                    consumer.getGenericInterfaceBarBazFooImpl().getId());
            assertNotNull(consumer.getGenericInterfaceBarFooImpl());
            assertEquals(GenericInterfaceBarFooImpl.class.getSimpleName(),
                    consumer.getGenericInterfaceBarFooImpl().getId());
            assertNotNull(consumer.getGenericInterfaceSuperBazImpl());
            assertEquals(GenericInterfaceSuperBazImpl.class.getSimpleName(),
                    consumer.getGenericInterfaceSuperBazImpl().getId());
        } finally {
            syringe.shutdown();
        }
    }

    private ConsumerMultipleBounds<?, ?> getConsumer(Syringe syringe) {
        return syringe.getBeanManager().createInstance()
                .select(new TypeLiteral<ConsumerMultipleBounds<?, ?>>() {
                })
                .get();
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Bar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BarFooImpl.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BarFooQualifier.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BarImpl.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Baz.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BazQualifier.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ConsumerMultipleBounds.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(GenericInterface.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(GenericInterfaceBarBazFooImpl.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(GenericInterfaceBarFooImpl.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(GenericInterfaceSuperBazImpl.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SuperBaz.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
