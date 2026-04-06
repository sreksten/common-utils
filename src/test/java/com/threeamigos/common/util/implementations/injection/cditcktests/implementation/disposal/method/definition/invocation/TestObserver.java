package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.invocation;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@Dependent
public class TestObserver {

    @Inject
    @DummyQualifier("B")
    Instance<FirstBean> beanB;

    @Inject
    @DummyQualifier("C")
    FirstBean beanC;

    @Inject
    @DummyQualifier("E")
    FirstBean beanE;

    public void observes(@Observes String message, @DummyQualifier("A") FirstBean dependentBean) {
        beanB.destroy(beanB.get());
        beanE.ping();
        beanC.ping();
    }
}
