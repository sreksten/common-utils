package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par56programmaticlookup;

public class DefaultPaymentProcessor implements PaymentProcessor {

    @Override
    public String process() {
        return "default";
    }
}
