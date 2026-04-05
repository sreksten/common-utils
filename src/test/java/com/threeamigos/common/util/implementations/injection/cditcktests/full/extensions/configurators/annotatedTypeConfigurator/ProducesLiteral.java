package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.annotatedTypeConfigurator;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.AnnotationLiteral;

public final class ProducesLiteral extends AnnotationLiteral<Produces> implements Produces {

    public static final ProducesLiteral INSTANCE = new ProducesLiteral();

    private ProducesLiteral() {
    }
}
