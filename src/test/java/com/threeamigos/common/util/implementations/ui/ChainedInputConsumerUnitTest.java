package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.InputConsumer;
import org.junit.jupiter.api.*;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import static org.mockito.Mockito.*;

@DisplayName("ChainedInputConsumer unit test")
@Tag("unit")
class ChainedInputConsumerUnitTest {

    @Nested
    @DisplayName("Should track consumers")
    class TrackConsumers {

        private InputConsumer firstInputConsumer;
        private InputConsumer secondInputConsumer;
        private ChainedInputConsumer sut;

        @BeforeEach
        void setup() {
            firstInputConsumer = mock(InputConsumer.class);
            secondInputConsumer = mock(InputConsumer.class);
            sut = new ChainedInputConsumer();
            sut.addConsumer(firstInputConsumer, ChainedInputConsumer.PRIORITY_MEDIUM);
            sut.addConsumer(secondInputConsumer, ChainedInputConsumer.PRIORITY_MEDIUM);
        }

        @Test
        @DisplayName("Should add a consumer")
        void shouldAddConsumer() {
            // Given
            MouseEvent mouseEvent = mock(MouseEvent.class);
            // When
            sut.mouseMoved(mouseEvent);
            // Then
            verify(firstInputConsumer, times(1)).mouseMoved(mouseEvent);
            verify(secondInputConsumer, times(1)).mouseMoved(mouseEvent);
        }

        @Test
        @DisplayName("Should remove a consumer")
        void shouldRemoveConsumer() {
            // Given
            MouseEvent mouseEvent = mock(MouseEvent.class);
            // When
            sut.removeConsumer(firstInputConsumer);
            sut.mouseMoved(mouseEvent);
            // Then
            verify(firstInputConsumer, times(0)).mouseMoved(mouseEvent);
            verify(secondInputConsumer, times(1)).mouseMoved(mouseEvent);
        }
    }

    @Nested
    @DisplayName("Should forward events to registered consumers")
    class ShouldForwardEvents {

        private InputConsumer inputConsumer;
        private ChainedInputConsumer sut;

        @BeforeEach
        void setup() {
            inputConsumer = mock(InputConsumer.class);
            sut = new ChainedInputConsumer();
            sut.addConsumer(inputConsumer, ChainedInputConsumer.PRIORITY_MEDIUM);
        }

        @Test
        @DisplayName("Should forward mouseClicked")
        void shouldForwardMouseClicked() {
            // Given
            MouseEvent event = mock(MouseEvent.class);
            // When
            sut.mouseClicked(event);
            // Then
            verify(inputConsumer, times(1)).mouseClicked(event);
        }

        @Test
        @DisplayName("Should forward mouseDragged")
        void shouldForwardMouseDragged() {
            // Given
            MouseEvent event = mock(MouseEvent.class);
            // When
            sut.mouseDragged(event);
            // Then
            verify(inputConsumer, times(1)).mouseDragged(event);
        }

        @Test
        @DisplayName("Should forward mouseEntered")
        void shouldForwardMouseEntered() {
            // Given
            MouseEvent event = mock(MouseEvent.class);
            // When
            sut.mouseEntered(event);
            // Then
            verify(inputConsumer, times(1)).mouseEntered(event);
        }

        @Test
        @DisplayName("Should forward mouseExited")
        void shouldForwardMouseExited() {
            // Given
            MouseEvent event = mock(MouseEvent.class);
            // When
            sut.mouseExited(event);
            // Then
            verify(inputConsumer, times(1)).mouseExited(event);
        }

        @Test
        @DisplayName("Should forward mouseMoved")
        void shouldForwardMouseMoved() {
            // Given
            MouseEvent event = mock(MouseEvent.class);
            // When
            sut.mouseMoved(event);
            // Then
            verify(inputConsumer, times(1)).mouseMoved(event);
        }

        @Test
        @DisplayName("Should forward mousePressed")
        void shouldForwardMousePressed() {
            // Given
            MouseEvent event = mock(MouseEvent.class);
            // When
            sut.mousePressed(event);
            // Then
            verify(inputConsumer, times(1)).mousePressed(event);
        }

        @Test
        @DisplayName("Should forward mouseReleased")
        void shouldForwardMouseReleased() {
            // Given
            MouseEvent event = mock(MouseEvent.class);
            // When
            sut.mouseReleased(event);
            // Then
            verify(inputConsumer, times(1)).mouseReleased(event);
        }

        @Test
        @DisplayName("Should forward mouseWheel")
        void shouldForwardMouseWheelMoved() {
            // Given
            MouseWheelEvent event = mock(MouseWheelEvent.class);
            // When
            sut.mouseWheelMoved(event);
            // Then
            verify(inputConsumer, times(1)).mouseWheelMoved(event);
        }

        @Test
        @DisplayName("Should forward keyPressed")
        void shouldForwardKeyPressed() {
            // Given
            KeyEvent event = mock(KeyEvent.class);
            // When
            sut.keyPressed(event);
            // Then
            verify(inputConsumer, times(1)).keyPressed(event);
        }

        @Test
        @DisplayName("Should forward keyReleased")
        void shouldForwardKeyReleased() {
            // Given
            KeyEvent event = mock(KeyEvent.class);
            // When
            sut.keyReleased(event);
            // Then
            verify(inputConsumer, times(1)).keyReleased(event);
        }

        @Test
        @DisplayName("Should forward keyTyped")
        void shouldForwardKeyTyped() {
            // Given
            KeyEvent event = mock(KeyEvent.class);
            // When
            sut.keyTyped(event);
            // Then
            verify(inputConsumer, times(1)).keyTyped(event);
        }
    }


    @Nested
    @DisplayName("Should forward according to priority")
    class ShouldForwardAccordingToPriority {

        private InputConsumer firstInputConsumer;
        private InputConsumer secondInputConsumer;
        private ChainedInputConsumer sut;
        private MouseEvent mouseEvent;

        @BeforeEach
        void setup() {
            firstInputConsumer = mock(InputConsumer.class);
            secondInputConsumer = mock(InputConsumer.class);
            sut = new ChainedInputConsumer();
            mouseEvent = mock(MouseEvent.class);
        }

        @Test
        @DisplayName("A consumer with high priority added after consumes the event")
        void shouldAddConsumer() {
            // Given
            sut.addConsumer(firstInputConsumer, ChainedInputConsumer.PRIORITY_MEDIUM);
            sut.addConsumer(secondInputConsumer, ChainedInputConsumer.PRIORITY_HIGH);
            when(mouseEvent.isConsumed()).thenReturn(true);
            // When
            sut.mouseMoved(mouseEvent);
            // Then
            verify(firstInputConsumer, times(0)).mouseMoved(mouseEvent);
            verify(secondInputConsumer, times(1)).mouseMoved(mouseEvent);
        }

        @Test
        @DisplayName("A consumer with high priority added before consumes the event")
        void shouldRemoveConsumer() {
            // Given
            sut.addConsumer(firstInputConsumer, ChainedInputConsumer.PRIORITY_MEDIUM);
            sut.addConsumer(secondInputConsumer, ChainedInputConsumer.PRIORITY_LOW);
            when(mouseEvent.isConsumed()).thenReturn(true);
            // When
            sut.mouseMoved(mouseEvent);
            // Then
            verify(firstInputConsumer, times(1)).mouseMoved(mouseEvent);
            verify(secondInputConsumer, times(0)).mouseMoved(mouseEvent);
        }
    }

}