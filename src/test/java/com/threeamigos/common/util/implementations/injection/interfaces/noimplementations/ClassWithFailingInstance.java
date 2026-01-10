package com.threeamigos.common.util.implementations.injection.interfaces.noimplementations;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

public class ClassWithFailingInstance {

    @Inject
    private Instance<NoImplementationsInterface> instance;

    public Instance<NoImplementationsInterface> getInstance() {
        return instance;
    }

}
