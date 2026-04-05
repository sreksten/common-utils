package com.threeamigos.common.util.implementations.injection.cditcktests.full.event.observer.extension;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

abstract class AbstractObserverNotificationTest {

    protected abstract void fireEvent(Giraffe payload, Annotation... qualifiers);

    protected abstract ObserverExtension extension();

    protected void testNotifyInvokedInternal() {
        reset(extension());

        Giraffe payload = new Giraffe();
        fireEvent(payload);

        verifyObserversNotNotified(extension().getFiveMeterTallGiraffeObserver(),
                extension().getSixMeterTallAngryGiraffeObserver(),
                extension().getAngryNubianGiraffeObserver());

        verifyObserversNotified(payload, extension().getAnyGiraffeObserver());
    }

    private void reset(ObserverExtension extension) {
        extension.getAnyGiraffeObserver().reset();
        extension.getFiveMeterTallGiraffeObserver().reset();
        extension.getSixMeterTallAngryGiraffeObserver().reset();
        extension.getAngryNubianGiraffeObserver().reset();
    }

    private void verifyObserversNotified(Giraffe payload, GiraffeObserver... observers) {
        for (GiraffeObserver observer : observers) {
            assertSame(payload, observer.getReceivedPayload());
        }
    }

    private void verifyObserversNotNotified(GiraffeObserver... observers) {
        for (GiraffeObserver observer : observers) {
            assertNull(observer.getReceivedPayload());
        }
    }
}
