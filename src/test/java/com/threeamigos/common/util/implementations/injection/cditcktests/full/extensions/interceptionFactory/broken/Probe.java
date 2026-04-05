package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.interceptionFactory.broken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
class Probe {

    @Inject
    @Any
    private Instance<UnproxyableType> unproxyableTypeInstance;

    @Inject
    @Any
    private Instance<Foo> fooInstance;

    Instance<UnproxyableType> getUnproxyableTypeInstance() {
        return unproxyableTypeInstance;
    }

    Instance<Foo> getFooInstance() {
        return fooInstance;
    }
}
