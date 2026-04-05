package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.broken.exception.validation;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Extension;

public class ValidationObserver implements Extension {

    void afterDeploymentValidation(@Observes AfterDeploymentValidation event) {
        throw new AssertionError("This error should be treated as a deployment error");
    }
}
