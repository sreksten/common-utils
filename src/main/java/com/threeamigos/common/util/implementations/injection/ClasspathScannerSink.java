package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClasspathScannerSink {

    private final Collection<Class<?>> sink;
    ParallelTaskExecutor taskExecutor;

    public ClasspathScannerSink(ParallelTaskExecutor taskExecutor) {
        this.sink = new ConcurrentLinkedQueue<>();
        this.taskExecutor = taskExecutor;
    }

    public void add(Class<?> clazz) {
        sink.add(clazz);
        taskExecutor.submit(() -> System.out.println(clazz.getName()));
    }

    public Collection<Class<?>> getSink() {
        return sink;
    }
}
