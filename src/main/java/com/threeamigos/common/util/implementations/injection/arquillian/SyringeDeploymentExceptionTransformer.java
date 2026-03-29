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

    @Override
    public Throwable transform(Throwable exception) {
        if (exception == null) {
            return null;
        }

        if (exception instanceof jakarta.enterprise.inject.spi.DefinitionException
                || exception instanceof jakarta.enterprise.inject.spi.DeploymentException) {
            return exception;
        }

        Throwable current = exception;
        Map<Throwable, Boolean> visited = new IdentityHashMap<>();
        while (current != null && !visited.containsKey(current)) {
            visited.put(current, Boolean.TRUE);
            String exceptionClassName = current.getClass().getName();

            if (isDefinitionExceptionClassName(exceptionClassName)) {
                return new jakarta.enterprise.inject.spi.DefinitionException(buildMessage(current, exception));
            }
            if (isDeploymentExceptionClassName(exceptionClassName)) {
                return new jakarta.enterprise.inject.spi.DeploymentException(buildMessage(current, exception));
            }

            String message = current.getMessage();
            if (message != null) {
                if (containsDeploymentExceptionMarker(message)) {
                    return new jakarta.enterprise.inject.spi.DeploymentException(message);
                }
                if (containsDefinitionExceptionMarker(message)) {
                    return new jakarta.enterprise.inject.spi.DefinitionException(message);
                }
            }

            current = current.getCause();
        }

        return exception;
    }

    private static boolean isDefinitionExceptionClassName(String exceptionClassName) {
        return "javax.enterprise.inject.spi.DefinitionException".equals(exceptionClassName)
                || "jakarta.enterprise.inject.spi.DefinitionException".equals(exceptionClassName)
                || "org.jboss.weld.exceptions.DefinitionException".equals(exceptionClassName);
    }

    private static boolean isDeploymentExceptionClassName(String exceptionClassName) {
        return "javax.enterprise.inject.spi.DeploymentException".equals(exceptionClassName)
                || "jakarta.enterprise.inject.spi.DeploymentException".equals(exceptionClassName)
                || "org.jboss.weld.exceptions.DeploymentException".equals(exceptionClassName)
                || "org.jboss.weld.exceptions.UnserializableDependencyException".equals(exceptionClassName)
                || "org.jboss.weld.exceptions.InconsistentSpecializationException".equals(exceptionClassName);
    }

    private static boolean containsDefinitionExceptionMarker(String message) {
        return message.contains("javax.enterprise.inject.spi.DefinitionException")
                || message.contains("jakarta.enterprise.inject.spi.DefinitionException")
                || message.contains("org.jboss.weld.exceptions.DefinitionException");
    }

    private static boolean containsDeploymentExceptionMarker(String message) {
        return message.contains("javax.enterprise.inject.spi.DeploymentException")
                || message.contains("jakarta.enterprise.inject.spi.DeploymentException")
                || message.contains("org.jboss.weld.exceptions.DeploymentException")
                || message.contains("org.jboss.weld.exceptions.UnserializableDependencyException")
                || message.contains("org.jboss.weld.exceptions.InconsistentSpecializationException");
    }

    private static String buildMessage(Throwable current, Throwable original) {
        String message = current.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        String fallback = original.getMessage();
        return fallback != null ? fallback : current.getClass().getName();
    }
}
