package com.threeamigos.common.util.implementations.injection.arquillian;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        assertTrue(transformed.getClass().getName().endsWith("DeploymentException"));
        assertTrue(transformed != transformed.getCause());
    }

    @Test
    void transformShouldNotPropagatePotentiallyCyclicCauseForMarkers() {
        SyringeDeploymentExceptionTransformer transformer = new SyringeDeploymentExceptionTransformer();

        RuntimeException exception = new RuntimeException(
                "jakarta.enterprise.inject.spi.DefinitionException: broken deployment");

        Throwable transformed = transformer.transform(exception);

        assertTrue(transformed.getClass().getName().endsWith("DeploymentException"));
        assertNotNull(transformed.getCause());
        assertTrue(
                transformed.getCause().getClass().getName().endsWith("DefinitionException")
                        || transformed.getCause().getClass().getName().endsWith("DeploymentException"));
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

    @Test
    void transformShouldKeepAcyclicUnknownExceptionsUntouched() {
        SyringeDeploymentExceptionTransformer transformer = new SyringeDeploymentExceptionTransformer();

        RuntimeException exception = new RuntimeException("plain failure");

        Throwable transformed = transformer.transform(exception);

        assertSame(exception, transformed);
    }
}
