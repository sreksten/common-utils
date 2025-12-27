package com.threeamigos.common.util.interfaces.json;

import com.threeamigos.common.util.implementations.json.JsonColorAdapter;
import org.jspecify.annotations.NonNull;

import java.awt.*;

/**
 * An interface used to build a JSON converter for a specific class. If the entity has one or more fields that need
 * a specific conversion, one or more {@link JsonAdapter}s can be registered using the
 * {@link #registerAdapter(Class, JsonAdapter)} method. Based on Google's JSON classes.
 *
 * @author Stefano Reksten
 */
public interface JsonBuilder {

	/**
	 * Use to register an adapter to convert a specific object from/to its JSON representation. For example, you
	 * can add a specific adapter to represent a java.awt.Color as a string in the AARRGGBB format. Such an adapter
	 * is already provided; see {@link com.threeamigos.common.util.implementations.json.JsonColorAdapter}.
	 *
	 * @param clazz class of the object to convert
	 * @param adapter the adapter to use to convert the class from/to its JSON representation
	 * @return the builder itself
	 * @param <C> class of the object to convert
	 * @param <A> an adapter able to convert an instance of C.
	 */
	<C, A extends JsonAdapter<C>> JsonBuilder registerAdapter(@NonNull Class<C> clazz, @NonNull A adapter);

	/**
	 * Use to register an adapter able to convert a java.awt.Color from/to its JSON representation.
	 * @return the builder itself
	 */
	default JsonBuilder registerColorAdapter() {
		return registerAdapter(Color.class, new JsonColorAdapter());
	}

	/**
	 * Builds a JSON converter for a specific class. This is the last step to call after registering all the adapters.
	 *
	 * @param tClass the class to convert
	 * @return a JSON converter
	 * @param <T> the class to convert
	 */
	<T> Json<T> build(@NonNull Class<T> tClass);

}
