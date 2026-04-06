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
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.InvokerFactory;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InvokerVisibilityTest {

    @Test
    void test() throws Exception {
        Syringe syringe = newSyringe();
        try {
            syringe.start();

            Instance<Object> lookup = syringe.getBeanManager().createInstance();
            InvokerHolder invokers = syringe.inject(InvokerHolder.class);

            for (Class<?> clazz : Arrays.<Class<?>>asList(MyPublicService.class, MyProtectedService.class, MyPackagePrivateService.class)) {
                Object service = lookup.select(clazz).get();

                for (String method : Arrays.asList(
                    "hello",
                    "helloProtected",
                    "helloPackagePrivate",
                    "helloStatic",
                    "helloProtectedStatic",
                    "helloPackagePrivateStatic"
                )) {
                    String id = clazz.getSimpleName() + "_" + method;
                    assertEquals(id, invokers.get(id).invoke(service, null));
                }
            }
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(TestExtension.class.getName());
        syringe.initialize();
        syringe.addDiscoveredClass(MyPublicService.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(MyProtectedService.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(MyPackagePrivateService.class, BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    public static class TestExtension extends InvokerHolderExtensionBase implements BuildCompatibleExtension {
        @Registration(types = MyPublicService.class)
        public void myPublicServiceRegistration(BeanInfo bean, InvokerFactory invokers) {
            invokersForAllMethods(bean, invokers);
        }

        @Registration(types = MyProtectedService.class)
        public void myProtectedServiceRegistration(BeanInfo bean, InvokerFactory invokers) {
            invokersForAllMethods(bean, invokers);
        }

        @Registration(types = MyPackagePrivateService.class)
        public void myPackagePrivateServiceRegistration(BeanInfo bean, InvokerFactory invokers) {
            invokersForAllMethods(bean, invokers);
        }

        private void invokersForAllMethods(BeanInfo bean, InvokerFactory invokers) {
            for (MethodInfo method : bean.declaringClass().methods()) {
                if (isServiceMethod(method)) {
                    registerInvoker(bean.declaringClass().simpleName() + "_" + method.name(),
                        invokers.createInvoker(bean, method).build());
                }
            }
        }

        private boolean isServiceMethod(MethodInfo method) {
            return method.name().startsWith("hello");
        }

        @Synthesis
        public void synthesis(SyntheticComponents syn) {
            synthesizeInvokerHolder(syn);
        }
    }

    @ApplicationScoped
    public static class MyPublicService {
        public String hello() {
            return "MyPublicService_hello";
        }

        protected String helloProtected() {
            return "MyPublicService_helloProtected";
        }

        String helloPackagePrivate() {
            return "MyPublicService_helloPackagePrivate";
        }

        public static String helloStatic() {
            return "MyPublicService_helloStatic";
        }

        protected static String helloProtectedStatic() {
            return "MyPublicService_helloProtectedStatic";
        }

        static String helloPackagePrivateStatic() {
            return "MyPublicService_helloPackagePrivateStatic";
        }
    }

    @ApplicationScoped
    protected static class MyProtectedService {
        public String hello() {
            return "MyProtectedService_hello";
        }

        protected String helloProtected() {
            return "MyProtectedService_helloProtected";
        }

        String helloPackagePrivate() {
            return "MyProtectedService_helloPackagePrivate";
        }

        public static String helloStatic() {
            return "MyProtectedService_helloStatic";
        }

        protected static String helloProtectedStatic() {
            return "MyProtectedService_helloProtectedStatic";
        }

        static String helloPackagePrivateStatic() {
            return "MyProtectedService_helloPackagePrivateStatic";
        }
    }

    @ApplicationScoped
    static class MyPackagePrivateService {
        public String hello() {
            return "MyPackagePrivateService_hello";
        }

        protected String helloProtected() {
            return "MyPackagePrivateService_helloProtected";
        }

        String helloPackagePrivate() {
            return "MyPackagePrivateService_helloPackagePrivate";
        }

        public static String helloStatic() {
            return "MyPackagePrivateService_helloStatic";
        }

        protected static String helloProtectedStatic() {
            return "MyPackagePrivateService_helloProtectedStatic";
        }

        static String helloPackagePrivateStatic() {
            return "MyPackagePrivateService_helloPackagePrivateStatic";
        }
    }
}
