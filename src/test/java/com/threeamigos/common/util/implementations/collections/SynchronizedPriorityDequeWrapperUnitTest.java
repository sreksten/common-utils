package com.threeamigos.common.util.implementations.collections;

import com.threeamigos.common.util.interfaces.collections.PriorityDeque;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SynchronizedPriorityDequeWrapper unit tests")
class SynchronizedPriorityDequeWrapperUnitTest {

    @Test
    @DisplayName("Constructor should reject null delegate")
    void constructorRejectsNullDelegate() {
        assertThrows(IllegalArgumentException.class, () -> new SynchronizedPriorityDequeWrapper<>(null));
    }

    @Test
    @DisplayName("Wrapper should delegate all basic methods")
    void delegatesAllMethods() {
        FakePriorityDeque delegate = new FakePriorityDeque();
        SynchronizedPriorityDequeWrapper<String> sut = new SynchronizedPriorityDequeWrapper<>(delegate);

        List<String> list = new ArrayList<>();
        list.add("a");
        Function<String, Boolean> filter = value -> true;

        delegate.policy = PriorityDeque.Policy.FIFO;
        delegate.peek = "peek";
        delegate.peekFifo = "peekFifo";
        delegate.peekLifo = "peekLifo";
        delegate.poll = "poll";
        delegate.pollFifo = "pollFifo";
        delegate.pollLifo = "pollLifo";
        delegate.isEmpty = true;
        delegate.size = 3;
        delegate.contains = true;
        delegate.containsAll = true;
        delegate.remove = true;
        delegate.removeValue = true;
        delegate.removeAll = true;
        delegate.retainAll = true;
        delegate.list = list;
        delegate.highestPriority = 7;
        delegate.peekPriority = "peek1";
        delegate.peekFifoPriority = "peekFifo1";
        delegate.peekLifoPriority = "peekLifo1";
        delegate.pollPriority = "poll1";
        delegate.pollFifoPriority = "pollFifo1";
        delegate.pollLifoPriority = "pollLifo1";
        delegate.isEmptyPriority = false;
        delegate.sizePriority = 2;
        delegate.containsPriority = true;
        delegate.containsAllPriority = true;
        delegate.removePriority = true;
        delegate.removeValuePriority = true;
        delegate.removeAllPriority = true;
        delegate.retainAllPriority = true;
        delegate.listPriority = list;

        sut.setPolicy(PriorityDeque.Policy.FIFO);
        assertEquals(PriorityDeque.Policy.FIFO, sut.getPolicy());
        sut.add("a", 1);
        sut.addAll(list, 1);
        assertEquals("peek", sut.peek());
        assertEquals("peekFifo", sut.peekFifo());
        assertEquals("peekLifo", sut.peekLifo());
        assertEquals("poll", sut.poll());
        assertEquals("pollFifo", sut.pollFifo());
        assertEquals("pollLifo", sut.pollLifo());
        assertTrue(sut.isEmpty());
        assertEquals(3, sut.size());
        assertTrue(sut.contains("a"));
        assertTrue(sut.containsAll(list));
        sut.clear();
        sut.clear(filter);
        assertTrue(sut.remove());
        assertTrue(sut.remove("a"));
        assertTrue(sut.removeAll(list));
        assertTrue(sut.retainAll(list));
        assertEquals(list, sut.toList());
        assertEquals(7, sut.getHighestNotEmptyPriority());
        assertEquals("peek1", sut.peek(1));
        assertEquals("peekFifo1", sut.peekFifo(1));
        assertEquals("peekLifo1", sut.peekLifo(1));
        assertEquals("poll1", sut.poll(1));
        assertEquals("pollFifo1", sut.pollFifo(1));
        assertEquals("pollLifo1", sut.pollLifo(1));
        assertFalse(sut.isEmpty(1));
        assertEquals(2, sut.size(1));
        assertTrue(sut.contains("a", 1));
        assertTrue(sut.containsAll(list, 1));
        sut.clear(1);
        sut.clear(filter, 1);
        assertTrue(sut.remove(1));
        assertTrue(sut.remove("a", 1));
        assertTrue(sut.removeAll(list, 1));
        assertTrue(sut.retainAll(list, 1));
        assertEquals(list, sut.toList(1));

        assertTrue(delegate.calledSetPolicy);
        assertTrue(delegate.calledGetPolicy);
        assertTrue(delegate.calledAdd);
        assertTrue(delegate.calledAddAll);
        assertTrue(delegate.calledPeek);
        assertTrue(delegate.calledPeekFifo);
        assertTrue(delegate.calledPeekLifo);
        assertTrue(delegate.calledPoll);
        assertTrue(delegate.calledPollFifo);
        assertTrue(delegate.calledPollLifo);
        assertTrue(delegate.calledIsEmpty);
        assertTrue(delegate.calledSize);
        assertTrue(delegate.calledContains);
        assertTrue(delegate.calledContainsAll);
        assertTrue(delegate.calledClear);
        assertTrue(delegate.calledClearFilter);
        assertTrue(delegate.calledRemove);
        assertTrue(delegate.calledRemoveValue);
        assertTrue(delegate.calledRemoveAll);
        assertTrue(delegate.calledRetainAll);
        assertTrue(delegate.calledToList);
        assertTrue(delegate.calledHighestPriority);
        assertTrue(delegate.calledPeekPriority);
        assertTrue(delegate.calledPeekFifoPriority);
        assertTrue(delegate.calledPeekLifoPriority);
        assertTrue(delegate.calledPollPriority);
        assertTrue(delegate.calledPollFifoPriority);
        assertTrue(delegate.calledPollLifoPriority);
        assertTrue(delegate.calledIsEmptyPriority);
        assertTrue(delegate.calledSizePriority);
        assertTrue(delegate.calledContainsPriority);
        assertTrue(delegate.calledContainsAllPriority);
        assertTrue(delegate.calledClearPriority);
        assertTrue(delegate.calledClearFilterPriority);
        assertTrue(delegate.calledRemovePriority);
        assertTrue(delegate.calledRemoveValuePriority);
        assertTrue(delegate.calledRemoveAllPriority);
        assertTrue(delegate.calledRetainAllPriority);
        assertTrue(delegate.calledToListPriority);
    }

