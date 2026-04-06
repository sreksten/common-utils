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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class StaticMethodInvokerTest {

    @Test
    void test() throws Exception {
        Syringe syringe = newSyringe();
        try {
            syringe.start();

            MyService.counter = 0;
            MyService service = syringe.inject(MyService.class);
            InvokerHolder invokers = syringe.inject(InvokerHolder.class);

            Invoker<MyService, String> hello = invokers.get("hello");
            assertEquals("foobar0[]", hello.invoke(service, new Object[] { 0, Collections.<String>emptyList() }));
            assertEquals("foobar1[]", hello.invoke(new MyService(), new Object[] { 1, Collections.<String>emptyList() }));
            assertEquals("foobar2[]", hello.invoke(null, new Object[] { 2, Collections.<String>emptyList() }));

            @SuppressWarnings("unchecked")
            Invoker<Object, Object> helloDetyped = (Invoker<Object, Object>) (Invoker<?, ?>) hello;
            assertEquals("foobar3[]", helloDetyped.invoke(service, new Object[] { 3, Collections.<String>emptyList() }));
            assertEquals("foobar4[]", helloDetyped.invoke(new MyService(), new Object[] { 4, Collections.<String>emptyList() }));
            assertEquals("foobar5[]", helloDetyped.invoke(null, new Object[] { 5, Collections.<String>emptyList() }));

            Invoker<MyService, Void> doSomething = invokers.get("doSomething");
            assertEquals(0, MyService.counter);
            assertNull(doSomething.invoke(service, null));
            assertEquals(1, MyService.counter);
            assertNull(doSomething.invoke(new MyService(), new Object[] {}));
            assertEquals(2, MyService.counter);
            assertNull(doSomething.invoke(null, new Object[] {}));
            assertEquals(3, MyService.counter);

            Invoker<MyService, Void> fail = invokers.get("fail");
            assertNull(fail.invoke(null, new Object[] { false }));
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fail.invoke(null, new Object[] { true })
            );
            assertEquals("expected", ex.getMessage());
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
            registerInvokers(bean, invokers, setOf("hello", "doSomething", "fail"));
        }

        @Synthesis
        public void synthesis(SyntheticComponents syn) {
            synthesizeInvokerHolder(syn);
        }
    }

    @ApplicationScoped
    public static class MyService {
        public static int counter = 0;

        public static String hello(int param1, List<String> param2) {
            return "foobar" + param1 + param2;
        }

        public static void doSomething() {
            counter++;
        }

        public static void fail(boolean doFail) {
            if (doFail) {
                throw new IllegalArgumentException("expected");
            }
        }
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }
}
