package com.threeamigos.common.util.implementations.injection;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * A class used as a key for the TypeChecker internal cache.
 * As I am still supporting Java 1.8, I can't use a record.
 */
class TypePair {
    private final Type target;
    private final Type implementation;

    TypePair(Type target, Type implementation) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.implementation = Objects.requireNonNull(implementation, "implementation cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypePair)) return false;
        TypePair typePair = (TypePair) o;
        return Objects.equals(target, typePair.target) &&
                Objects.equals(implementation, typePair.implementation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, implementation);
    }
}
