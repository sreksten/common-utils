package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.implementations.TestClass;
import com.threeamigos.common.util.implementations.json.JsonBuilderImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.implementations.persistence.file.rootpathprovider.RootPathProviderImpl;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import com.threeamigos.common.util.interfaces.persistence.PersistResult;
import com.threeamigos.common.util.interfaces.persistence.file.FilePersistResult;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.attribute.PosixFilePermission;

import static com.threeamigos.common.util.implementations.TestClass.*;
import static com.threeamigos.common.util.implementations.persistence.file.FileUtils.applyFileAttributes;
import static com.threeamigos.common.util.implementations.persistence.file.JsonFilePersister.FILENAME_EXTENSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonFilePersister integration test")
@Tag("integration")
@Tag("persistence")
@Tag("json")
class JsonFilePersisterIntegrationTest {

    private static final String ENTITY_DESCRIPTION = "entityDescription";

    private ExceptionHandler exceptionHandler;

    private File temporaryDirectory;

    @BeforeEach
    void setup(@TempDir File temporaryDirectory) {
        exceptionHandler = new InMemoryMessageHandler();
        this.temporaryDirectory = temporaryDirectory;
    }

    @AfterEach
    void cleanup() {
        System.clearProperty(RootPathProviderImpl.ROOT_PATH_DIRECTORY_PARAMETER);
    }

    @Test
    @DisplayName("Should save a file to target directory")
    void shouldSaveFileToTargetDirectory() throws IOException {
        // Given
        TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
        JsonFilePersister<TestClass> sut = createSystemUnderTest(temporaryDirectory);
        // When
        PersistResult result = sut.save(instance);
        // Then
        assertThat(result, hasProperty("successful", is(true)));
        assertEquals(TestClass.JSON_REPRESENTATION, FileUtils.readTextFileContent(new File(sut.getFilenameWithPath())));
        assertTrue(((FilePersistResult) result).getFilename().endsWith("filename.json"));
    }

    @Test
    @DisplayName("Should read a file from target directory")
    void shouldReadFileFromTargetDirectory() throws IOException {
        // Given
        TestClass entity = new TestClass();
        JsonFilePersister<TestClass> sut = createSystemUnderTest(temporaryDirectory);
        createFileWithJsonRepresentation(sut.getFilenameWithPath());
        // When
        PersistResult persistResult = sut.load(entity);
        // Then
        assertThat(persistResult, hasProperty("successful", is(true)));
        assertThat(persistResult, hasProperty("notFound", is(false)));
        assertThat(persistResult, hasProperty("error", nullValue()));
        assertThat(entity, hasProperty("string", is(TEST_STRING)));
        assertThat(entity, hasProperty("value", is(TEST_VALUE)));
    }

    @Test
    @DisplayName("Should not be successful when reading a corrupt file from target directory")
    void shouldNotBeSuccessfulWhenReadingCorruptFileFromTargetDirectory() throws IOException {
        // Given
        TestClass entity = new TestClass();
        JsonFilePersister<TestClass> sut = createSystemUnderTest(temporaryDirectory);
        createCorruptedFileWithJsonRepresentation(sut.getFilenameWithPath());
        // When
        PersistResult persistResult = sut.load(entity);
        // Then
        assertThat(persistResult, hasProperty("successful", is(false)));
        assertThat(persistResult, hasProperty("notFound", is(false)));
        assertThat(persistResult, hasProperty("error", notNullValue()));
    }

    @Test
    @DisplayName("Should report file not found if file is not present in target directory")
    void shouldReportFileNotFoundIfFileNotPresentInTargetDirectory() throws IOException {
        // Given
        TestClass entity = new TestClass();
        JsonFilePersister<TestClass> sut = createSystemUnderTest(temporaryDirectory);
        // When
        PersistResult persistResult = sut.load(entity);
        // Then
        assertThat(persistResult, hasProperty("successful", is(false)));
        assertThat(persistResult, hasProperty("notFound", is(true)));
        assertThat(persistResult, hasProperty("error", is(FilePersistResultImpl.notFound(ENTITY_DESCRIPTION).getError())));
    }

    @Test
    @DisplayName("Should not be successful if target directory is not readable")
    void shouldNotBeSuccessfulIfTargetDirectoryIsNotReadable() throws IOException {
        // Given
        TestClass entity = new TestClass();
        applyFileAttributes(temporaryDirectory, PosixFilePermission.OWNER_READ);
        JsonFilePersister<TestClass> sut = createSystemUnderTest(temporaryDirectory);
        // When
        PersistResult persistResult = sut.load(entity);
        // Then
        assertThat(persistResult, hasProperty("successful", is(false)));
        assertThat(persistResult, hasProperty("error", is(FilePersistResultImpl.pathNotAccessible().getError())));
    }

    @Test
    @DisplayName("Should not be successful if target file is not accessible for loading")
    void checkTargetFileIsNotAccessibleForLoading() throws IOException {
        // Given
        createUnreadableFileIn(temporaryDirectory);
        JsonFilePersister<TestClass> sut = createSystemUnderTest(temporaryDirectory);
        TestClass entity = new TestClass();
        // When
        PersistResult persistResult = sut.load(entity);
        // Then
        assertThat(persistResult, hasProperty("successful", is(false)));
        assertThat(persistResult, hasProperty("error", is(FilePersistResultImpl.cannotBeRead(ENTITY_DESCRIPTION).getError())));
    }

    private void createUnreadableFileIn(File temporaryDirectory) throws IOException {
        createFileIn(temporaryDirectory, PosixFilePermission.OWNER_WRITE);
    }

    private void createUnwritableFileIn(File temporaryDirectory) throws IOException {
        createFileIn(temporaryDirectory, PosixFilePermission.OWNER_READ);
    }

