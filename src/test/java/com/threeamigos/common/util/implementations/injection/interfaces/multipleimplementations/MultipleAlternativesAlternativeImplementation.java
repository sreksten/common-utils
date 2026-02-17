package com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;

@Alternative
@Priority(1)
public class MultipleAlternativesAlternativeImplementation implements MultipleImplementationsInterface {
}
