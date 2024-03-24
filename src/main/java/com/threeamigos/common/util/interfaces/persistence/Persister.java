package com.threeamigos.common.util.interfaces.persistence;

/**
 * An object that can be used to persist something or to retrieve its previous state.
 * When the load or save operations are called it will return a {@link PersistResult}
 * containing the outcome of the operation.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public interface Persister<T> {

	/**
	 * @param entity an entity whose state should be loaded
	 * @return a {@link PersistResult} with the outcome of the load operation
	 */
	PersistResult load(T entity);

	/**
	 * @param entity an entity whose state should be saved
	 * @return a {@link PersistResult} with the outcome of the save operation
	 */
	PersistResult save(T entity);

}
