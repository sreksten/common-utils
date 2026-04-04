package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test;

import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface MyQualifier {
}
