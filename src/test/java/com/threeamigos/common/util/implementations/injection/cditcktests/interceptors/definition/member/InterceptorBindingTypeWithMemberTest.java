package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.definition.member;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InterceptorBindingTypeWithMemberTest {

    @Test
    void testInterceptorBindingTypeWithMember() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();
            Farm farm = syringe.getBeanManager().createInstance().select(Farm.class).get();
            assertEquals(20, farm.getAnimalCount());
            assertTrue(IncreasingInterceptor.isIntercepted());
            assertFalse(DecreasingInterceptor.isIntercepted());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterceptorBindingTypeWithNonBindingMember() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();
            Farm farm = syringe.getBeanManager().createInstance().select(Farm.class).get();
            assertEquals(20, farm.getVehicleCount());
            assertTrue(VehicleCountInterceptor.isIntercepted());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Farm.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AnimalCountInterceptorBinding.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(VehicleCountInterceptorBinding.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IncreasingInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DecreasingInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(VehicleCountInterceptor.class, BeanArchiveMode.EXPLICIT);
        return syringe;
    }
}
