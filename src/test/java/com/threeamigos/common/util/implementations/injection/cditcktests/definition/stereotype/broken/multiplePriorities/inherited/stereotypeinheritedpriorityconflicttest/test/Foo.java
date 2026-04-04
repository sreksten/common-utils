package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited.stereotypeinheritedpriorityconflicttest.test;

@AnotherDumbStereotype
public class Foo extends FooAncestor {

    public String ping() {
        return Foo.class.getSimpleName();
    }
}
