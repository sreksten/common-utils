package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.persistence.PersistResultReturnCodeEnum;
import com.threeamigos.common.util.interfaces.persistence.file.FilePersistResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FilePersistResultImpl unit test")
class FilePersistResultImplTest {

    @Test
    @DisplayName("Should throw exception if null file description provided")
    void shouldThrowExceptionIfNullFileDescriptionProvided() {
        // Given
        String fileDescription = null;
        String filename = "filename";
        // Then
        assertThrows(IllegalArgumentException.class, () -> new FilePersistResultImpl(fileDescription, filename));
    }

    @Test
    @DisplayName("Should throw exception if null filename provided")
    void shouldThrowExceptionIfNullFilenameProvided() {
        // Given
        String fileDescription = "file description";
        String filename = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> new FilePersistResultImpl(fileDescription, filename));
    }

    @Test
    @DisplayName("Constructor should retain file description")
    void constructorShouldRetainFileDescription() {
        // Given
        String fileDescription = "file description";
        String filename = "filename";
        // When
        FilePersistResultImpl sut = new FilePersistResultImpl(fileDescription, filename);
        // Then
        assertEquals(fileDescription, sut.getDescription());
    }

    @Test
    @DisplayName("Constructor should retain filename")
    void constructorShouldRetainFilename() {
        // Given
        String fileDescription = "file description";
        String filename = "filename";
        // When
        FilePersistResultImpl sut = new FilePersistResultImpl(fileDescription, filename);
        // Then
        assertEquals(filename, sut.getFilename());
    }

    @Test
    @DisplayName("successful() should return a successful result")
    void successfulShouldReturnASuccessfulResult() {
        // Given
        FilePersistResult result = FilePersistResultBuilder.successful("file description", "filename");
        // When
        boolean successful = result.isSuccessful();
        // Then
        assertTrue(successful);
        assertEquals(PersistResultReturnCodeEnum.SUCCESSFUL, result.getReturnCode());
    }

    @Test
    @DisplayName("notFound() should return a not found result")
    void notFoundShouldReturnANotFoundResult() {
        // Given
        FilePersistResult result = FilePersistResultBuilder.notFound("file description", "filename");
        // When
        boolean notFound = result.isNotFound();
        // Then
        assertTrue(notFound);
        assertEquals(PersistResultReturnCodeEnum.NOT_FOUND, result.getReturnCode());
    }

    @Test
    @DisplayName("cannotBeRead() should return an unsuccessful result")
    void cannotBeReadShouldReturnACannotBeReadResult() {
        // Given
        FilePersistResult result = FilePersistResultBuilder.notReadable("file description", "filename");
        // When
        boolean successful = result.isSuccessful();
        // Then
        assertFalse(successful);
        assertEquals(PersistResultReturnCodeEnum.CANNOT_BE_READ, result.getReturnCode());
    }

    @Test
    @DisplayName("fileNotWriteable() should return an unsuccessful result")
    void fileNotWriteableShouldReturnAFileNotWriteableResult() {
        // Given
        FilePersistResult result = FilePersistResultBuilder.notWriteable("file description", "filename");
        // When
        boolean successful = result.isSuccessful();
        // Then
        assertFalse(successful);
        assertEquals(PersistResultReturnCodeEnum.CANNOT_BE_WRITTEN, result.getReturnCode());
    }

    @Test
    @DisplayName("error() should throw an exception if no message is provided")
    void errorShouldThrowAnExceptionIfNoMessageIsProvided() {
        // Given
        String error = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> FilePersistResultBuilder.error("file description", "filename", error));
    }

    @Test
    @DisplayName("error() should return an unsuccessful result")
    void errorShouldReturnAnErrorResult() {
        // Given
        FilePersistResult result = FilePersistResultBuilder.error("file description", "filename", "error");
        // When
        boolean successful = result.isSuccessful();
        // Then
        assertFalse(successful);
        assertEquals(PersistResultReturnCodeEnum.ERROR, result.getReturnCode());
    }

    @Test
    @DisplayName("getProblemOccurredForFileDescription() should contain the entity description")
    void getProblemOccurredForFileDescriptionShouldContainEntityDescription() {
        // Given
        FilePersistResult result = FilePersistResultBuilder.error("file description", "filename", "error");
        // When
        String problemOccurredForFileDescription = result.getProblemOccurredForFileDescription();
        // Then
        assertEquals("A problem occurred for file description:", problemOccurredForFileDescription);
    }
}
