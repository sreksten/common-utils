package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par22beantypes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;

@Typed()
@ApplicationScoped
public class EmptyTypedBookshop extends Business implements Shop<Book> {
}
