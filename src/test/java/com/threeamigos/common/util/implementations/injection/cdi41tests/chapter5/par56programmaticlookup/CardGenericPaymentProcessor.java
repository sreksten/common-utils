package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par56programmaticlookup;

public class CardGenericPaymentProcessor implements GenericPaymentProcessor<Card> {

    @Override
    public String process() {
        return "generic-card";
    }
}
