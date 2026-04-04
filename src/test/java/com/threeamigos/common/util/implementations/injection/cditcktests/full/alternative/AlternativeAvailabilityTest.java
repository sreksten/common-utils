package com.threeamigos.common.util.implementations.injection.cditcktests.full.alternative;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

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
class AlternativeAvailabilityTest {

    private static final AnnotationLiteral<Wild> WILD_LITERAL = new Wild.Literal();
    private static final AnnotationLiteral<Tame> TAME_LITERAL = new Tame.Literal();

    @Test
    void testAlternativeAvailability() {
        Syringe syringe = newSyringe();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            Set<Bean<Animal>> animals = getBeans(beanManager, Animal.class);
            Set<Type> types = new HashSet<Type>();
            for (Bean<Animal> animal : animals) {
                types.addAll(animal.getTypes());
            }

            assertTrue(types.contains(Chicken.class));
            assertTrue(types.contains(Cat.class));
            assertTrue(types.contains(Bird.class));
            assertFalse(types.contains(Horse.class));
            assertFalse(types.contains(Dog.class));

            assertEquals(1, beanManager.getBeans("cat").size());
            assertEquals(0, beanManager.getBeans("dog").size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testIsAlternative() {
        Syringe syringe = newSyringe();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<?> cat = beanManager.resolve(beanManager.getBeans(Cat.class));
            assertTrue(cat.isAlternative());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAnyEnabledAlternativeStereotypeMakesAlternativeEnabled() {
        Syringe syringe = newSyringe();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, Bird.class).size());
            assertEquals(1, beanManager.getBeans("bird").size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducersOnAlternativeClass() {
        Syringe syringe = newSyringe();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(2, getBeans(beanManager, Sheep.class, WILD_LITERAL).size());
            assertEquals(0, getBeans(beanManager, Sheep.class, TAME_LITERAL).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerAlternativesOnMethodAndField() {
        Syringe syringe = newSyringe();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(2, getBeans(beanManager, Cat.class, WILD_LITERAL).size());
            assertEquals(0, getBeans(beanManager, Cat.class, TAME_LITERAL).size());

            Set<Bean<?>> snakeBeans = beanManager.getBeans(Snake.class, WILD_LITERAL);
            assertNotNull(beanManager.resolve(snakeBeans));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStereotypeAlternativeOnProducerMethodAndField() {
        Syringe syringe = newSyringe();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(0, getBeans(beanManager, Bird.class, WILD_LITERAL).size());
            assertEquals(2, getBeans(beanManager, Bird.class, TAME_LITERAL).size());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(EnabledAlternativeStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NotEnabledAlternativeStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Wild.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tame.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Chicken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Horse.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Cat.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bird.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Dog.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(CatProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BirdProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(EnabledSheepProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NotEnabledSheepProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SnakeProducer.class, BeanArchiveMode.EXPLICIT);
        addBeansXmlAlternatives(
                syringe,
                new Class<?>[]{Chicken.class, EnabledSheepProducer.class, SnakeProducer.class},
                new Class<?>[]{EnabledAlternativeStereotype.class});
        syringe.start();
        return syringe;
    }

    private void addBeansXmlAlternatives(Syringe syringe,
                                         Class<?>[] alternativeClasses,
                                         Class<?>[] alternativeStereotypes) {
        StringBuilder classEntries = new StringBuilder();
        for (Class<?> alternativeClass : alternativeClasses) {
            classEntries.append("<class>").append(alternativeClass.getName()).append("</class>");
        }
        StringBuilder stereotypeEntries = new StringBuilder();
        for (Class<?> alternativeStereotype : alternativeStereotypes) {
            stereotypeEntries.append("<stereotype>")
                    .append(alternativeStereotype.getName())
                    .append("</stereotype>");
        }

        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\">" +
                "<alternatives>" + classEntries + stereotypeEntries + "</alternatives>" +
                "</beans>";

        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @SuppressWarnings("unchecked")
    private <T> Set<Bean<T>> getBeans(BeanManager beanManager, Class<T> type, java.lang.annotation.Annotation... qualifiers) {
        return (Set<Bean<T>>) (Set<?>) beanManager.getBeans(type, qualifiers);
    }

    interface Animal {
    }

    @Alternative
    @RequestScoped
    static class Chicken implements Animal {
    }

    @Alternative
    @RequestScoped
    static class Horse implements Animal {
    }

    @EnabledAlternativeStereotype
    @Named
    @Default
    static class Cat implements Animal {
    }

    @EnabledAlternativeStereotype
    @NotEnabledAlternativeStereotype
    @Named
    @Default
    static class Bird implements Animal {
    }

    @NotEnabledAlternativeStereotype
    @Named
    static class Dog implements Animal {
    }

    @Dependent
    static class CatProducer {
        @Produces
        @Wild
        static final Cat wildCat = new Cat();

        @Produces
        @Tame
        @Alternative
        static final Cat cat = new Cat();

        @Produces
        @Wild
        Cat produceWildCat() {
            return cat;
        }

        @Produces
        @Tame
        @Alternative
        Cat produce() {
            return cat;
        }
    }

    @Dependent
    static class BirdProducer {
        @EnabledAlternativeStereotype
        @Produces
        @Tame
        final Bird tameBird = new Bird();

        @NotEnabledAlternativeStereotype
        @Produces
        @Wild
        final Bird wildBird = new Bird();

        @EnabledAlternativeStereotype
        @Produces
        @Tame
        Bird produceTame() {
            return new Bird();
        }

        @NotEnabledAlternativeStereotype
        @Produces
        @Wild
        Bird produceWild() {
            return new Bird();
        }
    }

    static class Sheep {
    }

    static class Snake {
    }

    @Alternative
    @Dependent
    static class EnabledSheepProducer {
        @Produces
        @Wild
        static final Sheep sheep = new Sheep();

        @Produces
        @Wild
        Sheep produce() {
            return sheep;
        }
    }

    @Alternative
    @Dependent
    static class NotEnabledSheepProducer {
        @Produces
        @Tame
        static final Sheep sheep = new Sheep();

        @Produces
        @Tame
        Sheep produce() {
            return sheep;
        }
    }

    @Dependent
    static class SnakeProducer {
        @Produces
        @Wild
        Snake snake = new Snake();

        @Produces
        @Alternative
        @Wild
        Snake produceWildSnake() {
            return snake;
        }
    }

    @RequestScoped
    @Stereotype
    @Alternative
    @Target({TYPE, METHOD, FIELD})
    @Retention(RUNTIME)
    public @interface EnabledAlternativeStereotype {
    }

    @RequestScoped
    @Stereotype
    @Alternative
    @Target({TYPE, METHOD, FIELD})
    @Retention(RUNTIME)
    public @interface NotEnabledAlternativeStereotype {
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Qualifier
    public @interface Wild {
        class Literal extends AnnotationLiteral<Wild> implements Wild {
            private static final long serialVersionUID = 1L;
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Qualifier
    public @interface Tame {
        class Literal extends AnnotationLiteral<Tame> implements Tame {
            private static final long serialVersionUID = 1L;
        }
    }
}
