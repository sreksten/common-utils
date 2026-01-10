package com.threeamigos.common.util.implementations.injection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DefaultLiteral unit tests")
class DefaultLiteralUnitTest {

    @Test
    @DisplayName("equals should work")
    void equalsShouldWork() {
        DefaultLiteral defaultLiteral = new DefaultLiteral();
        assertEquals(defaultLiteral, defaultLiteral);
        assertNotEquals(null, defaultLiteral);
        assertNotEquals(new Object(), defaultLiteral);
        DefaultLiteral other = new DefaultLiteral();
        assertEquals(defaultLiteral, other);
    }

}