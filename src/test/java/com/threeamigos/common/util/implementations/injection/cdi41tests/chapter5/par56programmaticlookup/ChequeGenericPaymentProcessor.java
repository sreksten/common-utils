package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par56programmaticlookup;

public class ChequeGenericPaymentProcessor implements GenericPaymentProcessor<Cheque> {

    @Override
    public String process() {
        return "generic-cheque";
    }
}
