package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer.broken.generic;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import java.util.List;

@Dependent
public class Foo {

    private List<?> injectedList;

    @Inject
    public <T> void init(List<T> list) {
        injectedList = list;
    }

    public List<?> getInjectedList() {
        return injectedList;
    }
}
