package com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.injection.beancontainerinjectiontest.testinjectionofbeancontainertype;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.inject.Inject;

@ApplicationScoped
public class MyBean {

    @Inject
    BeanContainer beanContainer;

    public BeanContainer getBeanContainer() {
        return beanContainer;
    }
}
