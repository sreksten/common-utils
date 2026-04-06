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
package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.contract.lifecycleCallback.wrapped;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifecycleCallbackInterceptorTest {

    @Test
    void testLifecycleCallbackInterception() {
        Bird.reset();
        Eagle.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Bar.class, Bird.class, Eagle.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(WrappingExtension.class.getName());

        try {
            syringe.setup();

            BeanManager beanManager = syringe.getBeanManager();
            Bean<Eagle> eagleBean = getUniqueBean(beanManager, Eagle.class);
            CreationalContext<Eagle> creationalContext = beanManager.createCreationalContext(eagleBean);
            Eagle eagle = eagleBean.create(creationalContext);

            eagle.ping();
            eagleBean.destroy(eagle, creationalContext);

            assertEquals(1, Eagle.getInitEagleCalled().get());
            assertEquals(1, Eagle.getDestroyEagleCalled().get());
            assertEquals(1, Bird.getInitBirdCalled().get());
            assertEquals(1, Bird.getDestroyBirdCalled().get());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> beanClass) {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass);
        assertEquals(1, beans.size());
        return (Bean<T>) beans.iterator().next();
    }
}
