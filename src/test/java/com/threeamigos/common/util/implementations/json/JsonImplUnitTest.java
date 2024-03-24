package com.threeamigos.common.util.implementations.json;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.interfaces.json.Json;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static com.threeamigos.common.util.implementations.TestClass.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("JsonImpl unit test")
@Tag("unit")
@Tag("json")
class JsonImplUnitTest {

    @Test
    @DisplayName("Should produce a valid Json as a String")
    void shouldProduceValidJsonToString() {
        // Given
        TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
        Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
        // When
        String jsonRepresentation = sut.toJson(instance);
        // Then
        assertEquals(JSON_REPRESENTATION, jsonRepresentation, "Wrong Json representation");
    }

    @Test
    @DisplayName("Should produce a valid Json to an OutputStream")
    void shouldProduceValidJsonToOutputStream() throws IOException {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
        // When
        sut.toJson(new TestClass(TEST_STRING, TEST_VALUE), outputStream);
        // Then
        assertEquals(JSON_REPRESENTATION, outputStream.toString(), "Wrong Json representation");
    }

    @Test
    @DisplayName("Should retrieve an object from a valid Json string")
    void shouldRetrieveObjectFromValidJsonString() {
        // Given
        Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
        // When
        TestClass instance = sut.fromJson(JSON_REPRESENTATION);
        // Then
        assertThat(instance, hasProperty("string", is(TEST_STRING)));
        assertThat(instance, hasProperty("value", is(TEST_VALUE)));
    }

    @Test
    @DisplayName("Should populate an object from a valid Json string")
    void shouldPopulateObjectFromValidJsonString() {
        // Given
        TestClass instance = new TestClass();
        Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
        // When
        sut.fromJson(JSON_REPRESENTATION, instance);
        // Then
        assertThat(instance, hasProperty("string", is(TEST_STRING)));
        assertThat(instance, hasProperty("value", is(TEST_VALUE)));
    }

    @Test
    @DisplayName("Should retrieve an object from a valid Json InputStream")
    void shouldRetrieveObjectFromValidJsonInputStream() throws IOException {
        // Given
        InputStream inputStream = new ByteArrayInputStream(JSON_REPRESENTATION.getBytes(StandardCharsets.UTF_8));
        Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
        // When
        TestClass instance = sut.fromJson(inputStream);
        // Then
        assertThat(instance, hasProperty("string", is(TEST_STRING)));
        assertThat(instance, hasProperty("value", is(TEST_VALUE)));
    }

    @Test
    @DisplayName("Should populate an object from a valid Json InputStream")
    void shouldPopulateObjectFromValidJsonInputStream() throws IOException {
        // Given
        TestClass instance = new TestClass();
        InputStream inputStream = new ByteArrayInputStream(JSON_REPRESENTATION.getBytes(StandardCharsets.UTF_8));
        Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
        // When
        sut.fromJson(inputStream, instance);
        // Then
        assertThat(instance, hasProperty("string", is(TEST_STRING)));
        assertThat(instance, hasProperty("value", is(TEST_VALUE)));
    }
}