package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;

import java.util.concurrent.atomic.AtomicInteger;

@Dependent
public class MyDependentBean {

    private static final AtomicInteger CREATED_COUNTER = new AtomicInteger(0);
    private static final AtomicInteger DESTROYED_COUNTER = new AtomicInteger(0);

    @PostConstruct
    void postConstruct() {
        CREATED_COUNTER.incrementAndGet();
    }

    @PreDestroy
    void preDestroy() {
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
