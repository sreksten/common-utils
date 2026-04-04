package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotinheritedtest.testresolution;

import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier.False;
import jakarta.enterprise.context.Dependent;

@False
@Dependent
public class Monster {

    public int ping() {
        return 1;
    }
}
