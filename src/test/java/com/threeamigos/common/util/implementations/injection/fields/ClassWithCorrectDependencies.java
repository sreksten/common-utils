package com.threeamigos.common.util.implementations.injection.fields;

import javax.inject.Inject;

public class ClassWithCorrectDependencies {

    @Inject
    private ClassFirstDependency firstDependency;

    @Inject
    private ClassSecondDependency secondDependency;

    public ClassFirstDependency getFirstDependency() {
        return firstDependency;
    }

    public ClassSecondDependency getSecondDependency() {
        return secondDependency;
    }
}
