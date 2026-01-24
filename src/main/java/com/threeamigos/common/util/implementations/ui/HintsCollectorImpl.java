package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.Hint;
import com.threeamigos.common.util.interfaces.ui.HintsCollector;
import com.threeamigos.common.util.interfaces.ui.HintsProducer;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * An implementation of the {@link HintsCollector} interface.
 *
 * @param <T> type of hint (e.g., java.lang.String)
 * @author Stefano Reksten
 */
public class HintsCollectorImpl<T> implements HintsCollector<T> {

    private final Collection<Hint<T>> hints = new ArrayList<>();

    @Override
    public void addHint(final @Nonnull Hint<T> hint) {
        hints.add(hint);
    }

    @Override
    public void addHints(final @Nonnull Collection<Hint<T>> hints) {
        this.hints.addAll(hints);
    }

    @Override
    public void addHints(final @Nonnull HintsProducer<T> hintsProducer) {
        hints.addAll(hintsProducer.getHints());
    }

    @Override
    public @Nonnull Collection<Hint<T>> getHints() {
        return Collections.unmodifiableCollection(hints);
    }

}
