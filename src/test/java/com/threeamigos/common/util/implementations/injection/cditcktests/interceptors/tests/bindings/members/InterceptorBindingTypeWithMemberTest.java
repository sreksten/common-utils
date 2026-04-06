package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.bindings.members;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.util.AnnotationLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InterceptorBindingTypeWithMemberTest {

    private abstract static class PlantInterceptorBindingLiteral extends AnnotationLiteral<PlantInterceptorBinding>
            implements PlantInterceptorBinding {
    }

    @Test
    void testInterceptorBindingTypeWithMember() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Farm farm = getContextualReference(syringe, Farm.class);
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
            syringe.setup();
            Farm farm = getContextualReference(syringe, Farm.class);
            assertEquals(20, farm.getVehicleCount());
            assertTrue(VehicleCountInterceptor.isIntercepted());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterceptorBindingTypeMemberValuesComparedWithEquals() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            List<Interceptor<?>> interceptors = syringe.getBeanManager().resolveInterceptors(
                    InterceptionType.AROUND_INVOKE,
                    new PlantInterceptorBindingLiteral() {
                        @Override
                        public String name() {
                            return new String("TEST");
                        }

                        @Override
                        public int age() {
                            return 1;
                        }
                    }
            );
            assertTrue(interceptors.size() > 0);
            Plant plant = getContextualReference(syringe, Plant.class);
            plant.grow();
            assertTrue(PlantInterceptor.isIntercepted());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AnimalCountInterceptorBinding.class,
                DecreasingInterceptor.class,
                Farm.class,
                IncreasingInterceptor.class,
                Plant.class,
                PlantInterceptor.class,
                PlantInterceptorBinding.class,
                VehicleCountInterceptor.class,
                VehicleCountInterceptorBinding.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }
}
