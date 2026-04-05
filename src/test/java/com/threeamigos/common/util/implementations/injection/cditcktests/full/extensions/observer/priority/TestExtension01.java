package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.observer.priority;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.interceptor.Interceptor;

public class TestExtension01 implements Extension {

    void processBeanEarly(@Observes @Priority(Interceptor.Priority.LIBRARY_BEFORE) ProcessBean<TestBean> processBean) {
        ActionSequence.reset();
        ActionSequence.addAction("1");
    }

    void processBeanSomewhereInTheMiddle(@Observes ProcessBean<TestBean> processBean) {
        ActionSequence.addAction("4");
    }

    void processBeanLate(@Observes @Priority(Interceptor.Priority.LIBRARY_AFTER) ProcessBean<TestBean> processBean) {
        ActionSequence.addAction("7");
    }
}
