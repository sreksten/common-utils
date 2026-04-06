package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.inheritance;

import java.util.ArrayList;
import java.util.List;

public class Apple {

    public static List<Class<?>> disposedBy = new ArrayList<Class<?>>();

    private final AppleTree tree;

    public Apple(AppleTree tree) {
        this.tree = tree;
    }

    public AppleTree getTree() {
        return tree;
    }
}
