package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par22beantypes;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VariableArrayShop<T> implements Shop<T[]> {
}
