package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DisplayName("CompositeMessageHandler unit test")
@Tag("unit")
@Tag("messageHandler")
class CompositeMessageHandlerUnitTest {

    private static final String FIRST_MESSAGE = "First message";
    private static final String SECOND_MESSAGE = "Second message";

    private MessageHandler firstMessageHandler;
    private MessageHandler secondMessageHandler;

    @BeforeEach
    void setup() {
        firstMessageHandler = mock(MessageHandler.class);
        secondMessageHandler = mock(MessageHandler.class);
    }

    @Test
    @DisplayName("Varargs constructor should keep track of arguments")
    void varargsConstructorShouldKeepTrackOfArguments() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        Collection<MessageHandler> messageHandlers = sut.getMessageHandlers();
        // Then
        assertThat(messageHandlers, containsInAnyOrder(firstMessageHandler, secondMessageHandler));
    }

    @Test
    @DisplayName("Should add a handler")
    void shouldAddAHandler() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler();
        sut.addMessageHandler(firstMessageHandler);
        // When
        Collection<MessageHandler> messageHandlers = sut.getMessageHandlers();
        // Then
        assertEquals(1, messageHandlers.size());
        assertEquals(firstMessageHandler, messageHandlers.iterator().next(), "Does not contain added handler");
    }

    @Test
    @DisplayName("Should remove a handler")
    void shouldRemoveAHandler() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        MessageHandler thirdMessageHandler = mock(MessageHandler.class);
        sut.addMessageHandler(thirdMessageHandler);
        sut.removeMessageHandler(secondMessageHandler);
        // When
        Collection<MessageHandler> messageHandlers = sut.getMessageHandlers();
        // Then
        assertThat(messageHandlers, containsInAnyOrder(firstMessageHandler, thirdMessageHandler));
    }

    @Test
    @DisplayName("Should propagate info messages to all handlers")
    void shouldPropagateInfoMessagesToAllHandlers() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.handleInfoMessage(FIRST_MESSAGE);
        sut.handleInfoMessage(SECOND_MESSAGE);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleInfoMessage(FIRST_MESSAGE);
            verify(messageHandler, times(1)).handleInfoMessage(SECOND_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should propagate warn messages to all handlers")
    void shouldPropagateWarnMessagesToAllHandlers() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.handleWarnMessage(FIRST_MESSAGE);
        sut.handleWarnMessage(SECOND_MESSAGE);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleWarnMessage(FIRST_MESSAGE);
            verify(messageHandler, times(1)).handleWarnMessage(SECOND_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should propagate error messages to all handlers")
    void shouldPropagateErrorMessagesToAllHandlers() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.handleErrorMessage(FIRST_MESSAGE);
        sut.handleErrorMessage(SECOND_MESSAGE);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleErrorMessage(FIRST_MESSAGE);
            verify(messageHandler, times(1)).handleErrorMessage(SECOND_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should propagate exceptions to all handlers")
    void shouldPropagateExceptionsToAllHandlers() {
        // Given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("My IllegalArgumentException");
        ClassCastException classCastException = new ClassCastException("My ClassCastException");
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.handleException(illegalArgumentException);
        sut.handleException(classCastException);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleException(illegalArgumentException);
            verify(messageHandler, times(1)).handleException(classCastException);
        }
    }
}
