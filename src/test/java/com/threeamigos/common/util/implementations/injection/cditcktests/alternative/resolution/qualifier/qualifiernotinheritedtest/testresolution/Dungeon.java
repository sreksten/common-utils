package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotinheritedtest.testresolution;

import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier.False;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class Dungeon {

    @Inject
    @False
    private Monster monster;

    public Monster getMonster() {
        return monster;
    }
}
