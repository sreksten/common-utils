package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.broken.observerMethod;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ObserverMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

public class ExtensionAddingCustomObserverMethod implements Extension {

    void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        event.addObserverMethod(new ObserverMethod<Foo>() {
            @Override
            public Class<?> getBeanClass() {
                return AfterBeanDiscovery.class;
            }

            @Override
            public Type getObservedType() {
                return Foo.class;
            }

            @Override
            public Set<Annotation> getObservedQualifiers() {
                return Collections.emptySet();
            }

            @Override
            public Reception getReception() {
                return Reception.ALWAYS;
            }

            @Override
            public TransactionPhase getTransactionPhase() {
                return TransactionPhase.IN_PROGRESS;
            }
        });
    }
}
