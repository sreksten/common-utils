package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.invocation;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
class DisposesMethodCalledOnceTest {

    @Test
    void testDisposerCalledOnce1() {
        ProducerBean.reset();
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            beanManager.getEvent().select(String.class).fire("Hello");
            assertEquals(1, ProducerBean.firstDisposerCalled.get(), "Disposer method called multiple times!");
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDisposerCalledOnce2() {
        ProducerBean.reset();
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            beanManager.getEvent().select(String.class).fire("Hello");
            assertEquals(1, ProducerBean.secondDisposerCalled.get(), "Disposer method called multiple times!");
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDisposerCalledOnce3() {
        ProducerBean.reset();
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            beanManager.getEvent().select(String.class).fire("Hello");
            assertEquals(1, ProducerBean.thirdDisposerCalled.get(), "Disposer method called multiple times!");
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDisposerCalledOnce4() {
        ProducerBean.reset();
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            beanManager.getEvent().select(String.class).fire("Hello");
            assertEquals(1, ProducerBean.forthDisposerCalled.get(), "Disposer method called multiple times!");
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDisposerCalledOnce5() {
        ProducerBean.reset();
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            beanManager.getEvent().select(String.class).fire("Hello");
            assertEquals(1, ProducerBean.fifthDisposerCalled.get(), "Disposer method called multiple times!");
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDisposerCalledOnce6() {
        ProducerBean.reset();
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            beanManager.getEvent().select(String.class).fire("Hello");
            assertEquals(1, ProducerBean.sixthDisposerCalled.get(), "Disposer method called multiple times!");
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe startContainer() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(DummyQualifier.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FirstBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SecondBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ProducerBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TestObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
