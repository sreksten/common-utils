package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.broken.normalScope;

import jakarta.inject.Inject;

class Caesar {

    @SuppressWarnings("unused")
    @Inject
    private RomanEmpire empire;
}
