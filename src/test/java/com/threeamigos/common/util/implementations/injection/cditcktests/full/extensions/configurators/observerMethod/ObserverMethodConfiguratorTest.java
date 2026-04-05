package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.observerMethod;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ObserverMethod;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ObserverMethodConfiguratorTest {

    private Syringe syringe;
    private BeanManager beanManager;
    private ProcessSyntheticObserverMethodObserver extension;

    @BeforeAll
    void setUp() {
        resetFlags();
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                AfterBeanDiscoveryObserver.class,
                Apple.class,
                Banana.class,
                Cherry.class,
                Delicious.class,
                Fruit.class,
                FruitObserver.class,
                Kiwi.class,
                Melon.class,
                Orange.class,
                Papaya.class,
                Peach.class,
                Pear.class,
                Pineapple.class,
                ProcessObserverMethodObserver.class,
                ProcessSyntheticObserverMethodObserver.class,
                Ripe.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProcessObserverMethodObserver.class.getName());
        syringe.addExtension(AfterBeanDiscoveryObserver.class.getName());
        syringe.addExtension(ProcessSyntheticObserverMethodObserver.class.getName());
        syringe.setup();
        beanManager = syringe.getBeanManager();
        extension = beanManager.getExtension(ProcessSyntheticObserverMethodObserver.class);
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void addQualifiersAndSetPriorityAndChangeToAsync() throws InterruptedException {
        Set<ObserverMethod<? super Pear>> pearEventObservers = beanManager
                .resolveObserverMethods(new Pear(), Any.Literal.INSTANCE, Ripe.RipeLiteral.INSTANCE,
                        Delicious.DeliciousLiteral.INSTANCE);
        assertEquals(1, pearEventObservers.size());
        assertEquals(ObserverMethod.DEFAULT_PRIORITY + 100, pearEventObservers.iterator().next().getPriority());
        assertTrue(pearEventObservers.iterator().next().isAsync());
        assertEquals(Stream.of(Ripe.RipeLiteral.INSTANCE, Delicious.DeliciousLiteral.INSTANCE).collect(Collectors.toSet()),
                pearEventObservers.iterator().next().getObservedQualifiers());

        BlockingQueue<Pear> queue = new LinkedBlockingQueue<Pear>();
        Event<Pear> pearEvent = beanManager.getEvent().select(Pear.class);
        pearEvent.select(Any.Literal.INSTANCE, Ripe.RipeLiteral.INSTANCE, Delicious.DeliciousLiteral.INSTANCE)
                .fireAsync(new Pear()).thenAccept(queue::offer);
        Pear pear = queue.poll(2, TimeUnit.SECONDS);
        assertNotNull(pear);
        assertTrue(FruitObserver.pearObserverNotified.get());
    }

    @Test
    void setReceptionAndTransactionPhase() {
        Set<ObserverMethod<? super Orange>> orangeEventObservers = beanManager
                .resolveObserverMethods(new Orange(), Any.Literal.INSTANCE, Delicious.DeliciousLiteral.INSTANCE);
        assertEquals(1, orangeEventObservers.size());
        assertEquals(Reception.IF_EXISTS, orangeEventObservers.iterator().next().getReception());
        assertEquals(TransactionPhase.AFTER_SUCCESS, orangeEventObservers.iterator().next().getTransactionPhase());
        assertEquals(Collections.singleton(Delicious.DeliciousLiteral.INSTANCE),
                orangeEventObservers.iterator().next().getObservedQualifiers());
    }

    @Test
    void notifyAcceptingConsumerNotified() {
        beanManager.getEvent().select(Pineapple.class, Any.Literal.INSTANCE, Delicious.DeliciousLiteral.INSTANCE)
                .fire(new Pineapple());
        assertTrue(ProcessObserverMethodObserver.consumerNotified.get());
        assertEquals(Stream.of(Any.Literal.INSTANCE, Delicious.DeliciousLiteral.INSTANCE).collect(Collectors.toSet()),
                ProcessObserverMethodObserver.pineAppleQualifiers);
    }

    @Test
    void addNewObserverMethodFromReadingExistingOne() {
        AfterBeanDiscoveryObserver.reset();
        beanManager.getEvent().select(Banana.class, Any.Literal.INSTANCE, Ripe.RipeLiteral.INSTANCE).fire(new Banana());
        beanManager.getEvent().select(Melon.class, Any.Literal.INSTANCE).fire(new Melon());
        beanManager.getEvent().select(Peach.class, Any.Literal.INSTANCE).fire(new Peach());

        Set<ObserverMethod<? super Peach>> peachEventObservers = beanManager.resolveObserverMethods(new Peach(),
                Any.Literal.INSTANCE);
        Set<ObserverMethod<? super Banana>> bananaEventObservers = beanManager
                .resolveObserverMethods(new Banana(), Any.Literal.INSTANCE, Ripe.RipeLiteral.INSTANCE);

        assertEquals(2, peachEventObservers.size());
        assertEquals(2, bananaEventObservers.size());
        assertTrue(AfterBeanDiscoveryObserver.newBananaObserverNotified.get());
        assertTrue(AfterBeanDiscoveryObserver.newMelonObserverNotified.get());
        assertTrue(AfterBeanDiscoveryObserver.newPeachObserverNotified.get());

        assertTrue(FruitObserver.melonObserverNotified.get());
        assertTrue(FruitObserver.peachObserverNotified.get());
        assertTrue(FruitObserver.bananaObserverNotified.get());
    }

    @Test
    void configuratorInitializedWithOriginalObserverMethod() {
        ObserverMethod<? super Kiwi> configuredOne = beanManager
                .resolveObserverMethods(new Kiwi(), Ripe.RipeLiteral.INSTANCE).iterator().next();
        ObserverMethod<Kiwi> originalOne = beanManager.getExtension(ProcessObserverMethodObserver.class).getOriginalOM();
        assertEquals(configuredOne.getObservedType(), originalOne.getObservedType());
        assertEquals(configuredOne.getObservedQualifiers(), originalOne.getObservedQualifiers());
        assertEquals(configuredOne.getPriority(), originalOne.getPriority());
    }

    @Test
    void syntheticEventInvokedAndReturningSourceTest() {
        assertEquals(Integer.valueOf(4), extension.timesInvoked());
        Map<Type, Extension> map = extension.getSources();
        assertEquals(AfterBeanDiscoveryObserver.class, map.get(Peach.class).getClass());
        assertEquals(AfterBeanDiscoveryObserver.class, map.get(Banana.class).getClass());
        assertEquals(AfterBeanDiscoveryObserver.class, map.get(Melon.class).getClass());
        assertEquals(AfterBeanDiscoveryObserver.class, map.get(Cherry.class).getClass());
    }

    @Test
    void defaultBeanClassIsExtensionClass() {
        Set<ObserverMethod<? super Papaya>> papayaEventObservers = beanManager.resolveObserverMethods(new Papaya(),
                Any.Literal.INSTANCE);
        ObserverMethod<? super Papaya> papayaObserver = papayaEventObservers.iterator().next();
        assertNotNull(papayaObserver, "There is no Papaya Observer available!");
        assertEquals(AfterBeanDiscoveryObserver.class, papayaObserver.getBeanClass());
    }

    private static void resetFlags() {
        FruitObserver.pearObserverNotified.set(false);
        FruitObserver.bananaObserverNotified.set(false);
        FruitObserver.melonObserverNotified.set(false);
        FruitObserver.peachObserverNotified.set(false);
        ProcessObserverMethodObserver.consumerNotified.set(false);
        ProcessObserverMethodObserver.pineAppleQualifiers = null;
        AfterBeanDiscoveryObserver.newBananaObserverNotified.set(false);
        AfterBeanDiscoveryObserver.newMelonObserverNotified.set(false);
        AfterBeanDiscoveryObserver.newPeachObserverNotified.set(false);
        AfterBeanDiscoveryObserver.newPapayaObserverNotified.set(false);
    }
}
