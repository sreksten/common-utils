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
package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.dynamic.destroy.normal;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for https://issues.jboss.org/browse/CDI-139
 */
@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class DestroyingNormalScopedInstanceTest {

    private static final String[] VALUES = {"foo", "bar", "baz"};

    @Test
    void testApplicationScopedComponent() {
        Syringe syringe = newSyringe();
        try {
            @SuppressWarnings("unchecked")
            Instance<ApplicationScopedComponent> instance =
                    (Instance<ApplicationScopedComponent>) syringe.getBeanManager().createInstance().select(ApplicationScopedComponent.class);
            testComponent(instance);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testRequestScopedComponent() {
        Syringe syringe = newSyringe();
        try {
            @SuppressWarnings("unchecked")
            Instance<RequestScopedComponent> instance =
                    (Instance<RequestScopedComponent>) syringe.getBeanManager().createInstance().select(RequestScopedComponent.class);
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                testComponent(instance);
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
    void testNothingHappensIfNoInstanceToDestroy() {
        Syringe syringe = newSyringe();
        try {
            ApplicationScopedComponent application = syringe.getBeanManager().createInstance().select(ApplicationScopedComponent.class).get();
            Bean<?> bean = resolveBean(ApplicationScopedComponent.class, syringe);
            AlterableContext context = (AlterableContext) syringe.getBeanManager().getContext(bean.getScope());

            AbstractComponent.reset();
            application.setValue("value");
            context.destroy(bean);
            assertTrue(AbstractComponent.isDestroyed());

            context.destroy(bean);
            context.destroy(bean);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNullParameter() {
        Syringe syringe = newSyringe();
        try {
            @SuppressWarnings("unchecked")
            Instance<ApplicationScopedComponent> instance =
                    (Instance<ApplicationScopedComponent>) syringe.getBeanManager().createInstance().select(ApplicationScopedComponent.class);
            assertThrows(NullPointerException.class, () -> instance.destroy(null));
        } finally {
            syringe.shutdown();
        }
    }

    private <T extends AbstractComponent> void testComponent(Instance<T> instance) {
        for (String string : VALUES) {
            T reference = instance.get();
            assertNull(reference.getValue());
            reference.setValue(string);
            assertEquals(string, reference.getValue());

            AbstractComponent.reset();
            instance.destroy(reference);
            assertTrue(AbstractComponent.isDestroyed());
            assertNull(reference.getValue(), reference.getValue());
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(AbstractComponent.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ApplicationScopedComponent.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(RequestScopedComponent.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> Bean<T> resolveBean(Class<T> type, Syringe syringe) {
        return (Bean<T>) syringe.getBeanManager().resolve((java.util.Set) syringe.getBeanManager().getBeans(type));
    }
}
