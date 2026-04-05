package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;

import java.util.HashMap;
import java.util.Map;

public class VerifyingExtension implements Extension {

    private static volatile VerifyingExtension instance;

    private int alphaAttributesObserved = 0;
    private BeanAttributes<Alpha> alphaAttributes;
    private BeanAttributes<Bravo> producedBravoAttributes;
    private BeanAttributes<BravoInterceptor> bravoInterceptorAttributes;
    private BeanAttributes<BravoDecorator> bravoDecoratorAttributes;
    private BeanAttributes<Charlie> producedCharlieAttributes;
    private BeanAttributes<Mike> mike;

    private final Map<Class<?>, Annotated> annotatedMap = new HashMap<Class<?>, Annotated>();

    public VerifyingExtension() {
        instance = this;
    }

    public static void reset() {
        instance = null;
    }

    public static VerifyingExtension getInstance() {
        return instance;
    }

    void observeAlpha(@Observes ProcessBeanAttributes<Alpha> event) {
        alphaAttributesObserved++;
        alphaAttributes = event.getBeanAttributes();
        annotatedMap.put(Alpha.class, event.getAnnotated());
    }

    void observeBravo(@Observes ProcessBeanAttributes<Bravo> event) {
        if (event.getAnnotated() instanceof AnnotatedMethod) {
            producedBravoAttributes = event.getBeanAttributes();
            annotatedMap.put(Bravo.class, event.getAnnotated());
        }
    }

    void observeBravoInterceptor(@Observes ProcessBeanAttributes<BravoInterceptor> event) {
        bravoInterceptorAttributes = event.getBeanAttributes();
    }

    void observeBravoDecorator(@Observes ProcessBeanAttributes<BravoDecorator> event) {
        bravoDecoratorAttributes = event.getBeanAttributes();
    }

    void observeCharlie(@Observes ProcessBeanAttributes<Charlie> event) {
        if (event.getAnnotated() instanceof AnnotatedField) {
            producedCharlieAttributes = event.getBeanAttributes();
            annotatedMap.put(Charlie.class, event.getAnnotated());
        }
    }

    void observeMike(@Observes ProcessBeanAttributes<Mike> event) {
        mike = event.getBeanAttributes();
    }

    public BeanAttributes<Alpha> getAlphaAttributes() {
        return alphaAttributes;
    }

    public int getAlphaAttributesObserved() {
        return alphaAttributesObserved;
    }

    public BeanAttributes<Bravo> getProducedBravoAttributes() {
        return producedBravoAttributes;
    }

    public BeanAttributes<Charlie> getProducedCharlieAttributes() {
        return producedCharlieAttributes;
    }

    public BeanAttributes<BravoInterceptor> getBravoInterceptorAttributes() {
        return bravoInterceptorAttributes;
    }

    public BeanAttributes<BravoDecorator> getBravoDecoratorAttributes() {
        return bravoDecoratorAttributes;
    }

    public BeanAttributes<Mike> getMike() {
        return mike;
    }

    public Map<Class<?>, Annotated> getAnnotatedMap() {
        return annotatedMap;
    }
}
