package com.threeamigos.common.util.implementations.injection.literals;

import jakarta.enterprise.inject.Any;
import java.lang.annotation.Annotation;

/**
 * Helper class to create instances of @Any.
 */
@SuppressWarnings("all")
public class AnyLiteral implements Any {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Any.class;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Any;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
