package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par22beantypes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.messagehandler.ConsoleMessageHandler;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Chapter 2 - Beans")
public class BeanTypesTest {

    private MessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        messageHandler = new ConsoleMessageHandler();
    }

    @Nested
    @DisplayName("2.2 - Bean Types")
    class BeanTypes {

        @Test
        @DisplayName("2.2 - Bookshop: has four bean types")
        void bookshopHasFourTypes() {
            // Given
            Syringe sut = new Syringe(messageHandler, BeanTypesTest.class);
            // These are classes supposed to fail later tests
            sut.exclude(VariableArrayShop.class, WildcardBookshopProducer.class);
            sut.setup();
            // When
            List<Bean<?>> beans = sut.getKnowledgeBase()
                    .getValidBeans()
                    .stream()
                    .filter(b -> b.getBeanClass().equals(Bookshop.class))
                    .collect(Collectors.toList());
            // Then
            assertEquals(1, beans.size());
            @SuppressWarnings("unchecked")
            Bean<Bookshop> bookshopBean = (Bean<Bookshop>) beans.get(0);
            assertEquals(4, bookshopBean.getTypes().size());

            List<Type> expected = Arrays.asList(Bookshop.class, Business.class,
                    new TypeToken<Shop<Book>>() {}.getType(), Object.class);
            assertThat("Collections should contain the same elements", bookshopBean.getTypes(),
                    containsInAnyOrder(expected.toArray()));
        }

        @Test
        @DisplayName("2.2 - TypedBookshop: has two bean types")
        void typedBookshopHasTwoTypes() {
            // Given
            Syringe sut = new Syringe(messageHandler, BeanTypesTest.class);
            // These are classes supposed to fail later tests
            sut.exclude(VariableArrayShop.class, WildcardBookshopProducer.class);
            sut.setup();
            // When
            List<Bean<?>> beans = sut.getKnowledgeBase()
                    .getValidBeans()
                    .stream()
                    .filter(b -> b.getBeanClass().equals(TypedBookshop.class))
                    .collect(Collectors.toList());
            // Then
            assertEquals(1, beans.size());
            @SuppressWarnings("unchecked")
            Bean<TypedBookshop> bookshopBean = (Bean<TypedBookshop>) beans.get(0);
            assertEquals(2, bookshopBean.getTypes().size());
            List<Type> expected = Arrays.asList(Business.class, Object.class);
            assertThat("Collections should contain the same elements", bookshopBean.getTypes(),
                    containsInAnyOrder(expected.toArray()));
        }

        @Test
        @DisplayName("2.2 - EmptyTypedBookshop: has one bean type")
        void emptyTypedBookshopHasOneType() {
            // Given
            Syringe sut = new Syringe(messageHandler, BeanTypesTest.class);
            // These are classes supposed to fail later tests
            sut.exclude(VariableArrayShop.class, WildcardBookshopProducer.class);
            sut.setup();
            // When
            List<Bean<?>> beans = sut.getKnowledgeBase()
                    .getValidBeans()
                    .stream()
                    .filter(b -> b.getBeanClass().equals(EmptyTypedBookshop.class))
                    .collect(Collectors.toList());
            // Then
            assertEquals(1, beans.size());
            @SuppressWarnings("unchecked")
            Bean<TypedBookshop> bookshopBean = (Bean<TypedBookshop>) beans.get(0);
            assertEquals(1, bookshopBean.getTypes().size());
            List<Type> expected = Collections.singletonList(Object.class);
            assertThat("Collections should contain the same elements", bookshopBean.getTypes(),
                    containsInAnyOrder(expected.toArray()));
        }

        @Test
        @DisplayName("2.2.1 - GenericBookshop: parameterized type with type variable (Shop<T>) is legal")
        void parameterizedTypeWithTypeVariableIsLegal() {
            // Given
            Syringe sut = new Syringe(messageHandler, BeanTypesTest.class);
            // These are classes supposed to fail later tests
            sut.exclude(VariableArrayShop.class, WildcardBookshopProducer.class);
            sut.setup();
            // When
            Bean<?> bean = beanOf(GenericBookshop.class, sut);
            // Then
            boolean hasTypeVariableInterface = bean.getTypes().stream()
                    .filter(t -> t instanceof ParameterizedType)
                    .map(t -> (ParameterizedType) t)
                    .anyMatch(pt -> pt.getRawType().equals(Shop.class)
                            && pt.getActualTypeArguments()[0] instanceof TypeVariable);
            assertTrue(hasTypeVariableInterface, "Shop<T> should be present as a bean type");
        }

        @Test
        @DisplayName("2.2.1 - WildcardBookshopProducer: wildcard bean type is pruned")
        void parameterizedTypeWithWildcardProducerIsIllegal() {
            // Given
            Syringe sut = new Syringe(messageHandler, BeanTypesTest.class);
            // These are classes supposed to fail later tests
            sut.exclude(VariableArrayShop.class);
            // When
            sut.setup();
            // Then
            boolean hasWildcardInterface = sut.getKnowledgeBase()
                    .getBeans()
                    .stream()
                    .flatMap(bean -> bean.getTypes().stream())
                    .filter(t -> t instanceof ParameterizedType)
                    .map(t -> (ParameterizedType) t)
                    .anyMatch(pt -> pt.getRawType().equals(Shop.class)
                            && pt.getActualTypeArguments()[0] instanceof WildcardType);
            assertFalse(hasWildcardInterface, "Shop<? extends Book> should not be in producer bean types");
        }

        @Test
        @DisplayName("2.2.1 - ConcreteArrayShop: array type with legal component is legal")
        void arrayTypeWithConcreteComponentIsLegal() {
            // Given
            Syringe sut = new Syringe(messageHandler, BeanTypesTest.class);
            sut.exclude(VariableArrayShop.class, WildcardBookshopProducer.class);
            sut.setup();
            // When
            Bean<?> bean = beanOf(ConcreteArrayShop.class, sut);
            // Then
            boolean hasArrayInterface = bean.getTypes().stream()
                    .filter(t -> t instanceof ParameterizedType)
                    .map(t -> (ParameterizedType) t)
                    .anyMatch(pt -> pt.getRawType().equals(Shop.class)
                            && Book[].class.equals(pt.getActualTypeArguments()[0]));
            assertTrue(hasArrayInterface, "Shop<Book[]> should be present as a legal bean type");
        }

        @Test
        @DisplayName("2.2.1 - VariableArrayShop: Array type with non-legal component (type variable) is not a legal bean type")
        void arrayTypeWithTypeVariableComponentIsIllegal() {
            // Given
            Syringe sut = new Syringe(messageHandler, BeanTypesTest.class);
            sut.exclude(WildcardBookshopProducer.class);
            sut.setup();
            // When
            Bean<?> bean = beanOf(VariableArrayShop.class, sut);
            // Then
            boolean hasIllegalArrayInterface = bean.getTypes().stream()
                    .filter(t -> t instanceof ParameterizedType)
                    .map(t -> (ParameterizedType) t)
                    .anyMatch(pt -> pt.getRawType().equals(Shop.class) && hasTypeVariableArrayArgument(pt));
            assertFalse(hasIllegalArrayInterface, "Shop<T[]> should not be a legal bean type when component is a type variable");

            assertEquals(2, bean.getTypes().size());
            List<Type> expected = Arrays.asList(VariableArrayShop.class, Object.class);
            assertThat("Collections should contain the same elements", bean.getTypes(),
                    containsInAnyOrder(expected.toArray()));
        }

    }

    private Bean<?> beanOf(Class<?> beanClass, Syringe sut) {
        return sut.getKnowledgeBase()
                .getValidBeans()
                .stream()
                .filter(b -> b.getBeanClass().equals(beanClass))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Bean not found: " + beanClass));
    }

    abstract static class TypeToken<T> {
        private final Type type;

        protected TypeToken() {
            // Get the superclass' parameterized type (e.g., TypeToken<Shop<Book>>)
            Type superclass = getClass().getGenericSuperclass();
            this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
        }

        public Type getType() {
            return type;
        }
    }

    private boolean hasTypeVariableArrayArgument(ParameterizedType pt) {
        Type arg = pt.getActualTypeArguments()[0];
        if (arg instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) arg;
            return gat.getGenericComponentType() instanceof TypeVariable;
        }
        return false;
    }

}
