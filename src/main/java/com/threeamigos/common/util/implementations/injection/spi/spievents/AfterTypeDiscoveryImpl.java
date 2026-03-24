package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.implementations.injection.spi.configurators.AnnotatedTypeConfiguratorImpl;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic AfterTypeDiscovery event implementation.
 * Supports mutable interceptor/decorator/alternative ordering lists and addAnnotatedType().
 */
public class AfterTypeDiscoveryImpl extends PhaseAware implements AfterTypeDiscovery {

    private final MessageHandler messageHandler;
    private final KnowledgeBase knowledgeBase;
    private final BeanManager beanManager;
    private final List<Class<?>> alternatives;
    private final List<Class<?>> interceptors;
    private final List<Class<?>> decorators;

    public AfterTypeDiscoveryImpl(MessageHandler messageHandler,
                                  KnowledgeBase knowledgeBase,
                                  BeanManager beanManager,
                                  List<Class<?>> alternatives,
                                  List<Class<?>> interceptors,
                                  List<Class<?>> decorators) {
        super(messageHandler);
        this.messageHandler = messageHandler;
        this.knowledgeBase = knowledgeBase;
        this.beanManager = beanManager;
        this.alternatives = alternatives != null ? alternatives : new ArrayList<Class<?>>();
        this.interceptors = interceptors != null ? interceptors : new ArrayList<Class<?>>();
        this.decorators = decorators != null ? decorators : new ArrayList<Class<?>>();
    }

    @Override
    public List<Class<?>> getAlternatives() {
        return alternatives;
    }

    @Override
    public List<Class<?>> getInterceptors() {
        return interceptors;
    }

    @Override
    public List<Class<?>> getDecorators() {
        return decorators;
    }

    @Override
    public void addAnnotatedType(AnnotatedType<?> type, String id) {
        if (type == null) {
            throw new IllegalArgumentException("AnnotatedType cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        info(Phase.PROCESS_ANNOTATED_TYPE, "Adding annotated type: " + type.getJavaClass().getName() + " with ID: " + id);
        knowledgeBase.addAnnotatedType(type, id);
    }

    @Override
    public <T> AnnotatedTypeConfigurator<T> addAnnotatedType(Class<T> clazz, String id) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        info(Phase.PROCESS_ANNOTATED_TYPE, "Creating AnnotatedTypeConfigurator for: " + clazz.getName() + " with ID: " + id);

        final AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
        return new AnnotatedTypeConfiguratorImpl<T>(annotatedType) {
            @Override
            public AnnotatedType<T> complete() {
                AnnotatedType<T> configured = super.complete();
                knowledgeBase.addAnnotatedType(configured, id);
                return configured;
            }
        };
    }
}
