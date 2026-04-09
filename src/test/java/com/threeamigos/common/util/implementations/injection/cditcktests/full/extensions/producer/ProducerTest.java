package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.producer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.Producer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class ProducerTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        resetState();

        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bird.class,
                BirdCage.class,
                CanSpeakDecorator.class,
                Cat.class,
                CatFoodDish.class,
                CatHolder.class,
                CatHolderInterceptor.class,
                CatInterceptor.class,
                CatSpectator.class,
                CheckableInjectionTarget.class,
                Cow.class,
                CowProducer.class,
                Dog.class,
                DogBed.class,
                DogBone.class,
                DogProducer.class,
                LitterBox.class,
                Noisy.class,
                Preferred.class,
                ProducerProcessor.class,
                Quiet.class,
                Speakable.class,
                Tabby.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProducerProcessor.class.getName());
        addBeansXmlConfiguration(syringe);
        syringe.setup();

        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
        resetState();
    }

    @Test
    void testProduceAndInjectCallsInitializerAndConstructor() {
        Cat.reset();

        InjectionTarget<Cat> injectionTarget = ProducerProcessor.getCatInjectionTarget();
        CreationalContext<Cat> ctx = beanManager.createCreationalContext(null);
        Cat instance = injectionTarget.produce(ctx);

        assertTrue(Cat.isConstructorCalled());

        injectionTarget.inject(instance, ctx);
        assertTrue(Cat.isInitializerCalled());
    }

    @Test
    void testInterceptorAndDecoratorStackBuilt() {
        InjectionTarget<Cat> injectionTarget = ProducerProcessor.getCatInjectionTarget();
        CreationalContext<Cat> ctx = beanManager.createCreationalContext(null);
        Cat cat = injectionTarget.produce(ctx);
        assertEquals(11, cat.foo());
        assertEquals("Meow meow", cat.saySomething());
    }

    @Test
    void testDisposeDoesNothing() {
        InjectionTarget<Cat> injectionTarget = ProducerProcessor.getCatInjectionTarget();

        Cat cat = getContextualReference(Cat.class);
        injectionTarget.dispose(cat);
        cat.ping();
    }

    @Test
    void testGetInjectionPointsForFields() {
        InjectionTarget<Cat> injectionTarget = ProducerProcessor.getCatInjectionTarget();
        assertEquals(3, injectionTarget.getInjectionPoints().size());

        boolean injectionPointPresent = false;
        for (InjectionPoint injectionPoint : injectionTarget.getInjectionPoints()) {
            if (CatFoodDish.class.equals(injectionPoint.getType())) {
                injectionPointPresent = true;
            }
        }
        assertTrue(injectionPointPresent);
    }

    @Test
    void testGetInjectionPointsForConstructorAndInitializer() {
        InjectionTarget<Cat> injectionTarget = ProducerProcessor.getCatInjectionTarget();
        assertEquals(3, injectionTarget.getInjectionPoints().size());

        boolean constructorIPPresent = false;
        boolean initializerMethodIPPresent = false;
        for (InjectionPoint injectionPoint : injectionTarget.getInjectionPoints()) {
            if (LitterBox.class.equals(injectionPoint.getType())) {
                constructorIPPresent = true;
            }
            if (Bird.class.equals(injectionPoint.getType())) {
                initializerMethodIPPresent = true;
            }
        }

        assertTrue(initializerMethodIPPresent);
        assertTrue(constructorIPPresent);
    }

    @Test
    void testProduceCallsProducerMethod() {
        Producer<Dog> producer = ProducerProcessor.getNoisyDogProducer();
        Bean<Dog> dogBean = getUniqueBean(Dog.class, new Noisy.Literal());

        DogProducer.reset();

        Dog dog = producer.produce(beanManager.createCreationalContext(dogBean));
        assertTrue(DogProducer.isNoisyDogProducerCalled());
        assertEquals(DogProducer.NOISY_DOG_COLOR, dog.getColor());
    }

    @Test
    void testSetProducerOverridesProducer() {
        ProducerProcessor.reset();

        assertTrue(getContextualReference(Cow.class, new Noisy.Literal()) instanceof Cow);
        assertTrue(ProducerProcessor.isOverriddenCowProducerCalled());
    }

    @Test
    void testProduceAccessesProducerField() {
        Producer<Dog> producer = ProducerProcessor.getQuietDogProducer();
        Bean<Dog> dogBean = getUniqueBean(Dog.class, new Quiet.Literal());

        DogProducer.reset();

        Dog dog = producer.produce(beanManager.createCreationalContext(dogBean));
        assertEquals(DogProducer.QUIET_DOG_COLOR, dog.getColor());
    }

    @Test
    void testProducerForMethodDisposesProduct() {
        Bean<Dog> dogBean = getUniqueBean(Dog.class, new Noisy.Literal());
        Producer<Dog> producer = ProducerProcessor.getNoisyDogProducer();

        DogProducer.reset();

        Dog dog = producer.produce(beanManager.createCreationalContext(dogBean));
        assertTrue(DogProducer.isNoisyDogProducerCalled());

        producer.dispose(dog);
        assertTrue(DogProducer.isNoisyDogDisposerCalled());
    }

    @Test
    void testInjectionPointsForProducerMethod() {
        Producer<Dog> producer = ProducerProcessor.getNoisyDogProducer();
        DogProducer.reset();

        assertEquals(1, producer.getInjectionPoints().size());
        assertEquals(DogBed.class, producer.getInjectionPoints().iterator().next().getType());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInjectionTargetInject() {
        InjectionTarget<Dog> injectionTarget = ProducerProcessor.getDogInjectionTarget();
        Bean<Dog> dogBean = (Bean<Dog>) beanManager.getBeans(Dog.class).iterator().next();

        CreationalContext<Dog> dogCreationalContext = beanManager.createCreationalContext(dogBean);
        Dog dog = dogBean.create(dogCreationalContext);
        dog.setDogBone(null);

        injectionTarget.inject(dog, dogCreationalContext);
        assertNotNull(dog.getDogBone());
    }

    @Test
    void testInjectionTargetPostConstruct() {
        InjectionTarget<Dog> injectionTarget = ProducerProcessor.getDogInjectionTarget();
        Dog dog = getContextualReference(Dog.class, new Noisy.Literal());

        Dog.setPostConstructCalled(false);

        injectionTarget.postConstruct(dog);
        assertTrue(Dog.isPostConstructCalled());
    }

    @Test
    void testInjectionTargetPreDestroy() {
        InjectionTarget<Dog> injectionTarget = ProducerProcessor.getDogInjectionTarget();
        Dog dog = getContextualReference(Dog.class, new Noisy.Literal());

        Dog.setPreDestroyCalled(false);

        injectionTarget.preDestroy(dog);
        assertTrue(Dog.isPreDestroyCalled());
    }

    @Test
    void testSettingInjectionTargetReplacesIt() {
        CheckableInjectionTarget.setInjectCalled(false);

        getContextualReference(BirdCage.class);
        assertTrue(CheckableInjectionTarget.isInjectCalled());
    }

    @Test
    void testGetAnnotatedTypeOnProcessInjectionTarget() {
        AnnotatedType<Dog> dogAnnotatedType = ProducerProcessor.getDogAnnotatedType();

        assertNotNull(dogAnnotatedType);
        assertEquals(Dog.class, dogAnnotatedType.getBaseType());
    }

    private <T> Bean<T> getUniqueBean(Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        Bean<?> resolved = beanManager.resolve(beans);
        assertNotNull(resolved, "No bean resolved for type " + type.getName());
        @SuppressWarnings("unchecked")
        Bean<T> typedBean = (Bean<T>) resolved;
        return typedBean;
    }

    private <T> T getContextualReference(Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = getUniqueBean(type, qualifiers);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" "
                + "version=\"3.0\" bean-discovery-mode=\"all\">"
                + "<interceptors>"
                + "<class>" + CatHolderInterceptor.class.getName() + "</class>"
                + "<class>" + CatInterceptor.class.getName() + "</class>"
                + "</interceptors>"
                + "<decorators>"
                + "<class>" + CanSpeakDecorator.class.getName() + "</class>"
                + "</decorators>"
                + "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    private static void resetState() {
        Cat.reset();
        DogProducer.reset();
        ProducerProcessor.reset();
        CheckableInjectionTarget.setInjectCalled(false);
        Dog.setPostConstructCalled(false);
        Dog.setPreDestroyCalled(false);
    }
}
