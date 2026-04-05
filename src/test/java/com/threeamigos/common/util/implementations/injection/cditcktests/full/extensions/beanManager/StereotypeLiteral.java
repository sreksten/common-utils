package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager;

import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.util.AnnotationLiteral;

class StereotypeLiteral extends AnnotationLiteral<Stereotype> implements Stereotype {

    static final StereotypeLiteral INSTANCE = new StereotypeLiteral();
}
