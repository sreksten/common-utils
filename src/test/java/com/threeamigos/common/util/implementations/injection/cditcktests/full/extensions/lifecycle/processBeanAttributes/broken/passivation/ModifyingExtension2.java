package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.broken.passivation;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;

import java.lang.annotation.Annotation;

public class ModifyingExtension2 implements Extension {

    public void modify(@Observes ProcessBeanAttributes<Wheel> event) {
        final BeanAttributes<Wheel> attributes = event.getBeanAttributes();
        event.setBeanAttributes(new ForwardingBeanAttributes<Wheel>() {

            @Override
            public Class<? extends Annotation> getScope() {
                return Dependent.class;
            }

            @Override
            protected BeanAttributes<Wheel> attributes() {
                return attributes;
            }
        });
    }
}
