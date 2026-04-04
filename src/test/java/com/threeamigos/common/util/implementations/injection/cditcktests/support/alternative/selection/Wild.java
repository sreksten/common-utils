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
public @interface Wild {

    final class Literal extends AnnotationLiteral<Wild> implements Wild {
        public static final Wild INSTANCE = new Literal();
    }
}
