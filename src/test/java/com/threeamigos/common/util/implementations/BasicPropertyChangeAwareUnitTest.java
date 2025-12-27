package com.threeamigos.common.util.implementations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DisplayName("BasicPropertyChangeAware unit test")
@Tag("unit")
class BasicPropertyChangeAwareUnitTest {

    private PropertyChangeListener firstListener;
    private PropertyChangeListener secondListener;
    ArgumentCaptor<PropertyChangeEvent> firstCaptor;
    ArgumentCaptor<PropertyChangeEvent> secondCaptor;


    @BeforeEach
    void setup() {
        firstListener = mock(PropertyChangeListener.class);
        secondListener = mock(PropertyChangeListener.class);
        firstCaptor = ArgumentCaptor.forClass(PropertyChangeEvent.class);
        secondCaptor = ArgumentCaptor.forClass(PropertyChangeEvent.class);
    }

    @Test
    @DisplayName("Should notify both listeners with only property name")
    void shouldNotifyBothListenersOnlyPropertyName() {
        // Given
        BasicPropertyChangeAware sut = new BasicPropertyChangeAware();
        sut.addPropertyChangeListener(firstListener);
        sut.addPropertyChangeListener(secondListener);
        // When
        sut.firePropertyChange("Property change");
        // Then
        verify(firstListener, times(1)).propertyChange(firstCaptor.capture());
        verify(secondListener, times(1)).propertyChange(secondCaptor.capture());
        assertEquals("Property change", firstCaptor.getValue().getPropertyName());
        assertEquals("Property change", secondCaptor.getValue().getPropertyName());
    }

    @Test
    @DisplayName("Should notify both listeners with all parameters")
    void shouldNotifyBothListenersAllParameters() {
        // Given
        BasicPropertyChangeAware sut = new BasicPropertyChangeAware();
        sut.addPropertyChangeListener(firstListener);
        sut.addPropertyChangeListener(secondListener);
        // When
        sut.firePropertyChange("Other property change", "Old", "New");
        // Then
        verify(firstListener, times(1)).propertyChange(any());
        verify(secondListener, times(1)).propertyChange(any());
        verify(firstListener, times(1)).propertyChange(firstCaptor.capture());
        verify(secondListener, times(1)).propertyChange(secondCaptor.capture());
        assertEquals("Other property change", firstCaptor.getValue().getPropertyName());
        assertEquals("Old", firstCaptor.getValue().getOldValue());
        assertEquals("New", firstCaptor.getValue().getNewValue());
        assertEquals("Other property change", secondCaptor.getValue().getPropertyName());
        assertEquals("Old", secondCaptor.getValue().getOldValue());
        assertEquals("New", secondCaptor.getValue().getNewValue());
    }

    @Test
    @DisplayName("Should notify only one listener")
    void shouldNotifyOneListener() {
        // Given
        BasicPropertyChangeAware sut = new BasicPropertyChangeAware();
        sut.addPropertyChangeListener(firstListener);
        sut.addPropertyChangeListener(secondListener);
        // When
        sut.removePropertyChangeListener(firstListener);
        sut.firePropertyChange("Another property change", "Previous", "Current");
        // Then
        verify(firstListener, times(0)).propertyChange(firstCaptor.capture());
        verify(secondListener, times(1)).propertyChange(secondCaptor.capture());
        assertEquals("Another property change", secondCaptor.getValue().getPropertyName());
        assertEquals("Previous", secondCaptor.getValue().getOldValue());
        assertEquals("Current", secondCaptor.getValue().getNewValue());
    }

}