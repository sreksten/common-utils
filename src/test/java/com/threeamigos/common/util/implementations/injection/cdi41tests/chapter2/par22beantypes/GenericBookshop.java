package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par22beantypes;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class GenericBookshop<T> implements Shop<T> {
}