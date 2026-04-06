package com.threeamigos.common.util.implementations.injection.cditcktests.se.context.request;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class RequestContextTest {

    @Test
    void requestContextIsActiveDuringPostConstructCallback() {
        try (SeContainer container = newInitializer().initialize()) {
            TestBean testBean = container.select(TestBean.class).get();
            assertTrue(testBean.isReqContextActiveDuringPostConstruct());
            assertThrows(ContextNotActiveException.class, testBean::fail);
        }
    }

    @Test
    void requestContextIsActiveDuringAsyncObserverNotification() throws InterruptedException {
        try (SeContainer container = newInitializer().initialize()) {
            Event<Object> event = container.getBeanManager().getEvent();
            BlockingQueue<Payload> queue = new LinkedBlockingQueue<Payload>();

            event.select(Payload.class).fireAsync(new Payload()).thenAccept(queue::offer);
            Payload payload = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(payload);

            event.select(Payload.class).fireAsync(payload).thenAccept(queue::offer);
            payload = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(payload);

            assertEquals(2, payload.getI());
        }
    }

    private SeContainerInitializer newInitializer() {
        return SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(TestBean.class, ReqScopedCounter.class, Payload.class, TestObserver.class);
    }
}
