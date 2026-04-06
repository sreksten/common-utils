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
    public Throwable transform(final Throwable throwable) {
//        if (throwable == null) {
//            return new DeploymentException(new Exception());
//        }
        return throwable;
    }

    public Throwable transformOLD(Throwable exception) {
        if (exception == null) {
            return null;
        }

        ScanResult scan = scan(exception);

        if (scan.cycleDetected) {
            if (scan.hasDefinitionType() || scan.definitionMarkerMessage != null) {
                return createDefinitionWrappedAsDeployment(selectDefinitionMessage(scan, exception));
            }
            return createDeploymentException(selectDeploymentMessage(scan, exception));
        }

        // Idempotent handling: preserve already-compatible transformed throwables.
        if (isDefinitionWrappedAsDeployment(exception)) {
            return exception;
        }

        if (scan.hasJakartaDefinition && scan.hasJakartaDeployment) {
            return exception;
        }

        // Normalize a standalone definition failure so it can satisfy both DefinitionException
        // and DeploymentException expectations in ShouldThrowException checks.
        if (scan.hasJakartaDefinition && !scan.hasJakartaDeployment) {
            return createDefinitionWrappedAsDeployment(selectDefinitionMessage(scan, exception));
        }

        if (scan.hasJakartaDeployment) {
            return exception;
        }

        if (scan.hasLegacyDefinition || scan.definitionMarkerMessage != null) {
            return createDefinitionWrappedAsDeployment(selectDefinitionMessage(scan, exception));
        }

        if (scan.hasLegacyDeployment || scan.deploymentMarkerMessage != null) {
            return createDeploymentException(selectDeploymentMessage(scan, exception));
        }

        return exception;
    }

    private static RuntimeException createDefinitionWrappedAsDeployment(String message) {
        RuntimeException definition = new jakarta.enterprise.inject.spi.DefinitionException(message);
        return new jakarta.enterprise.inject.spi.DeploymentException(message, definition);
    }

    private static RuntimeException createDeploymentException(String message) {
        return new jakarta.enterprise.inject.spi.DeploymentException(message);
    }

    private static ScanResult scan(Throwable exception) {
        ScanResult result = new ScanResult();
        Throwable current = exception;
        Map<Throwable, Boolean> visited = new IdentityHashMap<Throwable, Boolean>();
        while (current != null) {
            if (visited.containsKey(current)) {
                result.cycleDetected = true;
                return result;
            }
            visited.put(current, Boolean.TRUE);

            String className = current.getClass().getName();
            if (isJakartaDefinitionExceptionClassName(className)) {
                result.hasJakartaDefinition = true;
            } else if (isLegacyDefinitionExceptionClassName(className)) {
                result.hasLegacyDefinition = true;
            }
            if (isJakartaDeploymentExceptionClassName(className)) {
                result.hasJakartaDeployment = true;
            } else if (isLegacyDeploymentExceptionClassName(className)) {
                result.hasLegacyDeployment = true;
            }

            String message = safeGetMessage(current);
            if (result.firstNonEmptyMessage == null && message != null && !message.trim().isEmpty()) {
                result.firstNonEmptyMessage = message;
            }
            if (result.definitionMarkerMessage == null && message != null && containsDefinitionExceptionMarker(message)) {
                result.definitionMarkerMessage = message;
            }
            if (result.deploymentMarkerMessage == null && message != null && containsDeploymentExceptionMarker(message)) {
                result.deploymentMarkerMessage = message;
            }

            current = current.getCause();
        }
        return result;
    }

    private static boolean isDefinitionWrappedAsDeployment(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (!isJakartaDeploymentExceptionClassName(throwable.getClass().getName())) {
            return false;
        }
        Throwable cause = throwable.getCause();
        return cause != null && isJakartaDefinitionExceptionClassName(cause.getClass().getName());
    }

    private static boolean isLegacyDefinitionExceptionClassName(String exceptionClassName) {
        return "javax.enterprise.inject.spi.DefinitionException".equals(exceptionClassName)
                || "org.jboss.weld.exceptions.DefinitionException".equals(exceptionClassName);
    }

    private static boolean isLegacyDeploymentExceptionClassName(String exceptionClassName) {
        return "javax.enterprise.inject.spi.DeploymentException".equals(exceptionClassName)
                || "org.jboss.weld.exceptions.DeploymentException".equals(exceptionClassName)
                || "org.jboss.weld.exceptions.UnserializableDependencyException".equals(exceptionClassName)
                || "org.jboss.weld.exceptions.InconsistentSpecializationException".equals(exceptionClassName);
    }

    private static boolean isJakartaDefinitionExceptionClassName(String exceptionClassName) {
        return "jakarta.enterprise.inject.spi.DefinitionException".equals(exceptionClassName);
    }

    private static boolean isJakartaDeploymentExceptionClassName(String exceptionClassName) {
        return "jakarta.enterprise.inject.spi.DeploymentException".equals(exceptionClassName);
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
        String message = safeGetMessage(current);
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        String fallback = safeGetMessage(original);
        return fallback != null ? fallback : current.getClass().getName();
    }

    private static String selectDefinitionMessage(ScanResult scan, Throwable original) {
        if (scan.definitionMarkerMessage != null && !scan.definitionMarkerMessage.trim().isEmpty()) {
            return scan.definitionMarkerMessage;
        }
        if (scan.firstNonEmptyMessage != null && !scan.firstNonEmptyMessage.trim().isEmpty()) {
            return scan.firstNonEmptyMessage;
        }
        return buildMessage(original, original);
    }

    private static String selectDeploymentMessage(ScanResult scan, Throwable original) {
        if (scan.deploymentMarkerMessage != null && !scan.deploymentMarkerMessage.trim().isEmpty()) {
            return scan.deploymentMarkerMessage;
        }
        if (scan.firstNonEmptyMessage != null && !scan.firstNonEmptyMessage.trim().isEmpty()) {
            return scan.firstNonEmptyMessage;
        }
        return buildMessage(original, original);
    }

    private static String safeGetMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        try {
            return throwable.getMessage();
        } catch (StackOverflowError e) {
            return throwable.getClass().getName();
        } catch (Throwable ignored) {
            return throwable.getClass().getName();
        }
    }

    private static final class ScanResult {
        private boolean cycleDetected;
        private boolean hasJakartaDefinition;
        private boolean hasJakartaDeployment;
        private boolean hasLegacyDefinition;
        private boolean hasLegacyDeployment;
        private String definitionMarkerMessage;
        private String deploymentMarkerMessage;
        private String firstNonEmptyMessage;

        private boolean hasDefinitionType() {
            return hasJakartaDefinition || hasLegacyDefinition;
        }
    }
}
