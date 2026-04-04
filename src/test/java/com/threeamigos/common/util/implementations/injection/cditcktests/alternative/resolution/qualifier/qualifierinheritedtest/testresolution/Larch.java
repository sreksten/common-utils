package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifierinheritedtest.testresolution;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;

@Alternative
@Priority(1)
@Dependent
public class Larch extends Tree {

    @Override
    public int ping() {
        return 0;
    }
}
