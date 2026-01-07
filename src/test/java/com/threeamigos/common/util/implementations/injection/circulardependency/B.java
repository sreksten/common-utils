package com.threeamigos.common.util.implementations.injection.circulardependency;

import javax.inject.Inject;

public class B {

    @Inject
    A a;
}
