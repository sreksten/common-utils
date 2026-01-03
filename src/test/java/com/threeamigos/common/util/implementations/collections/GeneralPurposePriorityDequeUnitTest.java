package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeneralPurposePriorityDeque unit test")
class GeneralPurposePriorityDequeUnitTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor policy should be remembered")
        void constructorPolicyShouldBeRemembered() {
            // Given
            GeneralPurposePriorityDeque<String> sut = new GeneralPurposePriorityDeque<>(PriorityDeque.Policy.LIFO);
            // When
            PriorityDeque.Policy policy = sut.getPolicy();
            // Then
            assertEquals(PriorityDeque.Policy.LIFO, policy);
        }
    }
}