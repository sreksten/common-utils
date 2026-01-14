package com.threeamigos.common.util.implementations.injection.bind;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME) @Qualifier
public @interface BindingQualifier { }