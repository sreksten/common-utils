package com.threeamigos.common.util.interfaces.injection;

import java.util.function.Supplier;

/**
 * Strategy interface for managing the lifecycle of scoped beans.<br/>
 */
public interface ScopeHandler {

    <T> T get(Class<T> clazz, Supplier<T> provider);

}
