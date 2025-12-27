package com.threeamigos.common.util.implementations.json;

import com.google.gson.JsonSyntaxException;
import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.json.JsonAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static com.threeamigos.common.util.implementations.TestClass.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JsonImpl unit test")
@Tag("unit")
@Tag("json")
class JsonImplUnitTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Should throw exception if no class is provided in constructor")
        void shouldThrowExceptionIfNoClassIsProvided() {
            // Given
            Class<TestClass> instance = null;
            Map<Class<?>, JsonAdapter<?>> typeAdapters = Collections.emptyMap();
            // Then
            assertThrows(IllegalArgumentException.class, () -> new JsonImpl<>(instance, typeAdapters));
        }

        @Test
        @DisplayName("Should throw exception if no map is provided in constructor")
        void shouldThrowExceptionIfNoMapIsProvided() {
            // Given
            Class<TestClass> instance = TestClass.class;
            Map<Class<?>, JsonAdapter<?>> typeAdapters = null;
            // Then
            assertThrows(IllegalArgumentException.class, () -> new JsonImpl<>(instance, typeAdapters));
        }

    }

    @Nested
    @DisplayName("toJson(instance)")
    class ToJsonObject {

        @Test
        @DisplayName("Should throw Exception if a null object is passed")
        void shouldThrowExceptionIfNullObjectIsPassed() {
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.toJson(null));
        }

        @Test
        @DisplayName("Should produce a valid JSON as a String")
        void shouldProduceValidJsonToString() {
            // Given
            TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // When
            String jsonRepresentation = sut.toJson(instance);
            // Then
            assertEquals(JSON_REPRESENTATION, jsonRepresentation, "Wrong JSON representation");
        }
    }

    @Nested
    @DisplayName("toJson(instance, OutputStream)")
    class ToJsonOutputStream {

        @Test
        @DisplayName("Should throw Exception if a null object is passed")
        void shouldThrowExceptionIfNullObjectIsPassed() {
            // Given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TestClass instance = null;
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.toJson(instance, outputStream));
        }

        @Test
        @DisplayName("Should throw Exception if a null OutputStream is passed")
        void shouldThrowExceptionIfNullOutputStreamIsPassed() {
            // Given
            ByteArrayOutputStream outputStream = null;
            TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.toJson(instance, outputStream));
        }

        @Test
        @DisplayName("Should produce a valid JSON to an OutputStream")
        void shouldProduceValidJsonToOutputStream() throws IOException {
            // Given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // When
            sut.toJson(new TestClass(TEST_STRING, TEST_VALUE), outputStream);
            // Then
            assertEquals(JSON_REPRESENTATION, outputStream.toString(), "Wrong Json representation");
        }

    }

    @Nested
    @DisplayName("fromJson(String)")
    class FromJsonString {

        @Test
        @DisplayName("Should throw Exception if null JSON is passed")
        void shouldThrowExceptionIfNoJsonIsPassed() {
            // Given
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            String json = null;
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.fromJson(json));
        }

        @Test
        @DisplayName("Should throw exception if an invalid JSON is passed")
        void shouldThrowExceptionIfInvalidJsonIsPassed() {
            // Given
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // Then
            assertThrows(JsonSyntaxException.class, () -> sut.fromJson(INVALID_JSON_REPRESENTATION));
        }

        @Test
        @DisplayName("Should retrieve an object from a valid JSON string")
        void shouldRetrieveObjectFromValidJsonString() {
            // Given
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // When
            TestClass instance = sut.fromJson(JSON_REPRESENTATION);
            // Then
            assertThat(instance, hasProperty("string", is(TEST_STRING)));
            assertThat(instance, hasProperty("value", is(TEST_VALUE)));
        }
    }

    @Nested
    @DisplayName("fromJson(InputStream)")
    class FromJsonInputStream {

        @Test
        @DisplayName("Should throw Exception if null InputStream is passed")
        void shouldThrowExceptionIfNoInputStreamIsPassed() {
            // Given
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            InputStream inputStream = null;
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.fromJson(inputStream));
        }

        @Test
        @DisplayName("Should throw Exception if an invalid InputStream is passed")
        void shouldThrowExceptionIfAnInvalidInputStreamIsPassed() {
            // Given
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            InputStream inputStream = new ByteArrayInputStream(INVALID_JSON_REPRESENTATION.getBytes(StandardCharsets.UTF_8));
            // Then
            assertThrows(JsonSyntaxException.class, () -> sut.fromJson(inputStream));
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
    }

    @Nested
    @DisplayName("fromJson(String, instance)")
    class FromJsonStringAndInstance {

        @Test
        @DisplayName("Should throw an exception if no JSON is passed")
        void shouldThrowExceptionIfNoJsonIsPassed() {
            // Given
            TestClass instance = new TestClass();
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            String json = null;
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.fromJson(json, instance));
        }

        @Test
        @DisplayName("Should throw an exception if no instance is passed")
        void shouldThrowExceptionIfNoInstanceIsPassed() {
            // Given
            TestClass instance = null;
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.fromJson(JSON_REPRESENTATION, instance));
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
    }

    @Nested
    @DisplayName("fromJson(InputStream, instance)")
    class FromJsonInputStreamAndInstance {

        @Test
        @DisplayName("Should throw an exception if no InputStream is passed")
        void shouldThrowExceptionIfNoInputStreamIsPassed() {
            // Given
            TestClass instance = new TestClass();
            InputStream json = null;
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.fromJson(json, instance));
        }

        @Test
        @DisplayName("Should throw an exception if no instance is passed")
        void shouldThrowExceptionIfNoInstanceIsPassed() {
            // Given
            TestClass instance = null;
            InputStream inputStream = new ByteArrayInputStream(JSON_REPRESENTATION.getBytes(StandardCharsets.UTF_8));
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // Then
            assertThrows(IllegalArgumentException.class, () -> sut.fromJson(inputStream, instance));
        }

        @Test
        @DisplayName("Should throw an Exception if an invalid InputStream is passed")
        void shouldThrowExceptionIfAnInvalidInputStreamIsPassed() throws IOException {
            // Given
            TestClass instance = new TestClass();
            InputStream inputStream = new ByteArrayInputStream(INVALID_JSON_REPRESENTATION.getBytes(StandardCharsets.UTF_8));
            Json<TestClass> sut = new JsonImpl<>(TestClass.class, Collections.emptyMap());
            // Then
            assertThrows(JsonSyntaxException.class, () -> sut.fromJson(inputStream, instance));
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
}