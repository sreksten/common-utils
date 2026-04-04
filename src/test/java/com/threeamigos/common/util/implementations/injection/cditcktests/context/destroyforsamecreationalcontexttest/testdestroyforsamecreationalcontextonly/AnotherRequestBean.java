package com.threeamigos.common.util.implementations.injection.cditcktests.context.destroyforsamecreationalcontexttest.testdestroyforsamecreationalcontextonly;

import jakarta.enterprise.context.RequestScoped;

import java.io.Serializable;

@RequestScoped
public class AnotherRequestBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public void ping() {
    }
}
