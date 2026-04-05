package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.producer;

import jakarta.enterprise.inject.spi.BeanManager;

public class AnotherFactory {

    final Toy jessie = new Toy("Jessie");

    public static Toy getRex(BeanManager manager, SpaceSuit<Toy> suit) {
        if (manager == null || suit == null) {
            throw new AssertionError();
        }
        return new Toy("Rex");
    }
}
