package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.order.overriden.lifecycleCallback.wrapped;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

public class WrappingExtension implements Extension {

    public <T extends Bird> void observeBirdTypes(@Observes ProcessAnnotatedType<T> event) {
        final AnnotatedType<T> annotatedType = event.getAnnotatedType();
        event.setAnnotatedType(new ForwardingAnnotatedType<T>() {
            @Override
            public AnnotatedType<T> delegate() {
                return annotatedType;
            }
        });
    }
}
