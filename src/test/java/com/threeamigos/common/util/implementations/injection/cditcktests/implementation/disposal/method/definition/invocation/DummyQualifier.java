package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.invocation;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface DummyQualifier {

    String value();

    @SuppressWarnings("all")
    class Literal extends AnnotationLiteral<DummyQualifier> implements DummyQualifier {

        private final String value;

        public Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
