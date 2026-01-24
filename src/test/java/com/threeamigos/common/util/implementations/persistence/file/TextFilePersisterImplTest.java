package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.implementations.persistence.file.rootpathprovider.RootPathProviderImpl;
import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import com.threeamigos.common.util.interfaces.persistence.Persister;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static com.threeamigos.common.util.implementations.TestClass.TEST_STRING;
import static com.threeamigos.common.util.implementations.TestClass.TEST_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TextFilePersister unit test")
@Tag("unit")
@Tag("persistence")
@Tag("text")
class TextFilePersisterImplTest {

    private static final String EXPECTED_VALUE =
            "STRING:" + TEST_STRING + System.lineSeparator() + "VALUE:" + TEST_VALUE + System.lineSeparator();

    private ExceptionHandler exceptionHandler;
    private RootPathProvider rootPathProvider;

    @BeforeEach
    void setup() {
        exceptionHandler = new InMemoryMessageHandler();
        rootPathProvider = new RootPathProviderImpl(this.getClass(), exceptionHandler);
    }


    @Test
    @DisplayName("Should save to OutputStream")
    void shouldSaveToFile() throws IOException {
        // Given
        TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
        TestClassTextFilePersister sut = new TestClassTextFilePersister(rootPathProvider, exceptionHandler);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // When
        sut.save(outputStream, instance);
        // Then
        assertEquals(EXPECTED_VALUE, outputStream.toString());
    }

    @Test
    @DisplayName("Should read from InputStream")
    void shouldReadFromInputStream() throws IOException {
        // Given
        TestClass instance = new TestClass();
        TestClassTextFilePersister sut = new TestClassTextFilePersister(rootPathProvider, exceptionHandler);
        InputStream inputStream = new ByteArrayInputStream(EXPECTED_VALUE.getBytes(StandardCharsets.UTF_8));
        // When
        sut.load(inputStream, instance);
        // Then
        assertThat(instance, hasProperty("string", is(TEST_STRING)));
        assertThat(instance, hasProperty("value", is(TEST_VALUE)));
    }

    private static class TestClassTextFilePersister extends TextFilePersister<TestClass> implements Persister<TestClass> {

        /**
         * @param rootPathProvider to provide the root path where the entity should be persisted
         * @param exceptionHandler to inform the end user if any error arises
         */
        protected TestClassTextFilePersister(RootPathProvider rootPathProvider, ExceptionHandler exceptionHandler) {
            super(rootPathProvider, exceptionHandler);
        }

        @Override
        protected String getEntityDescription() {
            return "TestClass";
        }

        @Override
        protected String getNamePart() {
            return "testClass";
        }

        @Override
        protected void loadFromText(@Nonnull BufferedReader reader, @Nonnull TestClass entity) throws IllegalArgumentException {
            reader.lines().forEach(line -> {
                if (line.startsWith("STRING:")) {
                    entity.setString(line.substring(7));
                } else if (line.startsWith("VALUE:")) {
                    entity.setValue(Integer.parseInt(line.substring(6)));
                }
            });
        }

        @Override
        protected void saveAsText(@Nonnull PrintWriter printWriter, @Nonnull TestClass entity) throws IllegalArgumentException {
            printWriter.print("STRING:");
            printWriter.println(entity.getString());
            printWriter.print("VALUE:");
            printWriter.println(entity.getValue());
        }
    }

}