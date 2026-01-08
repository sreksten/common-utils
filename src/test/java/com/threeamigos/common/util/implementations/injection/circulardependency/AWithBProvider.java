package com.threeamigos.common.util.implementations.injection.circulardependency;

import javax.inject.Inject;
import javax.inject.Provider;

public class AWithBProvider {

    @Inject
    @SuppressWarnings("all")
    Provider<BWithAProvider> provider;

    public BWithAProvider getB() {
        return provider.get();
    }
}
