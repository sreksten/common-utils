package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testalternativeproducerwithpriority;

import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ProducedByMethod {
}
