package com.threeamigos.common.util.interfaces.persistence.file;

import com.threeamigos.common.util.interfaces.persistence.PersistResult;

/**
 * The result of a load from file or save to file operation on an object
 *
 * @author Stefano Reksten
 */
public interface FilePersistResult extends PersistResult {

    /**
     * @return the name of the file bound to the object
     */
    String getFilename();

}
