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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JsonFilePreferencesCollector unit test")
@Tag("unit")
@Tag("persistence")
@Tag("json")
class JsonFilePreferencesCollectorUnitTest {

    @Test
    @DisplayName("Should add preferences")
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
    @DisplayName("Should keep track of preferences")
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