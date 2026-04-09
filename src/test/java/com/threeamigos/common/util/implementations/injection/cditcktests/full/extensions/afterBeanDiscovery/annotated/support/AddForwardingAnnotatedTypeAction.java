package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.support;

import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;

/**
 * Utility abstraction used by the original CDI TCK tests when adding custom annotated types.
 */
public abstract class AddForwardingAnnotatedTypeAction<X> extends ForwardingAnnotatedType<X> {

    public abstract String getBaseId();

    public String getId() {
        return buildId(getBaseId(), delegate().getJavaClass());
    }

    public void perform(BeforeBeanDiscovery event) {
        event.addAnnotatedType(this, getId());
    }

    public static String buildId(String baseId, Class<?> javaClass) {
        return baseId + "_" + javaClass.getName();
    }
}
