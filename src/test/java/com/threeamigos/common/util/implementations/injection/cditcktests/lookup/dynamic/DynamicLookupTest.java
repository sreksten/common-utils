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
package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.dynamic;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class DynamicLookupTest {

    @Test
    void testObtainsInjectsInstanceOfInstance() {
        Syringe syringe = newSyringe();
        try {
            ObtainsInstanceBean injectionPoint = getContextualReference(syringe.getBeanManager(), ObtainsInstanceBean.class);
            assertNotNull(injectionPoint.getPaymentProcessor());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDuplicateBindingsThrowsException() {
        Syringe syringe = newSyringe();
        try {
            try {
                ObtainsInstanceBean injectionPoint = getContextualReference(syringe.getBeanManager(), ObtainsInstanceBean.class);
                injectionPoint.getAnyPaymentProcessor().select(
                        new PayByBinding(PayBy.PaymentMethod.CASH) {
                        },
                        new PayByBinding(PayBy.PaymentMethod.CREDIT_CARD) {
                        });
            } catch (Throwable throwable) {
                assertTrue(isThrowablePresent(IllegalArgumentException.class, throwable));
                return;
            }
            fail("Expected duplicate qualifier bindings to fail");
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonBindingThrowsException() {
        Syringe syringe = newSyringe();
        try {
            try {
                ObtainsInstanceBean injectionPoint = getContextualReference(syringe.getBeanManager(), ObtainsInstanceBean.class);
                injectionPoint.getAnyPaymentProcessor().select(new NonBinding.Literal());
            } catch (Throwable throwable) {
                assertTrue(isThrowablePresent(IllegalArgumentException.class, throwable));
                return;
            }
            fail("Expected non-qualifier annotation usage to fail");
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetMethod() {
        Syringe syringe = newSyringe();
        try {
            getContextualReference(syringe.getBeanManager(), AdvancedPaymentProcessor.class, Any.Literal.INSTANCE).setValue(10);

            Instance<AsynchronousPaymentProcessor> instance = getContextualReference(syringe.getBeanManager(), ObtainsInstanceBean.class)
                    .getPaymentProcessor();
            assertTrue(instance.get() instanceof AdvancedPaymentProcessor);
            assertEquals(10, instance.get().getValue());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testUnsatisfiedDependencyThrowsException() {
        Syringe syringe = newSyringe();
        try {
            try {
                getContextualReference(syringe.getBeanManager(), ObtainsInstanceBean.class)
                        .getPaymentProcessor()
                        .select(RemotePaymentProcessor.class)
                        .get();
            } catch (Throwable throwable) {
                assertTrue(isThrowablePresent(UnsatisfiedResolutionException.class, throwable));
                return;
            }
            fail("Expected unsatisfied resolution failure");
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAmbiguousDependencyThrowsException() {
        Syringe syringe = newSyringe();
        try {
            try {
                getContextualReference(syringe.getBeanManager(), ObtainsInstanceBean.class).getAnyPaymentProcessor().get();
            } catch (Throwable throwable) {
                assertTrue(isThrowablePresent(AmbiguousResolutionException.class, throwable));
                return;
            }
            fail("Expected ambiguous resolution failure");
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testIteratorMethod() {
        Syringe syringe = newSyringe();
        try {
            getContextualReference(syringe.getBeanManager(), AdvancedPaymentProcessor.class, Any.Literal.INSTANCE).setValue(1);
            getContextualReference(syringe.getBeanManager(), RemotePaymentProcessor.class, Any.Literal.INSTANCE).setValue(2);

            Instance<PaymentProcessor> instance = getContextualReference(syringe.getBeanManager(), ObtainsInstanceBean.class)
                    .getAnyPaymentProcessor();
            Iterator<AsynchronousPaymentProcessor> iterator1 = instance.select(AsynchronousPaymentProcessor.class).iterator();

            AdvancedPaymentProcessor advanced = null;
            RemotePaymentProcessor remote = null;
            int instances = 0;
            while (iterator1.hasNext()) {
                PaymentProcessor processor = iterator1.next();
                if (processor instanceof AdvancedPaymentProcessor) {
                    advanced = (AdvancedPaymentProcessor) processor;
                } else if (processor instanceof RemotePaymentProcessor) {
                    remote = (RemotePaymentProcessor) processor;
                } else {
                    throw new RuntimeException("Unexpected instance returned by iterator.");
                }
                instances++;
            }

            assertEquals(2, instances);
            assertNotNull(advanced);
            assertEquals(1, advanced.getValue());
            assertNotNull(remote);
            assertEquals(2, remote.getValue());

            Iterator<RemotePaymentProcessor> iterator2 = instance.select(
                    RemotePaymentProcessor.class,
                    new PayByBinding(PayBy.PaymentMethod.CREDIT_CARD) {
                    })
                    .iterator();

            assertEquals(2, iterator2.next().getValue());
            assertFalse(iterator2.hasNext());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAlternatives() {
        Syringe syringe = newSyringe();
        try {
            Instance<Common> instance = getContextualReference(syringe.getBeanManager(), ObtainsInstanceBean.class).getCommon();
            assertFalse(instance.isAmbiguous());
            Iterator<Common> iterator = instance.iterator();
            assertTrue(iterator.hasNext());
            assertTrue(iterator.next() instanceof Baz);
            assertFalse(iterator.hasNext());
            assertTrue(instance.get().ping());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testIsUnsatisfied() {
        Syringe syringe = newSyringe();
        try {
            ObtainsInstanceBean injectionPoint = getContextualReference(syringe.getBeanManager(), ObtainsInstanceBean.class);
            assertFalse(injectionPoint.getAnyPaymentProcessor().isUnsatisfied());
            assertTrue(injectionPoint.getPaymentProcessor().select(RemotePaymentProcessor.class).isUnsatisfied());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testIsAmbiguous() {
        Syringe syringe = newSyringe();
        try {
            ObtainsInstanceBean injectionPoint = getContextualReference(syringe.getBeanManager(), ObtainsInstanceBean.class);
            assertTrue(injectionPoint.getAnyPaymentProcessor().isAmbiguous());
            assertFalse(injectionPoint.getPaymentProcessor().isAmbiguous());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStream() {
        Syringe syringe = newSyringe();
        try {
            Instance<Uncommon> uncommonInstance = syringe.getBeanManager().createInstance().select(Uncommon.class);
            assertFalse(uncommonInstance.isResolvable());
            Stream<Uncommon> stream = uncommonInstance.stream();
            assertEquals(2L, stream.count());
            assertTrue(uncommonInstance.stream().filter(p -> p.getClass().equals(Garply.class)).findFirst().isPresent());
            assertTrue(uncommonInstance.stream().filter(p -> p.getClass().equals(Corge.class)).findFirst().isPresent());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void beanManageCreateInstance() {
        Syringe syringe = newSyringe();
        try {
            Instance<Object> instance = syringe.getBeanManager().createInstance();
            Instance<AsynchronousPaymentProcessor> asyncProcessors = instance.select(AsynchronousPaymentProcessor.class);
            assertTrue(asyncProcessors.isUnsatisfied());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void cdiSelectLookup() {
        Syringe syringe = newSyringe();
        try {
            Instance<AsynchronousPaymentProcessor> asyncProcessors = CDI.current().select(AsynchronousPaymentProcessor.class);
            assertTrue(asyncProcessors.isUnsatisfied());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(AdvancedPaymentProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AsynchronousPaymentProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Baz.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Common.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Corge.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Garply.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NonBinding.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ObtainsInstanceBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PayBy.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PayByBinding.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PaymentProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(RemotePaymentProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SimplePaymentProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SynchronousPaymentProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Uncommon.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T getContextualReference(BeanManager beanManager, Class<T> beanClass, Annotation... qualifiers) {
        Set<Bean<?>> beans = qualifiers != null && qualifiers.length > 0
                ? beanManager.getBeans(beanClass, qualifiers)
                : beanManager.getBeans(beanClass);
        Bean<?> bean = beanManager.resolve(beans);
        return (T) beanManager.getReference(bean, beanClass, beanManager.createCreationalContext((Bean) bean));
    }

    private boolean isThrowablePresent(Class<? extends Throwable> throwableType, Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (throwableType.isAssignableFrom(current.getClass())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
