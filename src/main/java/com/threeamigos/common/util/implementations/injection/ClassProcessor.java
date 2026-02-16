package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;

import java.util.Objects;

class ClassProcessor implements ClasspathScannerSink {

    private final ParallelTaskExecutor taskExecutor;
    private final KnowledgeBase knowledgeBase;
    private final JSR330Validator jsr330Validator;

    public ClassProcessor(ParallelTaskExecutor taskExecutor, KnowledgeBase knowledgeBase) {
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor cannot be null");
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.jsr330Validator = new JSR330Validator(knowledgeBase);
    }

    public void add(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class cannot be null");
        taskExecutor.schedulePlatformThread(() -> accept(clazz));
    }

    private void accept(Class<?> clazz) {
        if (jsr330Validator.isValid(clazz)) {
            knowledgeBase.add(clazz);
        }
    }
}
