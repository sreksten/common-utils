package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotdeclaredtest.testresolution;

import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier.True;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class Qux {

    @Inject
    @True
    private Foo foo;

    public Foo getFoo() {
        return foo;
    }
}
