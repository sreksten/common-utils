package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.annotatedTypeConfigurator;

import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.util.AnnotationLiteral;

public final class DisposesLiteral extends AnnotationLiteral<Disposes> implements Disposes {

    public static final DisposesLiteral INSTANCE = new DisposesLiteral();

    private DisposesLiteral() {
    }
}