    @Test
    @DisplayName("Iterator should return snapshot of elements")
    void iteratorReturnsSnapshot() {
        FakePriorityDeque delegate = new FakePriorityDeque();
        delegate.list = new ArrayList<>(Arrays.asList("a", "b", "c"));

        SynchronizedPriorityDequeWrapper<String> sut = new SynchronizedPriorityDequeWrapper<>(delegate);
        Iterator<String> wrapped = sut.iterator();
        assertTrue(wrapped.hasNext());
        assertEquals("a", wrapped.next());
        assertEquals("b", wrapped.next());
        assertEquals("c", wrapped.next());
        assertFalse(wrapped.hasNext());
        assertTrue(delegate.calledToList);
    }

    @Test
    @DisplayName("Iterator should handle repeated hasNext false")
    void iteratorHandlesRepeatedHasNextFalse() {
        FakePriorityDeque delegate = new FakePriorityDeque();
        delegate.list = new ArrayList<>();

        SynchronizedPriorityDequeWrapper<String> sut = new SynchronizedPriorityDequeWrapper<>(delegate);
        Iterator<String> wrapped = sut.iterator();
        assertFalse(wrapped.hasNext());
        assertFalse(wrapped.hasNext());
    }

    @Test
    @DisplayName("Iterator should return empty iterator when toList is empty")
    void iteratorReturnsEmptyIteratorWhenEmpty() {
        FakePriorityDeque delegate = new FakePriorityDeque();
        delegate.list = new ArrayList<>();

        SynchronizedPriorityDequeWrapper<String> sut = new SynchronizedPriorityDequeWrapper<>(delegate);
        Iterator<String> wrapped = sut.iterator();
        assertFalse(wrapped.hasNext());
        assertTrue(delegate.calledToList);
    }

    @Test
    @DisplayName("Iterator should support remove on snapshot")
    void iteratorSupportsRemoveOnSnapshot() {
        FakePriorityDeque delegate = new FakePriorityDeque();
        delegate.list = new ArrayList<>(Arrays.asList("a", "b"));

        SynchronizedPriorityDequeWrapper<String> sut = new SynchronizedPriorityDequeWrapper<>(delegate);
        Iterator<String> wrapped = sut.iterator();
        assertTrue(wrapped.hasNext());
        assertEquals("a", wrapped.next());
        wrapped.remove();  // Remove from snapshot (not affecting delegate)
        assertEquals("b", wrapped.next());
        assertFalse(wrapped.hasNext());
    }

