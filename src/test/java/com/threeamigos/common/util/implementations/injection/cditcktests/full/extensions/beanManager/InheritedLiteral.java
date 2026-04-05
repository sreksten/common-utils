package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager;

import java.lang.annotation.Inherited;

import jakarta.enterprise.util.AnnotationLiteral;

class InheritedLiteral extends AnnotationLiteral<Inherited> implements Inherited {

    static final InheritedLiteral INSTANCE = new InheritedLiteral();
}
