package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.inheritance;

import java.util.ArrayList;
import java.util.List;

public class Meal {

    public static List<Class<?>> disposedBy = new ArrayList<Class<?>>();

    private final Cook cook;

    public Meal(Cook cook) {
        this.cook = cook;
    }

    public Cook getCook() {
        return cook;
    }
}
