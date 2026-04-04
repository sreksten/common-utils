package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.invalid.enhancementonlymessagestest.test;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Messages;

public class EnhancementOnlyMessagesExtension implements BuildCompatibleExtension {

    @Enhancement(types = SomeBean.class)
    public void enhance(Messages messages) {
        // no-op, deployment should fail
    }
}
