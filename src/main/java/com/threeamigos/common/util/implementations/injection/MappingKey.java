package com.threeamigos.common.util.implementations.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Represents a key for mapping types to their implementations in the dependency injection system.
 * Used by ClassResolver.
 */
class MappingKey {
    private final Type type;
    private final Set<Annotation> qualifiers;

    MappingKey(Type type, Collection<Annotation> qualifiers) {
        this.type = type;
        this.qualifiers = qualifiers == null ? Collections.emptySet() : new HashSet<>(qualifiers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MappingKey)) return false;
        MappingKey that = (MappingKey) o;
        return Objects.equals(type, that.type) && Objects.equals(qualifiers, that.qualifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, qualifiers);
    }
}
