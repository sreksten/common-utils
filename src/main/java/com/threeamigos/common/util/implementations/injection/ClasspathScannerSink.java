package com.threeamigos.common.util.implementations.injection;

@FunctionalInterface
interface ClasspathScannerSink {

    void add(Class<?> clazz);

}
