package com.threeamigos.common.util.implementations.injection.cditcktests.full.implementation.builtin.metadata.broken.typeparam.decorator;

import com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam.Cream;
import com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam.Milk;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Decorated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

@Decorator
@Priority(100)
public class MilkDecoratedBeanConstructor implements Milk {

    @Inject
    @Delegate
    Milk milk;

    @Inject
    public MilkDecoratedBeanConstructor(@Decorated Bean<Cream> bean) {
    }

    @Override
    public void ping() {
        milk.ping();
    }
}
