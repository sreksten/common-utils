package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.invalid.enhancementmultipleparamstest.test;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.lang.model.declarations.ClassInfo;

public class EnhancementMultipleParamsExtension implements BuildCompatibleExtension {

    @Enhancement(types = SomeBean.class)
    public void enhance(ClassConfig classConfig, ClassInfo classInfo, Messages messages) {
        // no-op, deployment should fail
    }
}
