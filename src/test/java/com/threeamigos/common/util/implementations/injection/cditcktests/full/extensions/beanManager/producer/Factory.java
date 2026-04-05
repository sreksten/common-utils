package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.producer;

import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

@Vetoed
public class Factory {

    public static final Toy WOODY = new Toy("Woody");

    public static final SpaceSuit<?> INVALID_FIELD1 = new SpaceSuit<Object>();

    @Inject
    public static final Toy INVALID_FIELD2 = null;

    public final Object INVALID_FIELD3 = null;

    public static Toy getBuzz(BeanManager manager, SpaceSuit<Toy> suit) {
        if (manager == null || suit == null) {
            throw new AssertionError();
        }
        return new Toy("Buzz Lightyear");
    }

    public static <T> T invalidProducerMethod1(T t) {
        return null;
    }

    public Toy invalidProducerMethod2() {
        return new Toy("nonStaticNonBean");
    }
}
