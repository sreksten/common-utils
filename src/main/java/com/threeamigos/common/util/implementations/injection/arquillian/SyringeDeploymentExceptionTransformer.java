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
    public DeploymentException transform(Throwable exception) {
        Throwable definitionCause = findDefinitionOrDeploymentException(exception);
        if (definitionCause != null) {
            return new DeploymentException("CDI definition error during deployment", definitionCause);
        }
        return null; // let Arquillian handle other exceptions
    }

    private Throwable findDefinitionOrDeploymentException(Throwable throwable) {
        while (throwable != null) {
            if (isDefinitionOrDeploymentException(throwable)) {
                return throwable;
            }
            throwable = throwable.getCause();
        }
        return null;
    }

    private boolean isDefinitionOrDeploymentException(Throwable t) {
        // Accept both jakarta and legacy javax DefinitionException
        String cn = t.getClass().getName();
        return "jakarta.enterprise.inject.spi.DefinitionException".equals(cn) ||
                "javax.enterprise.inject.spi.DefinitionException".equals(cn) ||
                "jakarta.enterprise.inject.spi.DeploymentException".equals(cn) ||
                "javax.enterprise.inject.spi.DeploymentException".equals(cn);
    }
}
