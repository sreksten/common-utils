package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.invalid.enhancementonlytypestest.test;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Types;

public class EnhancementOnlyTypesExtension implements BuildCompatibleExtension {

    @Enhancement(types = SomeBean.class)
    public void enhance(Types types) {
        // no-op, deployment should fail
    }
}
