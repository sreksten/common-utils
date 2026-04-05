package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.el;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Named;

@Named
public class Foo {

    public Boolean getValue() {
        return Boolean.TRUE;
    }

    @PostConstruct
    public void init() {
        ActionSequence.addAction("create", Foo.class.getName());
    }

    @PreDestroy
    public void destroy() {
        ActionSequence.addAction("destroy", Foo.class.getName());
    }
}
