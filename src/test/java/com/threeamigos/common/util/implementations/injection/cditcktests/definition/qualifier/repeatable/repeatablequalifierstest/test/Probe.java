package com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.repeatable.repeatablequalifierstest.test;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@Dependent
public class Probe {

    @Inject
    @Any
    private Instance<Process> processInstance;

    @Inject
    private ProcessObserver observer;

    public Instance<Process> getProcessInstance() {
        return processInstance;
    }

    public ProcessObserver getObserver() {
        return observer;
    }
}
