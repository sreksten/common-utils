package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.injection;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.inject.Inject;

@Dependent
public class Foo {

    @Inject
    Interceptor<Foo> interceptor;
}
