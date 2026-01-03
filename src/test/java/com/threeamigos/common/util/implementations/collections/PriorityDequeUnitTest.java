package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("PriorityDeque unit test")
public class PriorityDequeUnitTest {

    private static Stream<Arguments> createSut() {
        final int DEFAULT_BUCKET_SIZE = 10;
        return Stream.of(
                Arguments.of("GeneralPurposePriorityDeque", new GeneralPurposePriorityDeque<>()),
                Arguments.of("BucketedPriorityDeque", new BucketedPriorityDeque<>(DEFAULT_BUCKET_SIZE))
        );
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Standard policy should be FIFO")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void standardPolicyShouldBeFifo(String sutName, PriorityDeque<String> sut) {
            // When
            PriorityDeque.Policy policy = sut.getPolicy();
            // Then
            assertEquals(PriorityDeque.Policy.FIFO, policy);
        }
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Policy can be changed")
    @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
    void policyCanBeChanged(String sutName, PriorityDeque<String> sut) {
        // When
        sut.setPolicy(PriorityDeque.Policy.LIFO);
        PriorityDeque.Policy policy = sut.getPolicy();
        // Then
        assertEquals(PriorityDeque.Policy.LIFO, policy);
    }

    @Nested
    @DisplayName("Empty deque tests")
    class EmptyDequeTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("When empty, method isEmpty returns true")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void whenEmptyMethodIsEmptyReturnsTrue(String sutName, PriorityDeque<String> sut) {
            // When
            boolean isEmpty = sut.isEmpty();
            // Then
            assertTrue(isEmpty);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("When not empty, method isEmpty returns false")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void whenNotEmptyMethodIsEmptyReturnsFalse(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            // When
            boolean isEmpty = sut.isEmpty();
            // Then
            assertFalse(isEmpty);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("When empty highest priority is -1")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void whenEmptyHighestPriorityIsMinusOne(String sutName, PriorityDeque<String> sut) {
            // When
            int highestPriority = sut.getHighestNotEmptyPriority();
            // Then
            assertEquals(-1, highestPriority);
        }
    }

