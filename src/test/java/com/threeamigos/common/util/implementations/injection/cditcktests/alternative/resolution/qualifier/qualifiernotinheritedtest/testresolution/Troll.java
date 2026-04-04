package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotinheritedtest.testresolution;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;

@Alternative
@Priority(1)
@Dependent
public class Troll extends Monster {

    @Override
    public int ping() {
        return 0;
    }
}
