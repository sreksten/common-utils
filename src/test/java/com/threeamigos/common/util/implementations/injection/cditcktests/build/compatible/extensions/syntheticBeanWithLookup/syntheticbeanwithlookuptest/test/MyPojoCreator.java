package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;

import java.util.concurrent.atomic.AtomicInteger;

public class MyPojoCreator implements SyntheticBeanCreator<MyPojo> {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    @Override
    public MyPojo create(Instance<Object> lookup, Parameters params) {
        COUNTER.incrementAndGet();
        lookup.select(MyDependentBean.class).get();
        return new MyPojo();
    }

    public static void reset() {
        COUNTER.set(0);
    }

    public static int getCounter() {
        return COUNTER.get();
    }
}
