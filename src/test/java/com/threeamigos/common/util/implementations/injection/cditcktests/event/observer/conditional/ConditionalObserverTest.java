package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.conditional;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.scopes.RequestScopedContext;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.event.Reception;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class ConditionalObserverTest {

    @Test
    void testConditionalObserver() {
        Syringe syringe = newSyringe();
        try {
            resetState();

            Event<ConditionalEvent> conditionalEvent = syringe.getBeanManager().getEvent().select(ConditionalEvent.class);
            conditionalEvent.fire(new ConditionalEvent());
            assertFalse(WidowSpider.isNotified());

            WidowSpider bean = contextualReference(syringe, WidowSpider.class);
            assertNotNull(bean);
            assertFalse(bean.isInstanceNotified());

            conditionalEvent.fire(new ConditionalEvent());
            assertTrue(WidowSpider.isNotified() && bean.isInstanceNotified());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodInvokedOnReturnedInstanceFromContext() {
        Syringe syringe = newSyringe();
        try {
            resetState();

            RecluseSpider spider = contextualReference(syringe, RecluseSpider.class);
            spider.setWeb(new Web());
            syringe.getBeanManager().getEvent().select(ConditionalEvent.class).fire(new ConditionalEvent());

            assertTrue(spider.isInstanceNotified());
            assertEquals(1, spider.getWeb().getRings());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNotifyEnumerationContainsNotifyValues() {
        assertEquals(2, Reception.values().length);
        List<String> notifyValueNames = new ArrayList<String>();
        for (Reception value : Reception.values()) {
            notifyValueNames.add(value.name());
        }

        assertTrue(notifyValueNames.contains("IF_EXISTS"));
        assertTrue(notifyValueNames.contains("ALWAYS"));
    }

    @Test
    void testConditionalObserverMethodNotInvokedIfNoActiveContext() throws Exception {
        Syringe syringe = newSyringe();
        try {
            resetState();

            Tarantula tarantula = contextualReference(syringe, Tarantula.class);
            tarantula.ping();

            Event<TarantulaEvent> tarantulaEvent = syringe.getBeanManager().getEvent().select(TarantulaEvent.class);
            try {
                setRequestContextActive(syringe, false);
                tarantulaEvent.fire(new TarantulaEvent());
                assertFalse(Tarantula.isNotified());
            } finally {
                setRequestContextActive(syringe, true);
            }

            tarantulaEvent.fire(new TarantulaEvent());
            assertTrue(Tarantula.isNotified());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAsyncConditionalObserver() throws Exception {
        Syringe syringe = newSyringe();
        try {
            resetState();

            BlockingQueue<AsyncConditionalEvent> queue = new LinkedBlockingQueue<AsyncConditionalEvent>();
            syringe.getBeanManager().getEvent()
                    .select(AsyncConditionalEvent.class)
                    .fireAsync(new AsyncConditionalEvent())
                    .thenAccept(queue::offer);
            AsyncConditionalEvent event = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(event);
            assertFalse(AsyncConditionalObserver.IsNotified().get());

            AsyncConditionalObserver observer = contextualReference(syringe, AsyncConditionalObserver.class);
            assertNotNull(observer);
            observer.ping();

            syringe.getBeanManager().getEvent()
                    .select(AsyncConditionalEvent.class)
                    .fireAsync(new AsyncConditionalEvent())
                    .thenAccept(queue::offer);
            event = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(event);
            assertTrue(AsyncConditionalObserver.IsNotified().get());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                AsyncConditionalEvent.class,
                AsyncConditionalObserver.class,
                ConditionalEvent.class,
                RecluseSpider.class,
                Spun.class,
                Tarantula.class,
                TarantulaEvent.class,
                Web.class,
                WidowSpider.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        ((BeanManagerImpl) syringe.getBeanManager()).getContextManager().activateRequest();
        return syringe;
    }

    private <T> T contextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }

    @SuppressWarnings("unchecked")
    private void setRequestContextActive(Syringe syringe, boolean active) throws Exception {
        BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
        RequestScopedContext requestContext = (RequestScopedContext) beanManager.getContextManager().getContext(RequestScoped.class);
        Field requestActiveField = RequestScopedContext.class.getDeclaredField("requestActive");
        requestActiveField.setAccessible(true);
        ThreadLocal<Boolean> requestActive = (ThreadLocal<Boolean>) requestActiveField.get(requestContext);
        requestActive.set(active);
    }

    private void resetState() {
        WidowSpider.reset();
        RecluseSpider.reset();
        Tarantula.reset();
        AsyncConditionalObserver.reset();
    }

    static class AsyncConditionalEvent {
    }

    @ApplicationScoped
    static class AsyncConditionalObserver {
        private static final AtomicBoolean notified = new AtomicBoolean(false);

        static AtomicBoolean IsNotified() {
            return notified;
        }

        void observeAsync(@ObservesAsync(notifyObserver = Reception.IF_EXISTS) AsyncConditionalEvent event) {
            notified.set(true);
        }

        static void reset() {
            notified.set(false);
        }

        void ping() {
        }
    }

    static class ConditionalEvent {
    }

    @RequestScoped
    static class RecluseSpider {
        private static boolean notified;
        private static boolean instanceNotified;
        private Web web;

        void observe(@Observes(notifyObserver = Reception.IF_EXISTS) ConditionalEvent someEvent) {
            notified = true;
            instanceNotified = true;
            if (web != null) {
                web.addRing();
            }
        }

        boolean isInstanceNotified() {
            return instanceNotified;
        }

        static boolean isNotified() {
            return notified;
        }

        void setWeb(Web web) {
            this.web = web;
        }

        Web getWeb() {
            return web;
        }

        static void reset() {
            notified = false;
            instanceNotified = false;
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Spun {
    }

    @RequestScoped
    static class Tarantula {
        private static boolean notified;

        void observe(@Observes(notifyObserver = Reception.IF_EXISTS) TarantulaEvent someEvent) {
            notified = true;
        }

        void ping() {
        }

        static boolean isNotified() {
            return notified;
        }

        static void reset() {
            notified = false;
        }
    }

    static class TarantulaEvent {
    }

    @Spun
    static class Web {
        private int rings;

        void addRing() {
            rings++;
        }

        int getRings() {
            return rings;
        }
    }

    @RequestScoped
    static class WidowSpider {
        private static boolean notified;
        private static boolean instanceNotified;

        void observe(@Observes(notifyObserver = Reception.IF_EXISTS) ConditionalEvent someEvent) {
            notified = true;
            instanceNotified = true;
        }

        boolean isInstanceNotified() {
            return instanceNotified;
        }

        static boolean isNotified() {
            return notified;
        }

        static void reset() {
            notified = false;
            instanceNotified = false;
        }
    }
}
