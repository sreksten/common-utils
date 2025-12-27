package com.threeamigos.common.util.implementations.persistence;

import com.threeamigos.common.util.interfaces.persistence.Persistable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("BasicPersistablesCollector unit test")
@Tag("unit")
@Tag("persistence")
class BasicPersistablesCollectorUnitTest {

    private Persistable persistable1;
    private Persistable persistable2;

    @BeforeEach
    void setup() {
        persistable1 = mock(Persistable.class);
        persistable2 = mock(Persistable.class);
    }

    @Test
    @DisplayName("Collection constructor should throw exception if null collection is passed")
    void collectionConstructorShouldThrowExceptionIfNullArrayIsPassed() {
        assertThrows(IllegalArgumentException.class, () -> new BasicPersistablesCollector((Collection<Persistable>) null));
    }

    @Test
    @DisplayName("Collection constructor should throw exception if null Persistable is passed")
    void collectionConstructorShouldThrowExceptionIfNullPersistableIsPassed() {
        // Given
        Collection<Persistable> persistables = new ArrayList<>();
        persistables.add(null);
        assertThrows(IllegalArgumentException.class, () -> new BasicPersistablesCollector(persistables));
    }

    @Test
    @DisplayName("Varargs constructor should throw exception if null array is passed")
    void varargConstructorShouldThrowExceptionIfNullArrayIsPassed() {
        assertThrows(IllegalArgumentException.class, () -> new BasicPersistablesCollector((Persistable[]) null));
    }

    @Test
    @DisplayName("Varargs constructor should throw exception if null Persistable is passed")
    void varargConstructorShouldThrowExceptionIfNullPersistableIsPassed() {
        assertThrows(IllegalArgumentException.class, () -> new BasicPersistablesCollector(persistable1, null));
    }

    @Test
    @DisplayName("Varargs constructor should keep track of all persistables")
    void varargConstructorShouldKeepTrackOfAllPersistables() {
        // Given
        BasicPersistablesCollector collector = new BasicPersistablesCollector(persistable1, persistable2);
        // When
        Collection<Persistable> allPersistables = collector.getPersistables();
        // Then
        assertThat(allPersistables, containsInAnyOrder(persistable1, persistable2));
    }

    @Test
    @DisplayName("add method should throw Exception if adding a null Persistable")
    void addMethodShouldThrowExceptionIfAddingNullPersistable() {
        // Given
        BasicPersistablesCollector collector = new BasicPersistablesCollector();
        // Then
        assertThrows(IllegalArgumentException.class, () -> collector.add(null));
    }

    @Test
    @DisplayName("add method should keep track of all persistables")
    void addMethodShouldKeepTrackOfAllPersistables() {
        // Given
        BasicPersistablesCollector collector = new BasicPersistablesCollector();
        collector.add(persistable1);
        collector.add(persistable2);
        // When
        Collection<Persistable> allPersistables = collector.getPersistables();
        // Then
        assertThat(allPersistables, containsInAnyOrder(persistable1, persistable2));
    }

    @Test
    @DisplayName("remove  method should throw Exception if adding a null Persistable")
    void removeMethodShouldThrowExceptionIfAddingNullPersistable() {
        // Given
        BasicPersistablesCollector collector = new BasicPersistablesCollector();
        // Then
        assertThrows(IllegalArgumentException.class, () -> collector.remove(null));
    }

    @Test
    @DisplayName("remove method should remove a persistable")
    void removeMethodShouldRemovePersistable() {
        // Given
        BasicPersistablesCollector collector = new BasicPersistablesCollector(persistable1, persistable2);
        collector.remove(persistable1);
        // When
        Collection<Persistable> allPersistables = collector.getPersistables();
        // Then
        assertEquals(1, allPersistables.size());
        assertEquals(persistable2, allPersistables.iterator().next());
    }

    @Test
    @DisplayName("Should call all persistables")
    void shouldCallsAllPersistables() {
        // Given
        BasicPersistablesCollector collector = new BasicPersistablesCollector(persistable1, persistable2);
        // When
        collector.persist();
        // Then
        collector.getPersistables().forEach(p -> verify(p, times(1)).persist());
    }
}
