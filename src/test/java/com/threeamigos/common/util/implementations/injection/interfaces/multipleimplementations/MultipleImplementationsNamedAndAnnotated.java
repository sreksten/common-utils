package com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations;

import jakarta.annotation.Priority;
import jakarta.inject.Named;

@Named("name")
@MyQualifier
public class MultipleImplementationsNamedAndAnnotated implements MultipleImplementationsInterface {
}
