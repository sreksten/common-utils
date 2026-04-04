package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanWithLookup.syntheticbeanwithlookuptest.test;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;

public class SyntheticBeanWithLookupExtension implements BuildCompatibleExtension {

    @Discovery
    public void discovery(ScannedClasses scan) {
        scan.add(MyDependentBean.class.getName());
    }

    @Synthesis
    public void synthesis(SyntheticComponents syn) {
        syn.addBean(MyPojo.class)
                .type(MyPojo.class)
                .scope(Dependent.class)
                .createWith(MyPojoCreator.class)
                .disposeWith(MyPojoDisposer.class);
    }
}
