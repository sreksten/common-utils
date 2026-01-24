package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PriorityDeque Codex final unit tests")
public class PriorityDequeCodexUnitTest {

    private static final int DEFAULT_BUCKET_SIZE = 5;

    private static Stream<Arguments> createSut() {
        return Stream.of(
                Arguments.of("GeneralPurposePriorityDeque", (Supplier<PriorityDeque<String>>) GeneralPurposePriorityDeque::new),
                Arguments.of("BucketedPriorityDeque", (Supplier<PriorityDeque<String>>) () -> new BucketedPriorityDeque<>(DEFAULT_BUCKET_SIZE))
        );
    }

    private static PriorityDeque<String> newSut(Supplier<PriorityDeque<String>> factory) {
        return factory.get();
    }

    @Nested
    @DisplayName("Constructors")
    class ConstructorTests {
        @ParameterizedTest(name = "{0}")
        @DisplayName("default policy should be FIFO")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void defaultPolicyShouldBeFifo(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            assertEquals(PriorityDeque.Policy.FIFO, sut.getPolicy());
        }

        @Test
        @DisplayName("GeneralPurposePolicy constructor should use provided policy")
        void generalPurposePolicyConstructorUsesProvidedPolicy() {
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>(PriorityDeque.Policy.LIFO);
            assertEquals(PriorityDeque.Policy.LIFO, sut.getPolicy());
        }

        @Test
        @DisplayName("Bucketed default constructor should create max range deque")
        void bucketedDefaultConstructorCreatesDeque() {
            PriorityDeque<String> sut = new BucketedPriorityDeque<>();
            sut.add("a", BucketedPriorityDeque.MAX_PRIORITY);
            assertEquals("a", sut.peekFifo());
        }

        @Test
        @DisplayName("Bucketed constructor should validate bounds")
        void bucketedConstructorValidation() {
            assertThrows(IllegalArgumentException.class, () -> new BucketedPriorityDeque<String>(-1));
            assertThrows(IllegalArgumentException.class, () -> new BucketedPriorityDeque<String>(BucketedPriorityDeque.MAX_PRIORITY + 1));
        }

        @Test
        @DisplayName("Bucketed constructor should accept MIN and MAX bounds")
        void bucketedConstructorAcceptsBounds() {
            assertDoesNotThrow(() -> new BucketedPriorityDeque<String>(BucketedPriorityDeque.MIN_PRIORITY));
            assertDoesNotThrow(() -> new BucketedPriorityDeque<String>(BucketedPriorityDeque.MAX_PRIORITY));
        }

        @Test
        @DisplayName("Bucketed constructor should enforce instance max priority")
        void bucketedConstructorEnforcesInstanceMaxPriority() {
            BucketedPriorityDeque<String> sut = new BucketedPriorityDeque<>(5);
            assertThrows(IllegalArgumentException.class, () -> sut.add("x", 7));
        }
    }

