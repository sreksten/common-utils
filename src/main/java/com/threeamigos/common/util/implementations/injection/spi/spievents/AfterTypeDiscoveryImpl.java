package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.implementations.injection.spi.configurators.AnnotatedTypeConfiguratorImpl;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import jakarta.enterprise.inject.Alternative;
import jakarta.interceptor.Interceptor;
import jakarta.decorator.Decorator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Basic AfterTypeDiscovery event implementation.
 * Supports mutable interceptor/decorator/alternative ordering lists and addAnnotatedType().
 */
public class AfterTypeDiscoveryImpl extends PhaseAware implements AfterTypeDiscovery, ObserverInvocationLifecycle, ExtensionAwareObserverInvocation {

    private final MessageHandler messageHandler;
    private final KnowledgeBase knowledgeBase;
    private final BeanManager beanManager;
    private final List<Class<?>> alternatives;
    private final List<Class<?>> interceptors;
    private final List<Class<?>> decorators;
    private final ThreadLocal<Extension> currentObserverExtension = new ThreadLocal<Extension>();
    private final ThreadLocal<java.util.List<Runnable>> endOfObserverActions =
            new ThreadLocal<java.util.List<Runnable>>() {
                @Override
                protected java.util.List<Runnable> initialValue() {
                    return new java.util.ArrayList<Runnable>();
                }
            };
    private final ThreadLocal<Boolean> observerInvocationActive = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

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
        assertObserverInvocationActive();
        return alternatives;
    }

    @Override
    public List<Class<?>> getInterceptors() {
        assertObserverInvocationActive();
        return interceptors;
    }

    @Override
    public List<Class<?>> getDecorators() {
        assertObserverInvocationActive();
        return decorators;
    }

    @Override
    public void addAnnotatedType(AnnotatedType<?> type, String id) {
        assertObserverInvocationActive();
        if (type == null) {
            throw new IllegalArgumentException("AnnotatedType cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        rejectNonPortableType(type.getJavaClass());
        info(Phase.PROCESS_ANNOTATED_TYPE, "Adding annotated type: " + type.getJavaClass().getName() + " with ID: " + id);
        knowledgeBase.addAnnotatedType(type, id, currentObserverExtension.get());
    }

    @Override
    public <T> AnnotatedTypeConfigurator<T> addAnnotatedType(Class<T> clazz, String id) {
        assertObserverInvocationActive();
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        info(Phase.PROCESS_ANNOTATED_TYPE, "Creating AnnotatedTypeConfigurator for: " + clazz.getName() + " with ID: " + id);

        final AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
        final AtomicBoolean applied = new AtomicBoolean(false);
        final AnnotatedTypeConfiguratorImpl<T> configurator = new AnnotatedTypeConfiguratorImpl<T>(annotatedType) {
            @Override
            public AnnotatedType<T> complete() {
                AnnotatedType<T> configured = super.complete();
                if (applied.compareAndSet(false, true)) {
                    rejectNonPortableType(configured.getJavaClass());
                    knowledgeBase.addAnnotatedType(configured, id, currentObserverExtension.get());
                }
                return configured;
            }
        };
        registerEndOfObserverAction(() -> configurator.complete());
        return configurator;
    }

    @Override
    public void beginObserverInvocation() {
        observerInvocationActive.set(Boolean.TRUE);
    }

    @Override
    public void enterObserverInvocation(Extension extension) {
        currentObserverExtension.set(extension);
    }

    @Override
    public void exitObserverInvocation() {
        currentObserverExtension.remove();
    }

    @Override
    public void endObserverInvocation() {
        try {
            java.util.List<Runnable> actions = endOfObserverActions.get();
            if (actions.isEmpty()) {
                return;
            }

            java.util.List<Runnable> pending = new java.util.ArrayList<Runnable>(actions);
            actions.clear();
            for (Runnable action : pending) {
                action.run();
            }
        } finally {
            observerInvocationActive.set(Boolean.FALSE);
        }
    }

    private void registerEndOfObserverAction(Runnable action) {
        if (action == null) {
            return;
        }
        endOfObserverActions.get().add(action);
    }

    private void rejectNonPortableType(Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        if (clazz.isAnnotationPresent(Alternative.class) ||
            clazz.isAnnotationPresent(Interceptor.class) ||
            clazz.isAnnotationPresent(Decorator.class)) {
            throw new NonPortableBehaviourException(
                    "Adding alternative/interceptor/decorator via AfterTypeDiscovery.addAnnotatedType is non-portable: " +
                            clazz.getName());
        }
    }

    private void assertObserverInvocationActive() {
        if (!observerInvocationActive.get()) {
            throw new IllegalStateException("AfterTypeDiscovery methods may only be called during observer method invocation");
        }
    }
}
