package com.threeamigos.common.util.interfaces.injection;

/**
 * A dependency injector.
 *
 * @author Stefano Reksten
 */
public interface Injector {

    <T> T inject(Class<T> clazz) throws Exception;

}