    @Nested
    @DisplayName("Size tests")
    class SizeTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should count elements added")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldCountElementsAdded(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            int size = sut.size();
            // Then
            assertEquals(2, size);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should count elements added with given priority")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldCountElementsAddedWithGivenPriority(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 2);
            sut.add("test3", 1);
            // When
            int sizeWithPriority1 = sut.size(1);
            // Then
            assertEquals(2, sizeWithPriority1);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return zero as size when no objects with given priority exist")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnZeroWhenNoObjectsWithGivenPriorityExist(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 2);
            sut.add("test3", 1);
            // When
            int sizeWithPriority3 = sut.size(3);
            // Then
            assertEquals(0, sizeWithPriority3);
        }
    }

    @Nested
    @DisplayName("Clearing operations")
    class ClearingOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should clear deque")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldClearDeque(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            sut.clear();
            int size = sut.size();
            // Then
            assertEquals(0, size);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should clear by priority")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldClearDequeByPriority(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 1);
            sut.add("test3", 2);
            // When
            sut.clear(2);
            int size = sut.size();
            // Then
            assertEquals(2, size);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should clear by filter")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldClearDequeByFilter(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test-A", 1);
            sut.add("test-A", 2);
            sut.add("test-B", 2);
            // When
            sut.clear(t -> t.endsWith("-A"));
            int size = sut.size();
            // Then
            assertEquals(1, size);
        }
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Should return max priority between added objects")
    @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
    void shouldReturnMaxPriorityBetweenAddedObjects(String sutName, PriorityDeque<String> sut) {
        // Given
        sut.add("test", 1);
        sut.add("test2", 2);
        // When
        int maxPriority = sut.getHighestNotEmptyPriority();
        // Then
        assertEquals(2, maxPriority);
    }

    @Nested
    @DisplayName("Peek tests")
    class PeekTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return null if empty using FIFO")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnNullIfEmptyUsingFIFO(String sutName, PriorityDeque<String> sut) {
            // When
            String element = sut.peek();
            // Then
            assertNull(element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return null if empty using LIFO")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnNullIfEmptyUsingLIFO(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.setPolicy(PriorityDeque.Policy.LIFO);
            // When
            String element = sut.peek();
            // Then
            assertNull(element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return object by FIFO policy without removing it")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnObjectByFIFOPolicyWithoutRemoving(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 1);
            // When
            String element = sut.peek();
            // Then
            assertEquals("test", element);
            assertEquals(2, sut.size());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return object by LIFO policy without removing it")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnObjectByLIFOPolicyWithoutRemoving(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.setPolicy(PriorityDeque.Policy.LIFO);
            sut.add("test", 1);
            sut.add("test2", 1);
            // When
            String element = sut.peek();
            // Then
            assertEquals("test2", element);
            assertEquals(2, sut.size());
        }
    }

    @Nested
    @DisplayName("Policy tests")
    class PolicyTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return object by FIFO policy")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnObjectByFIFOPolicy(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 1);
            // When
            String element = sut.poll();
            // Then
            assertEquals("test", element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return object by LIFO policy")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnObjectByLIFOPolicy(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.setPolicy(PriorityDeque.Policy.LIFO);
            sut.add("test", 1);
            sut.add("test2", 1);
            // When
            String element = sut.poll();
            // Then
            assertEquals("test2", element);
        }
    }

    @Nested
    @DisplayName("FIFO tests")
    class FIFOTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("peek() should return no element if deque is empty")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void peekShouldReturnNoElementIfDequeIsEmptyNoPriority(String sutName, PriorityDeque<String> sut) {
            // When
            String element = sut.peekFifo();
            // Then
            assertNull(element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("poll() should return no element if deque is empty (no priority)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void pollShouldReturnNoElementIfDequeIsEmptyNoPriority(String sutName, PriorityDeque<String> sut) {
            // When
            String element = sut.pollFifo();
            // Then
            assertNull(element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return no element if deque is empty (priority)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnNoElementIfDequeIsEmptyPriority(String sutName, PriorityDeque<String> sut) {
            // When
            String element = sut.pollFifo(1);
            // Then
            assertNull(element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return no element if deque has been emptied (no priority)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnNoElementIfDequeHasBeenEmptiedNoPriority(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.pollFifo();
            // When
            String element2 = sut.pollFifo();
            // Then
            assertNull(element2);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return no element if deque has been emptied (priority)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnNoElementIfDequeHasBeenEmptiedPriority(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.pollFifo(1);
            // When
            String element2 = sut.pollFifo(1);
            // Then
            assertNull(element2);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return no element if deque is not empty but no element with given priority exists")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnNoElementIfDequeNotEmptyButNoElementWithGivenPriorityExists(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 2);
            // When
            String element = sut.pollFifo(1);
            // Then
            assertNull(element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should add and retrieve elements (fixed priority)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldAddAndRetrieveElementsNoPriority(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 1);
            // When
            String element = sut.pollFifo();
            // Then
            assertEquals("test", element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Given different priorities, should remove higher priority element from the deque (1)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveHigherPriorityElementFromDeque1(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            String s = sut.pollFifo();
            // Then
            assertEquals("test2", s);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Given different priorities, should remove higher priority element from the deque (2)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveHigherPriorityElementFromDeque2(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test2", 2);
            sut.add("test", 1);
            // When
            String s = sut.pollFifo();
            // Then
            assertEquals("test2", s);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Given different priorities, should remove given priority element from the deque (1)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveGivenPriorityElementFromDeque1(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            String s = sut.pollFifo(1);
            // Then
            assertEquals("test", s);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Given different priorities, should remove given priority element from the deque (2)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveGivenPriorityElementFromDeque2(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test2", 2);
            sut.add("test", 1);
            // When
            String s = sut.pollFifo(1);
            // Then
            assertEquals("test", s);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Given different priorities, should remove given priority element from the deque (3)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveGivenPriorityElementFromDeque3(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test2", 2);
            sut.add("test", 1);
            sut.add("test3", 1);
            // When
            String s = sut.pollFifo(1);
            // Then
            assertEquals("test", s);
        }

    }

    @Nested
    @DisplayName("LIFO tests")
    class LIFOTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("peek() should return no element if deque is empty")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void peekShouldReturnNoElementIfDequeIsEmptyNoPriority(String sutName, PriorityDeque<String> sut) {
            // When
            String element = sut.peekLifo();
            // Then
            assertNull(element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("poll() should return no element if deque is empty (no priority)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void pollShouldReturnNoElementIfDequeIsEmptyNoPriority(String sutName, PriorityDeque<String> sut) {
            // When
            String element = sut.pollLifo();
            // Then
            assertNull(element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return no element if deque has been emptied (no priority)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnNoElementIfDequeHasBeenEmptiedNoPriority(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            String element1 = sut.pollLifo();
            // When
            String element2 = sut.pollLifo();
            // Then
            assertNull(element2);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return no element if deque has been emptied (priority)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnNoElementIfDequeHasBeenEmptiedPriority(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.pollLifo(1);
            // When
            String element2 = sut.pollLifo(1);
            // Then
            assertNull(element2);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return no element if deque is not empty but no element with given priority exists")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnNoElementIfDequeNotEmptyButNoElementWithGivenPriorityExists(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 2);
            // When
            String element = sut.pollLifo(1);
            // Then
            assertNull(element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should add and retrieve elements (fixed priority)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldAddAndRetrieveElementsNoPriority(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 1);
            // When
            String element = sut.pollLifo();
            // Then
            assertEquals("test2", element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Given different priorities, should remove higher priority element from the deque (1)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveHigherPriorityElementFromDeque1(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            String s = sut.pollLifo();
            // Then
            assertEquals("test2", s);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Given different priorities, should remove higher priority element from the deque (2)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveHigherPriorityElementFromDeque2(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test2", 2);
            sut.add("test", 1);
            // When
            String s = sut.pollLifo();
            // Then
            assertEquals("test2", s);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Given different priorities, should remove given priority element from the deque (1)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveGivenPriorityElementFromDeque1(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            String s = sut.pollLifo(1);
            // Then
            assertEquals("test", s);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Given different priorities, should remove given priority element from the deque (2)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveGivenPriorityElementFromDeque2(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test2", 2);
            sut.add("test", 1);
            // When
            String s = sut.pollLifo(1);
            // Then
            assertEquals("test", s);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Given different priorities, should remove given priority element from the deque (3)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveGivenPriorityElementFromDeque3(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test2", 2);
            sut.add("test", 1);
            sut.add("test3", 1);
            // When
            String s = sut.pollLifo(1);
            // Then
            assertEquals("test3", s);
        }
    }

    @Nested
    @DisplayName("Contains tests")
    class ContainsTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("When object is contained")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void whenObjectIsContained(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            // When
            boolean contains = sut.contains("test");
            // Then
            assertTrue(contains);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("When object is not contained")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void whenObjectIsNotContained(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            // When
            boolean contains = sut.contains("test2");
            // Then
            assertFalse(contains);
        }
    }

    @Nested
    @DisplayName("ContainsAll tests")
    class ContainsAllTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("When collection is contained")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void whenObjectIsContained(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 2);
            List<String> iterable = new ArrayList<>();
            iterable.add("test");
            iterable.add("test2");
            // When
            boolean contains = sut.containsAll(iterable);
            // Then
            assertTrue(contains);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("When collection is not contained")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void whenObjectIsNotContained(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 2);
            List<String> iterable = new ArrayList<>();
            iterable.add("test");
            iterable.add("test3");
            // When
            boolean contains = sut.containsAll(iterable);
            // Then
            assertFalse(contains);
        }
    }

    @Nested
    @DisplayName("Remove tests")
    class RemoveTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should remove object if contained")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveObjectIfContained(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            // When
            sut.remove("test");
            // Then
            assertEquals(0, sut.size());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should not remove object if not contained")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldNotRemoveObjectIfNotContained(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            // When
            sut.remove("test2");
            // Then
            assertEquals(1, sut.size());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw exception when remove() is called on an empty Deque")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldThrowExceptionWhenRemoveOnEmptyDeque(String sutName, PriorityDeque<String> sut) {
            // Then
            assertThrows(NoSuchElementException.class, sut::remove);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should remove an element")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveAnElement(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            // When
            boolean removed = sut.remove();
            // Then
            assertTrue(removed);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should remove all elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveAllElements(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 1);
            sut.add("test3", 2);
            List<String> iterable = new ArrayList<>();
            iterable.add("test");
            iterable.add("test2");
            iterable.add("test3");
            // When
            boolean result = sut.removeAll(iterable);
            // Then
            assertEquals(0, sut.size());
            assertTrue(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should not remove all elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldNotRemoveAllElements(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 1);
            sut.add("test3", 2);
            List<String> iterable = new ArrayList<>();
            iterable.add("test");
            iterable.add("test2");
            iterable.add("test4");
            // When
            boolean result = sut.removeAll(iterable);
            // Then
            assertEquals(1, sut.size());
            assertFalse(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should retain elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRetainElements(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 1);
            sut.add("test3", 2);
            List<String> iterable = new ArrayList<>();
            iterable.add("test");
            iterable.add("test2");
            // When
            sut.retainAll(iterable);
            // Then
            assertEquals(2, sut.size());
        }
    }

    @Nested
    @DisplayName("List Operations")
    class ListOperationsTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return a list containing all elements in priority order and then in insertion order")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnListInPriorityAndInsertionOrder(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test3", 2);
            sut.add("test2", 1);
            sut.add("test4", 2);
            // When
            List<String> list = sut.toList();
            // Then
            assertEquals(4, list.size());
            // First, always the higher priority elements
            assertEquals("test3", list.get(0));
            assertEquals("test4", list.get(1));
            // Then, the elements in inserting order
            assertEquals("test", list.get(2));
            assertEquals("test2", list.get(3));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return a list containing all elements in priority order, and then in reversed insertion order")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnListInPriorityAndReverseInsertionOrder(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.setPolicy(PriorityDeque.Policy.LIFO);
            sut.add("test", 1);
            sut.add("test3", 2);
            sut.add("test2", 1);
            sut.add("test4", 2);
            // When
            List<String> list = sut.toList();
            // Then
            assertEquals(4, list.size());
            // First, always the higher priority elements
            assertEquals("test4", list.get(0));
            assertEquals("test3", list.get(1));
            // Then, the elements in reverse inserting order
            assertEquals("test2", list.get(2));
            assertEquals("test", list.get(3));
        }
    }

    @Nested
    @DisplayName("Iterator Operations")
    class IteratorOperationsTests {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return an iterator that iterates over elements in priority and then in insertion order")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnIteratorInPriorityAndInsertionOrder(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test3", 2);
            sut.add("test2", 1);
            sut.add("test4", 2);
            List<String> expected = Arrays.asList("test3", "test4", "test", "test2");
            List<String> actual = new ArrayList<>();
            // When
            Iterator<String> iterator = sut.iterator();
            while (iterator.hasNext()) {
                actual.add(iterator.next());
            }
            // Then
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return an iterator that iterates over elements in priority and then in reverse insertion order")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldReturnIteratorInPriorityAndReverseInsertionOrder(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.setPolicy(PriorityDeque.Policy.LIFO);
            sut.add("test", 1);
            sut.add("test3", 2);
            sut.add("test2", 1);
            sut.add("test4", 2);
            List<String> expected = Arrays.asList("test4", "test3", "test2", "test");
            List<String> actual = new ArrayList<>();
            // When
            Iterator<String> iterator = sut.iterator();
            while (iterator.hasNext()) {
                actual.add(iterator.next());
            }
            // Then
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw an exception if calling next() on an empty iterator")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldThrowExceptionIfCallingNextOnEmptyIterator(String sutName, PriorityDeque<String> sut) {
            // Then
            assertThrows(NoSuchElementException.class, sut.iterator()::next);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw an exception if removing an element from an empty iterator")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldThrowExceptionIfRemovingFromEmptyIterator(String sutName, PriorityDeque<String> sut) {
            // Then
            assertThrows(IllegalStateException.class, sut.iterator()::remove);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should remove an element")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeUnitTest#createSut")
        void shouldRemoveElement(String sutName, PriorityDeque<String> sut) {
            // Given
            sut.add("test", 1);
            sut.add("test2", 1);
            // When
            Iterator<String> iterator = sut.iterator();
            iterator.next();
            iterator.remove();
            iterator.next();
            iterator.remove();
            // Then
            assertEquals(0, sut.size());
        }
    }
}