    private void createFileIn(File temporaryDirectory, PosixFilePermission permission) throws IOException {
        // Create a subdirectory within
        File subdirectory = new File(temporaryDirectory.getAbsolutePath() + File.separatorChar + "." + this.getClass().getPackageName());
        if (!subdirectory.mkdirs()) {
            fail("Can't create subdirectories");
        }
        String completeFilename = subdirectory.getAbsolutePath() + File.separatorChar + "filename" + FILENAME_EXTENSION;
        File file = new File(completeFilename);
        if (!file.createNewFile()) {
            fail("Can't create file " + completeFilename);
        }
        applyFileAttributes(file, permission);
    }

    @Test
    @DisplayName("Target file is not accessible for saving")
    void checkTargetFileIsNotAccessibleForSaving() throws IOException {
        // Given
        createUnwritableFileIn(temporaryDirectory);
        JsonFilePersister<TestClass> sut = createSystemUnderTest(temporaryDirectory);
        TestClass entity = new TestClass();
        // When
        PersistResult persistResult = sut.save(entity);
        // Then
        assertThat(persistResult, hasProperty("successful", is(false)));
        assertThat(persistResult, hasProperty("error", is(FilePersistResultImpl.fileNotWriteable(ENTITY_DESCRIPTION).getError())));
    }

    @Test
    @DisplayName("Should not be successful if target directory is not writeable")
    void shouldNotBeSuccessfulIfTargetDirectoryIsNotWriteable() throws IOException {
        // Given
        TestClass entity = new TestClass();
        applyFileAttributes(temporaryDirectory, PosixFilePermission.OWNER_READ);
        JsonFilePersister<TestClass> sut = createSystemUnderTest(temporaryDirectory);
        // When
        PersistResult persistResult = sut.save(entity);
        // Then
        assertThat(persistResult, hasProperty("successful", is(false)));
        assertThat(persistResult, hasProperty("error", is(FilePersistResultImpl.pathNotAccessible().getError())));
    }

    @Nested
    @DisplayName("When disk fails")
    class DiskFail {

        private RootPathProvider rootPathProvider;
        private Json<TestClass> json;

        @BeforeEach
        void setup() throws IOException {
            synchronized (System.getProperties()) {
                System.setProperty(RootPathProviderImpl.ROOT_PATH_DIRECTORY_PARAMETER, temporaryDirectory.getAbsolutePath());
                rootPathProvider = new RootPathProviderImpl(this, exceptionHandler);
            }
            json = new JsonBuilderImpl().build(TestClass.class);
        }

        @Test
        @DisplayName("Should not be successful when saving to target directory")
        void shouldNotBeSuccessfulWhenFailingToSaveFileToTargetDirectory() throws IOException {
            // Given
            TestClass instance = new TestClass(TEST_STRING, TEST_VALUE);
            JsonFilePersister<TestClass> sut = new FailingJsonFilePersister<>("filename-write", ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
            // When
            PersistResult result = sut.save(instance);
            // Then
            assertFalse(result.isSuccessful());
            assertEquals(FileUtils.DISK_FULL_OR_ANY_OTHER_CAUSE, result.getError());
        }

        @Test
        @DisplayName("Should not be successful when reading an existing file from target directory")
        void shouldNotBeSuccessfulWhenFailingToReadExistingFileFromTargetDirectory() throws IOException {
            // Given
            TestClass entity = new TestClass();
            JsonFilePersister<TestClass> sut = new FailingJsonFilePersister<>("corrupted-filename", ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
            createFileWithJsonRepresentation(sut.getFilenameWithPath());
            // When
            PersistResult persistResult = sut.load(entity);
            // Then
            assertFalse(persistResult.isSuccessful());
            assertFalse(persistResult.isNotFound());
            assertEquals(FileUtils.FILE_CORRUPTED_OR_ANY_OTHER_CAUSE, persistResult.getError());
        }

        private static class FailingJsonFilePersister<T> extends JsonFilePersister<T> {

            public FailingJsonFilePersister(String filename, String entityDescription, RootPathProvider rootPathProvider, ExceptionHandler exceptionHandler, Json<T> json) {
                super(filename, entityDescription, rootPathProvider, exceptionHandler, json);
            }

            @Override
            protected InputStream createInputStream(String filename) throws IOException {
                return FileUtils.createFailingInputStream();
            }

            @Override
            protected OutputStream createOutputStream(String filename) throws IOException {
                return FileUtils.createFailingOutputStream();
            }
        }
    }

    private JsonFilePersister<TestClass> createSystemUnderTest(File directory) {
        RootPathProvider rootPathProvider;
        synchronized (System.getProperties()) {
            System.setProperty(RootPathProviderImpl.ROOT_PATH_DIRECTORY_PARAMETER, directory.getAbsolutePath());
            rootPathProvider = new RootPathProviderImpl(this, exceptionHandler);
        }
        Json<TestClass> json = new JsonBuilderImpl().build(TestClass.class);
        return new JsonFilePersister<>("filename", ENTITY_DESCRIPTION, rootPathProvider, exceptionHandler, json);
    }

    private void createFileWithJsonRepresentation(String filename) throws IOException {
        File file = new File(filename);
        try (PrintStream stream = new PrintStream(new FileOutputStream(file))) {
            stream.println(JSON_REPRESENTATION);
        }
    }

    private void createCorruptedFileWithJsonRepresentation(String filename) throws IOException {
        File file = new File(filename);
        try (PrintStream stream = new PrintStream(new FileOutputStream(file))) {
            stream.println(JSON_REPRESENTATION.substring(0, JSON_REPRESENTATION.length() / 2));
        }
    }
}