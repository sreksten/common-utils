package com.threeamigos.common.util.implementations.injection.arquillian;

import java.util.IdentityHashMap;
import java.util.Map;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.DeploymentExceptionTransformer;

/**
 * Transforms container deployment failures into Arquillian {@link DeploymentException}s
 * when the root cause is a CDI definition/validation error. This mirrors the behavior
 * of the Weld adapter, so TCK tests that expect DefinitionException don’t get marked
 * as generic deployment failures.
 */
public class SyringeDeploymentExceptionTransformer implements DeploymentExceptionTransformer {

    private static final String JAKARTA_DEFINITION_EXCEPTION_MARKER =
            "jakarta.enterprise.inject.spi.DefinitionException";
    private static final String NON_PORTABLE_BEHAVIOUR_EXCEPTION_MARKER =
            "com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException";

    @Override
    public Throwable transform(final Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        if (containsDefinitionException(throwable)) {
            return throwable;
        }

        String definitionMessage = findDefinitionFailureMessage(throwable);
        if (definitionMessage != null) {
            RuntimeException definition = new jakarta.enterprise.inject.spi.DefinitionException(definitionMessage);
            return new jakarta.enterprise.inject.spi.DeploymentException(definitionMessage, definition);
        }

        if (containsDeploymentException(throwable)) {
            return throwable;
        }

        String deploymentMessage =
                findJakartaExceptionMarkerMessage(throwable, "jakarta.enterprise.inject.spi.DeploymentException");
        if (deploymentMessage != null) {
            return new jakarta.enterprise.inject.spi.DeploymentException(deploymentMessage);
        }

        return throwable;
    }

    private static String findDefinitionFailureMessage(Throwable throwable) {
        String jakartaDefinitionMessage = findJakartaExceptionMarkerMessage(throwable, JAKARTA_DEFINITION_EXCEPTION_MARKER);
        if (jakartaDefinitionMessage != null) {
            return jakartaDefinitionMessage;
        }
        return findJakartaExceptionMarkerMessage(throwable, NON_PORTABLE_BEHAVIOUR_EXCEPTION_MARKER);
    }

    private static boolean containsDefinitionException(Throwable throwable) {
        return containsExceptionType(throwable, jakarta.enterprise.inject.spi.DefinitionException.class);
    }

    private static boolean containsDeploymentException(Throwable throwable) {
        return containsExceptionType(throwable, jakarta.enterprise.inject.spi.DeploymentException.class);
    }

    private static boolean containsExceptionType(Throwable throwable, Class<?> exceptionType) {
        Throwable current = throwable;
        Map<Throwable, Boolean> visited = new IdentityHashMap<Throwable, Boolean>();
        while (current != null && !visited.containsKey(current)) {
            visited.put(current, Boolean.TRUE);
            if (exceptionType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String findJakartaExceptionMarkerMessage(Throwable throwable, String marker) {
        Throwable current = throwable;
        Map<Throwable, Boolean> visited = new IdentityHashMap<Throwable, Boolean>();
        while (current != null && !visited.containsKey(current)) {
            visited.put(current, Boolean.TRUE);
            String message = current.getMessage();
            if (message != null && message.contains(marker)) {
                return message;
            }
            current = current.getCause();
        }
        return null;
    }
}
