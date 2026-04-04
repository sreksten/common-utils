package com.threeamigos.common.util.implementations.injection.cditcktests.context.getfromcontextualtest.testgetmaynotcreatenewinstanceunlesscreationalcontextgiven;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.Serializable;

@ApplicationScoped
public class MyApplicationBean implements Serializable {

    private static final long serialVersionUID = 1L;
}
