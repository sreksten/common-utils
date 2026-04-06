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
package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.dynamic.builtin;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for built-in Instance.
 */
@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class BuiltinInstanceTest {

    @SuppressWarnings("serial")
    private static final AnnotationLiteral<FarmBased> FARM_BASED_LITERAL = new FarmBased.Literal();

    @SuppressWarnings("serial")
    private static final TypeLiteral<Predator<?>> PREDATOR_LITERAL = new TypeLiteral<Predator<?>>() {
    };

    @Test
    void testScopeOfBuiltinInstance() {
        Syringe syringe = newSyringe();
        try {
            @SuppressWarnings("serial")
            Bean<Instance<Cow>> bean = getUniqueBean(syringe, new TypeLiteral<Instance<Cow>>() {
            });
            assertEquals(Dependent.class, bean.getScope());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNameOfBuiltinInstance() {
        Syringe syringe = newSyringe();
        try {
            @SuppressWarnings("serial")
            Bean<Instance<Cow>> bean = getUniqueBean(syringe, new TypeLiteral<Instance<Cow>>() {
            });
            assertNull(bean.getName());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"rawtypes", "serial"})
    @Test
    void testInstanceProvidedForEveryLegalBeanType() {
        Syringe syringe = newSyringe();
        try {
            Farm farm = syringe.getBeanManager().createInstance().select(Farm.class).get();
            Instance<Predator<?>> predatorInstance = syringe.getBeanManager().createInstance().select(PREDATOR_LITERAL);

            Bean<?> instanceBean = getUniqueBean(syringe, new TypeLiteral<Instance<Animal>>() {
            });
            assertEquals(instanceBean, getUniqueBean(syringe, new TypeLiteral<Instance<AbstractAnimal>>() {
            }));
            assertEquals(instanceBean, getUniqueBean(syringe, new TypeLiteral<Instance<FinalAnimal>>() {
            }));
            assertEquals(instanceBean, getUniqueBean(syringe, new TypeLiteral<Instance<Wolf>>() {
            }));
            assertEquals(instanceBean, getUniqueBean(syringe, new TypeLiteral<Instance<Predator>>() {
            }));
            assertEquals(instanceBean, getUniqueBean(syringe, new TypeLiteral<Instance<Sheep[]>>() {
            }));
            assertEquals(instanceBean, getUniqueBean(syringe, new TypeLiteral<Instance<Integer>>() {
            }, FARM_BASED_LITERAL));

            assertNotNull(predatorInstance);
            predatorInstance.select(Wolf.class).get().attack(null);

            assertNotNull(farm.getAnimal());
            assertNotNull(farm.getAbstractAnimal());
            assertNotNull(farm.getCow());
            farm.getCow().get().ping();
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInstanceIsPassivationCapable() throws Exception {
        Syringe syringe = newSyringe();
        try {
            Field field = syringe.getBeanManager().createInstance().select(Field.class).get();
            assertNotNull(field);

            Object object = activate(passivate(field));
            assertTrue(field.getInstance().get() instanceof Cow);
            assertTrue(object instanceof Field);

            Field field2 = (Field) object;
            assertTrue(field2.getInstance().get() instanceof Cow);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AbstractAnimal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Cow.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Farm.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FarmBased.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Field.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FinalAnimal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Predator.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Sheep.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Wolf.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Syringe syringe, TypeLiteral<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(type.getType(), qualifiers);
        return (Bean<T>) syringe.getBeanManager().resolve((Set) beans);
    }

    private byte[] passivate(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(baos);
        outputStream.writeObject(object);
        outputStream.flush();
        return baos.toByteArray();
    }

    private Object activate(byte[] bytes) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
        return inputStream.readObject();
    }
}
