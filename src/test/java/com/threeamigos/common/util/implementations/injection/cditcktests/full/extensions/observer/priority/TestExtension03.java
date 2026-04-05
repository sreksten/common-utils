package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.observer.priority;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.interceptor.Interceptor;

public class TestExtension03 implements Extension {

    void processBean(@Observes @Priority(Interceptor.Priority.LIBRARY_AFTER - 50) ProcessBean<TestBean> testBeanEvent) {
        ActionSequence.addAction("5");
    }

    void processBeanLater(@Observes @Priority(Interceptor.Priority.LIBRARY_AFTER - 10) ProcessBean<TestBean> testBeanEvent) {
        ActionSequence.addAction("6");
    }
}
