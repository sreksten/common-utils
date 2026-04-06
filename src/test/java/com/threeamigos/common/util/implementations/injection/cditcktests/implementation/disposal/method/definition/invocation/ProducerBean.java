package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.invocation;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.TransientReference;

import java.util.concurrent.atomic.AtomicInteger;

@Dependent
public class ProducerBean {

    public static AtomicInteger firstDisposerCalled = new AtomicInteger();
    public static AtomicInteger secondDisposerCalled = new AtomicInteger();
    public static AtomicInteger thirdDisposerCalled = new AtomicInteger();
    public static AtomicInteger forthDisposerCalled = new AtomicInteger();
    public static AtomicInteger fifthDisposerCalled = new AtomicInteger();
    public static AtomicInteger sixthDisposerCalled = new AtomicInteger();

    @Produces
    @DummyQualifier("A")
    public FirstBean produceA() {
        return new FirstBean();
    }

    @Produces
    @DummyQualifier("B")
    public FirstBean produceB() {
        return new FirstBean();
    }

    @Produces
    @DummyQualifier("C")
    public FirstBean produceC(@DummyQualifier("D") SecondBean secondBean) {
        secondBean.ping();
        return new FirstBean();
    }

    @Produces
    @DummyQualifier("E")
    public FirstBean produceE(@TransientReference @DummyQualifier("F") SecondBean secondBean) {
        secondBean.ping();
        return new FirstBean();
    }

    @Produces
    @DummyQualifier("D")
    public SecondBean produceD() {
        return new SecondBean();
    }

    @Produces
    @DummyQualifier("F")
    public SecondBean produceF() {
        return new SecondBean();
    }

    public void disposeA(@Disposes @DummyQualifier("A") FirstBean bean) {
        firstDisposerCalled.incrementAndGet();
    }

    public void disposeB(@Disposes @DummyQualifier("B") FirstBean bean) {
        secondDisposerCalled.incrementAndGet();
    }

    public void disposeC(@Disposes @DummyQualifier("C") FirstBean bean) {
        thirdDisposerCalled.incrementAndGet();
    }

    public void disposeE(@Disposes @DummyQualifier("E") FirstBean bean) {
        sixthDisposerCalled.incrementAndGet();
    }

    public void disposeD(@Disposes @DummyQualifier("D") SecondBean bean) {
        forthDisposerCalled.incrementAndGet();
    }

    public void disposeF(@Disposes @DummyQualifier("F") SecondBean bean) {
        fifthDisposerCalled.incrementAndGet();
    }

    public static void reset() {
        firstDisposerCalled = new AtomicInteger();
        secondDisposerCalled = new AtomicInteger();
        thirdDisposerCalled = new AtomicInteger();
        forthDisposerCalled = new AtomicInteger();
        fifthDisposerCalled = new AtomicInteger();
        sixthDisposerCalled = new AtomicInteger();
    }
}
