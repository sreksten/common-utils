/*
 * Copyright 2024, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package com.threeamigos.common.util.implementations.injection.cditcktests.invokers.lookup;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.invokers.InvokerHolder;
import com.threeamigos.common.util.implementations.injection.cditcktests.invokers.InvokerHolderExtensionBase;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.InvokerFactory;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.invoke.Invoker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class ArgumentArraySizeWithLookupTest {

    @Test
    void test() throws Exception {
        Syringe syringe = newSyringe();
        try {
            syringe.start();
            MyService service = syringe.inject(MyService.class);
            InvokerHolder invokers = syringe.inject(InvokerHolder.class);

            Invoker<MyService, String> invoker = invokers.get("hello");
            assertThrows(RuntimeException.class, () -> invoker.invoke(service, null));
            assertThrows(RuntimeException.class, () -> invoker.invoke(service, new Object[] {}));
            assertThrows(RuntimeException.class, () -> invoker.invoke(service, new Object[] { null }));
            assertEquals("foobar_1_2", invoker.invoke(service, new Object[] { null, null }));
            assertEquals("foobar_1_2", invoker.invoke(service, new Object[] { null, null, null }));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(TestExtension.class.getName());
        syringe.initialize();
        syringe.addDiscoveredClass(MyService.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(MyDependency1.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(MyDependency2.class, BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    public static class TestExtension extends InvokerHolderExtensionBase implements BuildCompatibleExtension {
        @Registration(types = MyService.class)
        public void myServiceRegistration(BeanInfo bean, InvokerFactory invokers) {
            registerInvokers(bean, invokers, setOf("hello"), builder -> builder
                .withArgumentLookup(0)
                .withArgumentLookup(1));
        }

        @Synthesis
        public void synthesis(SyntheticComponents syn) {
            synthesizeInvokerHolder(syn);
        }
    }

    @ApplicationScoped
    static class MyService {
        public String hello(MyDependency1 dependency1, MyDependency2 dependency2) {
            return "foobar_" + dependency1 + "_" + dependency2;
        }
    }

    @Dependent
    static class MyDependency1 {
        @Override
        public String toString() {
            return "1";
        }
    }

    @Dependent
    static class MyDependency2 {
        @Override
        public String toString() {
            return "2";
        }
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }
}
