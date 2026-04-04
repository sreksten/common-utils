package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotdeclaredtest.testresolution;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;

@Alternative
@Priority(1)
@Dependent
public class Bar implements Foo {

    @Override
    public int ping() {
        return 0;
    }
}
