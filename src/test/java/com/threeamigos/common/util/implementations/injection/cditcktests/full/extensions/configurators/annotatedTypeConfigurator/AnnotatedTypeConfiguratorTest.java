package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.annotatedTypeConfigurator;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnnotatedTypeConfiguratorTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                AnimalShelter.class,
                AnnotatedTypes.class,
                Cat.class,
                Cats.class,
                Countryside.class,
                DisposesLiteral.class,
                Dog.class,
                DogDependenciesProducer.class,
                DogProducer.class,
                Dogs.class,
                Feed.class,
                ProcessAnnotatedTypeObserver.class,
                ProducesLiteral.class,
                Room.class,
                Wild.class,
                WildAnimalProducer.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProcessAnnotatedTypeObserver.class.getName());
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void addMethodsOfAnnotationTypecConfigurator() {
        Bean<Dog> dogBean = getUniqueBean(Dog.class);
        CreationalContext<Dog> creationalContext = beanManager.createCreationalContext(dogBean);
        Dog dog = dogBean.create(creationalContext);

        assertNotNull(dogBean);
        assertEquals(RequestScoped.class, dogBean.getScope());

        assertNotNull(dog.getFeed());
        assertEquals(DogDependenciesProducer.dogName, dog.getName());

        List<InjectionPoint> dogsInjectionPoints = dogBean.getInjectionPoints().stream()
                .filter(injectionPoint -> injectionPoint.getQualifiers().contains(new Dogs.DogsLiteral()))
                .collect(Collectors.toList());
        assertEquals(2, dogsInjectionPoints.size());
        Optional<InjectionPoint> feedIpOptional = dogsInjectionPoints.stream()
                .filter(injectionPoint -> injectionPoint.getType().equals(Feed.class))
                .findFirst();
        assertTrue(feedIpOptional.isPresent());

        dogBean.destroy(dog, creationalContext);
        assertTrue(DogDependenciesProducer.disposerCalled.get());
    }

    @Test
    void removeMethodsOfAnnotationTypeConfigurator() {
        Bean<Cat> catBean = getUniqueBean(Cat.class);
        CreationalContext<Cat> creationalContext = beanManager.createCreationalContext(catBean);
        Cat cat = catBean.create(creationalContext);

        assertNotNull(catBean);
        assertEquals(Dependent.class, catBean.getScope());

        assertNull(cat.getFeed());
        Set<Bean<Feed>> catFeedBeans = getBeans(Feed.class, Cats.CatsLiteral.INSTANCE);
        assertEquals(0, catFeedBeans.size());

        beanManager.getEvent().select(Feed.class).fire(new Feed());
        assertFalse(cat.isFeedObserved());
    }

    @Test
    void annotatedTypesAndMemebersEqual() {
        assertTrue(ProcessAnnotatedTypeObserver.annotatedTypesEqual.get());
        assertTrue(ProcessAnnotatedTypeObserver.annotatedMethodEqual.get());
        assertTrue(ProcessAnnotatedTypeObserver.annotatedFieldEqual.get());
        assertTrue(ProcessAnnotatedTypeObserver.annotatedConstructorEqual.get());
        assertTrue(ProcessAnnotatedTypeObserver.annotatedParameterEqual.get());
    }

    @Test
    void annotationsRemovedFromAnimalShelter() {
        Bean<AnimalShelter> animalShelterBean = getUniqueBean(AnimalShelter.class);

        CreationalContext<AnimalShelter> creationalContext = beanManager.createCreationalContext(animalShelterBean);
        AnimalShelter animalShelter = animalShelterBean.create(creationalContext);
        beanManager.getEvent().select(Room.class, Cats.CatsLiteral.INSTANCE, Any.Literal.INSTANCE).fire(new Room());

        assertNotNull(animalShelterBean);
        assertNull(animalShelterBean.getName());
        assertEquals(Dependent.class, animalShelterBean.getScope());
        assertFalse(animalShelter.isPostConstructCalled());
        assertFalse(animalShelter.isRoomObserved());
        assertNull(animalShelter.getCat());
    }

    @Test
    void configuratorInitializedWithOriginalAT() {
        AnnotatedType<Cat> catAT = beanManager.getExtension(ProcessAnnotatedTypeObserver.class).getOriginalCatAT();
        assertTrue(catAT.isAnnotationPresent(RequestScoped.class));
        AnnotatedConstructor<Cat> annotatedConstructor = catAT.getConstructors().stream()
                .filter(ac -> ac.getParameters().size() == 1 && ac.getParameters().get(0).getBaseType().equals(Feed.class))
                .findFirst().get();
        assertTrue(annotatedConstructor.getParameters().iterator().next().isAnnotationPresent(Cats.class));
        assertTrue(annotatedConstructor.isAnnotationPresent(Inject.class));
    }

    @Test
    void configureAndTestConstructorAnnotatedParams() {
        Set<Bean<?>> countrysideBeans = beanManager.getBeans(Countryside.class);
        assertFalse(countrysideBeans.isEmpty());

        Countryside countryside = getContextualReference(Countryside.class);
        assertEquals("wild dog", countryside.getWildDog().getName());
        assertEquals("wild cat", countryside.getWildCat().getName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Set<Bean<T>> getBeans(Class<T> type, Annotation... qualifiers) {
        return (Set<Bean<T>>) (Set) beanManager.getBeans(type, qualifiers);
    }

    private <T> T getContextualReference(Class<T> beanType, Annotation... qualifiers) {
        Bean<T> bean = getUniqueBean(beanType, qualifiers);
        return beanType.cast(beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean)));
    }
}
