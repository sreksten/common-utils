package com.threeamigos.common.util.implementations.persistence.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FilePersistResultImpl unit test")
class FilePersistResultImplTest {

    @Test
    @DisplayName("Should throw exception if null file description provided")
    void shouldThrowExceptionIfNullFileDescriptionProvided() {
        assertThrows(IllegalArgumentException.class, () -> new FilePersistResultImpl(null));
    }

    @Test
    @DisplayName("notFound() should throw an error if no file description provided")
    void notFoundShouldThrowAnErrorIfNoFileDescriptionProvided() {
        assertThrows(IllegalArgumentException.class, () -> FilePersistResultImpl.notFound(null));
    }

    @Test
    @DisplayName("cannotBeRead() should throw an exception if no file description provided")
    void cannotBeReadShouldThrowAnExceptionIfNoFileDescriptionProvided() {
        assertThrows(IllegalArgumentException.class, () -> FilePersistResultImpl.cannotBeRead(null));
    }

    @Test
    @DisplayName("fileNotWriteable() should throw an exception if no file description provided")
    void fileNotWriteableShouldThrowAnExceptionIfNoFileDescriptionProvided() {
        assertThrows(IllegalArgumentException.class, () -> FilePersistResultImpl.fileNotWriteable(null));
    }

    @Test
    @DisplayName("setFilename() shuold throw an exception if no filename provided")
    void setFilenameShouldThrowAnExceptionIfNoFilenameProvided() {
        assertThrows(IllegalArgumentException.class, () -> new FilePersistResultImpl().setFilename(null));
    }
}
