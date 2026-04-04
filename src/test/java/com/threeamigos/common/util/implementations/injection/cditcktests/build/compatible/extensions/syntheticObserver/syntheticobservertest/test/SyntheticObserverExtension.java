package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.Any;

public class SyntheticObserverExtension implements BuildCompatibleExtension {

    @Synthesis
    public void synthesize(SyntheticComponents syn) {
        syn.addObserver(MyEvent.class)
                .qualifier(Any.Literal.INSTANCE)
                .priority(10)
                .observeWith(MyObserver.class);

        syn.addObserver(Object.class)
                .qualifier(MyQualifierLiteral.INSTANCE)
                .priority(20)
                .withParam("name", "@MyQualifier")
                .observeWith(MyQualifiedObserver.class);
    }
}
