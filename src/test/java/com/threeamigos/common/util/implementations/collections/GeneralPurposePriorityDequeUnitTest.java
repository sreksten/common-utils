package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeneralPurposePriorityDeque unit test")
class GeneralPurposePriorityDequeUnitTest {

    @Nested
    @DisplayName("Empty deque tests")
    class EmptyDequeTests {

        @Test
        @DisplayName("When empty, method isEmpty returns true")
        void whenEmptyMethodIsEmptyReturnsTrue() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            // When
            boolean isEmpty = sut.isEmpty();
            // Then
            assertTrue(isEmpty);
        }

        @Test
        @DisplayName("When not empty, method isEmpty returns false")
        void whenNotEmptyMethodIsEmptyReturnsFalse() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            // When
            boolean isEmpty = sut.isEmpty();
            // Then
            assertFalse(isEmpty);
        }

        @Test
        @DisplayName("When empty highest priority is -1")
        void whenEmptyHighestPriorityIsMinusOne() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            // When
            int highestPriority = sut.getHighestNotEmptyPriority();
            // Then
            assertEquals(-1, highestPriority);
        }
    }

    @Nested
    @DisplayName("Size tests")
    class SizeTests {

        @Test
        @DisplayName("Should count elements added")
        void shouldCountElementsAdded() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            int size = sut.size();
            // Then
            assertEquals(2, size);
        }

        @Test
        @DisplayName("Should count elements added with given priority")
        void shouldCountElementsAddedWithGivenPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            sut.add("test2", 2);
            sut.add("test3", 1);
            // When
            int sizeWithPriority1 = sut.size(1);
            // Then
            assertEquals(2, sizeWithPriority1);
        }

        @Test
        @DisplayName("Should return zero as size when no objects with given priority exist")
        void shouldReturnZeroWhenNoObjectsWithGivenPriorityExist() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
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

        @Test
        @DisplayName("Should clear deque")
        void shouldClearDeque() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            sut.clear();
            int size = sut.size();
            // Then
            assertEquals(0, size);
        }

        @Test
        @DisplayName("Should clear by priority")
        void shouldClearDequeByPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            sut.add("test2", 1);
            sut.add("test3", 2);
            // When
            sut.clear(2);
            int size = sut.size();
            // Then
            assertEquals(2, size);
        }

        @Test
        @DisplayName("Should clear by filter")
        void shouldClearDequeByFilter() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
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

    @Test
    @DisplayName("Should return max priority between added objects")
    void shouldReturnMaxPriorityBetweenAddedObjects() {
        // Given
        PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
        sut.add("test", 1);
        sut.add("test2", 2);
        // When
        int maxPriority = sut.getHighestNotEmptyPriority();
        // Then
        assertEquals(2, maxPriority);
    }

    @Nested
    @DisplayName("FIFO Tests")
    class FIFOTests {

        @Test
        @DisplayName("Should return no element if deque is empty (no priority)")
        void shouldReturnNoElementIfDequeIsEmptyNoPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            // When
            String element = sut.pollFifo();
            // Then
            assertNull(element);
        }

        @Test
        @DisplayName("Should return no element if deque is empty (priority)")
        void shouldReturnNoElementIfDequeIsEmptyPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            // When
            String element = sut.pollFifo(1);
            // Then
            assertNull(element);
        }

        @Test
        @DisplayName("Should return no element if deque has been emptied (no priority)")
        void shouldReturnNoElementIfDequeHasBeenEmptiedNoPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            String element1 = sut.pollFifo();
            // When
            String element2 = sut.pollFifo();
            // Then
            assertNull(element2);
        }

        @Test
        @DisplayName("Should return no element if deque has been emptied (priority)")
        void shouldReturnNoElementIfDequeHasBeenEmptiedPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            String element1 = sut.pollFifo(1);
            // When
            String element2 = sut.pollFifo(1);
            // Then
            assertNull(element2);
        }

        @Test
        @DisplayName("Should return no element if deque is not empty but no element with given priority exists")
        void shouldReturnNoElementIfDequeNotEmptyButNoElementWithGivenPriorityExists() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 2);
            // When
            String element = sut.pollFifo(1);
            // Then
            assertNull(element);
        }

        @Test
        @DisplayName("Should add and retrieve elements (fixed priority)")
        void shouldAddAndRetrieveElementsNoPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            sut.add("test2", 1);
            // When
            String element = sut.pollFifo();
            // Then
            assertEquals("test", element);
        }

        @Test
        @DisplayName("Given different priorities, should remove higher priority element from the deque (1)")
        void shouldRemoveHigherPriorityElementFromDeque1() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            String s = sut.pollFifo();
            // Then
            assertEquals("test2", s);
        }

        @Test
        @DisplayName("Given different priorities, should remove higher priority element from the deque (2)")
        void shouldRemoveHigherPriorityElementFromDeque2() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test2", 2);
            sut.add("test", 1);
            // When
            String s = sut.pollFifo();
            // Then
            assertEquals("test2", s);
        }

        @Test
        @DisplayName("Given different priorities, should remove given priority element from the deque (1)")
        void shouldRemoveGivenPriorityElementFromDeque1() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            String s = sut.pollFifo(1);
            // Then
            assertEquals("test", s);
        }

        @Test
        @DisplayName("Given different priorities, should remove given priority element from the deque (2)")
        void shouldRemoveGivenPriorityElementFromDeque2() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test2", 2);
            sut.add("test", 1);
            // When
            String s = sut.pollFifo(1);
            // Then
            assertEquals("test", s);
        }

        @Test
        @DisplayName("Given different priorities, should remove given priority element from the deque (3)")
        void shouldRemoveGivenPriorityElementFromDeque3() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
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
    @DisplayName("LIFO Tests")
    class LIFOTests {

        @Test
        @DisplayName("Should return no element if deque is empty (no priority)")
        void shouldReturnNoElementIfDequeIsEmptyNoPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            // When
            String element = sut.pollLifo();
            // Then
            assertNull(element);
        }

        @Test
        @DisplayName("Should return no element if deque has been emptied (no priority)")
        void shouldReturnNoElementIfDequeHasBeenEmptiedNoPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            String element1 = sut.pollLifo();
            // When
            String element2 = sut.pollLifo();
            // Then
            assertNull(element2);
        }

        @Test
        @DisplayName("Should return no element if deque has been emptied (priority)")
        void shouldReturnNoElementIfDequeHasBeenEmptiedPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            String element1 = sut.pollLifo(1);
            // When
            String element2 = sut.pollLifo(1);
            // Then
            assertNull(element2);
        }

        @Test
        @DisplayName("Should return no element if deque is not empty but no element with given priority exists")
        void shouldReturnNoElementIfDequeNotEmptyButNoElementWithGivenPriorityExists() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 2);
            // When
            String element = sut.pollLifo(1);
            // Then
            assertNull(element);
        }

        @Test
        @DisplayName("Should add and retrieve elements (fixed priority)")
        void shouldAddAndRetrieveElementsNoPriority() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            sut.add("test2", 1);
            // When
            String element = sut.pollLifo();
            // Then
            assertEquals("test2", element);
        }

        @Test
        @DisplayName("Given different priorities, should remove higher priority element from the deque (1)")
        void shouldRemoveHigherPriorityElementFromDeque1() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            String s = sut.pollLifo();
            // Then
            assertEquals("test2", s);
        }

        @Test
        @DisplayName("Given different priorities, should remove higher priority element from the deque (2)")
        void shouldRemoveHigherPriorityElementFromDeque2() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test2", 2);
            sut.add("test", 1);
            // When
            String s = sut.pollLifo();
            // Then
            assertEquals("test2", s);
        }

        @Test
        @DisplayName("Given different priorities, should remove given priority element from the deque (1)")
        void shouldRemoveGivenPriorityElementFromDeque1() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test", 1);
            sut.add("test2", 2);
            // When
            String s = sut.pollLifo(1);
            // Then
            assertEquals("test", s);
        }

        @Test
        @DisplayName("Given different priorities, should remove given priority element from the deque (2)")
        void shouldRemoveGivenPriorityElementFromDeque2() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test2", 2);
            sut.add("test", 1);
            // When
            String s = sut.pollLifo(1);
            // Then
            assertEquals("test", s);
        }

        @Test
        @DisplayName("Given different priorities, should remove given priority element from the deque (3)")
        void shouldRemoveGivenPriorityElementFromDeque3() {
            // Given
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("test2", 2);
            sut.add("test", 1);
            sut.add("test3", 1);
            // When
            String s = sut.pollLifo(1);
            // Then
            assertEquals("test3", s);
        }

    }
}