package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.InputConsumer;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * An implementation of the {@link InputConsumer} that accepts one or more
 * prioritized delegates. An event is passed to all delegates until one of them
 * consumes it.
 *
 * @author Stefano Reksten
 */
public class ChainedInputConsumer implements InputConsumer {

    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 5;
    public static final int PRIORITY_HIGH = 10;

    private final Map<Integer, List<InputConsumer>> inputConsumers = new TreeMap<>(Comparator.reverseOrder());
    private List<InputConsumer> sortedConsumers = Collections.emptyList();

    public void addConsumer(InputConsumer consumer, int priority) {
        inputConsumers.computeIfAbsent(priority, key -> new ArrayList<>()).add(consumer);
        sortedConsumers = new LinkedList<>();
        inputConsumers.values().forEach(sortedConsumers::addAll);
    }

    public void removeConsumer(InputConsumer consumer) {
        inputConsumers.values().forEach(list -> list.remove(consumer));
        sortedConsumers.remove(consumer);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        consumeEvent(InputConsumer::mouseClicked, e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        consumeEvent(InputConsumer::mouseDragged, e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        consumeEvent(InputConsumer::mouseEntered, e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        consumeEvent(InputConsumer::mouseExited, e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        consumeEvent(InputConsumer::mouseMoved, e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        consumeEvent(InputConsumer::mousePressed, e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        consumeEvent(InputConsumer::mouseReleased, e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        consumeEvent(InputConsumer::mouseWheelMoved, e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        consumeEvent(InputConsumer::keyPressed, e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        consumeEvent(InputConsumer::keyReleased, e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        consumeEvent(InputConsumer::keyTyped, e);
    }

    private <E extends InputEvent> void consumeEvent(BiConsumer<InputConsumer, E> biConsumer, E e) {
        for (InputConsumer consumer : sortedConsumers) {
            biConsumer.accept(consumer, e);
            if (e.isConsumed()) {
                break;
            }
        }
    }

}
