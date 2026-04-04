package com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier;

import jakarta.enterprise.util.AnnotationLiteral;

public final class FalseLiteral extends AnnotationLiteral<False> implements False {

    public static final FalseLiteral INSTANCE = new FalseLiteral();

    private FalseLiteral() {
    }
}
