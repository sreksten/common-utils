package com.threeamigos.common.util.implementations.json;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.json.JsonBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("JsonBuilderImpl unit test")
@Tag("unit")
@Tag("json")
class JsonBuilderImplUnitTest {

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

}