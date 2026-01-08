package com.threeamigos.common.util.implementations.injection.circulardependencies;

import javax.inject.Inject;
import javax.inject.Provider;

public class BWithAProvider {

    @Inject
    @SuppressWarnings("all")
    private Provider<AWithBProvider> provider;

    public AWithBProvider getA() {
        return provider.get();
    }

}
