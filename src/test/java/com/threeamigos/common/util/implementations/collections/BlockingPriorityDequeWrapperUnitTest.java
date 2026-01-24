package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlockingPriorityDequeWrapper unit tests")
class BlockingPriorityDequeWrapperUnitTest {

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

        @Test
        @DisplayName("Should throw IllegalArgumentException if priorityDeque is null (PriorityDeque variant)")
        void shouldThrowIllegalArgumentExceptionIfPriorityDequeIsNullPriorityDequeVariant() {
            assertThrows(IllegalArgumentException.class, () -> new BlockingPriorityDequeWrapper<>(null));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if priorityDeque is null (PriorityDeque and default priority variant)")
        void shouldThrowIllegalArgumentExceptionIfPriorityDequeIsNullPriorityDequeDefaultPriorityVariant() {
            assertThrows(IllegalArgumentException.class, () -> new BlockingPriorityDequeWrapper<>(null, 0));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Default priority should be " + BlockingPriorityDequeWrapper.DEFAULT_PRIORITY)
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        void standardPriorityShouldBeDEFAULT_PRIORITY(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // When
            int priority = sut.getDefaultPriority();
            // Then
            assertEquals(BlockingPriorityDequeWrapper.DEFAULT_PRIORITY, priority);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should remember constructor priority")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        void shouldRememberConstructorPriority(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque, 5);
            // When
            int priority = sut.getDefaultPriority();
            // Then
            assertEquals(5, priority);
        }
    }

    @Nested
    @DisplayName("Add operations")
    class AddOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should add an element with standard priority")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldAddAnElementWithStandardPriority(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // When
            sut.add("test");
            // Then
            assertEquals("test", sut.poll());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should add an element with specified priority")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldAddAnElementWithSpecifiedPriority(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // When
            sut.add("test", 1);
            // Then
            assertEquals("test", sut.poll());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw when adding null with specified priority")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldThrowWhenAddingNullWithSpecifiedPriority(String sutName, PriorityDeque<String> priorityDeque) {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            assertThrows(IllegalArgumentException.class, () -> sut.add(null, 1));
        }
    }

    @Nested
    @DisplayName("Offer operations")
    class OfferOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should offer an element with standard priority")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldOfferAnElementWithStandardPriority(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // When
            sut.offer("test");
            // Then
            assertEquals("test", sut.poll());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should offer an element with standard priority, with timeout")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldOfferAnElementWithStandardPriorityWithTimeout(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // When
            sut.offer("test", 5, TimeUnit.SECONDS);
            // Then
            assertEquals("test", sut.poll());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw when offering with null TimeUnit")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldThrowWhenOfferingWithNullTimeUnit(String sutName, PriorityDeque<String> priorityDeque) {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            assertThrows(IllegalArgumentException.class, () -> sut.offer("test", 1, null));
        }
    }

    @Nested
    @DisplayName("Put operations")
    class PutOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should put an element with standard priority")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldPutAnElementWithStandardPriority(String sutName, PriorityDeque<String> priorityDeque) throws InterruptedException {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // When
            sut.put("test");
            // Then
            assertEquals("test", sut.poll());
        }
    }

    @Nested
    @DisplayName("Take operations")
    class TakeOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should take an element when already available")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldTakeAnElementWhenAlreadyAvailable(String sutName, PriorityDeque<String> priorityDeque) throws InterruptedException {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            // When
            String result = sut.take();
            // Then
            assertEquals("test", result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should take an element when it becomes available")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldTakeAnElementWhenAvailable(String sutName, PriorityDeque<String> priorityDeque) throws InterruptedException {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            Runnable runnable = () -> {
                try {
                    Thread.sleep(1000);
                    sut.offer("test");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
            new Thread(runnable).start();
            String result = sut.take();
            assertEquals("test", result);
        }
    }

    @Nested
    @DisplayName("Poll operations")
    class PollOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return null if poll()ing but no element available")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldReturnNullWhenPollingButNoElementAvailable(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // When
            String result = sut.poll();
            // Then
            assertNull(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should poll an element when already available")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldPollAnElementWhenAlreadyAvailable(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            // When
            String result = sut.poll();
            // Then
            assertEquals("test", result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should poll an element with timeout when already available")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldPollAnElementWithTimeoutWhenAlreadyAvailable(String sutName, PriorityDeque<String> priorityDeque) throws InterruptedException {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            // When
            String result = sut.poll(5, TimeUnit.SECONDS);
            // Then
            assertEquals("test", result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should poll an element when it becomes available")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldPollAndElementWhenItBecomesAvailable(String sutName, PriorityDeque<String> priorityDeque) throws InterruptedException {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            Runnable runnable = () -> {
                try {
                    Thread.sleep(1000);
                    sut.offer("test");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
            new Thread(runnable).start();
            String result = sut.poll(5, TimeUnit.SECONDS);
            assertEquals("test", result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Shuold return null when polling with timeout and no elements are available")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldReturnNullWhenPollingWithTimeoutAndNoElementsAvailable(String sutName, PriorityDeque<String> priorityDeque) throws InterruptedException {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // When
            String result = sut.poll(10, TimeUnit.MILLISECONDS);
            // Then
            assertNull(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw when polling with null TimeUnit")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldThrowWhenPollingWithNullTimeUnit(String sutName, PriorityDeque<String> priorityDeque) {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            assertThrows(IllegalArgumentException.class, () -> sut.poll(1, null));
        }
    }

    @Nested
    @DisplayName("Peek Operations")
    class PeekOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should peek an element")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldPeekAnElement(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            // When
            String result = sut.peek();
            // Then
            assertEquals("test", result);
            assertEquals(1, sut.size());
        }
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Remaining capacity should be (nearly) unlimited")
    @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
    void remainingCapacityShouldBeUnlimited(String sutName, PriorityDeque<String> priorityDeque) {
        // Given
        BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
        // When
        for (int i = 0; i < 100000; i++) {
            sut.offer("test");
        }
        // Then
        assertEquals(Integer.MAX_VALUE, sut.remainingCapacity());
    }

    @Nested
    @DisplayName("Remove operations")
    class RemoveOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("remove() should throw exception if no element is present")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        void removeShouldThrowExceptionIfNoElementIsPresent(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // Then
            assertThrows(NoSuchElementException.class, sut::remove);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should remove the first available element")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldRemoveTheFirstAvailableElement(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            sut.offer("test2");
            // When
            String result = sut.remove();
            // Then
            assertEquals("test", result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should remove an element")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldRemoveAnElement(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            // When
            boolean result = sut.remove("test");
            // Then
            assertTrue(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should not remove a null element")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldNotRemoveANullElement(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            // When
            boolean result = sut.remove(null);
            // Then
            assertFalse(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should not remove an element if not present")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldNotRemoveAnElementIfNotPresent(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            // When
            boolean result = sut.remove("test2");
            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Contains operations")
    class ContainsOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should contain an element")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldContainAnElement(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            // When
            boolean result = sut.contains("test");
            // Then
            assertTrue(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should not contain a null element")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldNotContainANullElement(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            // When
            boolean result = sut.contains(null);
            // Then
            assertFalse(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should not contain an element if not present")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldNotContainAnElementIfNotPresent(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.offer("test");
            // When
            boolean result = sut.contains("test2");
            // Then
            assertFalse(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return false for non-T inputs without throwing")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldReturnFalseForNonTInputs(String sutName, PriorityDeque<String> priorityDeque) {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            Object nonT = Integer.valueOf(42);

            assertDoesNotThrow(() -> assertFalse(sut.contains(nonT)));
            assertDoesNotThrow(() -> assertFalse(sut.remove(nonT)));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("contains should return false for non-T inputs without throwing")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void containsReturnsFalseForNonTInputs(String sutName, PriorityDeque<String> priorityDeque) {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            Object nonT = Integer.valueOf(7);

            assertDoesNotThrow(() -> assertFalse(sut.contains(nonT)));
        }
    }

    @Nested
    @DisplayName("Size operations")
    class SizeOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Size should be zero if empty")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        void sizeShouldBeZeroIfEmpty(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // When
            int result = sut.size();
            // Then
            assertEquals(0, result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Size should return the correct result")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void sizeShouldReturnTheCorrectResult(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test");
            sut.add("test2", 2);
            sut.add("test3");
            sut.add("test4", 3);
            // When
            int result = sut.size();
            // Then
            assertEquals(4, result);
        }
    }

    @Nested
    @DisplayName("Empty operations")
    class EmptyOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("isEmpty() should be true if empty")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        void isEmptyWouldBeTrueIfEmpty(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // When
            boolean result = sut.isEmpty();
            // Then
            assertTrue(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("isEmpty() should be false if not empty")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        void isEmptyWouldBeFalseIfNotEmpty(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test");
            // When
            boolean result = sut.isEmpty();
            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Clear operations")
    class ClearOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should clear the Deque")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldClearTheDeque(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test");
            sut.add("test2", 2);
            // When
            sut.clear();
            // Then
            assertTrue(sut.isEmpty());
        }
    }

    @Nested
    @DisplayName("Element operations")
    class ElementOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw exception if no element is present")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldThrowExceptionIfNoElementIsPresent(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            // Then
            assertThrows(NoSuchElementException.class, sut::element);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return the first element without removing it")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldReturnTheFirstElementWithoutRemovingIt(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test");
            // When
            String result = sut.element();
            // Then
            assertEquals("test", result);
            assertEquals(1, sut.size());
        }
    }

    @Nested
    @DisplayName("Drain operations")
    class DrainOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should drain all available elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldDrainAllAvailableElements(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            priorityDeque.setPolicy(PriorityDeque.Policy.LIFO);
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test");
            sut.add("test2");
            sut.add("test3");
            List<String> drainage = new ArrayList<>();
            List<String> expected = new ArrayList<>();
            expected.add("test3");
            expected.add("test2");
            expected.add("test");
            // When
            sut.drainTo(drainage);
            // Then
            assertEquals(3, drainage.size());
            assertEquals(expected, drainage);
            assertEquals(0, sut.size());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should drain some elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldDrainSomeElements(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            priorityDeque.setPolicy(PriorityDeque.Policy.LIFO);
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test");
            sut.add("test2");
            sut.add("test3");
            List<String> drainage = new ArrayList<>();
            List<String> expected = new ArrayList<>();
            expected.add("test3");
            expected.add("test2");
            // When
            sut.drainTo(drainage, 2);
            // Then
            assertEquals(2, drainage.size());
            assertEquals(expected, drainage);
            assertEquals(1, sut.size());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw when drainTo collection is null")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldThrowWhenDrainToCollectionIsNull(String sutName, PriorityDeque<String> priorityDeque) {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            assertThrows(IllegalArgumentException.class, () -> sut.drainTo(null));
            assertThrows(IllegalArgumentException.class, () -> sut.drainTo(null, 1));
        }
    }

    @Nested
    @DisplayName("Iterator operations")
    class IteratorOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should return a working iterator")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldReturnWorkingIterator(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            priorityDeque.setPolicy(PriorityDeque.Policy.LIFO);
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test");
            sut.add("test2");
            sut.add("test3");
            List<String> expected = new ArrayList<>();
            expected.add("test3");
            expected.add("test2");
            expected.add("test");
            List<String> elements = new ArrayList<>();
            // When
            Iterator<String> iterator = sut.iterator();
            while (iterator.hasNext()) {
                elements.add(iterator.next());
            }
            // Then
            assertEquals(expected, elements);
            assertEquals(3, sut.size());
            assertFalse(iterator.hasNext());
        }
    }

    @Nested
    @DisplayName("Array operations")
    class ArrayOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should build an array in correct order (no parameter variant)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldBuildAnArrayInCorrectOrderNoParameter(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test", 0);
            sut.add("test2", 0);
            sut.add("test3", 1);
            String[] expected = new String[]{"test3", "test", "test2"};
            // When
            Object[] actual = sut.toArray();
            // Then
            assertArrayEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should build an array in correct order (parameter variant)")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldBuildAnArrayInCorrectOrderWithParameter(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test", 0);
            sut.add("test2", 0);
            sut.add("test3", 1);
            String[] expected = new String[]{"test3", "test", "test2"};
            // When
            String[] actual = sut.toArray(new String[3]);
            // Then
            assertArrayEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("ContainsAll operations")
    class ToStringOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should contain all elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldContainAllElements(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test", 0);
            sut.add("test2", 0);
            sut.add("test3", 1);
            List<String> contained = new ArrayList<>();
            contained.add("test");
            contained.add("test3");
            // When
            boolean result = sut.containsAll(contained);
            // Then
            assertTrue(result);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw when containsAll collection is null")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldThrowWhenContainsAllCollectionIsNull(String sutName, PriorityDeque<String> priorityDeque) {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            assertThrows(IllegalArgumentException.class, () -> sut.containsAll(null));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should not contain all elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldNotContainAllElements(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test", 0);
            sut.add("test2", 0);
            sut.add("test3", 1);
            List<String> contained = new ArrayList<>();
            contained.add("test");
            contained.add("test4");
            // When
            boolean result = sut.containsAll(contained);
            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("AddAll operations")
    class AddAllOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should add all elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldAddAllElements(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            List<String> elements = new ArrayList<>();
            elements.add("test");
            elements.add("test2");
            // When
            boolean result = sut.addAll(elements);
            // Then
            assertTrue(result);
            assertEquals(2, sut.size());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw when addAll collection is null")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldThrowWhenAddAllCollectionIsNull(String sutName, PriorityDeque<String> priorityDeque) {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            assertThrows(IllegalArgumentException.class, () -> sut.addAll(null));
        }
    }

    @Nested
    @DisplayName("RemoveAll operations")
    class RemoveAllOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should remove all elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldRemoveAllElements(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test");
            sut.add("test2");
            sut.add("test3");
            List<String> elementsToRemove = new ArrayList<>();
            elementsToRemove.add("test");
            elementsToRemove.add("test2");
            // When
            boolean result = sut.removeAll(elementsToRemove);
            // Then
            assertTrue(result);
            assertEquals(1, sut.size());
            assertEquals("test3", sut.peek());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw when removeAll collection is null")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldThrowWhenRemoveAllCollectionIsNull(String sutName, PriorityDeque<String> priorityDeque) {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            assertThrows(IllegalArgumentException.class, () -> sut.removeAll(null));
        }
    }

    @Nested
    @DisplayName("RetainAll operations")
    class RetainAllOperations {

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should retain all elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldRetainAllElements(String sutName, PriorityDeque<String> priorityDeque) {
            // Given
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            sut.add("test");
            sut.add("test2");
            sut.add("test3");
            List<String> elementsToRetain = new ArrayList<>();
            elementsToRetain.add("test");
            elementsToRetain.add("test2");
            // When
            boolean result = sut.retainAll(elementsToRetain);
            // Then
            assertTrue(result);
            assertEquals(2, sut.size());
            assertEquals("test", sut.poll());
            assertEquals("test2", sut.poll());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("Should throw when retainAll collection is null")
        @MethodSource("com.threeamigos.common.util.implementations.collections.BlockingPriorityDequeWrapperUnitTest#createSut")
        void shouldThrowWhenRetainAllCollectionIsNull(String sutName, PriorityDeque<String> priorityDeque) {
            BlockingPriorityDequeWrapper<String> sut = new BlockingPriorityDequeWrapper<>(priorityDeque);
            assertThrows(IllegalArgumentException.class, () -> sut.retainAll(null));
        }
    }
}
