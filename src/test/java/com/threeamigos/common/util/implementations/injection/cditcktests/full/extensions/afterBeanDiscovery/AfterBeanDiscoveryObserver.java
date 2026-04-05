package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.enterprise.inject.spi.ProcessObserverMethod;
import jakarta.enterprise.inject.spi.ProcessSyntheticBean;
import jakarta.enterprise.inject.spi.ProcessSyntheticObserverMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class AfterBeanDiscoveryObserver implements Extension {

    public static TestableObserverMethod<Talk> addedObserverMethod;

    private final AtomicInteger talkPOMObservedCount = new AtomicInteger(0);
    private final AtomicInteger talkPSOMObservedCount = new AtomicInteger(0);
    private final AtomicInteger cockatooPBObservedCount = new AtomicInteger(0);
    private final AtomicInteger cockatooPSBObservedCount = new AtomicInteger(0);

    public void observeProcessSyntheticBean(@Observes ProcessSyntheticBean<Cockatoo> event) {
        cockatooPSBObservedCount.incrementAndGet();
        assert event.getBean().getName().equals("cockatoo");
    }

    public void observeProcessBean(@Observes ProcessBean<Cockatoo> event) {
        cockatooPBObservedCount.incrementAndGet();
    }

    public void observeProcessObserverMethod(@Observes ProcessObserverMethod<Talk, Listener> event) {
        talkPOMObservedCount.incrementAndGet();
    }

    public void observeProcessSyntheticObserverMethod(@Observes ProcessSyntheticObserverMethod<Talk, Listener> event) {
        talkPSOMObservedCount.incrementAndGet();
    }

    public void addABean(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        addBean(event, beanManager);
        addObserverMethod(event);
        event.addContext(new SuperContext());
    }

    private void addBean(AfterBeanDiscovery event, final BeanManager beanManager) {
        event.addBean(new Bean<Cockatoo>() {

            private final Set<Annotation> qualifiers = new HashSet<Annotation>(Arrays.asList(Default.Literal.INSTANCE));
            private final Set<Type> types = new HashSet<Type>(Arrays.<Type>asList(Cockatoo.class));

            @Override
            public Class<?> getBeanClass() {
                return Cockatoo.class;
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return Collections.emptySet();
            }

            @Override
            public String getName() {
                return "cockatoo";
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return qualifiers;
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return Dependent.class;
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return Collections.emptySet();
            }

            @Override
            public Set<Type> getTypes() {
                return types;
            }

            @Override
            public boolean isAlternative() {
                return false;
            }

            @Override
            public Cockatoo create(CreationalContext<Cockatoo> creationalContext) {
                Cockatoo cockatoo = new Cockatoo("Billy");
                try {
                    AnnotatedType<Cockatoo> annotatedType = beanManager.createAnnotatedType(Cockatoo.class);
                    AnnotatedField<? super Cockatoo> injectionPointField = null;
                    for (AnnotatedField<? super Cockatoo> field : annotatedType.getFields()) {
                        if (field.getBaseType().equals(InjectionPoint.class)) {
                            injectionPointField = field;
                            break;
                        }
                    }
                    if (injectionPointField != null) {
                        Object injectionPoint = beanManager.getInjectableReference(
                                beanManager.createInjectionPoint(injectionPointField),
                                creationalContext);
                        if (injectionPoint != null) {
                            cockatoo.setInjectionPoint((InjectionPoint) injectionPoint);
                        }
                    }
                } catch (Exception ignored) {
                    // Some implementations may not expose InjectionPoint metadata for synthetic beans.
                }
                return cockatoo;
            }

            @Override
            public void destroy(Cockatoo instance, CreationalContext<Cockatoo> creationalContext) {
                // No-op
            }
        });
    }

    private void addObserverMethod(AfterBeanDiscovery event) {
        addedObserverMethod = new TestableObserverMethod<Talk>() {

            private boolean observed = false;

            @Override
            public void notify(Talk event) {
                observed = true;
            }

            @Override
            public boolean isObserved() {
                return observed;
            }

            @Override
            public Class<?> getBeanClass() {
                return Listener.class;
            }

            @Override
            public Set<Annotation> getObservedQualifiers() {
                return Collections.<Annotation>singleton(Any.Literal.INSTANCE);
            }

            @Override
            public Type getObservedType() {
                return Talk.class;
            }

            @Override
            public Reception getReception() {
                return Reception.ALWAYS;
            }

            @Override
            public TransactionPhase getTransactionPhase() {
                return TransactionPhase.IN_PROGRESS;
            }
        };
        event.addObserverMethod(addedObserverMethod);
    }

    public AtomicInteger getTalkPOMObservedCount() {
        return talkPOMObservedCount;
    }

    public AtomicInteger getTalkPSOMObservedCount() {
        return talkPSOMObservedCount;
    }

    public AtomicInteger getCockatooPBObservedCount() {
        return cockatooPBObservedCount;
    }

    public AtomicInteger getCockatooPSBObservedCount() {
        return cockatooPSBObservedCount;
    }
}
