/*
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.circular;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class CircularDependencyTest {

    @Test
    void testCircularInjectionOnTwoNormalBeans() {
        Syringe syringe = newSyringe();
        try {
            RequestContextController controller = resolveRequestContextController(syringe);
            controller.activate();
            try {
                Pig pig = syringe.inject(Pig.class);
                Food food = syringe.inject(Food.class);
                assertEquals(food.getName(), pig.getNameOfFood());
                assertEquals(pig.getName(), food.getNameOfPig());
            } finally {
                controller.deactivate();
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testCircularInjectionOnOneNormalAndOneDependentBean() {
        Syringe syringe = newSyringe();
        try {
            Petrol petrol = syringe.inject(Petrol.class);
            Car car = syringe.inject(Car.class);
            assertEquals(car.getName(), petrol.getNameOfCar());
            assertEquals(petrol.getName(), car.getNameOfPetrol());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNormalProducerMethodDeclaredOnNormalBeanWhichInjectsProducedBean() {
        Syringe syringe = newSyringe();
        try {
            syringe.inject(NormalSelfConsumingNormalProducer.class).ping();
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNormalProducerMethodDeclaredOnDependentBeanWhichInjectsProducedBean() {
        Syringe syringe = newSyringe();
        try {
            syringe.inject(DependentSelfConsumingNormalProducer.class).ping();
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNormalCircularConstructors() {
        Syringe syringe = newSyringe();
        try {
            assertNotNull(syringe.inject(Bird.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNormalAndDependentCircularConstructors() {
        Syringe syringe = newSyringe();
        try {
            assertNotNull(syringe.inject(Planet.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSelfConsumingConstructorsOnNormalBean() {
        Syringe syringe = newSyringe();
        try {
            assertNotNull(syringe.inject(House.class));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Air.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bird.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Car.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DependentSelfConsumingNormalProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Food.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(House.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NormalSelfConsumingNormalProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Petrol.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Pig.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Planet.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SelfConsumingDependent1.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SelfConsumingNormal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SelfConsumingNormal1.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Space.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Violation.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings("unchecked")
    private RequestContextController resolveRequestContextController(Syringe syringe) {
        Bean<RequestContextController> controllerBean = (Bean<RequestContextController>) syringe.getBeanManager()
                .resolve(syringe.getBeanManager().getBeans(RequestContextController.class));
        return (RequestContextController) syringe.getBeanManager().getReference(
                controllerBean,
                RequestContextController.class,
                syringe.getBeanManager().createCreationalContext(controllerBean));
    }
}
