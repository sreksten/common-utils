package com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires;

import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.AFTER_BEAN_DISCOVERY;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.AFTER_DEPLOYMENT_VALIDATION;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.BEFORE_BEAN_DISCOVERY;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.BEFORE_SHUTDOWN;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.PROCESS_ANNOTATED_TYPE;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.PROCESS_BEAN;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.PROCESS_BEAN_ATTRIBUTES;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.PROCESS_INJECTION_POINT;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.PROCESS_INJECTION_TARGET;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.PROCESS_OBSERVER_METHOD;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.PROCESS_PRODUCER;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.PROCESS_PRODUCER_FIELD;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.PROCESS_PRODUCER_METHOD;
import static com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires.ContainerLifecycleEvents.PROCESS_SESSION_BEAN;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.ProcessInjectionTarget;
import jakarta.enterprise.inject.spi.ProcessObserverMethod;
import jakarta.enterprise.inject.spi.ProcessProducer;
import jakarta.enterprise.inject.spi.ProcessProducerField;
import jakarta.enterprise.inject.spi.ProcessProducerMethod;
import jakarta.enterprise.inject.spi.ProcessSessionBean;
import jakarta.inject.Inject;

@RequestScoped
public class ContainerLifecycleEventDispatcher {

    @Inject
    Event<Object> event;

    public void fireContainerLifecycleEvents() {
        tryFire(AfterBeanDiscovery.class, AFTER_BEAN_DISCOVERY);
        tryFire(AfterDeploymentValidation.class, AFTER_DEPLOYMENT_VALIDATION);
        tryFire(BeforeShutdown.class, BEFORE_SHUTDOWN);
        tryFire(ProcessAnnotatedType.class, PROCESS_ANNOTATED_TYPE);
        tryFire(ProcessInjectionPoint.class, PROCESS_INJECTION_POINT);
        tryFire(ProcessInjectionTarget.class, PROCESS_INJECTION_TARGET);
        tryFire(ProcessProducer.class, PROCESS_PRODUCER);
        tryFire(ProcessBeanAttributes.class, PROCESS_BEAN_ATTRIBUTES);
        tryFire(ProcessBean.class, PROCESS_BEAN);
        tryFire(ProcessObserverMethod.class, PROCESS_OBSERVER_METHOD);
        tryFire(ProcessSessionBean.class, PROCESS_SESSION_BEAN);
        tryFire(ProcessProducerField.class, PROCESS_PRODUCER_FIELD);
        tryFire(ProcessProducerMethod.class, PROCESS_PRODUCER_METHOD);
        tryFire(BeforeBeanDiscovery.class, BEFORE_BEAN_DISCOVERY);
    }

    private <T> void tryFire(Class<T> clazz, T payload) {
        try {
            event.select(clazz).fire(payload);
            throw new IllegalStateException("Expected exception (IllegalArgumentException) not thrown");
        } catch (IllegalArgumentException expected) {
            expected.getCause();
        }
    }
}
