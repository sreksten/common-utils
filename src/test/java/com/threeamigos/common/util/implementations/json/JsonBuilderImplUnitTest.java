package com.threeamigos.common.util.implementations.json;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.json.JsonAdapter;
import com.threeamigos.common.util.interfaces.json.JsonBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("JsonBuilderImpl unit test")
@Tag("unit")
@Tag("json")
class JsonBuilderImplUnitTest {

    @Test
    @DisplayName("Should throw an IllegalArgumentException if no class is provided")
    void shouldThrowIllegalArgumentIfNoClassIsProvided() {
        // Given
        JsonBuilder sut = new JsonBuilderImpl();
        // When
        Class<?> clazz = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.build(clazz));
    }

    @Test
    @DisplayName("Should build a Json")
    void shouldBuildAJson() {
        // Given
        JsonBuilder sut = new JsonBuilderImpl();
        // When
        Json<TestClass> json = sut.build(TestClass.class);
        // Then
        assertNotNull(json, "Produced Json should not be null");
    }

    @Nested
    @DisplayName("When registering an adapter")
    class WhenRegisteringAnAdapter {

        @Test
        @DisplayName("Should throw IllegalArgumentException if no class is provided")
        void shouldThrowIllegalArgumentIfNoClassIsProvided() {
            // Given
            JsonBuilder sut = new JsonBuilderImpl();
            // When
            Class<?> clazz = null;
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.registerAdapter(clazz, null));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if no adapter is provided")
        void shouldThrowIllegalArgumentIfNoAdapterIsProvided() {
            // Given
            JsonBuilder sut = new JsonBuilderImpl();
            // When
            Class<?> clazz = java.awt.Color.class;
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.registerAdapter(clazz, null));
        }

        @Test
        @DisplayName("Should accept an adapter")
        void shouldAcceptAnAdapter() {
            // Given
            JsonBuilder sut = new JsonBuilderImpl();
            // When
            JsonAdapter<java.awt.Color> adapter = mock(JsonAdapter.class);
            // Then
            assertDoesNotThrow(() -> sut.registerAdapter(java.awt.Color.class, adapter));
        }
    }

}