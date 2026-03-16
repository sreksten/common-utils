package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par22beantypes;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WildcardBookshopProducer {

    @jakarta.enterprise.inject.Produces
    Shop<? extends Book> shop() {
        return null;
    }
}
