package com.threeamigos.common.util.implementations.preferences.file;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.implementations.json.JsonBuilderImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.implementations.persistence.JsonStatusTrackerFactory;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import com.threeamigos.common.util.interfaces.persistence.PersistResult;
import com.threeamigos.common.util.interfaces.persistence.Persister;
import com.threeamigos.common.util.interfaces.persistence.StatusTrackerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

@DisplayName("JsonFilePreferencesManager integration test")
@Tag("json")
@Tag("integration")
@Tag("preferences")
class JsonFilePreferencesManagerIntegrationTest {

    private TestClass instance;
    private MessageHandler messageHandler;
    private Persister<TestClass> persister;
    private StatusTrackerFactory<TestClass> statusTrackerFactory;


    @BeforeEach
    void setup() {
        instance = new TestClass();

        messageHandler = new InMemoryMessageHandler();

        PersistResult notFound = mock(PersistResult.class);
        when(notFound.isSuccessful()).thenReturn(false);
        when(notFound.isNotFound()).thenReturn(true);

        PersistResult savedSuccessfully = mock(PersistResult.class);
        when(savedSuccessfully.isSuccessful()).thenReturn(true);

        persister = mock(Persister.class);
        when(persister.load(instance)).thenReturn(notFound);
        when(persister.save(instance)).thenReturn(savedSuccessfully);

        Json<TestClass> json = new JsonBuilderImpl().build(TestClass.class);
        statusTrackerFactory = new JsonStatusTrackerFactory<>(json);
    }

    @Test
    @DisplayName("Should not save preferences if unchanged")
    void shouldNotSavePreferencesIfUnchanged() {
        // Given
        JsonFilePreferencesManager<TestClass> sut = new JsonFilePreferencesManager<>(instance,
                statusTrackerFactory, persister, messageHandler);
        // When
        sut.persist();
        // Then
        verify(persister, times(0)).save(instance);
    }

    @Test
    @DisplayName("Should save preferences if changed")
    void shouldSavePreferencesIfChanged() {
        // Given
        JsonFilePreferencesManager<TestClass> sut = new JsonFilePreferencesManager<>(instance,
                statusTrackerFactory, persister, messageHandler);
        instance.setString("Changed value");
        // When
        sut.persist();
        // Then
        verify(persister, times(1)).save(instance);
    }

    @Test
    @DisplayName("Should not save preferences if set back to original value")
    void shouldNotSavePreferencesIfSetBackToOriginalValue() {
        // Given
        JsonFilePreferencesManager<TestClass> sut = new JsonFilePreferencesManager<>(instance,
                statusTrackerFactory, persister, messageHandler);
        instance.setString("Changed value");
        instance.loadDefaultValues();
        // When
        sut.persist();
        // Then
        verify(persister, times(0)).save(instance);
    }

}