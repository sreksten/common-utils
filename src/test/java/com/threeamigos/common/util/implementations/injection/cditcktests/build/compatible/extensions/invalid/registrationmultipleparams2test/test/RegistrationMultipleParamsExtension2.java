package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.invalid.registrationmultipleparams2test.test;

import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Types;

public class RegistrationMultipleParamsExtension2 implements BuildCompatibleExtension {

    @Registration(types = {SomeBean.class})
    public void register(ObserverInfo observerInfo, Types types, BeanInfo beanInfo) {
        // no-op, deployment should fail
    }
}
