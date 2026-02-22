package com.threeamigos.common.util.implementations.injection.scopehandlers;

import java.util.function.Supplier;

/**
 * Strategy interface for managing the lifecycle of scoped beans.<br/>
 */
@Deprecated
public interface ScopeHandler extends AutoCloseable {

    <T> T get(Class<T> clazz, Supplier<T> provider);

    @Override
    void close() throws Exception;
}
