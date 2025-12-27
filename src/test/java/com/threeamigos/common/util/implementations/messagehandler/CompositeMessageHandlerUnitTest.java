package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("Collection constructor should throw exception if null collection provided")
    void collectionConstructorShouldThrowExceptionIfNullCollectionProvided() {
        assertThrows(IllegalArgumentException.class, () -> new CompositeMessageHandler((Collection<MessageHandler>) null));
    }

    @Test
    @DisplayName("Collection constructor should keep track of arguments")
    void collectionConstructorShouldKeepTrackOfArguments() {
        // Given
        Collection<MessageHandler> messageHandlersParameter = new ArrayList<>();
        messageHandlersParameter.add(firstMessageHandler);
        messageHandlersParameter.add(secondMessageHandler);
        CompositeMessageHandler sut = new CompositeMessageHandler(messageHandlersParameter);
        // When
        Collection<MessageHandler> messageHandlers = sut.getMessageHandlers();
        // Then
        assertThat(messageHandlers, containsInAnyOrder(firstMessageHandler, secondMessageHandler));
    }

    @Test
    @DisplayName("Varargs constructor should throw exception if null array provided")
    void varargsConstructorShouldThrowExceptionIfNullArrayProvided() {
        assertThrows(IllegalArgumentException.class, () -> new CompositeMessageHandler((MessageHandler[]) null));
    }

    @Test
    @DisplayName("Varargs constructor should throw exception if null MessageHandler provided")
    void varargsConstructorShouldThrowExceptionIfNullArgumentProvided() {
        assertThrows(IllegalArgumentException.class, () -> new CompositeMessageHandler(new MessageHandler[]{null}));
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
    @DisplayName("Should throw exception if adding a null handler")
    void shouldThrowExceptionIfAddingNullHandler() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler();
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.addMessageHandler(null));
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
    @DisplayName("Should throw exception if removing a null handler")
    void shouldThrowExceptionIfRemovingNullHandler() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler();
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.removeMessageHandler(null));
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

    @ParameterizedTest
    @DisplayName("Should remember if info level is active")
    @CsvSource({"true, true", "false, false"})
    void shouldRememberIfInfoLevelIsActive(boolean isActive, boolean expectedResult) {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setInfoEnabled(isActive);
        // Then
        assertEquals(expectedResult, sut.isInfoEnabled(), "Info level does not match expected result");
    }

    @Test
    @DisplayName("Should throw an exception if a null info message supplier is provided")
    void shouldThrowAnExceptionIfANullInfoMessageSupplierIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        Supplier<String> infoMessageSupplier = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleInfoMessage(infoMessageSupplier));
    }

    @Test
    @DisplayName("Should throw an exception if a null info message is provided")
    void shouldThrowAnExceptionIfANullInfoMessageIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        String infoMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleInfoMessage(infoMessage));
    }

    @Test
    @DisplayName("Should propagate supplied info messages to all handlers if info level is active")
    void shouldPropagateSuppliedInfoMessagesToAllHandlersIfActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        Supplier<String> infoMessageSupplier = () -> FIRST_MESSAGE;
        Supplier<String> secondInfoMessageSupplier = () -> SECOND_MESSAGE;
        // When
        sut.handleInfoMessage(infoMessageSupplier);
        sut.handleInfoMessage(secondInfoMessageSupplier);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleInfoMessage(FIRST_MESSAGE);
            verify(messageHandler, times(1)).handleInfoMessage(SECOND_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should propagate info messages to all handlers if info level is active")
    void shouldPropagateInfoMessagesToAllHandlersIfActive() {
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
    @DisplayName("Should not propagate info messages to all handlers if info level is not active")
    void shouldNotPropagateInfoMessagesToAllHandlersIfNotActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setInfoEnabled(false);
        sut.handleInfoMessage(FIRST_MESSAGE);
        sut.handleInfoMessage(SECOND_MESSAGE);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(0)).handleInfoMessage(FIRST_MESSAGE);
            verify(messageHandler, times(0)).handleInfoMessage(SECOND_MESSAGE);
        }
    }

    @ParameterizedTest
    @DisplayName("Should remember if warn level is active")
    @CsvSource({"true, true", "false, false"})
    void shouldRememberIfWarnLevelIsActive(boolean isActive, boolean expectedResult) {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setWarnEnabled(isActive);
        // Then
        assertEquals(expectedResult, sut.isWarnEnabled(), "Warn level does not match expected result");
    }

    @Test
    @DisplayName("Should throw an exception if a null warn message supplier is provided")
    void shouldThrowAnExceptionIfANullWarnMessageSupplierIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        Supplier<String> warnMessageSupplier = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleWarnMessage(warnMessageSupplier));
    }

    @Test
    @DisplayName("Should throw an exception if a null warn message is provided")
    void shouldThrowAnExceptionIfANullWarnMessageIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        String warnMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleWarnMessage(warnMessage));
    }

    @Test
    @DisplayName("Should propagate supplied warn messages to all handlers if warn level is active")
    void shouldPropagateSuppliedWarnMessagesToAllHandlersIfActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        Supplier<String> warnMessageSupplier = () -> FIRST_MESSAGE;
        Supplier<String> secondWarnMessageSupplier = () -> SECOND_MESSAGE;
        // When
        sut.handleWarnMessage(warnMessageSupplier);
        sut.handleWarnMessage(secondWarnMessageSupplier);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleWarnMessage(FIRST_MESSAGE);
            verify(messageHandler, times(1)).handleWarnMessage(SECOND_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should propagate warn messages to all handlers if warn level is active")
    void shouldPropagateWarnMessagesToAllHandlersIfActive() {
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
    @DisplayName("Should not propagate warn messages to all handlers if warn level is not active")
    void shouldNotPropagateWarnMessagesToAllHandlersIfNotActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setWarnEnabled(false);
        sut.handleWarnMessage(FIRST_MESSAGE);
        sut.handleWarnMessage(SECOND_MESSAGE);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(0)).handleWarnMessage(FIRST_MESSAGE);
            verify(messageHandler, times(0)).handleWarnMessage(SECOND_MESSAGE);
        }
    }

    @ParameterizedTest
    @DisplayName("Should remember if error level is active")
    @CsvSource({"true, true", "false, false"})
    void shouldRememberIfErrorLevelIsActive(boolean isActive, boolean expectedResult) {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setErrorEnabled(isActive);
        // Then
        assertEquals(expectedResult, sut.isErrorEnabled(), "Error level does not match expected result");
    }

    @Test
    @DisplayName("Should throw an exception if a null error message supplier is provided")
    void shouldThrowAnExceptionIfANullErrorMessageSupplierIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        Supplier<String> errorMessageSupplier = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleErrorMessage(errorMessageSupplier));
    }

    @Test
    @DisplayName("Should throw an exception if a null error message is provided")
    void shouldThrowAnExceptionIfANullErrorMessageIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        String errorMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleErrorMessage(errorMessage));
    }
    @Test
    @DisplayName("Should propagate supplied error messages to all handlers if error level is active")
    void shouldPropagateSuppliedErrorMessagesToAllHandlersIfActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        Supplier<String> errorMessageSupplier = () -> FIRST_MESSAGE;
        Supplier<String> secondErrorMessageSupplier = () -> SECOND_MESSAGE;
        // When
        sut.handleErrorMessage(errorMessageSupplier);
        sut.handleErrorMessage(secondErrorMessageSupplier);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleErrorMessage(FIRST_MESSAGE);
            verify(messageHandler, times(1)).handleErrorMessage(SECOND_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should propagate error messages to all handlers if error level is active")
    void shouldPropagateErrorMessagesToAllHandlersIfActive() {
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
    @DisplayName("Should not propagate error messages to all handlers if error level is not active")
    void shouldNotPropagateErrorMessagesToAllHandlersIfNotActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setErrorEnabled(false);
        sut.handleErrorMessage(FIRST_MESSAGE);
        sut.handleErrorMessage(SECOND_MESSAGE);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(0)).handleErrorMessage(FIRST_MESSAGE);
            verify(messageHandler, times(0)).handleErrorMessage(SECOND_MESSAGE);
        }
    }

    @ParameterizedTest
    @DisplayName("Should remember if debug level is active")
    @CsvSource({"true, true", "false, false"})
    void shouldRememberIfDebugLevelIsActive(boolean isActive, boolean expectedResult) {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setDebugEnabled(isActive);
        // Then
        assertEquals(expectedResult, sut.isDebugEnabled(), "Debug level does not match expected result");
    }

    @Test
    @DisplayName("Should throw an exception if a null debug message supplier is provided")
    void shouldThrowAnExceptionIfANullDebugMessageSupplierIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        Supplier<String> debugMessageSupplier = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleDebugMessage(debugMessageSupplier));
    }

    @Test
    @DisplayName("Should throw an exception if a null debug message is provided")
    void shouldThrowAnExceptionIfANullDebugMessageIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        String debugMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleDebugMessage(debugMessage));
    }

    @Test
    @DisplayName("Should propagate supplied debug messages to all handlers if debug level is active")
    void shouldPropagateSuppliedDebugMessagesToAllHandlersIfActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        Supplier<String> debugMessageSupplier = () -> FIRST_MESSAGE;
        Supplier<String> secondDebugMessageSupplier = () -> SECOND_MESSAGE;
        // When
        sut.handleDebugMessage(debugMessageSupplier);
        sut.handleDebugMessage(secondDebugMessageSupplier);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleDebugMessage(FIRST_MESSAGE);
            verify(messageHandler, times(1)).handleDebugMessage(SECOND_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should propagate debug messages to all handlers if debug level is active")
    void shouldPropagateDebugMessagesToAllHandlersIfActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.handleDebugMessage(FIRST_MESSAGE);
        sut.handleDebugMessage(SECOND_MESSAGE);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleDebugMessage(FIRST_MESSAGE);
            verify(messageHandler, times(1)).handleDebugMessage(SECOND_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should not propagate debug messages to all handlers if debug level is not active")
    void shouldNotPropagateDebugMessagesToAllHandlersIfNotActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setDebugEnabled(false);
        sut.handleDebugMessage(FIRST_MESSAGE);
        sut.handleDebugMessage(SECOND_MESSAGE);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(0)).handleDebugMessage(FIRST_MESSAGE);
            verify(messageHandler, times(0)).handleDebugMessage(SECOND_MESSAGE);
        }
    }

    @ParameterizedTest
    @DisplayName("Should remember if trace level is active")
    @CsvSource({"true, true", "false, false"})
    void shouldRememberIfTraceLevelIsActive(boolean isActive, boolean expectedResult) {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setTraceEnabled(isActive);
        // Then
        assertEquals(expectedResult, sut.isTraceEnabled(), "Trace level does not match expected result");
    }

    @Test
    @DisplayName("Should throw an exception if a null trace message supplier is provided")
    void shouldThrowAnExceptionIfANullTraceMessageSupplierIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        Supplier<String> traceMessageSupplier = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleTraceMessage(traceMessageSupplier));
    }

    @Test
    @DisplayName("Should throw an exception if a null trace message is provided")
    void shouldThrowAnExceptionIfANullTraceMessageIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        String traceMessage = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleTraceMessage(traceMessage));
    }

    @Test
    @DisplayName("Should propagate supplied trace messages to all handlers if trace level is active")
    void shouldPropagateSuppliedTraceMessagesToAllHandlersIfActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        Supplier<String> traceMessageSupplier = () -> FIRST_MESSAGE;
        Supplier<String> secondTraceMessageSupplier = () -> SECOND_MESSAGE;
        // When
        sut.handleTraceMessage(traceMessageSupplier);
        sut.handleTraceMessage(secondTraceMessageSupplier);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleTraceMessage(FIRST_MESSAGE);
            verify(messageHandler, times(1)).handleTraceMessage(SECOND_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should propagate trace messages to all handlers if trace level is active")
    void shouldPropagateTraceMessagesToAllHandlersIfActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.handleTraceMessage(FIRST_MESSAGE);
        sut.handleTraceMessage(SECOND_MESSAGE);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(1)).handleTraceMessage(FIRST_MESSAGE);
            verify(messageHandler, times(1)).handleTraceMessage(SECOND_MESSAGE);
        }
    }

    @Test
    @DisplayName("Should not propagate trace messages to all handlers if trace level is not active")
    void shouldNotPropagateTraceMessagesToAllHandlersIfNotActive() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setTraceEnabled(false);
        sut.handleTraceMessage(FIRST_MESSAGE);
        sut.handleTraceMessage(SECOND_MESSAGE);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(0)).handleTraceMessage(FIRST_MESSAGE);
            verify(messageHandler, times(0)).handleTraceMessage(SECOND_MESSAGE);
        }
    }

    @ParameterizedTest
    @DisplayName("Should remember if exception level is active")
    @CsvSource({"true, true", "false, false"})
    void shouldRememberIfExceptionLevelIsActive(boolean isActive, boolean expectedResult) {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setExceptionEnabled(isActive);
        // Then
        assertEquals(expectedResult, sut.isExceptionEnabled(), "Exception level does not match expected result");
    }

    @Test
    @DisplayName("Should throw an exception if a null exception is provided")
    void shouldThrowAnExceptionIfANullExceptionIsProvided() {
        // Given
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        Exception exception = null;
        // Then
        assertThrows(IllegalArgumentException.class, () -> sut.handleException(exception));
    }

    @Test
    @DisplayName("Should propagate exceptions to all handlers if exception level is active")
    void shouldPropagateExceptionsToAllHandlersIfActive() {
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


    @Test
    @DisplayName("Should not propagate exceptions to all handlers if exception level is not active")
    void shouldNotPropagateExceptionsToAllHandlersIfNotActive() {
        // Given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("My IllegalArgumentException");
        ClassCastException classCastException = new ClassCastException("My ClassCastException");
        CompositeMessageHandler sut = new CompositeMessageHandler(firstMessageHandler, secondMessageHandler);
        // When
        sut.setExceptionEnabled(false);
        sut.handleException(illegalArgumentException);
        sut.handleException(classCastException);
        // Then
        for (MessageHandler messageHandler : sut.getMessageHandlers()) {
            verify(messageHandler, times(0)).handleException(illegalArgumentException);
            verify(messageHandler, times(0)).handleException(classCastException);
        }
    }
}
