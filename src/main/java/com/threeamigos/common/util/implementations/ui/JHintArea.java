package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.Hint;

import javax.swing.*;
import java.io.Serial;

public class JHintArea extends JTextArea {

    @Serial
    private static final long serialVersionUID = 1L;

    public JHintArea(Hint<String> hint) {
        super(hint.getHint());
    }

    void setHint(Hint<String> hint) {
        setText(hint.getHint());
    }

}