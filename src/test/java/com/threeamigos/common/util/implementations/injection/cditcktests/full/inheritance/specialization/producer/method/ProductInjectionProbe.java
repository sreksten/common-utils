package com.threeamigos.common.util.implementations.injection.cditcktests.full.inheritance.specialization.producer.method;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
class ProductInjectionProbe {

    @Inject
    @Expensive
    private Product product;

    Product getProduct() {
        return product;
    }
}
