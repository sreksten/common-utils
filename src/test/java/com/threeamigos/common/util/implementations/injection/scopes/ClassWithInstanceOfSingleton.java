package com.threeamigos.common.util.implementations.injection.scopes;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

public class ClassWithInstanceOfSingleton {
    private final Instance<SingletonDependency> instance;
    @Inject
    public ClassWithInstanceOfSingleton(Instance<SingletonDependency> instance) {
        this.instance = instance;
    }
    public Instance<SingletonDependency> getInstance() {
        return instance;
    }
}
