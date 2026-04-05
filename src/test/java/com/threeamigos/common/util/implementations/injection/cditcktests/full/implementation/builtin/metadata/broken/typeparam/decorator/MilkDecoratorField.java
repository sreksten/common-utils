package com.threeamigos.common.util.implementations.injection.cditcktests.full.implementation.builtin.metadata.broken.typeparam.decorator;

import com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam.Cream;
import com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam.Milk;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

@Decorator
@Priority(100)
public class MilkDecoratorField implements Milk {

    @Inject
    @Delegate
    Milk milk;

    @Inject
    jakarta.enterprise.inject.spi.Decorator<Cream> decorator;

    @Override
    public void ping() {
        milk.ping();
    }
}
