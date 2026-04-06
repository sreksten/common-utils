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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InstanceLookupTest {

    @Test
    void test() throws Exception {
        resetState();

        Syringe syringe = newSyringe();
        try {
            syringe.start();
            InvokerHolder invokers = syringe.inject(InvokerHolder.class);

            Invoker<MyService, String> hello = invokers.get("hello");
            assertEquals("foobar0", hello.invoke(null, null));
            assertEquals("foobar0", hello.invoke(null, null));
            assertEquals("foobar0", hello.invoke(null, null));
            assertEquals(1, MyService.CREATED);
            assertEquals(0, MyService.DESTROYED);
        } finally {
            syringe.shutdown();
            resetState();
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

    private static void resetState() {
        MyService.CREATED = 0;
        MyService.DESTROYED = 0;
    }

    public static class TestExtension extends InvokerHolderExtensionBase implements BuildCompatibleExtension {
        @Registration(types = MyService.class)
        public void myServiceRegistration(BeanInfo bean, InvokerFactory invokers) {
            registerInvokers(bean, invokers, setOf("hello"), builder -> builder.withInstanceLookup());
        }

        @Synthesis
        public void synthesis(SyntheticComponents syn) {
            synthesizeInvokerHolder(syn);
        }
    }

    @ApplicationScoped
    static class MyService {
        static int CREATED = 0;
        static int DESTROYED = 0;

        private int id;

        @PostConstruct
        public void init() {
            id = CREATED++;
        }

        @PreDestroy
        public void destroy() {
            DESTROYED++;
        }

        public String hello() {
            return "foobar" + id;
        }
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }
}
