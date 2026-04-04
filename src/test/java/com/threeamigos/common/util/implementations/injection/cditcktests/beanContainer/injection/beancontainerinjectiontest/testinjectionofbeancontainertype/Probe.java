package com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.injection.beancontainerinjectiontest.testinjectionofbeancontainertype;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@Dependent
public class Probe {

    @Inject
    private MyBean myBean;

    @Inject
    private Instance<Object> instance;

    public MyBean getMyBean() {
        return myBean;
    }

    public Instance<Object> getInstance() {
        return instance;
    }
}
