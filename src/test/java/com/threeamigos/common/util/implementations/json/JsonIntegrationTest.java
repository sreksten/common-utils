package com.threeamigos.common.util.implementations.json;

import com.google.gson.JsonParseException;
import com.threeamigos.common.util.interfaces.json.Json;
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

@DisplayName("Json integration test")
@Tag("integration")
@Tag("json")
class JsonIntegrationTest {

    private static final int RED = 0xAA;
    private static final int GREEN = 0xBB;
    private static final int BLUE = 0xCC;
    private static final int ALPHA = 0xDD;

    @Test
    @DisplayName("Should serialize an object with a color field")
    void shouldSerializeObject() {
        // Given
        Color color = new Color(RED, GREEN, BLUE, ALPHA);
        JsonColorClass entity = new JsonColorClass(color);
        Json<JsonColorClass> sut = buildSystemUnderTest();
        // When
        String serialized = sut.toJson(entity);
        // Then
        assertEquals("{\"color\":\"DDAABBCC\"}", serialized, "Wrong Json representation");
    }

    @Test
    @DisplayName("Should deserialize an object with a color field")
    void shouldDeserializeObject() {
        // Given
        String jsonRepresentation = "{\"color\":\"DDAABBCC\"}";
        Json<JsonColorClass> sut = buildSystemUnderTest();
        // When
        Color color = sut.fromJson(jsonRepresentation).getColor();
        // Then
        assertNotNull(color, "Color should not be null");
        assertThat(color, hasProperty("red", is(RED)));
        assertThat(color, hasProperty("green", is(GREEN)));
        assertThat(color, hasProperty("blue", is(BLUE)));
        assertThat(color, hasProperty("alpha", is(ALPHA)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcde", "FFAABBXX", "-10000001", "        "})
    @DisplayName("Should throws JsonParseException if representation is invalid")
    void shouldThrowJsonParseExceptionIfColorRepresentationIsInvalid(String value) {
        // Given
        Json<JsonColorClass> sut = buildSystemUnderTest();
        // When
        String invalidRepresentation = "{\"color\":\"" + value + "\"}";
        // Then
        assertThrows(JsonParseException.class,
                () -> sut.fromJson(invalidRepresentation));
    }

    private Json<JsonColorClass> buildSystemUnderTest() {
        return new JsonBuilderImpl()
                .registerAdapter(Color.class, new JsonColorAdapter())
                .build(JsonColorClass.class);
    }

    private static class JsonColorClass {
        private Color color;

        public JsonColorClass(Color color) {
            this.color = color;
        }

        public JsonColorClass() {
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }
    }
}
