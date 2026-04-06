package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.exceptions.aroundInvoke;

import jakarta.enterprise.context.Dependent;

@SimpleBinding
@Dependent
class SimpleBean {

    public boolean foo() throws NoSuchMethodException {
        throw new RuntimeException();
    }
}
