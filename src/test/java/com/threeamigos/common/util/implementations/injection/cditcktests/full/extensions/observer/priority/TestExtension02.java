package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.observer.priority;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.interceptor.Interceptor;

public class TestExtension02 implements Extension {

    void processBean(@Observes @Priority(Interceptor.Priority.APPLICATION - 50) ProcessBean<TestBean> testBeanEvent) {
        ActionSequence.addAction("2");
    }

    void processBeanLater(@Observes @Priority(Interceptor.Priority.APPLICATION - 10) ProcessBean<TestBean> testBeanEvent) {
        ActionSequence.addAction("3");
    }
}
