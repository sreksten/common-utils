package com.threeamigos.common.util.interfaces.ui;

import java.awt.event.KeyListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import javax.swing.event.MouseInputListener;

/**
 * An interface that consumes user inputs: Keyboard and Mouse wheel, buttons,
 * and movements.
 *
 * @author Stefano Reksten
 */
public interface InputConsumer extends MouseWheelListener, MouseInputListener, MouseMotionListener, KeyListener {

}
