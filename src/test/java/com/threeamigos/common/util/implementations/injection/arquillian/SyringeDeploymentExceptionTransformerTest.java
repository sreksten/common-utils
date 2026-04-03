package com.threeamigos.common.util.implementations.injection.arquillian;

import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyringeDeploymentExceptionTransformerTest {

    @Test
    void transformShouldHandleCyclicCauseChains() {
        SyringeDeploymentExceptionTransformer transformer = new SyringeDeploymentExceptionTransformer();

        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second");
        first.initCause(second);
        second.initCause(first);

        Throwable transformed = transformer.transform(first);

        assertSame(first, transformed);
    }

    @Test
    void transformShouldNotPropagatePotentiallyCyclicCauseForMarkers() {
        SyringeDeploymentExceptionTransformer transformer = new SyringeDeploymentExceptionTransformer();

        RuntimeException exception = new RuntimeException(
                "jakarta.enterprise.inject.spi.DefinitionException: broken deployment");

        Throwable transformed = transformer.transform(exception);

        assertTrue(transformed instanceof DefinitionException);
        assertNull(transformed.getCause());
    }

    @Test
    void transformShouldTolerateThrowableWithRecursiveMessage() {
        SyringeDeploymentExceptionTransformer transformer = new SyringeDeploymentExceptionTransformer();

        Throwable recursiveMessageThrowable = new Throwable() {
            @Override
            public String getMessage() {
                return getMessage();
            }
        };

        Throwable transformed = transformer.transform(recursiveMessageThrowable);

        assertSame(recursiveMessageThrowable, transformed);
    }
}
