package com.threeamigos.common.util.implementations.injection.cditcktests.full.event.observer.extension;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

class EventBeanObserverNotificationTest extends AbstractObserverNotificationTest {

    private Syringe syringe;

    @AfterEach
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
            syringe = null;
        }
    }

    @Override
    protected void fireEvent(Giraffe payload, Annotation... qualifiers) {
        EventDispatcher dispatcher = syringe.inject(EventDispatcher.class);
        dispatcher.event.select(qualifiers).fire(payload);
    }

    @Override
    protected ObserverExtension extension() {
        return syringe.getBeanManager().getExtension(ObserverExtension.class);
    }

    @Test
    void testNotifyInvoked() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                EventDispatcher.class,
                Giraffe.class,
                GiraffeObserver.class,
                Angry.class,
                Nubian.class,
                Tall.class,
                ObserverExtension.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ObserverExtension.class.getName());
        syringe.setup();

        testNotifyInvokedInternal();
    }

    public static class EventDispatcher {
        @Inject
        @Any
        Event<Giraffe> event;
    }
}
