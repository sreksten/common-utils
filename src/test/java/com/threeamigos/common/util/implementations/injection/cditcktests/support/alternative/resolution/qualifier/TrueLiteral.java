package com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier;

import jakarta.enterprise.util.AnnotationLiteral;

public final class TrueLiteral extends AnnotationLiteral<True> implements True {

    public static final TrueLiteral INSTANCE = new TrueLiteral();

    private TrueLiteral() {
    }
}
