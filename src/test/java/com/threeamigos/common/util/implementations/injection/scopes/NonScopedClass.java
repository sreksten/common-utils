package com.threeamigos.common.util.implementations.injection.scopes;

import javax.inject.Inject;

public class NonScopedClass {
    private final SingletonDependency dependency;

    @Inject
    public NonScopedClass(SingletonDependency dependency) {
        this.dependency = dependency;
    }

    public SingletonDependency getDependency() {
        return dependency;
    }
}
