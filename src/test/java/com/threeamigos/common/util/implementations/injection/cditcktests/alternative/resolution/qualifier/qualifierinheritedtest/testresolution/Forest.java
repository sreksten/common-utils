package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifierinheritedtest.testresolution;

import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier.True;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class Forest {

    @Inject
    @True
    private Tree tree;

    public Tree getTree() {
        return tree;
    }
}
