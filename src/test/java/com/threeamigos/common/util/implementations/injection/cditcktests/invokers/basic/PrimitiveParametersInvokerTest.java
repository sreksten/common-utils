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
package com.threeamigos.common.util.implementations.injection.cditcktests.invokers.basic;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.invokers.InvokerHolder;
import com.threeamigos.common.util.implementations.injection.cditcktests.invokers.InvokerHolderExtensionBase;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
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
class PrimitiveParametersInvokerTest {

    @Test
    void test() throws Exception {
        Syringe syringe = newSyringe();
        try {
            syringe.start();

            MyService service = syringe.inject(MyService.class);
            InvokerHolder invokers = syringe.inject(InvokerHolder.class);

            Invoker<MyService, String> hello = invokers.get("hello");
            assertEquals("foobar_true_a_1", hello.invoke(service, new Object[] { true, 'a', 1 }));
            assertEquals("foobar_false_b_2", hello.invoke(new MyService(), new Object[] { false, 'b', (short) 2 }));
            assertThrows(RuntimeException.class, () -> hello.invoke(new MyService(), new Object[] { null, null, null }));
            assertThrows(RuntimeException.class, () -> hello.invoke(service, new Object[] { true, 'a', 1L }));
            assertThrows(RuntimeException.class, () -> hello.invoke(service, new Object[] { true, 'a', 1.0 }));

            Invoker<MyService, String> helloStatic = invokers.get("helloStatic");
            assertEquals("quux_1_1.0", helloStatic.invoke(null, new Object[] { 1L, 1.0 }));
            assertEquals("quux_1_1.0", helloStatic.invoke(null, new Object[] { 1, 1.0 }));
            assertEquals("quux_1_1.0", helloStatic.invoke(null, new Object[] { 1L, 1.0F }));
            assertThrows(RuntimeException.class, () -> helloStatic.invoke(null, new Object[] { null, null }));
            assertThrows(RuntimeException.class, () -> helloStatic.invoke(null, new Object[] { 1.0, 1.0 }));
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
        return syringe;
    }

    public static class TestExtension extends InvokerHolderExtensionBase implements BuildCompatibleExtension {
        @Registration(types = MyService.class)
        public void myServiceRegistration(BeanInfo bean, InvokerFactory invokers) {
            registerInvokers(bean, invokers, setOf("hello", "helloStatic"));
        }

        @Synthesis
        public void synthesis(SyntheticComponents syn) {
            synthesizeInvokerHolder(syn);
        }
    }

    @ApplicationScoped
    public static class MyService {
        public String hello(boolean b, char c, int i) {
            return "foobar_" + b + "_" + c + "_" + i;
        }

        public static String helloStatic(long l, double d) {
            return "quux_" + l + "_" + d;
        }
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }
}
