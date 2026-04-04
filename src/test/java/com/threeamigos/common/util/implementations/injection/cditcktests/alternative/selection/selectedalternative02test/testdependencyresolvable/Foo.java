package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;

@Priority(1000)
@Alternative
@Dependent
public class Foo implements TestBean {

    @Override
    public String getId() {
        return Foo.class.getName();
    }
}
