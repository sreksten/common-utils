package com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.selection;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface Tame {

    final class Literal extends AnnotationLiteral<Tame> implements Tame {
        public static final Tame INSTANCE = new Literal();
    }
}
