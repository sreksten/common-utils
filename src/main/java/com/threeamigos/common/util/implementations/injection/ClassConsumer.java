package com.threeamigos.common.util.implementations.injection;

@FunctionalInterface
interface ClassConsumer {

    void add(Class<?> clazz);

}
