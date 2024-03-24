package com.threeamigos.common.util.interfaces.json;

public interface JsonBuilder {

	public <C, A extends JsonAdapter<C>> JsonBuilder registerAdapter(Class<C> clazz, A adapter);

	public <T> Json<T> build(Class<T> tClass);

}
