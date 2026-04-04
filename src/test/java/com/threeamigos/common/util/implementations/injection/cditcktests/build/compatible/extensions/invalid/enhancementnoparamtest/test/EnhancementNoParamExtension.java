package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.invalid.enhancementnoparamtest.test;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;

public class EnhancementNoParamExtension implements BuildCompatibleExtension {

    @Enhancement(types = SomeBean.class)
    public void enhance() {
        // no-op, deployment should fail
    }
}
