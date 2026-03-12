package com.threeamigos.common.util.implementations.injection.arquillian;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.DeploymentExceptionTransformer;

/**
 * Transforms container deployment failures into Arquillian {@link DeploymentException}s
 * when the root cause is a CDI definition/validation error. This mirrors the behavior
 * of the Weld adapter, so TCK tests that expect DefinitionException don’t get marked
 * as generic deployment failures.
 */
public class SyringeDeploymentExceptionTransformer implements DeploymentExceptionTransformer {

    @Override
    public Throwable transform(Throwable exception) {

        if (exception instanceof jakarta.enterprise.inject.spi.DefinitionException ||
                exception instanceof jakarta.enterprise.inject.spi.DeploymentException) {
            return exception;
        }

        if (exception instanceof javax.enterprise.inject.spi.DefinitionException) {
            return new jakarta.enterprise.inject.spi.DefinitionException(exception.getMessage());
        }
        if (exception instanceof javax.enterprise.inject.spi.DeploymentException) {
            return new jakarta.enterprise.inject.spi.DeploymentException(exception.getMessage());
        }
        return exception;
    }

}
