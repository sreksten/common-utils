package com.threeamigos.common.util.implementations.preferences;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.interfaces.persistence.PersistResult;
import com.threeamigos.common.util.interfaces.persistence.Persister;
import com.threeamigos.common.util.interfaces.persistence.StatusTracker;
import com.threeamigos.common.util.interfaces.persistence.StatusTrackerFactory;
import com.threeamigos.common.util.interfaces.preferences.PreferencesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.threeamigos.common.util.implementations.TestClass.TEST_STRING;
import static com.threeamigos.common.util.implementations.TestClass.TEST_VALUE;
import static com.threeamigos.common.util.implementations.preferences.BasicPreferencesManager.INVALID_PREFERENCES_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DisplayName("AbstractPreferencesManager unit test")
@Tag("unit")
@Tag("preferences")
@Tag("persistence")
class AbstractPreferencesManagerTest {

    InMemoryMessageHandler messageHandler;
    TestClass instance;
    StatusTracker<TestClass> mockStatusTracker;
    StatusTrackerFactory<TestClass> mockStatusTrackerFactory;
    Persister<TestClass> mockPersister;
    PersistResult successfulPersistResult;
    PersistResult notFoundPersistResult;
    PersistResult errorPersistResult;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        messageHandler = new InMemoryMessageHandler();
        instance = new TestClass(TEST_STRING, TEST_VALUE);

        mockStatusTracker = mock(StatusTracker.class);

        mockStatusTrackerFactory = mock(StatusTrackerFactory.class);
        when(mockStatusTrackerFactory.buildStatusTracker(any())).thenReturn(mockStatusTracker);

        successfulPersistResult = mock(PersistResult.class);
        when(successfulPersistResult.isSuccessful()).thenReturn(true);

        notFoundPersistResult = mock(PersistResult.class);
        when(notFoundPersistResult.isSuccessful()).thenReturn(false);
        when(notFoundPersistResult.isNotFound()).thenReturn(true);

        errorPersistResult = mock(PersistResult.class);
        when(errorPersistResult.isSuccessful()).thenReturn(false);
        when(errorPersistResult.isNotFound()).thenReturn(false);
        when(errorPersistResult.getError()).thenReturn("I/O error");

        mockPersister = mock(Persister.class);
    }

    @Test
    @DisplayName("Should try to load persistent data during initialization")
    void shouldTryToLoadPersistentDataDuringInitialization() {
        // Given
        when(mockPersister.load(any())).thenReturn(notFoundPersistResult);
        // When
        new BasicPreferencesManager<>(instance, mockPersister, mockStatusTrackerFactory, messageHandler);
        // Then
        verify(mockPersister, times(1)).load(instance);
    }

    @Test
    @DisplayName("Should load default values when file is not found")
    void shouldLoadDefaultValuesWhenFileIsNotFound() {
        // Given
        when(mockPersister.load(any())).thenReturn(notFoundPersistResult);
        TestClass mockInstance = mock(TestClass.class);
        // When
        new BasicPreferencesManager<>(mockInstance, mockPersister, mockStatusTrackerFactory, messageHandler);
        // Then
        verify(mockInstance, times(1)).loadDefaultValues();
    }

    @Test
    @DisplayName("Should warn when an error occurs during loading")
    void shouldWarnWhenErrorOccurs() {
        // Given
        when(mockPersister.load(any())).thenReturn(errorPersistResult);
        TestClass mockInstance = mock(TestClass.class);
        // When
        new BasicPreferencesManager<>(mockInstance, mockPersister, mockStatusTrackerFactory, messageHandler);
        // Then
        String expectedValue = String.format(INVALID_PREFERENCES_TEMPLATE, mockInstance.getDescription(), "I/O error");
        verify(mockInstance, times(1)).loadDefaultValues();
        assertEquals(1, messageHandler.getAllErrorMessages().size());
        assertEquals(expectedValue, messageHandler.getAllErrorMessages().get(0));
    }

    @Test
    @DisplayName("Should load default values when data validation fails")
    void shouldLoadDefaultValuesWhenDataValidationFails() {
        // Given
        when(mockPersister.load(any())).thenReturn(successfulPersistResult);
        TestClass mockInstance = mock(TestClass.class);
        doThrow(new IllegalArgumentException("Test exception")).when(mockInstance).validate();
        // When
        new BasicPreferencesManager<>(mockInstance, mockPersister, mockStatusTrackerFactory, messageHandler);
        // Then
        verify(mockInstance, times(1)).loadDefaultValues();
    }

    @Test
    @DisplayName("Should warn when data validation fails")
    void shouldWarnWhenDataValidationFails() {
        // Given
        when(mockPersister.load(any())).thenReturn(successfulPersistResult);
        TestClass mockInstance = mock(TestClass.class);
        when(mockInstance.getDescription()).thenReturn("Test preferences");
        doThrow(new IllegalArgumentException("Invalid data...")).when(mockInstance).validate();
        // When
        new BasicPreferencesManager<>(mockInstance, mockPersister, mockStatusTrackerFactory, messageHandler);
        // Then
        String expectedValue = String.format(INVALID_PREFERENCES_TEMPLATE, mockInstance.getDescription(), "Invalid data...");
        assertEquals(1, messageHandler.getAllErrorMessages().size());
        assertEquals(expectedValue, messageHandler.getAllErrorMessages().get(0));
    }

    @Test
    @DisplayName("Should not persist TestClass if it did not change")
    void shouldNotPersistTestClass() {
        // Given
        when(mockPersister.load(any())).thenReturn(notFoundPersistResult);
        when(mockStatusTracker.hasChanged()).thenReturn(false);

        PreferencesManager<TestClass> sut = new BasicPreferencesManager<>(instance, mockPersister,
                mockStatusTrackerFactory, messageHandler);
        // When
        sut.persist();
        // Then
        verify(mockPersister, times(0)).save(instance);
    }

    @Test
    @DisplayName("Should persist TestClass if it changed")
    void shouldPersistTestClass() {
        // Given
        when(mockPersister.load(any())).thenReturn(notFoundPersistResult);
        when(mockPersister.save(instance)).thenReturn(successfulPersistResult);
        when(mockStatusTracker.hasChanged()).thenReturn(true);

        PreferencesManager<TestClass> sut = new BasicPreferencesManager<>(instance, mockPersister,
                mockStatusTrackerFactory, messageHandler);
        // When
        sut.persist();
        // Then
        verify(mockPersister, times(1)).save(instance);
    }


    @Test
    @DisplayName("Should warn when an error occurs during save")
    void shouldWarnWhenErrorOccursDuringSave() {
        // Given
        when(mockPersister.load(any())).thenReturn(notFoundPersistResult);
        when(mockPersister.save(instance)).thenReturn(errorPersistResult);
        when(mockStatusTracker.hasChanged()).thenReturn(true);

        PreferencesManager<TestClass> sut = new BasicPreferencesManager<>(instance, mockPersister,
                mockStatusTrackerFactory, messageHandler);
        // When
        sut.persist();
        // Then
        verify(mockPersister, times(1)).save(instance);
        assertEquals(1, messageHandler.getAllErrorMessages().size());
        assertEquals("I/O error", messageHandler.getAllErrorMessages().get(0));
    }
}
