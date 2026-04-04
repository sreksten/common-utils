package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotdeclaredtest.testresolution;

import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier.True;
import jakarta.enterprise.context.Dependent;

@True
@Dependent
public class Baz implements Foo {

    @Override
    public int ping() {
        return 1;
    }
}
