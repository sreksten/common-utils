package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.concurrency.ParallelTaskExecutor;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class ClassProcessor implements ClassConsumer {

    private final ParallelTaskExecutor taskExecutor;
    private final KnowledgeBase knowledgeBase;
    private final CDI41BeanValidator cdi41BeanValidator;
    private final BeanArchiveMode beanArchiveMode;

    /**
     * Track classes that have been processed to prevent duplicate bean registration.
     * Although ParallelClasspathScanner deduplicates at scan time, this provides
     * an additional safety check to ensure each class is only processed once.
     */
    private final Set<Class<?>> processedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ClassProcessor(ParallelTaskExecutor taskExecutor, KnowledgeBase knowledgeBase, BeanArchiveMode beanArchiveMode) {
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor cannot be null");
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        this.beanArchiveMode = Objects.requireNonNull(beanArchiveMode, "beanArchiveMode cannot be null");
        this.cdi41BeanValidator = new CDI41BeanValidator(knowledgeBase);
    }

    public void add(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Class cannot be null");
        // Only schedule processing if we haven't seen this class before
        if (processedClasses.add(clazz)) {
            taskExecutor.schedulePlatformThread(() -> accept(clazz));
        }
    }

    private void accept(Class<?> clazz) {
        // CDI41BeanValidator always registers beans (even invalid ones)
        // and marks them with hasValidationErrors flag
        cdi41BeanValidator.validateAndRegister(clazz, beanArchiveMode);
        // Always add to knowledge base for class resolution
        knowledgeBase.add(clazz);
    }
}
