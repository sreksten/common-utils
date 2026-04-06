package com.threeamigos.common.util.implementations.injection.cditcktests.full.vetoed;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class VerifyingExtension implements Extension {

    private final Set<Class<?>> classes = new HashSet<Class<?>>();

    public void observeAnnotatedType(@Observes ProcessAnnotatedType<?> event) {
        classes.add(event.getAnnotatedType().getJavaClass());
    }

    public Set<Class<?>> getClasses() {
        return Collections.unmodifiableSet(classes);
    }
}
