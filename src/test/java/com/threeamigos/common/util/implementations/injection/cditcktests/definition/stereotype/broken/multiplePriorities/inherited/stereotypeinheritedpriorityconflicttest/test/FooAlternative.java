package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited.stereotypeinheritedpriorityconflicttest.test;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;

@Dependent
@Alternative
public class FooAlternative extends Foo {

    public String ping() {
        return FooAlternative.class.getSimpleName();
    }
}
