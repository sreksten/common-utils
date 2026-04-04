package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test;

import java.util.concurrent.atomic.AtomicInteger;

public class MyPojo {

    private static final AtomicInteger CREATED_COUNTER = new AtomicInteger(0);
    private static final AtomicInteger DESTROYED_COUNTER = new AtomicInteger(0);

    public MyPojo() {
        CREATED_COUNTER.incrementAndGet();
    }

    public String hello() {
        return "Hello!";
    }

    public void destroy() {
        DESTROYED_COUNTER.incrementAndGet();
    }

    public static void reset() {
        CREATED_COUNTER.set(0);
        DESTROYED_COUNTER.set(0);
    }

    public static int getCreatedCounter() {
        return CREATED_COUNTER.get();
    }

    public static int getDestroyedCounter() {
        return DESTROYED_COUNTER.get();
    }
}
