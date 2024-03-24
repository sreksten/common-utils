package com.threeamigos.common.util.implementations.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Json Color Adapter unit test")
@Tag("unit")
@Tag("json")
class JsonColorAdapterUnitTest {

    private static final int RED = 0xAA;
    private static final int GREEN = 0xBB;
    private static final int BLUE = 0xCC;
    private static final int ALPHA = 0xDD;

    @Test
    @DisplayName("Should serialize a color")
    void shouldSerializeColor() {
        // Given
        Color color = new Color(RED, GREEN, BLUE, ALPHA);
        JsonColorAdapter sut = new JsonColorAdapter();
        // When
        JsonElement result = sut.serialize(color, null, null);
        // Then
        assertEquals("DDAABBCC", result.getAsString(), "Wrong serialized color representation");
    }

    @Test
    @DisplayName("Should deserialize a color")
    void shouldDeserializeColor() {
        // Given
        JsonPrimitive jsonPrimitive = new JsonPrimitive("DDAABBCC");
        JsonColorAdapter sut = new JsonColorAdapter();
        // When
        Color color = sut.deserialize(jsonPrimitive, null, null);
        // Then
        assertNotNull(color, "Deserialized color should not be null");
        assertThat(color, hasProperty("red", is(RED)));
        assertThat(color, hasProperty("green", is(GREEN)));
        assertThat(color, hasProperty("blue", is(BLUE)));
        assertThat(color, hasProperty("alpha", is(ALPHA)));
    }

    @ParameterizedTest(name = "Test #{index}: Value = <{0}>")
    @ValueSource(strings = {"abcde", "FFAABBXX", "-10000001", "        "})
    @DisplayName("Should throws JsonParseException if serialization is invalid")
    void shouldThrowJsonParseExceptionIfColorSerializationIsInvalid(String value) {
        // Given
        JsonColorAdapter sut = new JsonColorAdapter();
        // When
        JsonPrimitive invalidJsonPrimitive = new JsonPrimitive(value);
        // Then
        assertThrows(JsonParseException.class,
                () -> sut.deserialize(invalidJsonPrimitive, null, null));
    }

}
