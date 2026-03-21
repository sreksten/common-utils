package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par55injection;

public class NonStaticProducedPayload {

    private final String declaringBeanId;
    private final String producerDependencyId;

    public NonStaticProducedPayload(String declaringBeanId, String producerDependencyId) {
        this.declaringBeanId = declaringBeanId;
        this.producerDependencyId = producerDependencyId;
    }

    public String getDeclaringBeanId() {
        return declaringBeanId;
    }

    public String getProducerDependencyId() {
        return producerDependencyId;
    }
}