    @Nested
    @DisplayName("Methods without priority")
    class MethodsWithoutPriority {
        @ParameterizedTest(name = "{0}")
        @DisplayName("policy can be changed and affects peek/poll")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void policyChangeAffectsPeekAndPoll(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            sut.add("b", 1);

            assertEquals("a", sut.peek());
            assertEquals("a", sut.poll());

            sut.add("a", 1);
            sut.add("b", 1);
            sut.setPolicy(PriorityDeque.Policy.LIFO);
            assertEquals("b", sut.peek());
            assertEquals("b", sut.poll());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("add should reject null values")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void addRejectsNull(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            assertThrows(IllegalArgumentException.class, () -> sut.add(null, 1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("empty deque behavior")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void emptyDequeBehavior(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            assertTrue(sut.isEmpty());
            assertEquals(0, sut.size());
            assertEquals(-1, sut.getHighestNotEmptyPriority());
            assertNull(sut.peek());
            assertNull(sut.peekFifo());
            assertNull(sut.peekLifo());
            assertNull(sut.poll());
            assertNull(sut.pollFifo());
            assertNull(sut.pollLifo());
            assertNull(sut.poll(1));
            assertNull(sut.pollFifo(1));
            assertNull(sut.pollLifo(1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("highest priority should be tracked and updated")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void highestPriorityTracking(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("p1", 1);
            sut.add("p3", 3);
            sut.add("p2", 2);
            assertEquals(3, sut.getHighestNotEmptyPriority());
            sut.pollFifo();
            assertEquals(2, sut.getHighestNotEmptyPriority());
            sut.pollFifo();
            sut.pollFifo();
            assertEquals(-1, sut.getHighestNotEmptyPriority());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("peek does not remove elements")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void peekDoesNotRemove(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("x", 1);
            assertEquals("x", sut.peek());
            assertEquals(1, sut.size());
            assertEquals("x", sut.peekFifo());
            assertEquals("x", sut.peekLifo());
            assertEquals(1, sut.size());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("poll honors priority order for FIFO and LIFO")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void pollHonorsPriorityOrder(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("low-1", 1);
            sut.add("high-1", 3);
            sut.add("high-2", 3);
            sut.add("low-2", 1);

            assertEquals("high-1", sut.pollFifo());
            assertEquals("high-2", sut.pollFifo());
            assertEquals("low-1", sut.pollFifo());
            assertEquals("low-2", sut.pollFifo());

            sut.add("low-1", 1);
            sut.add("high-1", 3);
            sut.add("high-2", 3);
            sut.add("low-2", 1);
            assertEquals("high-2", sut.pollLifo());
            assertEquals("high-1", sut.pollLifo());
            assertEquals("low-2", sut.pollLifo());
            assertEquals("low-1", sut.pollLifo());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("contains and containsAll edge cases")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void containsAndContainsAllEdgeCases(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            sut.add("b", 2);

            assertTrue(sut.contains("a"));
            assertFalse(sut.contains("missing"));
            assertFalse(sut.contains(null));

            List<String> all = Arrays.asList("a", "b");
            List<String> missing = Arrays.asList("a", "x");
            assertTrue(sut.containsAll(all));
            assertFalse(sut.containsAll(missing));
            assertThrows(IllegalArgumentException.class, () -> sut.containsAll(null));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("containsAll should return false when iterable contains null")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void containsAllReturnsFalseWhenIterableHasNull(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            List<String> withNull = Arrays.asList("a", null);
            assertFalse(sut.containsAll(withNull));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("remove should throw when deque is empty")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeThrowsWhenEmpty(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            assertThrows(NoSuchElementException.class, sut::remove);
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("remove should return true and reduce size when not empty")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeReturnsTrueWhenNotEmpty(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            assertTrue(sut.remove());
            assertEquals(0, sut.size());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("remove by value should return false when argument is null")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeByValueNullReturnsFalse(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            assertFalse(sut.remove((String) null));
            assertEquals(1, sut.size());
        }

        @Test
        @DisplayName("Bucketed contains/remove should return false for null values")
        void bucketedContainsRemoveNullValue() {
            PriorityDeque<String> sut = new BucketedPriorityDeque<>(5);
            sut.add("a", 1);
            assertFalse(sut.contains(null));
            assertFalse(sut.remove((String) null));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("removeAll and retainAll behaviors")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeAllAndRetainAllBehaviors(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            sut.add("b", 1);
            sut.add("c", 2);

            assertTrue(sut.removeAll(Arrays.asList("a", "missing")));
            assertEquals(2, sut.size());
            sut.add("a", 1);
            assertTrue(sut.removeAll(Arrays.asList("a", "b", "c")));
            assertTrue(sut.isEmpty());

            sut.add("a", 1);
            sut.add("b", 1);
            sut.add("c", 2);
            assertTrue(sut.retainAll(Collections.singleton("b")));
            assertEquals(1, sut.size());
            assertEquals("b", sut.peekFifo());

            assertThrows(IllegalArgumentException.class, () -> sut.removeAll(null));
            assertThrows(IllegalArgumentException.class, () -> sut.retainAll(null));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("retainAll should return false when no changes are made")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void retainAllNoChangeReturnsFalse(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            sut.add("b", 2);
            List<String> all = Arrays.asList("a", "b");
            assertFalse(sut.retainAll(all));
            assertEquals(2, sut.size());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("clear behavior and filter validation")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void clearBehaviorAndFilterValidation(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            sut.add("b", 2);
            sut.clear(2);
            assertEquals(1, sut.size());

            sut.clear();
            assertTrue(sut.isEmpty());

            assertThrows(IllegalArgumentException.class, () -> sut.clear((Function<String, Boolean>) null));
            assertThrows(IllegalArgumentException.class, () -> sut.clear(null, 1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("clear by filter should remove matching values")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void clearByFilterRemovesMatching(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("keep", 1);
            sut.add("drop", 2);
            sut.clear(v -> v.startsWith("drop"));
            assertEquals(1, sut.size());
            assertEquals("keep", sut.peekFifo());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("toList should respect policy order")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void toListRespectsPolicy(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("low-1", 1);
            sut.add("high-1", 3);
            sut.add("low-2", 1);
            sut.add("high-2", 3);

            List<String> fifo = sut.toList();
            assertEquals(Arrays.asList("high-1", "high-2", "low-1", "low-2"), fifo);

            sut.setPolicy(PriorityDeque.Policy.LIFO);
            List<String> lifo = sut.toList();
            assertEquals(Arrays.asList("high-2", "high-1", "low-2", "low-1"), lifo);
        }

        @Nested
        @DisplayName("Iterators")
        class IteratorTests {
            @ParameterizedTest(name = "{0}")
            @DisplayName("iterator should traverse all elements in priority order")
            @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
            void iteratorTraversalOrder(String sutName, Supplier<PriorityDeque<String>> factory) {
                PriorityDeque<String> sut = newSut(factory);
                sut.add("low-1", 1);
                sut.add("high-1", 3);
                sut.add("low-2", 1);
                sut.add("high-2", 3);

                List<String> expectedFifo = Arrays.asList("high-1", "high-2", "low-1", "low-2");
                List<String> actualFifo = new ArrayList<>();
                Iterator<String> fifo = sut.iterator();
                while (fifo.hasNext()) {
                    actualFifo.add(fifo.next());
                }
                assertEquals(expectedFifo, actualFifo);

                sut.setPolicy(PriorityDeque.Policy.LIFO);
                List<String> expectedLifo = Arrays.asList("high-2", "high-1", "low-2", "low-1");
                List<String> actualLifo = new ArrayList<>();
                Iterator<String> lifo = sut.iterator();
                while (lifo.hasNext()) {
                    actualLifo.add(lifo.next());
                }
                assertEquals(expectedLifo, actualLifo);
            }

            @ParameterizedTest(name = "{0}")
            @DisplayName("iterator should support remove")
            @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
            void iteratorSupportsRemove(String sutName, Supplier<PriorityDeque<String>> factory) {
                PriorityDeque<String> sut = newSut(factory);
                sut.add("a", 1);
                sut.add("b", 1);
                Iterator<String> iterator = sut.iterator();
                iterator.next();
                iterator.remove();
                iterator.next();
                iterator.remove();
                assertTrue(sut.isEmpty());
            }

            @ParameterizedTest(name = "{0}")
            @DisplayName("iterator error cases")
            @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
            void iteratorErrorCases(String sutName, Supplier<PriorityDeque<String>> factory) {
                PriorityDeque<String> sut = newSut(factory);
                Iterator<String> empty = sut.iterator();
                assertThrows(NoSuchElementException.class, empty::next);
                assertThrows(IllegalStateException.class, empty::remove);
            }

            @Test
            @DisplayName("Bucketed iterator remove should throw when current iterator is null")
            void bucketedIteratorRemoveWithNullCurrentIterator() throws Exception {
                BucketedPriorityDeque<String> sut = new BucketedPriorityDeque<>(3);
                sut.add("a", 1);
                Iterator<String> iterator = sut.iterator();
                iterator.next();
                Field field = iterator.getClass().getDeclaredField("currentDequeIterator");
                field.setAccessible(true);
                field.set(iterator, null);
                assertThrows(IllegalStateException.class, iterator::remove);
            }
        }
    }

    @Nested
    @DisplayName("Methods with priority")
    class MethodsWithPriority {
        @ParameterizedTest(name = "{0}")
        @DisplayName("priority-specific peek/poll should use provided policy")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void prioritySpecificPeekPollUsesPolicy(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 2);
            sut.add("b", 2);

            assertEquals("a", sut.peek(2));
            assertEquals("a", sut.peekFifo(2));
            assertEquals("b", sut.peekLifo(2));

            sut.setPolicy(PriorityDeque.Policy.LIFO);
            assertEquals("b", sut.peek(2));
            assertEquals("b", sut.poll(2));
            assertEquals("a", sut.poll(2));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("pollFifo by priority should handle non-empty and empty buckets")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void pollFifoByPriorityHandlesBucketState(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 2);
            sut.add("b", 2);
            assertEquals("a", sut.pollFifo(2));
            assertEquals(1, sut.size(2));
            assertFalse(sut.isEmpty(2));

            PriorityDeque<String> sut2 = newSut(factory);
            sut2.add("x", 2);
            assertEquals("x", sut2.pollFifo(2));
            assertEquals(0, sut2.size(2));
            assertTrue(sut2.isEmpty(2));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("pollLifo by priority should handle non-empty and empty buckets")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void pollLifoByPriorityHandlesBucketState(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 2);
            sut.add("b", 2);
            assertEquals("b", sut.pollLifo(2));
            assertEquals(1, sut.size(2));

            PriorityDeque<String> sut2 = newSut(factory);
            sut2.add("x", 2);
            assertEquals("x", sut2.pollLifo(2));
            assertEquals(0, sut2.size(2));
            assertTrue(sut2.isEmpty(2));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("contains and containsAll by priority edge cases")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void containsAndContainsAllByPriorityEdgeCases(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            assertTrue(sut.contains("a", 1));
            assertFalse(sut.contains("a", 2));
            assertFalse(sut.contains(null, 1));
            assertFalse(sut.containsAll(Arrays.asList("a", null), 1));
            assertThrows(IllegalArgumentException.class, () -> sut.containsAll(null, 1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("remove by value and priority should handle null and bucket state")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeByValueAndPriorityNullAndBucketState(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            sut.add("b", 1);

            assertFalse(sut.remove(null, 1));
            assertTrue(sut.remove("a", 1));
            assertEquals(1, sut.size(1));
            assertTrue(sut.remove("b", 1));
            assertEquals(0, sut.size(1));
            assertTrue(sut.isEmpty(1));
        }

        @Test
        @DisplayName("Bucketed contains/remove by priority should return false for null values")
        void bucketedContainsRemoveByPriorityNullValue() {
            PriorityDeque<String> sut = new BucketedPriorityDeque<>(5);
            sut.add("a", 1);
            assertFalse(sut.contains(null, 1));
            assertFalse(sut.remove(null, 1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("remove by value and priority should not remove from other priorities")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeByValueAndPriorityDifferentPriority(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("p0", 0);
            assertFalse(sut.remove("p0", 1));
            assertEquals(1, sut.size(0));
            assertFalse(sut.isEmpty(0));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("remove by value and priority should not remove when bucket exists but value missing")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeByValueAndPriorityBucketExistsValueMissing(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            assertFalse(sut.remove("missing", 1));
            assertEquals(1, sut.size(1));
            assertFalse(sut.isEmpty(1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("remove by priority should throw when deque is empty")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeByPriorityThrowsWhenEmpty(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            assertThrows(NoSuchElementException.class, () -> sut.remove(1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("remove by priority should return true and reduce size when not empty")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeByPriorityReturnsTrueWhenNotEmpty(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            assertTrue(sut.remove(1));
            assertEquals(0, sut.size(1));
            assertTrue(sut.isEmpty(1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("removeAll and retainAll by priority should update bucket state")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeAllAndRetainAllByPriority(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            sut.add("b", 1);
            sut.add("c", 2);

            assertFalse(sut.removeAll(Collections.singleton("missing"), 1));
            assertTrue(sut.removeAll(Arrays.asList("a", "b"), 1));
            assertEquals(1, sut.size());

            assertFalse(sut.retainAll(Collections.singleton("c"), 2));
            assertEquals(1, sut.size());
            assertFalse(sut.isEmpty());
            assertFalse(sut.isEmpty(2));

            assertThrows(IllegalArgumentException.class, () -> sut.removeAll(null, 1));
            assertThrows(IllegalArgumentException.class, () -> sut.retainAll(null, 1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("removeAll by priority should handle bucket emptying and non-empty cases")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeAllByPriorityBucketState(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            sut.add("b", 1);
            sut.add("c", 1);
            assertTrue(sut.removeAll(Arrays.asList("a", "b"), 1));
            assertEquals(1, sut.size(1));
            assertFalse(sut.isEmpty(1));

            PriorityDeque<String> sut2 = newSut(factory);
            sut2.add("x", 1);
            sut2.add("y", 1);
            assertTrue(sut2.removeAll(Arrays.asList("x", "y"), 1));
            assertEquals(0, sut2.size(1));
            assertTrue(sut2.isEmpty(1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("retainAll by priority should handle bucket emptying and non-empty cases")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void retainAllByPriorityBucketState(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            sut.add("b", 1);
            sut.add("c", 1);
            assertTrue(sut.retainAll(Collections.singleton("b"), 1));
            assertEquals(1, sut.size(1));
            assertFalse(sut.isEmpty(1));

            PriorityDeque<String> sut2 = newSut(factory);
            sut2.add("x", 1);
            sut2.add("y", 1);
            assertTrue(sut2.retainAll(Collections.singleton("missing"), 1));
            assertEquals(0, sut2.size(1));
            assertTrue(sut2.isEmpty(1));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("removeAll/retainAll by priority should ignore other priorities")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void removeAllAndRetainAllByPriorityIgnoreOtherPriorities(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("p0-a", 0);
            sut.add("p0-b", 0);
            assertFalse(sut.removeAll(Arrays.asList("p0-a", "p0-b"), 1));
            assertEquals(2, sut.size(0));

            assertFalse(sut.retainAll(Collections.singleton("p0-a"), 1));
            assertEquals(2, sut.size(0));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("clear by filter and priority should update bucket")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void clearByFilterAndPriorityUpdatesBucket(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("keep-1", 1);
            sut.add("drop-1", 1);
            sut.add("p2", 2);

            sut.clear(v -> v.startsWith("drop"), 1);
            assertEquals(2, sut.size());
            assertEquals(1, sut.size(1));
            assertEquals(1, sut.size(2));

            sut.clear(v -> true, 1);
            assertTrue(sut.isEmpty(1));
            assertFalse(sut.isEmpty());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("size and isEmpty should track per priority changes")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void sizeAndIsEmptyTrackPerPriorityChanges(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            sut.add("a", 1);
            sut.add("b", 1);
            sut.add("c", 2);

            assertEquals(3, sut.size());
            assertEquals(2, sut.size(1));
            assertEquals(1, sut.size(2));
            assertFalse(sut.isEmpty(1));
            assertFalse(sut.isEmpty(2));
            assertTrue(sut.isEmpty(3));

            sut.clear(1);
            assertEquals(1, sut.size());
            assertTrue(sut.isEmpty(1));
            assertFalse(sut.isEmpty());
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("toList by priority should reflect bucket contents")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void toListByPriority(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            assertTrue(sut.toList(1).isEmpty());
            sut.add("a", 1);
            sut.add("b", 1);
            assertEquals(Arrays.asList("a", "b"), sut.toList(1));
        }

        @Nested
        @DisplayName("Iterators")
        class IteratorTests {
            @ParameterizedTest(name = "{0}")
            @DisplayName("iterator by priority should respect policy order")
            @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
            void iteratorByPriorityRespectsPolicy(String sutName, Supplier<PriorityDeque<String>> factory) {
                PriorityDeque<String> sut = newSut(factory);
                sut.add("a", 1);
                sut.add("b", 1);
                sut.add("c", 2);

                Iterator<String> fifo = sut.iterator(1);
                List<String> fifoList = new ArrayList<>();
                while (fifo.hasNext()) {
                    fifoList.add(fifo.next());
                }
                assertEquals(Arrays.asList("a", "b"), fifoList);

                sut.setPolicy(PriorityDeque.Policy.LIFO);
                Iterator<String> lifo = sut.iterator(1);
                List<String> lifoList = new ArrayList<>();
                while (lifo.hasNext()) {
                    lifoList.add(lifo.next());
                }
                assertEquals(Arrays.asList("b", "a"), lifoList);
            }

            @ParameterizedTest(name = "{0}")
            @DisplayName("iterator by priority error cases")
            @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
            void iteratorByPriorityErrorCases(String sutName, Supplier<PriorityDeque<String>> factory) {
                PriorityDeque<String> sut = newSut(factory);
                Iterator<String> empty = sut.iterator(1);
                assertThrows(NoSuchElementException.class, empty::next);
                assertThrows(IllegalStateException.class, empty::remove);
            }

            @Test
            @DisplayName("GeneralPurpose iterator remove should throw when current iterator is null")
            void generalPurposeIteratorRemoveWithNullCurrentIterator() {
                PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
                Iterator<String> empty = sut.iterator(42);
                assertThrows(IllegalStateException.class, empty::remove);
            }

            @Test
            @DisplayName("GeneralPurpose iterator remove should not trigger ConcurrentModificationException")
            void generalPurposeIteratorRemoveDoesNotThrowConcurrentModification() {
                PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
                sut.add("p2-a", 2);
                sut.add("p2-b", 2);
                sut.add("p1-a", 1);

                assertDoesNotThrow(() -> {
                    Iterator<String> iterator = sut.iterator();
                    while (iterator.hasNext()) {
                        String value = iterator.next();
                        if (value.startsWith("p2-")) {
                            iterator.remove();
                        }
                    }
                });

                assertEquals(1, sut.size());
                assertTrue(sut.contains("p1-a"));
            }
        }
    }

    @Nested
    @DisplayName("Miscellaneous")
    class MiscellaneousTests {
        @ParameterizedTest(name = "{0}")
        @DisplayName("policy null handling matches implementations")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void policyNullHandlingMatchesImplementations(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            assertThrows(IllegalArgumentException.class, () -> sut.setPolicy(null));
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("priority bounds are enforced for bucketed implementation")
        @MethodSource("com.threeamigos.common.util.implementations.collections.PriorityDequeCodexUnitTest#createSut")
        void priorityBoundsEnforcedForBucketed(String sutName, Supplier<PriorityDeque<String>> factory) {
            PriorityDeque<String> sut = newSut(factory);
            if (sut instanceof BucketedPriorityDeque) {
                assertThrows(IllegalArgumentException.class, () -> sut.add("x", -1));
                assertThrows(IllegalArgumentException.class, () -> sut.add("x", BucketedPriorityDeque.MAX_PRIORITY + 1));
                assertThrows(IllegalArgumentException.class, () -> sut.peek(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.poll(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.peekFifo(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.peekLifo(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.pollFifo(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.pollLifo(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.isEmpty(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.size(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.contains("x", -1));
                assertThrows(IllegalArgumentException.class, () -> sut.containsAll(Collections.emptyList(), -1));
                assertThrows(IllegalArgumentException.class, () -> sut.clear(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.clear(t -> true, -1));
                assertThrows(IllegalArgumentException.class, () -> sut.remove(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.remove("x", -1));
                assertThrows(IllegalArgumentException.class, () -> sut.removeAll(Collections.emptyList(), -1));
                assertThrows(IllegalArgumentException.class, () -> sut.retainAll(Collections.emptyList(), -1));
                assertThrows(IllegalArgumentException.class, () -> sut.toList(-1));
                assertThrows(IllegalArgumentException.class, () -> sut.iterator(-1));
            } else {
                sut.add("x", -1);
                sut.add("y", Integer.MAX_VALUE);
                assertEquals("y", sut.peek());
            }
        }

        @Test
        @DisplayName("GeneralPurpose peek by priority should return null when bucket missing")
        void generalPurposePeekByPriorityMissingBucketReturnsNull() {
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            assertNull(sut.peekFifo(1));
            assertNull(sut.peekLifo(1));
        }

        @Test
        @DisplayName("GeneralPurpose containsAll by priority should return true for empty iterable when bucket missing")
        void generalPurposeContainsAllByPriorityEmptyIterableMissingBucket() {
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            assertTrue(sut.containsAll(Collections.emptyList(), 1));
        }

        @Test
        @DisplayName("GeneralPurpose clear by priority should ignore missing bucket")
        void generalPurposeClearByPriorityMissingBucket() {
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("p0", 0);
            sut.clear(1);
            assertEquals(1, sut.size());
            assertFalse(sut.isEmpty());
        }

        @Test
        @DisplayName("GeneralPurpose clear by filter and priority should ignore missing bucket")
        void generalPurposeClearByFilterPriorityMissingBucket() {
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("p0", 0);
            sut.clear(v -> true, 1);
            assertEquals(1, sut.size());
            assertFalse(sut.isEmpty());
        }

        @Test
        @DisplayName("GeneralPurpose contains by priority should cover all branches")
        void generalPurposeContainsByPriorityBranches() {
            PriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("a", 1);
            assertTrue(sut.contains("a", 1));
            assertFalse(sut.contains("missing", 1));
            assertFalse(sut.contains("missing", 2));
        }

        @Test
        @DisplayName("GeneralPurpose isEmpty and clear should handle empty bucket entries")
        void generalPurposeEmptyBucketBranches() throws Exception {
            GeneralPurposePriorityDeque<String> sut = new GeneralPurposePriorityDeque<>();
            sut.add("a", 1);

            assertFalse(sut.isEmpty(1));
            sut.clear(1);
            assertTrue(sut.isEmpty(1));
            assertTrue(sut.isEmpty());
        }
    }
}
