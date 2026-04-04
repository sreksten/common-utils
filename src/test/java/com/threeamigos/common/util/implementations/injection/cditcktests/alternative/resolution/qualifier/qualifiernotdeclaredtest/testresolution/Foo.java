package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotdeclaredtest.testresolution;

import jakarta.enterprise.context.Dependent;

@Dependent
public interface Foo {

    int ping();
}