    @Test
    @DisplayName("Iterator by priority should return snapshot of priority elements")
    void iteratorByPriorityReturnsSnapshot() {
        FakePriorityDeque delegate = new FakePriorityDeque();
        delegate.listPriority = new ArrayList<>(Arrays.asList("a", "b"));

        SynchronizedPriorityDequeWrapper<String> sut = new SynchronizedPriorityDequeWrapper<>(delegate);
        Iterator<String> wrapped = sut.iterator(1);
        assertTrue(wrapped.hasNext());
        assertEquals("a", wrapped.next());
        assertEquals("b", wrapped.next());
        assertFalse(wrapped.hasNext());
        assertTrue(delegate.calledToListPriority);
    }

    @Test
    @DisplayName("Concurrent access should not corrupt state")
    void concurrentAccessShouldBeSafe() throws Exception {
        PriorityDeque<Integer> delegate = new GeneralPurposePriorityDeque<>();
        SynchronizedPriorityDequeWrapper<Integer> sut = new SynchronizedPriorityDequeWrapper<>(delegate);
        int total = 200;
        for (int i = 0; i < total; i++) {
            sut.add(i, 1);
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger polled = new AtomicInteger(0);

        Runnable worker = () -> {
            try {
                start.await();
                while (true) {
                    Integer value = sut.poll();
                    if (value == null) {
                        break;
                    }
                    polled.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < 4; i++) {
            pool.submit(worker);
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(total, polled.get());
        assertTrue(sut.isEmpty());
    }

    private static final class FakePriorityDeque implements PriorityDeque<String> {
        private Policy policy = Policy.FIFO;
        private boolean calledSetPolicy;
        private boolean calledGetPolicy;
        private boolean calledAdd;
        private boolean calledAddAll;
        private boolean calledPeek;
        private boolean calledPeekFifo;
        private boolean calledPeekLifo;
        private boolean calledPoll;
        private boolean calledPollFifo;
        private boolean calledPollLifo;
        private boolean calledIsEmpty;
        private boolean calledSize;
        private boolean calledContains;
        private boolean calledContainsAll;
        private boolean calledClear;
        private boolean calledClearFilter;
        private boolean calledRemove;
        private boolean calledRemoveValue;
        private boolean calledRemoveAll;
        private boolean calledRetainAll;
        private boolean calledToList;
        private boolean calledHighestPriority;
        private boolean calledPeekPriority;
        private boolean calledPeekFifoPriority;
        private boolean calledPeekLifoPriority;
        private boolean calledPollPriority;
        private boolean calledPollFifoPriority;
        private boolean calledPollLifoPriority;
        private boolean calledIsEmptyPriority;
        private boolean calledSizePriority;
        private boolean calledContainsPriority;
        private boolean calledContainsAllPriority;
        private boolean calledClearPriority;
        private boolean calledClearFilterPriority;
        private boolean calledRemovePriority;
        private boolean calledRemoveValuePriority;
        private boolean calledRemoveAllPriority;
        private boolean calledRetainAllPriority;
        private boolean calledToListPriority;
        private boolean calledIterator;
        private boolean calledIteratorPriority;

        private String peek;
        private String peekFifo;
        private String peekLifo;
        private String poll;
        private String pollFifo;
        private String pollLifo;
        private boolean isEmpty;
        private int size;
        private boolean contains;
        private boolean containsAll;
        private boolean remove;
        private boolean removeValue;
        private boolean removeAll;
        private boolean retainAll;
        private List<String> list = new ArrayList<>();
        private int highestPriority;
        private String peekPriority;
        private String peekFifoPriority;
        private String peekLifoPriority;
        private String pollPriority;
        private String pollFifoPriority;
        private String pollLifoPriority;
        private boolean isEmptyPriority;
        private int sizePriority;
        private boolean containsPriority;
        private boolean containsAllPriority;
        private boolean removePriority;
        private boolean removeValuePriority;
        private boolean removeAllPriority;
        private boolean retainAllPriority;
        private List<String> listPriority = new ArrayList<>();
        private Iterator<String> iterator = new ArrayDeque<String>().iterator();
        private Iterator<String> iteratorPriority = new ArrayDeque<String>().iterator();

        @Override
        public void setPolicy(@Nonnull Policy policy) {
            calledSetPolicy = true;
            this.policy = policy;
        }

        @Override
        public Policy getPolicy() {
            calledGetPolicy = true;
            return policy;
        }

        @Override
        public void add(@Nonnull String t, int priority) {
            calledAdd = true;
        }

        @Override
        public void addAll(@Nonnull Collection<String> iterable, int priority) {
            calledAddAll = true;
        }

        @Override
        public String peek() {
            calledPeek = true;
            return peek;
        }

        @Override
        public String peekFifo() {
            calledPeekFifo = true;
            return peekFifo;
        }

        @Override
        public String peekLifo() {
            calledPeekLifo = true;
            return peekLifo;
        }

        @Override
        public String poll() {
            calledPoll = true;
            return poll;
        }

        @Override
        public String pollFifo() {
            calledPollFifo = true;
            return pollFifo;
        }

        @Override
        public String pollLifo() {
            calledPollLifo = true;
            return pollLifo;
        }

        @Override
        public boolean isEmpty() {
            calledIsEmpty = true;
            return isEmpty;
        }

        @Override
        public int size() {
            calledSize = true;
            return size;
        }

        @Override
        public boolean contains(@Nonnull String t) {
            calledContains = true;
            return contains;
        }

        @Override
        public boolean containsAll(@Nonnull java.util.Collection<String> iterable) {
            calledContainsAll = true;
            return containsAll;
        }

        @Override
        public void clear() {
            calledClear = true;
        }

        @Override
        public void clear(@Nonnull Function<String, Boolean> filteringFunction) {
            calledClearFilter = true;
        }

        @Override
        public boolean remove() {
            calledRemove = true;
            return remove;
        }

        @Override
        public boolean remove(@Nonnull String t) {
            calledRemoveValue = true;
            return removeValue;
        }

        @Override
        public boolean removeAll(@Nonnull java.util.Collection<String> iterable) {
            calledRemoveAll = true;
            return removeAll;
        }

        @Override
        public boolean retainAll(@Nonnull java.util.Collection<String> iterable) {
            calledRetainAll = true;
            return retainAll;
        }

        @Nonnull
        @Override
        public List<String> toList() {
            calledToList = true;
            return list;
        }

        @Nonnull
        @Override
        public Iterator<String> iterator() {
            calledIterator = true;
            return iterator;
        }

        @Override
        public int getHighestNotEmptyPriority() {
            calledHighestPriority = true;
            return highestPriority;
        }

        @Override
        public String peek(int priority) {
            calledPeekPriority = true;
            return peekPriority;
        }

        @Override
        public String peekFifo(int priority) {
            calledPeekFifoPriority = true;
            return peekFifoPriority;
        }

        @Override
        public String peekLifo(int priority) {
            calledPeekLifoPriority = true;
            return peekLifoPriority;
        }

        @Override
        public String poll(int priority) {
            calledPollPriority = true;
            return pollPriority;
        }

        @Override
        public String pollFifo(int priority) {
            calledPollFifoPriority = true;
            return pollFifoPriority;
        }

        @Override
        public String pollLifo(int priority) {
            calledPollLifoPriority = true;
            return pollLifoPriority;
        }

        @Override
        public boolean isEmpty(int priority) {
            calledIsEmptyPriority = true;
            return isEmptyPriority;
        }

        @Override
        public int size(int priority) {
            calledSizePriority = true;
            return sizePriority;
        }

        @Override
        public boolean contains(@Nonnull String t, int priority) {
            calledContainsPriority = true;
            return containsPriority;
        }

        @Override
        public boolean containsAll(@Nonnull java.util.Collection<String> iterable, int priority) {
            calledContainsAllPriority = true;
            return containsAllPriority;
        }

        @Override
        public void clear(int priority) {
            calledClearPriority = true;
        }

        @Override
        public void clear(@Nonnull Function<String, Boolean> filteringFunction, int priority) {
            calledClearFilterPriority = true;
        }

        @Override
        public boolean remove(int priority) {
            calledRemovePriority = true;
            return removePriority;
        }

        @Override
        public boolean remove(@Nonnull String t, int priority) {
            calledRemoveValuePriority = true;
            return removeValuePriority;
        }

        @Override
        public boolean removeAll(@Nonnull java.util.Collection<String> iterable, int priority) {
            calledRemoveAllPriority = true;
            return removeAllPriority;
        }

        @Override
        public boolean retainAll(@Nonnull java.util.Collection<String> iterable, int priority) {
            calledRetainAllPriority = true;
            return retainAllPriority;
        }

        @Nonnull
        @Override
        public List<String> toList(int priority) {
            calledToListPriority = true;
            return listPriority;
        }

        @Nonnull
        @Override
        public Iterator<String> iterator(int priority) {
            calledIteratorPriority = true;
            return iteratorPriority;
        }
    }
}
