package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

import java.io.Serializable;

@Dependent
public class YoghurtField implements Serializable {

    @Inject
    private Bean<Cream> bean;
}
