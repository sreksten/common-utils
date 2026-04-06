/*
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.dynamic.destroy.dependent;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for CDI-139. It verifies that Instance.destroy() can be used to destroy a dependent bean instance and bean instances
 * depending on the bean instance are destroyed as well.
 */
@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class DestroyingDependentInstanceTest {

    @Test
    void testDestroyingDependentInstances() {
        Syringe syringe = newSyringe();
        try {
            @SuppressWarnings("unchecked")
            Instance<Foo> instance = (Instance<Foo>) syringe.getBeanManager().createInstance().select(Foo.class);
            assertNotNull(instance);

            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                List<Foo> foos = new ArrayList<Foo>();
                List<Qux> quxs = new ArrayList<Qux>();

                for (int i = 0; i < 10; i++) {
                    Foo foo = instance.get();
                    foo.ping();
                    foos.add(foo);
                    quxs.add(foo.getQux());
                }

                Foo.reset();
                Qux.reset();
                Baz.reset();

                for (Foo component : foos) {
                    instance.destroy(component);
                }
                assertEquals(foos, Foo.getDestroyedComponents());
                assertEquals(quxs, Qux.getDestroyedComponents());
                assertFalse(Baz.isDestroyed());
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDestroyingInterceptedDependentBean() {
        Syringe syringe = newSyringe();
        try {
            @SuppressWarnings("unchecked")
            Instance<Bar> instance = (Instance<Bar>) syringe.getBeanManager().createInstance().select(Bar.class);
            assertNotNull(instance);

            Bar bar = instance.get();
            bar.ping();

            Bar.reset();
            TransactionalInterceptor.reset();
            instance.destroy(bar);

            assertTrue(Bar.isDestroyed());
            assertTrue(TransactionalInterceptor.isDestroyed());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Qux.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Baz.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Transactional.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TransactionalInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
