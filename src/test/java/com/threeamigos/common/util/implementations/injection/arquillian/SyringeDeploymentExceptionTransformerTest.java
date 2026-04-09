package com.threeamigos.common.util.implementations.injection.arquillian;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class SyringeDeploymentExceptionTransformerTest {

    private final SyringeDeploymentExceptionTransformer transformer = new SyringeDeploymentExceptionTransformer();

    @Test
    void shouldWrapMessageOnlyDefinitionFailureAsDeploymentException() {
        RuntimeException containerFailure = new RuntimeException(
                "Could not deploy archive test.war. Caused by: jakarta.enterprise.inject.spi.DefinitionException: " +
                        "Deployment validation failed. See log for details.");

        Throwable transformed = transformer.transform(containerFailure);

        jakarta.enterprise.inject.spi.DeploymentException deploymentException =
                assertInstanceOf(jakarta.enterprise.inject.spi.DeploymentException.class, transformed);
        assertInstanceOf(jakarta.enterprise.inject.spi.DefinitionException.class, deploymentException.getCause());
    }

    @Test
    void shouldWrapNonPortableBehaviourMarkerAsDefinitionDeploymentException() {
        RuntimeException containerFailure = new RuntimeException(
                "WFLYCTL0080 ... Caused by: " +
                        "com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException: " +
                        "interceptor declares scope @RequestScoped");

        Throwable transformed = transformer.transform(containerFailure);

        jakarta.enterprise.inject.spi.DeploymentException deploymentException =
                assertInstanceOf(jakarta.enterprise.inject.spi.DeploymentException.class, transformed);
        assertInstanceOf(jakarta.enterprise.inject.spi.DefinitionException.class, deploymentException.getCause());
    }

    @Test
    void shouldWrapDefinitionMarkerInsideDeploymentExceptionMessage() {
        jakarta.enterprise.inject.spi.DeploymentException deploymentException =
                new jakarta.enterprise.inject.spi.DeploymentException(
                        "WFLYCTL0080 ... Caused by: jakarta.enterprise.inject.spi.DefinitionException: Invalid BCE method");

        Throwable transformed = transformer.transform(deploymentException);

        jakarta.enterprise.inject.spi.DeploymentException transformedDeployment =
                assertInstanceOf(jakarta.enterprise.inject.spi.DeploymentException.class, transformed);
        assertInstanceOf(jakarta.enterprise.inject.spi.DefinitionException.class, transformedDeployment.getCause());
    }

    @Test
    void shouldWrapMessageOnlyDeploymentFailureAsDeploymentException() {
        RuntimeException containerFailure = new RuntimeException(
                "Could not deploy archive test.war. Caused by: jakarta.enterprise.inject.spi.DeploymentException: " +
                        "Deployment validation failed. See log for details.");

        Throwable transformed = transformer.transform(containerFailure);

        assertInstanceOf(jakarta.enterprise.inject.spi.DeploymentException.class, transformed);
    }

    @Test
    void shouldPreserveStandaloneDefinitionException() {
        jakarta.enterprise.inject.spi.DefinitionException definitionException =
                new jakarta.enterprise.inject.spi.DefinitionException("Added definition error");

        Throwable transformed = transformer.transform(definitionException);

        assertSame(definitionException, transformed);
    }

    @Test
    void shouldPreserveAlreadyWrappedDefinitionDeploymentException() {
        jakarta.enterprise.inject.spi.DeploymentException alreadyWrapped =
                new jakarta.enterprise.inject.spi.DeploymentException(
                        "Deployment validation failed",
                        new jakarta.enterprise.inject.spi.DefinitionException("Added definition error"));

        Throwable transformed = transformer.transform(alreadyWrapped);

        assertSame(alreadyWrapped, transformed);
    }

    @Test
    void shouldPreserveJakartaDeploymentException() {
        jakarta.enterprise.inject.spi.DeploymentException deploymentException =
                new jakarta.enterprise.inject.spi.DeploymentException("General deployment problem");

        Throwable transformed = transformer.transform(deploymentException);

        assertSame(deploymentException, transformed);
    }
}
