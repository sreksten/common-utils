package com.threeamigos.common.util.implementations.persistence;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.implementations.json.JsonBuilderImpl;
import com.threeamigos.common.util.interfaces.json.Json;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.threeamigos.common.util.implementations.TestClass.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JsonStatusTracker unit test")
@Tag("unit")
@Tag("json")
@Tag("statusTracker")
class JsonStatusTrackerUnitTest {

    @Test
    @DisplayName("Should keep track of initial representation")
    void shouldKeepTrackOfInitialRepresentation() {
        // Given
        TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
        JsonStatusTracker<TestClass> sut = buildSystemUnderTest(instance);
        // When
        sut.loadInitialValues();
        // Then
        assertEquals(JSON_REPRESENTATION, sut.getEntityRepresentationAsString());
    }

    @Test
    @DisplayName("Should sense a change")
    void shouldSenseChange() {
        // Given
        TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
        JsonStatusTracker<TestClass> sut = buildSystemUnderTest(instance);
        sut.loadInitialValues();
        // When
        instance.setString("Another value");
        // Then
        assertTrue(sut.hasChanged());
    }

    private JsonStatusTracker<TestClass> buildSystemUnderTest(TestClass instance) {
        Json<TestClass> json = new JsonBuilderImpl().build(TestClass.class);
        JsonStatusTrackerFactory<TestClass> factory = new JsonStatusTrackerFactory<>(json);
        return (JsonStatusTracker<TestClass>) factory.buildStatusTracker(instance);
    }
}
