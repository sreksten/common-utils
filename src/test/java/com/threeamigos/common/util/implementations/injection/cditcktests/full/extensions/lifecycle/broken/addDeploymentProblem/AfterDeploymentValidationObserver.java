package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.broken.addDeploymentProblem;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Extension;

public class AfterDeploymentValidationObserver implements Extension {

    void afterDeploymentValidation(@Observes AfterDeploymentValidation event) {
        event.addDeploymentProblem(new AssertionError("This error should be treated as a deployment error"));
    }
}
