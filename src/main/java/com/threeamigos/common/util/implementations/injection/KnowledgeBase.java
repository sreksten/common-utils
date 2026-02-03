package com.threeamigos.common.util.implementations.injection;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

class KnowledgeBase {

    private final Collection<Class<?>> classes = new ConcurrentLinkedQueue<>();

    public void add(Class<?> clazz) {
        classes.add(clazz);
    }

    public Collection<Class<?>> getClasses() {
        return classes;
    }
}
