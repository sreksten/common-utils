package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

@Priority(900)
@Alternative
@Dependent
public class Boss {

    @Produces
    public TestBean produceSimpleTestBean() {
        return new SimpleTestBean();
    }
}
