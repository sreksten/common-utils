package com.threeamigos.common.util.interfaces.ui;

import java.util.Collection;

/**
 * An interface that produces {@link Hint}s for an application.
 *
 * @param <T> type of hint (e.g. java.lang.String)
 * @author Stefano Reksten
 */
public interface HintsProducer<T> {

	/**
	 * @return a collection of {@link Hint}s to show at startup.
	 */
	public Collection<Hint<T>> getHints();

}
