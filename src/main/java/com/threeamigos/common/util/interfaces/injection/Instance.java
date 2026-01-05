package com.threeamigos.common.util.interfaces.injection;

import com.threeamigos.common.util.annotations.injection.Alternative;
import com.threeamigos.common.util.annotations.injection.Any;

/**
 * Interface that can provide one or more instances of a specific type.<br/>
 * You can use {@link Alternative} to specify alternative implementations or
 * {@link Any} to get any known implementation.
 * @param <T> the type of the instances provided by this instance
 *
 * @author Stefano Reksten
 */
public interface Instance<T> extends Iterable<T> {
    T get() throws Exception;
}
