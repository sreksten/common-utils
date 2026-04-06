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
package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.clientProxy.unproxyable.finalMethod;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.control.RequestContextController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class PrivateFinalMethodTest {

    @Test
    void testClassWithPrivateFinalMethodCanBeProxied() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Whale.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(WhaleCovey.class, BeanArchiveMode.EXPLICIT);

        try {
            syringe.start();
            RequestContextController controller = syringe.inject(RequestContextController.class);
            controller.activate();
            try {
                syringe.inject(WhaleCovey.class).ping();
                assertEquals(1, syringe.getBeanManager().getBeans(Whale.class).size());
            } finally {
                controller.deactivate();
            }
        } finally {
            syringe.shutdown();
        }
    }
}
