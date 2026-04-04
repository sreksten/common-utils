package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited.stereotypeinheritedpriorityconflicttest.test;

@DumbStereotype
public class FooAncestor {

    public String ping() {
        return FooAncestor.class.getSimpleName();
    }
}
