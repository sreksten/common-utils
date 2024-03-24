package com.threeamigos.common.util.implementations.messagehandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("InMemoryMessageHandler unit test")
@Tag("unit")
@Tag("messageHandler")
class InMemoryMessageHandlerUnitTest {

    private static final String FIRST_MESSAGE = "First message";
    private static final String SECOND_MESSAGE = "Second message";

    @Test
    @DisplayName("Should store all info messages")
    void shouldStoreAllInfoMessages() {
        //Given
        InMemoryMessageHandler sut = new InMemoryMessageHandler();
        //When
        sut.handleInfoMessage(FIRST_MESSAGE);
        sut.handleInfoMessage(SECOND_MESSAGE);
        //Then
        assertEquals(2, sut.getAllMessages().size(), "Wrong all messages size");
        assertEquals(2, sut.getAllInfoMessages().size(), "Wrong info messages size");
        assertEquals(0, sut.getAllWarnMessages().size(), "Wrong warn messages size");
        assertEquals(0, sut.getAllErrorMessages().size(), "Wrong error messages size");
        assertEquals(0, sut.getAllExceptionMessages().size(), "Wrong exception messages size");
        assertEquals(0, sut.getAllExceptions().size(), "Wrong exceptions size");
        assertEquals(SECOND_MESSAGE, sut.getLastMessage(), "Wrong last message");
    }

    @Test
    @DisplayName("Should store all warn messages")
    void shouldStoreAllWarnMessages() {
        //Given
        InMemoryMessageHandler sut = new InMemoryMessageHandler();
        //When
        sut.handleWarnMessage(FIRST_MESSAGE);
        sut.handleWarnMessage(SECOND_MESSAGE);
        //Then
        assertEquals(2, sut.getAllMessages().size(), "Wrong all messages size");
        assertEquals(0, sut.getAllInfoMessages().size(), "Wrong info messages size");
        assertEquals(2, sut.getAllWarnMessages().size(), "Wrong warn messages size");
        assertEquals(0, sut.getAllErrorMessages().size(), "Wrong error messages size");
        assertEquals(0, sut.getAllExceptionMessages().size(), "Wrong exception messages size");
        assertEquals(0, sut.getAllExceptions().size(), "Wrong exceptions size");
        assertEquals(SECOND_MESSAGE, sut.getLastMessage(), "Wrong last message");
    }

    @Test
    @DisplayName("Should store all error messages")
    void shouldStoreAllErrorMessages() {
        //Given
        InMemoryMessageHandler sut = new InMemoryMessageHandler();
        //When
        sut.handleErrorMessage(FIRST_MESSAGE);
        sut.handleErrorMessage(SECOND_MESSAGE);
        //Then
        assertEquals(2, sut.getAllMessages().size(), "Wrong all messages size");
        assertEquals(0, sut.getAllInfoMessages().size(), "Wrong info messages size");
        assertEquals(0, sut.getAllWarnMessages().size(), "Wrong warn messages size");
        assertEquals(2, sut.getAllErrorMessages().size(), "Wrong error messages size");
        assertEquals(0, sut.getAllExceptionMessages().size(), "Wrong exception messages size");
        assertEquals(0, sut.getAllExceptions().size(), "Wrong exceptions size");
        assertEquals(SECOND_MESSAGE, sut.getLastMessage(), "Wrong last message");
    }

    @Test
    @DisplayName("Should store all exception messages")
    void shouldStoreAllExceptionMessages() {
        // Given
        InMemoryMessageHandler sut = new InMemoryMessageHandler();
        Exception firstException = new IllegalArgumentException("My IllegalArgumentException");
        Exception secondException = new ClassNotFoundException("My ClassNotFoundException");
        // When
        sut.handleException(firstException);
        sut.handleException(secondException);
        // Then
        assertEquals(2, sut.getAllMessages().size(), "Wrong all messages size");
        assertEquals(0, sut.getAllInfoMessages().size(), "Wrong info messages size");
        assertEquals(0, sut.getAllWarnMessages().size(), "Wrong warn messages size");
        assertEquals(0, sut.getAllErrorMessages().size(), "Wrong error messages size");
        assertEquals(2, sut.getAllExceptionMessages().size(), "Wrong exception messages size");
        assertEquals(2, sut.getAllExceptions().size(), "Wrong exceptions size");
        assertEquals("My ClassNotFoundException", sut.getLastMessage(), "Wrong last message");
    }

}