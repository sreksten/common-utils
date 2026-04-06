/*
 * Copyright 2024, Red Hat, Inc., and individual contributors
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
package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.contract.invocationContext;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterceptorBindingsWithInterceptorFactoryTest {

    @Test
    void testInterceptorBindingsAppliedViaInterceptorFactory() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler());
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);

        try {
            syringe.initialize();
            syringe.addDiscoveredClass(ProducerBean.class, BeanArchiveMode.EXPLICIT);
            syringe.addDiscoveredClass(ProductInterceptor1.class, BeanArchiveMode.EXPLICIT);
            syringe.addDiscoveredClass(ProductInterceptor2.class, BeanArchiveMode.EXPLICIT);
            syringe.addDiscoveredClass(ProductInterceptorBinding1.class, BeanArchiveMode.EXPLICIT);
            syringe.addDiscoveredClass(ProductInterceptorBinding2.class, BeanArchiveMode.EXPLICIT);
            syringe.addDiscoveredClass(ProductInterceptorBinding3.class, BeanArchiveMode.EXPLICIT);
            syringe.start();

            Product product = syringe.getBeanManager().createInstance().select(Product.class).get();
            assertEquals(42, product.ping());
            assertEquals(42, product.pong());

            Set<Annotation> interceptor1Bindings = ProductInterceptor1.getAllBindings();
            assertNotNull(interceptor1Bindings);
            assertEquals(2, interceptor1Bindings.size());
            assertTrue(interceptor1Bindings.contains(ProductInterceptorBinding1.Literal.INSTANCE));
            assertTrue(interceptor1Bindings.contains(ProductInterceptorBinding2.Literal.INSTANCE));

            Set<Annotation> interceptor2Bindings = ProductInterceptor2.getAllBindings();
            assertNotNull(interceptor2Bindings);
            assertEquals(2, interceptor2Bindings.size());
            assertTrue(interceptor2Bindings.contains(ProductInterceptorBinding1.Literal.INSTANCE));
            assertTrue(interceptor2Bindings.contains(ProductInterceptorBinding3.Literal.INSTANCE));
        } finally {
            syringe.shutdown();
        }
    }
}
