package com.threeamigos.common.util.implementations.persistence;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.persistence.StatusTracker;
import com.threeamigos.common.util.interfaces.persistence.StatusTrackerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@DisplayName("JsonStatusTrackerFactory unit test")
@Tag("unit")
@Tag("json")
@Tag("statusTracker")
class JsonStatusTrackerFactoryUnitTest {

    @Test
    @DisplayName("Should return a Status Tracker")
    void shouldReturnStatusTracker() {
        // Given
        TestClass entity = new TestClass(TestClass.TEST_STRING, TestClass.TEST_VALUE);
        @SuppressWarnings("unchecked") Json<TestClass> json = (Json<TestClass>) mock(Json.class);
        StatusTrackerFactory<TestClass> sut = new JsonStatusTrackerFactory<>(json);
        // When
        StatusTracker<TestClass> statusTracker = sut.buildStatusTracker(entity);
        // Then
        assertNotNull(statusTracker);
    }
}