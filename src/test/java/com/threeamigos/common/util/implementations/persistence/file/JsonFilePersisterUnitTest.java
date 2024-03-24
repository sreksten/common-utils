package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.implementations.json.JsonBuilderImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.implementations.persistence.file.rootpathprovider.RootPathProviderImpl;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static com.threeamigos.common.util.implementations.TestClass.*;
import static com.threeamigos.common.util.implementations.persistence.file.JsonFilePersister.FILENAME_EXTENSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JsonFilePersister unit test")
@Tag("unit")
@Tag("persistence")
@Tag("json")
class JsonFilePersisterUnitTest {

    private static final String FILENAME = "filename";
    private static final String ENTITY_DESCRIPTION = "entityDescription";

    private File targetDirectory;
    private RootPathProvider rootPathProvider;
    private Json<TestClass> json;

    private ExceptionHandler exceptionHandler;

    @BeforeEach
    void setup(@TempDir File targetDirectory) throws IOException {
        exceptionHandler = new InMemoryMessageHandler();
        this.targetDirectory = targetDirectory;
        synchronized (System.getProperties()) {
            System.setProperty(RootPathProviderImpl.ROOT_PATH_DIRECTORY_PARAMETER, targetDirectory.getAbsolutePath());
            rootPathProvider = new RootPathProviderImpl(this, exceptionHandler);
        }
        json = new JsonBuilderImpl().build(TestClass.class);
    }

    @AfterEach
    void cleanup() {
        System.clearProperty(RootPathProviderImpl.ROOT_PATH_DIRECTORY_PARAMETER);
    }

    @Test
    @DisplayName("Should build a filename adding extension")
    void shouldBuildFilenameAddingExtension() throws IOException {
        // Given
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>(FILENAME, ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        // When
        String filename = sut.getNamePart();
        // Then
        assertEquals(FILENAME + FILENAME_EXTENSION, filename);
    }

    @Test
    @DisplayName("Should keep track of entity description")
    void shouldKeepsTrackOfEntityDescription() throws IOException {
        // Given
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>(FILENAME, ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        // When
        String entityDescription = sut.getEntityDescription();
        // Then
        assertEquals(ENTITY_DESCRIPTION, entityDescription);
    }

    @Test
    @DisplayName("Should build complete filename")
    void shouldBuildCompleteFilename() throws IOException {
        // Given
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>(FILENAME, ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        String expectedCompleteFilename = targetDirectory.getAbsolutePath() + File.separatorChar +
                "." + this.getClass().getPackageName() + File.separatorChar + sut.getNamePart();
        // When
        String filenameWithPath = sut.getFilenameWithPath();
        // Then
        assertEquals(expectedCompleteFilename, filenameWithPath);
    }

    @Test
    @DisplayName("Should throw exception if filename contains path separator")
    void shouldThrowsExceptionIfFilenameContainsPathSeparator() throws IOException {
        // Given
        String illegalFilename = "file" + File.separatorChar + "name";
        // When
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>(illegalFilename, ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        // Then
        assertThrows(IllegalArgumentException.class, sut::getFilenameWithPath);
    }

    @Test
    @DisplayName("Should serialize an entity to OutputStream")
    void shouldSerializeToOutputStream() throws IOException {
        // Given
        TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
        OutputStream outputStream = new ByteArrayOutputStream();
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>("filename", ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        // When
        sut.save(outputStream, instance);
        // Then
        assertEquals(TestClass.JSON_REPRESENTATION, outputStream.toString());
    }

    @Test
    @DisplayName("Should deserialize an entity from InputStream")
    void shouldDeserializeEntityFromInputStream() throws IOException {
        // Given
        TestClass instance = new TestClass();
        InputStream inputStream = new ByteArrayInputStream(JSON_REPRESENTATION.getBytes(StandardCharsets.UTF_8));
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>("filename", ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        // When
        sut.load(inputStream, instance);
        // Then
        assertThat(instance, hasProperty("string", is(TEST_STRING)));
        assertThat(instance, hasProperty("value", is(TEST_VALUE)));
    }
}