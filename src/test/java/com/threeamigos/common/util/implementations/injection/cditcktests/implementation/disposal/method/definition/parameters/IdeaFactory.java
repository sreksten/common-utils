package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.parameters;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;

@Dependent
public class IdeaFactory {

    @Produces
    public Idea produce(Thinker thinker) {
        return thinker.think();
    }

    public void dispose(Thinker thinker, @Disposes Idea idea, BeanManager beanManager) {
        thinker.forget(idea);
    }
}
