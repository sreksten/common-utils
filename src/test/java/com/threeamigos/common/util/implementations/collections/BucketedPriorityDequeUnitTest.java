package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BucketedPriorityDeque unit test")
class BucketedPriorityDequeUnitTest {

    private static final int DEFAULT_BUCKET_SIZE = 10;

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw exception when maxPriority is negative")
        void shouldThrowExceptionWhenMaxPriorityIsNegative() {
            assertThrows(IllegalArgumentException.class, () -> new BucketedPriorityDeque<>(-1));
        }

        @Test
        @DisplayName("Should throw exception when maxPriority is greater than " + BucketedPriorityDeque.MAX_PRIORITY)
        void shouldThrowExceptionWhenMaxPriorityIsGreaterThanMaxPriority() {
            assertThrows(IllegalArgumentException.class, () -> new BucketedPriorityDeque<>(BucketedPriorityDeque.MAX_PRIORITY + 1));
        }

        @Test
        @DisplayName("Constructor policy should be remembered")
        void constructorPolicyShouldBeRemembered() {
            // Given
            BucketedPriorityDeque<String> sut = new BucketedPriorityDeque<>(DEFAULT_BUCKET_SIZE, PriorityDeque.Policy.LIFO);
            // When
            PriorityDeque.Policy policy = sut.getPolicy();
            // Then
            assertEquals(PriorityDeque.Policy.LIFO, policy);
        }
    }
}