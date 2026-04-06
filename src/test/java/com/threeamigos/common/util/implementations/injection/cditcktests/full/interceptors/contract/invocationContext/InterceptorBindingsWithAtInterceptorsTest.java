/*
 * Copyright 2023, Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.contract.invocationContext;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterceptorBindingsWithAtInterceptorsTest {

    @Test
    void testInterceptorBindingsEmptyWithAtInterceptors() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Dog.class, DogInterceptor.class, Fish.class, FishInterceptor.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);

        try {
            syringe.setup();

            Dog dog = syringe.getBeanManager().createInstance().select(Dog.class).get();
            assertEquals("dog: bar", dog.foo());
            assertNotNull(DogInterceptor.getAllBindings());
            assertTrue(DogInterceptor.getAllBindings().isEmpty());

            Fish fish = syringe.getBeanManager().createInstance().select(Fish.class).get();
            assertEquals("fish: bar", fish.foo());
            assertNotNull(FishInterceptor.getAllBindings());
            assertTrue(FishInterceptor.getAllBindings().isEmpty());
        } finally {
            syringe.shutdown();
        }
    }
}
