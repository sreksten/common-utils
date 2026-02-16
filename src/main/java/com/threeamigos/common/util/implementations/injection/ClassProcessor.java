package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;

import java.util.Objects;

class ClassProcessor implements ClasspathScannerSink {

    private final ParallelTaskExecutor taskExecutor;
    private final KnowledgeBase knowledgeBase;
    private final CDI41BeanValidator cdi41BeanValidator;

    public ClassProcessor(ParallelTaskExecutor taskExecutor, KnowledgeBase knowledgeBase) {
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor cannot be null");
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.cdi41BeanValidator = new CDI41BeanValidator(knowledgeBase);
    }

    public void add(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class cannot be null");
        taskExecutor.schedulePlatformThread(() -> accept(clazz));
    }

    private void accept(Class<?> clazz) {
        // CDI41BeanValidator always registers beans (even invalid ones)
        // and marks them with hasValidationErrors flag
        cdi41BeanValidator.validateAndRegister(clazz);
        // Always add to knowledge base for class resolution
        knowledgeBase.add(clazz);
    }
}
