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
package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.clientProxy;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class ClientProxyTest {

    @Test
    void testClientProxyUsedForNormalScope() {
        Syringe syringe = newSyringe();
        try {
            Tuna tuna = syringe.inject(Tuna.class);
            assertTrue(isProxy(tuna), tuna.getClass().getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSimpleBeanClientProxyIsSerializable() throws IOException, ClassNotFoundException {
        Syringe syringe = newSyringe();
        try {
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                TunedTuna tuna = syringe.inject(TunedTuna.class);
                assertTrue(isProxy(tuna), tuna.getClass().getName());
                byte[] bytes = passivate(tuna);
                tuna = (TunedTuna) activate(bytes);
                assertTrue(isProxy(tuna), tuna.getClass().getName());
                assertEquals("tuned", tuna.getState());
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
    void testClientProxyInvocation() {
        Syringe syringe = newSyringe();
        try {
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                TunedTuna tuna = syringe.inject(TunedTuna.class);
                assertTrue(isProxy(tuna), tuna.getClass().getName());
                assertEquals("tuned", tuna.getState());
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Tuna.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TunedTuna.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Fox.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    private byte[] passivate(Object object) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = new ObjectOutputStream(output);
        objectOutput.writeObject(object);
        objectOutput.flush();
        return output.toByteArray();
    }

    private Object activate(byte[] bytes) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes));
        return objectInput.readObject();
    }

    private boolean isProxy(Object instance) {
        if (instance == null) {
            return false;
        }
        String name = instance.getClass().getName();
        return name.contains("$$")
                || name.contains("$Proxy")
                || name.contains("$ByteBuddy$")
                || name.startsWith("com.sun.proxy.$Proxy");
    }
}
