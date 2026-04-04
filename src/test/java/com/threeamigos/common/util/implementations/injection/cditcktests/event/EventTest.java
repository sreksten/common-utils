package com.threeamigos.common.util.implementations.injection.cditcktests.event;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.scopes.ContextManager;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTest {

    @Test
    void testObserverMethodParameterInjectionPoints() {
        TerrierObserver.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.getBeanManager().getEvent().select(BullTerrier.class).fire(new BullTerrier());
            assertTrue(TerrierObserver.eventObserved);
            assertTrue(TerrierObserver.parametersInjected);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStaticObserverMethodInvoked() {
        StaticObserver.reset();
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            ContextManager contextManager = beanManager.getContextManager();
            contextManager.deactivateRequest();
            try {
                beanManager.getEvent().select(Delivery.class).fire(new Delivery());
            } finally {
                contextManager.activateRequest();
            }
            assertTrue(StaticObserver.isDeliveryReceived());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPrivateObserverMethodInvoked() {
        PrivateObserver.reset();
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateRequest();
            try {
                beanManager.getEvent().select(Delivery.class).fire(new Delivery());
            } finally {
                beanManager.getContextManager().deactivateRequest();
            }
            assertTrue(PrivateObserver.isObserved);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventObjectContainsTypeVariablesWhenResolvingFails() {
        Syringe syringe = newSyringe();
        try {
            assertThrows(IllegalArgumentException.class, new Runnable() {
                @Override
                public void run() {
                    invokeResolveObserverMethodsWithTypeVariable(syringe);
                }
            }::run);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodNotifiedWhenQualifiersMatch() {
        BullTerrier.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.getBeanManager().getEvent()
                    .select(MultiBindingEvent.class, new RoleLiteral("Admin"), TameAnnotationLiteral.INSTANCE)
                    .fire(new MultiBindingEvent());

            assertTrue(BullTerrier.isMultiBindingEventObserved());
            assertTrue(BullTerrier.isSingleBindingEventObserved());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonStaticObserverMethodInherited() {
        Egg egg = new Egg();
        Syringe syringe = newSyringe();
        try {
            syringe.getBeanManager().getEvent().select(Egg.class).fire(egg);
            assertTrue(typeSetMatches(egg.getClassesVisited(), Farmer.class, LazyFarmer.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonStaticObserverMethodIndirectlyInherited() {
        StockPrice price = new StockPrice();
        Syringe syringe = newSyringe();
        try {
            syringe.getBeanManager().getEvent().select(StockPrice.class).fire(price);
            assertTrue(typeSetMatches(price.getClassesVisited(), StockWatcher.class, IntermediateStockWatcher.class,
                    IndirectStockWatcher.class));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                TerrierObserver.class,
                BullTerrier.class,
                Delivery.class,
                StaticObserver.class,
                PrivateObserver.class,
                MultiBindingEvent.class,
                Role.class,
                Tame.class,
                Volume.class,
                OrangeCheekedWaxbill.class,
                Egg.class,
                Farmer.class,
                LazyFarmer.class,
                StockPrice.class,
                StockWatcher.class,
                IntermediateStockWatcher.class,
                IndirectStockWatcher.class,
                EventPayload.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private <T> void invokeResolveObserverMethodsWithTypeVariable(Syringe syringe) {
        eventObjectContainsTypeVariables(syringe, new ArrayList<T>());
    }

    private <E> void eventObjectContainsTypeVariables(Syringe syringe, ArrayList<E> eventToFire) {
        syringe.getBeanManager().resolveObserverMethods(eventToFire);
    }

    private boolean typeSetMatches(Collection<? extends Type> types, Type... requiredTypes) {
        List<Type> required = Arrays.asList(requiredTypes);
        return requiredTypes.length == types.size() && types.containsAll(required);
    }

    @Dependent
    public static class TerrierObserver {
        static boolean eventObserved;
        static boolean parametersInjected;

        public void observeDog(@Observes BullTerrier event,
                               BeanManager beanManager,
                               @Tame Volume volume,
                               OrangeCheekedWaxbill orangeCheekedWaxbill) {
            eventObserved = true;
            parametersInjected = (beanManager != null && volume != null && orangeCheekedWaxbill != null);
        }

        static void reset() {
            eventObserved = false;
            parametersInjected = false;
        }
    }

    @Dependent
    public static class BullTerrier {
        private static boolean multiBindingEventObserved;
        private static boolean singleBindingEventObserved;

        public void observesMultiBindingEvent(@Observes @Role("Admin") @Tame MultiBindingEvent someEvent) {
            multiBindingEventObserved = true;
        }

        public void observesSingleBindingEvent(@Observes @Tame MultiBindingEvent someEvent) {
            singleBindingEventObserved = true;
        }

        public static boolean isMultiBindingEventObserved() {
            return multiBindingEventObserved;
        }

        public static boolean isSingleBindingEventObserved() {
            return singleBindingEventObserved;
        }

        public static void reset() {
            multiBindingEventObserved = false;
            singleBindingEventObserved = false;
        }
    }

    public static class Delivery {
    }

    @RequestScoped
    public static class StaticObserver {
        private static boolean deliveryReceived;

        public static void accept(@Observes Delivery delivery) {
            deliveryReceived = true;
        }

        public static boolean isDeliveryReceived() {
            return deliveryReceived;
        }

        public static void reset() {
            deliveryReceived = false;
        }
    }

    @RequestScoped
    public static class PrivateObserver {
        static boolean isObserved;

        private void observesDelivery(@Observes Delivery delivery) {
            isObserved = true;
        }

        static void reset() {
            isObserved = false;
        }
    }

    public static class MultiBindingEvent {
    }

    public static class EventPayload {
        private final List<Class<?>> classesVisited = new ArrayList<Class<?>>();

        public List<Class<?>> getClassesVisited() {
            return classesVisited;
        }

        public void recordVisit(Object observer) {
            classesVisited.add(observer.getClass());
        }
    }

    public static class Egg extends EventPayload {
    }

    @Dependent
    public static class Farmer {
        public void observeEggLaying(@Observes Egg egg) {
            egg.recordVisit(this);
        }
    }

    @Dependent
    public static class LazyFarmer extends Farmer {
    }

    public static class StockPrice extends EventPayload {
    }

    @Dependent
    public static class StockWatcher {
        public void observeStockPrice(@Observes StockPrice price) {
            price.recordVisit(this);
        }
    }

    @Dependent
    public static class IntermediateStockWatcher extends StockWatcher {
    }

    @Dependent
    public static class IndirectStockWatcher extends IntermediateStockWatcher {
    }

    @Qualifier
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Tame {
    }

    public static class TameAnnotationLiteral extends AnnotationLiteral<Tame> implements Tame {
        static final TameAnnotationLiteral INSTANCE = new TameAnnotationLiteral();
    }

    @Qualifier
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Role {
        String value();
    }

    public static class RoleLiteral extends AnnotationLiteral<Role> implements Role {
        private final String value;

        RoleLiteral(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    @Dependent
    @Tame
    public static class Volume {
    }

    @Dependent
    public static class OrangeCheekedWaxbill {
    }
}
