package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.implementations.json.JsonBuilderFactory;
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
import static org.mockito.Mockito.mock;

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
    void setup(@TempDir File targetDirectory) {
        exceptionHandler = new InMemoryMessageHandler();
        this.targetDirectory = targetDirectory;
        synchronized (System.getProperties()) {
            System.setProperty(RootPathProviderImpl.ROOT_PATH_DIRECTORY_PARAMETER, targetDirectory.getAbsolutePath());
            rootPathProvider = new RootPathProviderImpl(this, exceptionHandler);
        }
        json = JsonBuilderFactory.builder().build(TestClass.class);
    }

    @AfterEach
    void cleanup() {
        System.clearProperty(RootPathProviderImpl.ROOT_PATH_DIRECTORY_PARAMETER);
    }

    @Test
    @DisplayName("Should throw exception if filename is null")
    void shouldThrowsExceptionIfFilenameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new JsonFilePersister<>(null, ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json));
    }

    @Test
    @DisplayName("Should throw exception if entity description is null")
    void shouldThrowsExceptionIfEntityDescriptionIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new JsonFilePersister<>(FILENAME, null, rootPathProvider, exceptionHandler, json));
    }

    @Test
    @DisplayName("Should throw exception if rootPathProvider is null")
    void shouldThrowsExceptionIfRootPathProviderIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new JsonFilePersister<>(FILENAME, ENTITY_DESCRIPTION, null, exceptionHandler, json));
    }

    @Test
    @DisplayName("Should throw exception if ExceptionHandler is null")
    void shouldThrowsExceptionIfExceptionHandlerIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new JsonFilePersister<>(FILENAME, ENTITY_DESCRIPTION, rootPathProvider, null, json));
    }

    @Test
    @DisplayName("Should throw exception if Json is null")
    void shouldThrowsExceptionIfJsonIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new JsonFilePersister<>(FILENAME, ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, null));
    }

    @Test
    @DisplayName("Should build a filename adding extension")
    void shouldBuildFilenameAddingExtension() {
        // Given
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>(FILENAME, ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        // When
        String filename = sut.getNamePart();
        // Then
        assertEquals(FILENAME + FILENAME_EXTENSION, filename);
    }

    @Test
    @DisplayName("Should keep track of entity description")
    void shouldKeepsTrackOfEntityDescription() {
        // Given
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>(FILENAME, ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        // When
        String entityDescription = sut.getEntityDescription();
        // Then
        assertEquals(ENTITY_DESCRIPTION, entityDescription);
    }

    @Test
    @DisplayName("Should build complete filename")
    void shouldBuildCompleteFilename() {
        // Given
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>(FILENAME, ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        String expectedCompleteFilename = targetDirectory.getAbsolutePath() + File.separatorChar +
                "." + this.getClass().getPackage().getName() + File.separatorChar + sut.getNamePart();
        // When
        String filenameWithPath = sut.getFilenameWithPath();
        // Then
        assertEquals(expectedCompleteFilename, filenameWithPath);
    }

    @Test
    @DisplayName("Should throw exception if filename contains path separator")
    void shouldThrowsExceptionIfFilenameContainsPathSeparator() {
        // Given
        String illegalFilename = "file" + File.separatorChar + "name";
        // When
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>(illegalFilename, ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        // Then
        assertThrows(IllegalArgumentException.class, sut::getFilenameWithPath);
    }

    @Test
    @DisplayName("Should throw an exception if entity to save is null")
    void shouldThrowExceptionIfEntityToSaveIsNull() {
        // Given
        TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
        OutputStream outputStream = new ByteArrayOutputStream();
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>("filename", ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.save(outputStream, null));
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
    @DisplayName("Shuold throw an exception if entity to load is null")
    void shouldThrowExceptionIfEntityToLoadIsNull() {
        // Given
        TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
        InputStream inputStream = mock(InputStream.class);
        JsonFilePersister<TestClass> sut = new JsonFilePersister<>("filename", ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.load(inputStream, null));
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