package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.util.AnnotationLiteral;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MappingKey unit tests")
class MappingKeyUnitTest {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Qualifier1 {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface Qualifier2 {}

    @Test
    @DisplayName("Constructor should handle null qualifiers")
    void constructorShouldHandleNullQualifiers() {
        MappingKey key = new MappingKey(String.class, null);
        assertNotNull(key);
    }

    @Test
    @DisplayName("equals() should return true for the same instance")
    void equalsShouldReturnTrueForSameInstance() {
        MappingKey key = new MappingKey(String.class, null);
        assertEquals(key, key);
    }

    @Test
    @DisplayName("equals() should return false for a different object")
    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    void equalsShouldReturnFalseForDifferentObject() {
        MappingKey key = new MappingKey(String.class, null);
        boolean areEqual = key.equals("string");
        assertFalse(areEqual);
    }

    @Test
    @DisplayName("equals() should return false for null or different class")
    void equalsShouldReturnFalseForNullOrDifferentClass() {
        MappingKey key = new MappingKey(String.class, null);
        assertNotEquals(null, key);
        assertNotEquals("string", key);
    }

    @Test
    @DisplayName("equals() should return true for keys with same type and qualifiers")
    void equalsShouldReturnTrueForSameContent() {
        Annotation q1 = AnnotationLiteral.of(Qualifier1.class);
        MappingKey key1 = new MappingKey(String.class, Collections.singletonList(q1));
        MappingKey key2 = new MappingKey(String.class, Collections.singletonList(q1));

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    @DisplayName("equals() should return false for keys with different types")
    void equalsShouldReturnFalseForDifferentTypes() {
        MappingKey key1 = new MappingKey(String.class, null);
        MappingKey key2 = new MappingKey(Integer.class, null);

        assertNotEquals(key1, key2);
    }

    @Test
    @DisplayName("equals() should return false for keys with different qualifiers")
    void equalsShouldReturnFalseForDifferentQualifiers() {
        Annotation q1 = AnnotationLiteral.of(Qualifier1.class);
        Annotation q2 = AnnotationLiteral.of(Qualifier2.class);

        MappingKey key1 = new MappingKey(String.class, Collections.singletonList(q1));
        MappingKey key2 = new MappingKey(String.class, Collections.singletonList(q2));
        MappingKey key3 = new MappingKey(String.class, Arrays.asList(q1, q2));
        MappingKey key4 = new MappingKey(String.class, null);

        assertNotEquals(key1, key2);
        assertNotEquals(key1, key3);
        assertNotEquals(key1, key4);
    }

    @Test
    @DisplayName("hashCode() should be consistent")
    void hashCodeShouldBeConsistent() {
        MappingKey key = new MappingKey(String.class, null);
        int initialHashCode = key.hashCode();
        assertEquals(initialHashCode, key.hashCode());
    }
}