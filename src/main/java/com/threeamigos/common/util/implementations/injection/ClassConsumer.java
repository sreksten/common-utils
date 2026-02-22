package com.threeamigos.common.util.implementations.injection;

/**
 * Consumer for classes discovered during classpath scanning.
 * Implementations process discovered classes and determine if they are CDI beans.
 */
interface ClassConsumer {

    /**
     * Adds a discovered class with its bean archive mode.
     *
     * @param clazz the discovered class
     * @param beanArchiveMode the bean archive mode for the archive containing this class
     */
    void add(Class<?> clazz, BeanArchiveMode beanArchiveMode);

}
