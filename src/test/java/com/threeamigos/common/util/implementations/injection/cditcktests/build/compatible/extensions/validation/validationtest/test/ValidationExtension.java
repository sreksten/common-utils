package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.validation.validationtest.test;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.Validation;

public class ValidationExtension implements BuildCompatibleExtension {

    @Validation
    public void validate(Messages messages) {
        messages.error("Deployment should fail");
    }
}
