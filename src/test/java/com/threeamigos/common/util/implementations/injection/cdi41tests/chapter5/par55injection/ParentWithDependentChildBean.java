package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par55injection;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class ParentWithDependentChildBean {

    @Inject
    private DependentChildBean child;

    @PreDestroy
    void onPreDestroy() {
        DependentDestructionRecorder.record("parent-pre");
        DependentDestructionRecorder.record("parent-sees-child-destroyed=" + DependentDestructionRecorder.isChildDestroyed());
    }
}
