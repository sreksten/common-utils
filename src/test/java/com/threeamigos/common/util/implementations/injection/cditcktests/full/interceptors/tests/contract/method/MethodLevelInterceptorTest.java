package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.tests.contract.method;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class MethodLevelInterceptorTest {

    @Test
    void testInterceptorCanBeAppliedToMoreThanOneMethod() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Dog.class,
                DogInterceptor.class,
                Fish.class,
                FishInterceptor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);

        try {
            syringe.setup();

            Fish fish = syringe.getBeanManager().createInstance().select(Fish.class).get();
            assertEquals("Intercepted bar", fish.foo());
            assertEquals("Intercepted pong", fish.ping());
            assertEquals("Salmon", fish.getName());
            assertEquals(1, FishInterceptor.getInstanceCount());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testExcludeClassInterceptors() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Dog.class,
                DogInterceptor.class,
                Fish.class,
                FishInterceptor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);

        try {
            syringe.setup();

            Dog dog = syringe.getBeanManager().createInstance().select(Dog.class).get();
            assertEquals("Intercepted bar", dog.foo());
            assertEquals("pong", dog.ping());
        } finally {
            syringe.shutdown();
        }
    }
}
