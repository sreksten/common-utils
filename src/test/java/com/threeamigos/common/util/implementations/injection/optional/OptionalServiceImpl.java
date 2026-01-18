package com.threeamigos.common.util.implementations.injection.optional;

/**
 * Concrete implementation of OptionalService for testing.
 */
public class OptionalServiceImpl implements OptionalService {
    @Override
    public String getValue() {
        return "OptionalService is present";
    }
}
