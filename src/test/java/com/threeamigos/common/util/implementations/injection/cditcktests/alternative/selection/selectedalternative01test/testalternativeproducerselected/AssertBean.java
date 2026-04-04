package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AssertBean {

    @Inject
    @Any
    private Instance<Object> instance;

    @Inject
    private BeanManager beanManager;

    public <T> T assertAvailable(Class<T> beanType, Annotation... qualifiers) {
        assertNotNull(beanManager.resolve(beanManager.getBeans(beanType, qualifiers)));
        Instance<T> selected = instance.select(beanType, qualifiers);
        T value = selected.get();
        assertNotNull(value);
        return value;
    }
}
