package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifierinheritedtest.testresolution;

import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier.True;
import jakarta.enterprise.context.Dependent;

@True
@Dependent
public class Tree {

    public int ping() {
        return 1;
    }
}
