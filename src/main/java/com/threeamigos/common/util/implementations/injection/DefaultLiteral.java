package com.threeamigos.common.util.implementations.injection;

import javax.enterprise.inject.Default;
import java.lang.annotation.Annotation;

/**
 * Helper class to create instances of @Default.
 */
@SuppressWarnings("all")
class DefaultLiteral implements Default {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Default.class;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Default;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}