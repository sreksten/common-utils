package com.threeamigos.common.util.implementations.injection;


import java.util.Collection;

@FunctionalInterface
interface ClasspathScannerSink {

    void add(Class<?> clazz);

}
