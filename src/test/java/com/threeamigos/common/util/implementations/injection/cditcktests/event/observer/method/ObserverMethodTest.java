package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.method;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static jakarta.enterprise.event.TransactionPhase.AFTER_COMPLETION;
import static jakarta.enterprise.event.TransactionPhase.AFTER_FAILURE;
import static jakarta.enterprise.event.TransactionPhase.AFTER_SUCCESS;
import static jakarta.enterprise.event.TransactionPhase.BEFORE_COMPLETION;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class ObserverMethodTest {

    @Test
    void testGetBeanClassOnObserverMethod() {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super StockPrice>> observers = syringe.getBeanManager().resolveObserverMethods(new StockPrice());
            assertEquals(1, observers.size());
            ObserverMethod<? super StockPrice> observerMethod = observers.iterator().next();
            assertEquals(StockWatcher.class, observerMethod.getBeanClass());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetDeclaringBeanOnObserverMethod() {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super StockPrice>> observers = syringe.getBeanManager().resolveObserverMethods(new StockPrice());
            assertEquals(1, observers.size());
            ObserverMethod<? super StockPrice> observerMethod = observers.iterator().next();
            Bean<?> declaringBean = observerMethod.getDeclaringBean();
            assertNotNull(declaringBean);
            assertEquals(StockWatcher.class, declaringBean.getBeanClass());
            assertEquals(Dependent.class, declaringBean.getScope());
            assertTrue(declaringBean.isAlternative());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetObservedTypeOnObserverMethod() {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super StockPrice>> observers = syringe.getBeanManager().resolveObserverMethods(new StockPrice());
            assertEquals(1, observers.size());
            ObserverMethod<?> observerMethod = observers.iterator().next();
            assertEquals(StockPrice.class, observerMethod.getObservedType());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetObservedQualifiersOnObserverMethod() {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super StockPrice>> observers = syringe.getBeanManager().resolveObserverMethods(new StockPrice());
            assertEquals(1, observers.size());
            ObserverMethod<?> observerMethod = observers.iterator().next();
            assertTrue(observerMethod.getObservedQualifiers().isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetNotifyOnObserverMethod() {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super StockPrice>> observers = syringe.getBeanManager().resolveObserverMethods(new StockPrice());
            assertEquals(1, observers.size());
            assertEquals(Reception.ALWAYS, observers.iterator().next().getReception());

            Set<ObserverMethod<? super ConditionalEvent>> conditionalObservers =
                    syringe.getBeanManager().resolveObserverMethods(new ConditionalEvent());
            assertFalse(conditionalObservers.isEmpty());
            assertEquals(Reception.IF_EXISTS, conditionalObservers.iterator().next().getReception());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetTransactionPhaseOnObserverMethod() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(TransactionPhase.IN_PROGRESS,
                    syringe.getBeanManager().resolveObserverMethods(new StockPrice()).iterator().next().getTransactionPhase());
            assertEquals(TransactionPhase.BEFORE_COMPLETION,
                    syringe.getBeanManager().resolveObserverMethods(new DisobedientDog()).iterator().next().getTransactionPhase());
            assertEquals(TransactionPhase.AFTER_COMPLETION,
                    syringe.getBeanManager().resolveObserverMethods(new ShowDog()).iterator().next().getTransactionPhase());
            assertEquals(TransactionPhase.AFTER_FAILURE,
                    syringe.getBeanManager().resolveObserverMethods(new SmallDog()).iterator().next().getTransactionPhase());
            assertEquals(TransactionPhase.AFTER_SUCCESS,
                    syringe.getBeanManager().resolveObserverMethods(new LargeDog()).iterator().next().getTransactionPhase());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInstanceOfBeanForEveryEnabledObserverMethod() {
        Syringe syringe = newSyringe();
        try {
            assertFalse(syringe.getBeanManager().resolveObserverMethods(new StockPrice()).isEmpty());
            assertFalse(syringe.getBeanManager().resolveObserverMethods(new DisobedientDog()).isEmpty());
            assertFalse(syringe.getBeanManager().resolveObserverMethods(new ShowDog()).isEmpty());
            assertFalse(syringe.getBeanManager().resolveObserverMethods(new SmallDog()).isEmpty());
            assertFalse(syringe.getBeanManager().resolveObserverMethods(new LargeDog()).isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNotifyOnObserverMethod() {
        Syringe syringe = newSyringe();
        try {
            IntegerObserver.wasNotified = false;
            Integer event = Integer.valueOf(1);
            Set<ObserverMethod<? super Integer>> observerMethods =
                    syringe.getBeanManager().resolveObserverMethods(event, new Number.Literal());
            assertEquals(1, observerMethods.size());
            observerMethods.iterator().next().notify(event);
            assertTrue(IntegerObserver.wasNotified);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                EventPayload.class,
                StockPrice.class,
                StockWatcher.class,
                ConditionalEvent.class,
                ConditionalObserver.class,
                DisobedientDog.class,
                ShowDog.class,
                SmallDog.class,
                LargeDog.class,
                TransactionalObservers.class,
                Number.class,
                IntegerObserver.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    static class ConditionalEvent {
    }

    @RequestScoped
    static class ConditionalObserver {
        void conditionalObserve(@Observes(notifyObserver = Reception.IF_EXISTS) ConditionalEvent event) {
        }
    }

    static class DisobedientDog {
    }

    static class ShowDog {
    }

    static class SmallDog {
    }

    static class LargeDog {
    }

    static class EventPayload {
        private final List<Class<?>> classesVisited = new ArrayList<Class<?>>();

        List<Class<?>> getClassesVisited() {
            return classesVisited;
        }

        void recordVisit(Object observer) {
            classesVisited.add(observer.getClass());
        }
    }

    static class StockPrice extends EventPayload {
    }

    @Dependent
    @Alternative
    @Priority(1)
    static class StockWatcher {
        private static Class<?> observerClazz;

        public void observeStockPrice(@Observes StockPrice price) {
            observerClazz = this.getClass();
            price.recordVisit(this);
        }

        public static Class<?> getObserverClazz() {
            return observerClazz;
        }
    }

    @Dependent
    static class TransactionalObservers {
        public void train(@Observes(during = BEFORE_COMPLETION) DisobedientDog dog) {
        }

        public void trainNewTricks(@Observes(during = AFTER_COMPLETION) ShowDog dog) {
        }

        public void trainCompanion(@Observes(during = AFTER_FAILURE) SmallDog dog) {
        }

        public void trainSightSeeing(@Observes(during = AFTER_SUCCESS) LargeDog dog) {
        }
    }

    @Dependent
    static class IntegerObserver {
        static boolean wasNotified = false;

        public static void observeInteger(@Observes @Number Integer event) {
            wasNotified = true;
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Number {
        class Literal extends AnnotationLiteral<Number> implements Number {
            private static final long serialVersionUID = 1L;
        }
    }
}
