package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.any;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnyInjectionTest {

    @Test
    void testAnyInjectionIfExactlyOneBeanForType() {
        Syringe syringe = newSyringe();
        try {
            Customer customer = syringe.getBeanManager().createInstance().select(Customer.class).get();
            assertNotNull(customer);
            assertNotNull(customer.drink);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Customer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Drink.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
