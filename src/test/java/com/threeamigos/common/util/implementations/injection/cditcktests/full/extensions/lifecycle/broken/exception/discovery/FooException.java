package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.broken.exception.discovery;

class FooException extends RuntimeException {

    private static final long serialVersionUID = 4441409975741605270L;

    FooException() {
        super();
    }

    FooException(String message, Throwable cause) {
        super(message, cause);
    }

    FooException(String message) {
        super(message);
    }

    FooException(Throwable cause) {
        super(cause);
    }
}
