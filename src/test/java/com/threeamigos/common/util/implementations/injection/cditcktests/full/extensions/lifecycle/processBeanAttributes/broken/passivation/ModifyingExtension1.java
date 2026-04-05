package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.broken.passivation;

import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;

import java.lang.annotation.Annotation;

public class ModifyingExtension1 implements Extension {

    public void modify(@Observes ProcessBeanAttributes<Laptop> event) {
        final BeanAttributes<Laptop> attributes = event.getBeanAttributes();
        event.setBeanAttributes(new ForwardingBeanAttributes<Laptop>() {

            @Override
            public Class<? extends Annotation> getScope() {
                return SessionScoped.class;
            }

            @Override
            protected BeanAttributes<Laptop> attributes() {
                return attributes;
            }
        });
    }
}
