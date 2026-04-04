package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.async.executor;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.event.ObservesAsync;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireAsyncWithCustomExecutorTest {

    @Test
    void testCustomExecutor() throws Exception {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Message.class, MessageObserver.class, CustomExecutor.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            MessageObserver.observed.set(false);
            CustomExecutor.executed.set(false);

            BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
            NotificationOptions notificationOptions = NotificationOptions.ofExecutor(new CustomExecutor());

            syringe.getBeanManager().getEvent()
                    .select(Message.class)
                    .fireAsync(new Message(), notificationOptions)
                    .thenAccept(queue::add);

            Message message = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(message);
            assertTrue(MessageObserver.observed.get());
            assertTrue(CustomExecutor.executed.get());
        } finally {
            syringe.shutdown();
        }
    }

    static class Message {
    }

    @Dependent
    static class MessageObserver {
        static final AtomicBoolean observed = new AtomicBoolean(false);

        void observes(@ObservesAsync Message message) {
            observed.set(true);
        }
    }

    static class CustomExecutor implements Executor {
        static final AtomicBoolean executed = new AtomicBoolean(false);

        @Override
        public void execute(Runnable command) {
            executed.set(true);
            command.run();
        }
    }
}
