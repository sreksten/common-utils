package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test;

import jakarta.enterprise.util.AnnotationLiteral;

public final class MyQualifierLiteral extends AnnotationLiteral<MyQualifier> implements MyQualifier {

    public static final MyQualifierLiteral INSTANCE = new MyQualifierLiteral();

    private MyQualifierLiteral() {
    }
}
