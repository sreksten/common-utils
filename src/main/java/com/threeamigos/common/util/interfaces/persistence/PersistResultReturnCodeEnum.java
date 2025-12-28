package com.threeamigos.common.util.interfaces.persistence;

public enum PersistResultReturnCodeEnum {

    // General
    SUCCESSFUL,
    NOT_FOUND,
    CANNOT_BE_READ,
    CANNOT_BE_WRITTEN,
    ERROR,

    // File based
    PATH_NOT_ACCESSIBLE,

}
