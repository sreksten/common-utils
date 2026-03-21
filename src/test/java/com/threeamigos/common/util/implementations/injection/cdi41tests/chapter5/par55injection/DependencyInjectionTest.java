package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par55injection;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.scopes.ContextManager;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName( "5.5 - Dependency Injection Test")
public class DependencyInjectionTest {

    @Test
    @DisplayName("5.5.1 - Container uses @Inject constructor and passes injectable references")
    void shouldUseInjectConstructorForManagedBeanInstantiation() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ConstructorInjectedBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        ConstructorInjectedBean bean = syringe.inject(ConstructorInjectedBean.class);
        assertTrue(bean.isUsedInjectConstructor());
        assertTrue(!bean.isUsedNoArgConstructor());
        assertNotNull(bean.getConstructorDependency());
    }

    @Test
    @DisplayName("5.5.1 - Container uses no-arg constructor when no constructor is annotated @Inject")
    void shouldUseNoArgConstructorWhenNoInjectConstructorExists() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), NoInjectConstructorBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        NoInjectConstructorBean bean = syringe.inject(NoInjectConstructorBean.class);
        assertTrue(bean.isUsedNoArgConstructor());
        assertTrue(!bean.isUsedArgConstructor());
        assertNotNull(bean.getFieldDependency());
    }

    @Test
    @DisplayName("5.5.2 - Fields are injected before initializer methods and @PostConstruct runs afterwards")
    void shouldInjectFieldsThenCallInitializersThenPostConstructForContextualInstance() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), LifecycleManagedBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        LifecycleManagedBean bean = syringe.inject(LifecycleManagedBean.class);
        assertNotNull(bean.getSubFieldDependency());
        assertTrue(bean.isBaseInitializerSawInjectedFields());
        assertTrue(bean.isSubInitializerSawInjectedFields());
        assertTrue(bean.isBasePostConstructAfterInitializers());
        assertTrue(bean.isSubPostConstructAfterAllInitializers());
        assertEquals(Arrays.asList("base-init", "sub-init", "base-post", "sub-post"), bean.getEvents());
    }

    @Test
    @DisplayName("5.5 - Container performs dependency injection for non-contextual managed bean instances")
    void shouldPerformDependencyInjectionForNonContextualManagedBeanInstance() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), LifecycleManagedBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        BeanManager beanManager = syringe.getBeanManager();
        AnnotatedType<LifecycleManagedBean> annotatedType = beanManager.createAnnotatedType(LifecycleManagedBean.class);
        InjectionTargetFactory<LifecycleManagedBean> factory = beanManager.getInjectionTargetFactory(annotatedType);
        InjectionTarget<LifecycleManagedBean> injectionTarget = factory.createInjectionTarget(null);
        CreationalContext<LifecycleManagedBean> creationalContext = beanManager.createCreationalContext(null);

        LifecycleManagedBean instance = injectionTarget.produce(creationalContext);
        injectionTarget.inject(instance, creationalContext);
        injectionTarget.postConstruct(instance);

        assertNotNull(instance.getSubFieldDependency());
        assertTrue(instance.isBaseInitializerSawInjectedFields());
        assertTrue(instance.isSubInitializerSawInjectedFields());
        assertTrue(instance.isBasePostConstructAfterInitializers());
        assertTrue(instance.isSubPostConstructAfterAllInitializers());
        assertEquals(Arrays.asList("base-init", "sub-init", "base-post", "sub-post"), instance.getEvents());
    }

    @Test
    @DisplayName("5.5.3 - Dependent objects are destroyed after parent @PreDestroy callback completes")
    void shouldDestroyDependentObjectsAfterParentPreDestroy() {
        DependentDestructionRecorder.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ParentWithDependentChildBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        BeanManager beanManager = syringe.getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(ParentWithDependentChildBean.class);
        @SuppressWarnings("unchecked")
        Bean<ParentWithDependentChildBean> bean = (Bean<ParentWithDependentChildBean>) beanManager.resolve((Set) beans);
        CreationalContext<ParentWithDependentChildBean> context = beanManager.createCreationalContext(bean);
        ParentWithDependentChildBean instance =
                (ParentWithDependentChildBean) beanManager.getReference(bean, ParentWithDependentChildBean.class, context);

        bean.destroy(instance, context);

        assertEquals(
                Arrays.asList("parent-pre", "parent-sees-child-destroyed=false", "child-pre"),
                DependentDestructionRecorder.events()
        );
    }

    @Test
    @DisplayName("5.5.4 - Static producer and disposer methods are invoked with injectable references")
    void shouldInvokeStaticProducerAndDisposerMethodsWithInjectedParameters() {
        ProducerInvocationRecorder.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), StaticProducerDisposerBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        BeanManager beanManager = syringe.getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(StaticProducedPayload.class);
        @SuppressWarnings("unchecked")
        Bean<StaticProducedPayload> bean = (Bean<StaticProducedPayload>) beanManager.resolve((Set) beans);
        CreationalContext<StaticProducedPayload> context = beanManager.createCreationalContext(bean);
        StaticProducedPayload payload =
                (StaticProducedPayload) beanManager.getReference(bean, StaticProducedPayload.class, context);

        assertNotNull(payload);
        bean.destroy(payload, context);

        List<String> events = ProducerInvocationRecorder.events();
        int producerIndex = indexOfPrefix(events, "static-producer:");
        int disposerIndex = indexOfPrefix(events, "static-disposer:");
        int dependentAfterProducer = indexOfPrefix(events, "dependent-pre:", producerIndex + 1);
        int dependentAfterDisposer = indexOfPrefix(events, "dependent-pre:", disposerIndex + 1);

        assertTrue(producerIndex >= 0, "Missing static producer invocation event: " + events);
        assertTrue(disposerIndex > producerIndex, "Missing static disposer invocation event: " + events);
        assertTrue(dependentAfterProducer > producerIndex && dependentAfterProducer < disposerIndex,
                "Expected dependent cleanup after static producer invocation: " + events);
        assertTrue(dependentAfterDisposer > disposerIndex,
                "Expected dependent cleanup after static disposer invocation: " + events);
    }

    @Test
    @DisplayName("5.5.4 - Non-static producer/disposer methods are invoked on contextual declaring bean instance")
    void shouldInvokeNonStaticProducerAndDisposerOnDeclaringContextualInstance() {
        ProducerInvocationRecorder.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), NonStaticProducerDisposerBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        BeanManager beanManager = syringe.getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(NonStaticProducedPayload.class);
        @SuppressWarnings("unchecked")
        Bean<NonStaticProducedPayload> bean = (Bean<NonStaticProducedPayload>) beanManager.resolve((Set) beans);
        CreationalContext<NonStaticProducedPayload> context = beanManager.createCreationalContext(bean);
        NonStaticProducedPayload payload =
                (NonStaticProducedPayload) beanManager.getReference(bean, NonStaticProducedPayload.class, context);

        assertNotNull(payload);
        bean.destroy(payload, context);

        List<String> events = ProducerInvocationRecorder.events();
        int producerIndex = indexOfPrefix(events, "nonstatic-producer:");
        int disposerIndex = indexOfPrefix(events, "nonstatic-disposer:");
        int dependentAfterProducer = indexOfPrefix(events, "dependent-pre:", producerIndex + 1);
        int dependentAfterDisposer = indexOfPrefix(events, "dependent-pre:", disposerIndex + 1);

        assertTrue(producerIndex >= 0, "Missing non-static producer invocation event: " + events);
        assertTrue(disposerIndex > producerIndex, "Missing non-static disposer invocation event: " + events);
        assertTrue(dependentAfterProducer > producerIndex && dependentAfterProducer < disposerIndex,
                "Expected dependent cleanup after non-static producer invocation: " + events);
        assertTrue(dependentAfterDisposer > disposerIndex,
                "Expected dependent cleanup after non-static disposer invocation: " + events);

        String producerInstanceId = events.get(producerIndex).split(":")[1];
        String disposerReceiverInstanceId = events.get(disposerIndex).split(":")[1];
        String payloadDeclaringBeanIdSeenByDisposer = events.get(disposerIndex).split(":")[2];

        assertEquals(payload.getDeclaringBeanId(), producerInstanceId);
        assertEquals(payload.getDeclaringBeanId(), payloadDeclaringBeanIdSeenByDisposer);
        assertFalse(producerInstanceId.isEmpty());
        assertFalse(disposerReceiverInstanceId.isEmpty());
    }

    @Test
    @DisplayName("5.5.5 - Static producer field value is accessed without declaring bean instance")
    void shouldAccessStaticProducerFieldValue() {
        StaticProducerFieldBean.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), StaticProducerFieldBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        StaticProducedFieldPayload payload = syringe.inject(StaticProducedFieldPayload.class);

        assertNotNull(payload);
        assertEquals("static-field", payload.getSource());
        assertTrue(StaticProducerFieldBean.getConstructedInstances() >= 1);
    }

    @Test
    @DisplayName("5.5.5 - Non-static producer field value is accessed from contextual declaring bean instance")
    void shouldAccessNonStaticProducerFieldValueFromDeclaringContextualInstance() {
        NonStaticProducerFieldBean.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), NonStaticProducerFieldBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        NonStaticProducedFieldPayload payload = syringe.inject(NonStaticProducedFieldPayload.class);

        assertNotNull(payload);
        assertEquals(1, NonStaticProducerFieldBean.getConstructedInstances());
        assertEquals(
                NonStaticProducerFieldBean.getLastConstructedInstanceId(),
                payload.getDeclaringBeanInstanceId()
        );
    }

    @Test
    @DisplayName("5.5.6 - Static observer methods are invoked with event and injected parameters")
    void shouldInvokeStaticObserverMethodWithInjectableParameters() {
        ObserverInvocationRecorder.reset();
        StaticObserverBean.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), StaticObserverBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        syringe.getBeanManager().getEvent().select(StaticObserverInvocationEvent.class).fire(new StaticObserverInvocationEvent("evt-static"));

        List<String> events = ObserverInvocationRecorder.events();
        int observerIndex = indexOfPrefix(events, "static-observer:");
        int dependentCleanupIndex = indexOfPrefix(events, "observer-dependent-pre:", observerIndex + 1);

        assertTrue(observerIndex >= 0, "Missing static observer invocation event: " + events);
        assertTrue(dependentCleanupIndex > observerIndex,
                "Expected dependent cleanup after static observer invocation: " + events);
        assertEquals(0, StaticObserverBean.getConstructedInstances(),
                "Static observer invocation should not require creating declaring bean instance");
    }

    @Test
    @DisplayName("5.5.6 - Non-static observer methods are invoked on contextual bean instances")
    void shouldInvokeNonStaticObserverMethodOnContextualInstance() {
        ObserverInvocationRecorder.reset();
        NonStaticObserverBean.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), NonStaticObserverBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        syringe.getBeanManager().getEvent().select(NonStaticObserverInvocationEvent.class).fire(new NonStaticObserverInvocationEvent("evt-nonstatic"));

        List<String> events = ObserverInvocationRecorder.events();
        int observerIndex = indexOfPrefix(events, "nonstatic-observer:");
        int dependentCleanupIndex = indexOfPrefix(events, "observer-dependent-pre:", observerIndex + 1);

        assertTrue(observerIndex >= 0, "Missing non-static observer invocation event: " + events);
        assertTrue(dependentCleanupIndex > observerIndex,
                "Expected dependent cleanup after non-static observer invocation: " + events);

        String observerInstanceId = events.get(observerIndex).split(":")[1];
        assertFalse(observerInstanceId.isEmpty());
        assertEquals(1, NonStaticObserverBean.getConstructedInstances());
    }

    @Test
    @DisplayName("5.5.6 - Conditional observer resolves only existing contextual instance when scope is active")
    void shouldInvokeConditionalObserverOnlyWhenContextualInstanceAlreadyExists() {
        ObserverInvocationRecorder.reset();
        ConditionalRequestScopedObserverBean.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ConditionalRequestScopedObserverBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
        ContextManager contextManager = beanManager.getContextManager();

        syringe.getBeanManager().getEvent().select(ConditionalObserverInvocationEvent.class).fire(new ConditionalObserverInvocationEvent("no-request"));
        assertEquals(0, ConditionalRequestScopedObserverBean.getObservedEvents());
        assertEquals(0, ConditionalRequestScopedObserverBean.getConstructedInstances());

        contextManager.activateRequest();
        try {
            syringe.getBeanManager().getEvent().select(ConditionalObserverInvocationEvent.class).fire(new ConditionalObserverInvocationEvent("active-no-instance"));
            assertEquals(0, ConditionalRequestScopedObserverBean.getObservedEvents());
            assertEquals(0, ConditionalRequestScopedObserverBean.getConstructedInstances());

            warmUpRequestScopedInstance(beanManager, ConditionalRequestScopedObserverBean.class);
            syringe.getBeanManager().getEvent().select(ConditionalObserverInvocationEvent.class).fire(new ConditionalObserverInvocationEvent("active-existing"));

            assertEquals(1, ConditionalRequestScopedObserverBean.getObservedEvents());
            assertTrue(ConditionalRequestScopedObserverBean.getConstructedInstances() >= 1);
        } finally {
            contextManager.deactivateRequest();
        }
    }

    private int indexOfPrefix(List<String> values, String prefix) {
        return indexOfPrefix(values, prefix, 0);
    }

    private int indexOfPrefix(List<String> values, String prefix, int start) {
        for (int i = Math.max(start, 0); i < values.size(); i++) {
            if (values.get(i).startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> void warmUpRequestScopedInstance(BeanManager beanManager, Class<T> beanClass) {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass);
        Bean bean = beanManager.resolve((Set) beans);
        CreationalContext creationalContext = beanManager.createCreationalContext(bean);
        beanManager.getContext(RequestScoped.class).get((Contextual) bean, creationalContext);
    }
}
