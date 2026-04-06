package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.exceptions.aroundInvoke;

import jakarta.enterprise.context.Dependent;

@Dependent
class ExceptionBean {

    private static int count = 0;

    @ExceptionBinding
    public boolean bar() {
        return false;
    }

    public static void failFirstTwoInvocations() {
        count++;
        if (count <= 2) {
            throw new RuntimeException();
        }
    }
}
