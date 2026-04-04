package com.threeamigos.common.util.implementations.injection.cditcktests.context.getfromcontextualtest.testgetmaynotcreatenewinstanceunlesscreationalcontextgiven;

import jakarta.enterprise.context.RequestScoped;

import java.io.Serializable;

@RequestScoped
public class MyRequestBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void ping() {
    }
}
