package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;

import java.util.Objects;

class ClassProcessor implements ClasspathScannerSink {

    private final ParallelTaskExecutor taskExecutor;
    private final KnowledgeBase knowledgeBase;

    public ClassProcessor(ParallelTaskExecutor taskExecutor, KnowledgeBase knowledgeBase) {
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor cannot be null");
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
    }

    public void add(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class cannot be null");
        taskExecutor.schedulePlatformThread(() -> accept(clazz));
    }

    private void accept(Class<?> clazz) {
        knowledgeBase.add(clazz);
        System.out.println(clazz.getName());
    }
}
