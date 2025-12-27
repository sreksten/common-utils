package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import com.threeamigos.common.util.interfaces.persistence.StatusTracker;
import com.threeamigos.common.util.interfaces.persistence.StatusTrackerFactory;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JsonFilePreferencesCollector unit test")
@Tag("unit")
@Tag("persistence")
@Tag("json")
class JsonFilePreferencesCollectorUnitTest {

    @Test
    @DisplayName("Should throw exception when null root path provider is provided")
    void shouldThrowExceptionWhenNullRootPathProviderIsProvided() {
        //Given
        MessageHandler messageHandler = mock(MessageHandler.class);
        Json<TestClass> json = mock(Json.class);
        StatusTrackerFactory<TestClass> statusTrackerFactory = mock(StatusTrackerFactory.class);
        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            new JsonFilePreferencesCollector<>(null, messageHandler, statusTrackerFactory, json);
        });
    }

    @Test
    @DisplayName("Should throw exception when null message handler is provided")
    void shouldThrowExceptionWhenNullMessageHandlerIsProvided() {
        //Given
        RootPathProvider rootPathProvider = mock(RootPathProvider.class);
        Json<TestClass> json = mock(Json.class);
        StatusTrackerFactory<TestClass> statusTrackerFactory = mock(StatusTrackerFactory.class);
        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            new JsonFilePreferencesCollector<>(rootPathProvider, null, statusTrackerFactory, json);
        });
    }

    @Test
    @DisplayName("Should throw exception when null status tracker factory is provided")
    void shouldThrowExceptionWhenNullStatusTrackerFactoryIsProvided() {
        //Given
        MessageHandler messageHandler = mock(MessageHandler.class);
        RootPathProvider rootPathProvider = mock(RootPathProvider.class);
        Json<TestClass> json = mock(Json.class);
        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            new JsonFilePreferencesCollector<>(rootPathProvider, messageHandler, null, json);
        });
    }

    @Test
    @DisplayName("Should throw exception when null json is provided")
    void shouldThrowExceptionWhenNullJsonIsProvided() {
        //Given
        MessageHandler messageHandler = mock(MessageHandler.class);
        RootPathProvider rootPathProvider = mock(RootPathProvider.class);
        StatusTrackerFactory<TestClass> statusTrackerFactory = mock(StatusTrackerFactory.class);
        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            new JsonFilePreferencesCollector<>(rootPathProvider, messageHandler, statusTrackerFactory, null);
        });

    }

    @Test
    @DisplayName("Should throw exception when adding a null Preferences")
    void shouldThrowExceptionWhenAddingANullPreferences() {
        //Given
        MessageHandler messageHandler = mock(MessageHandler.class);
        RootPathProvider rootPathProvider = mock(RootPathProvider.class);
        Json<TestClass> json = mock(Json.class);
        StatusTrackerFactory<TestClass> statusTrackerFactory = mock(StatusTrackerFactory.class);
        JsonFilePreferencesCollector<TestClass> sut = new JsonFilePreferencesCollector<>(rootPathProvider, messageHandler, statusTrackerFactory, json);
        // When
        TestClass preferences = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.add(preferences, "Test description"));
    }

    @Test
    @DisplayName("Should throw exception when adding Preferences with null description")
    void shouldThrowExceptionWhenAddingPreferencesWithNullDescription() {
        //Given
        MessageHandler messageHandler = mock(MessageHandler.class);
        RootPathProvider rootPathProvider = mock(RootPathProvider.class);
        Json<TestClass> json = mock(Json.class);
        StatusTrackerFactory<TestClass> statusTrackerFactory = mock(StatusTrackerFactory.class);
        JsonFilePreferencesCollector<TestClass> sut = new JsonFilePreferencesCollector<>(rootPathProvider, messageHandler, statusTrackerFactory, json);
        // When
        TestClass preferences = new TestClass();
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.add(preferences, null));
    }

    @Test
    @DisplayName("Should add preferences")
    @SuppressWarnings("unchecked")
    void shouldAddPreferences() {
        // Given
        MessageHandler messageHandler = mock(MessageHandler.class);
        RootPathProvider rootPathProvider = mock(RootPathProvider.class);
        Json<TestClass> json = mock(Json.class);
        StatusTrackerFactory<TestClass> statusTrackerFactory = mock(StatusTrackerFactory.class);
        StatusTracker<TestClass> statusTracker = mock(StatusTracker.class);
        when(statusTrackerFactory.buildStatusTracker(any())).thenReturn(statusTracker);
        TestClass instance = new TestClass();

        JsonFilePreferencesCollector<TestClass> sut = new JsonFilePreferencesCollector<>(rootPathProvider, messageHandler, statusTrackerFactory, json);
        // When
        sut.add(instance, "filename");
        // Then
        assertTrue(sut.isTracking(instance));
    }

    @Test
    @DisplayName("Should throw exception if isTracking is passed a null object")
    void isTrackingShouldThrowExceptionIfPreferencesIsNull() {
        // Given
        MessageHandler messageHandler = mock(MessageHandler.class);
        RootPathProvider rootPathProvider = mock(RootPathProvider.class);
        Json<TestClass> json = mock(Json.class);
        StatusTrackerFactory<TestClass> statusTrackerFactory = mock(StatusTrackerFactory.class);

        JsonFilePreferencesCollector<TestClass> sut = new JsonFilePreferencesCollector<>(rootPathProvider, messageHandler, statusTrackerFactory, json);
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.isTracking(null));
    }

    @Test
    @DisplayName("Should keep track of preferences")
    @SuppressWarnings("unchecked")
    void shouldKeepTrackOfPreferences() {
        // Given
        MessageHandler messageHandler = mock(MessageHandler.class);
        RootPathProvider rootPathProvider = mock(RootPathProvider.class);
        Json<TestClass> json = mock(Json.class);
        StatusTrackerFactory<TestClass> statusTrackerFactory = mock(StatusTrackerFactory.class);

        JsonFilePreferencesCollector<TestClass> sut = new JsonFilePreferencesCollector<>(rootPathProvider, messageHandler, statusTrackerFactory, json);
        // Then
        assertFalse(sut.isTracking(new TestClass()));
    }

}