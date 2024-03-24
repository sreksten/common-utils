package com.threeamigos.common.util.interfaces.ui;

import java.util.Collection;

/**
 * An interface that collects Hints for an application.
 *
 * @param <T> type of hint (e.g. java.lang.String)
 */
public interface HintsCollector<T> {

	/**
	 * @param hint a Hint to add
	 */
	public void addHint(Hint<T> hint);

	/**
	 * @param hints a collection of hints to add
	 */
	public void addHints(Collection<Hint<T>> hints);

	/**
	 * @param hintsProducer returns a collection of hints to add
	 */
	public void addHints(HintsProducer<T> hintsProducer);

	/**
	 * @return all hints collected so far
	 */
	public Collection<Hint<T>> getHints();

}
