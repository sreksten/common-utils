package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.Hint;

/**
 * An implementation of the {@link Hint} class that uses a String
 *
 * @author Stefano Reksten
 */
public class StringHint implements Hint<String> {

    private final String hint;

    public StringHint(final String hint) {
        this.hint = hint;
    }

    @Override
    public String getHint() {
        return hint;
    }
}
