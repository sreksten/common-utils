package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par22beantypes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.spi.Bean;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Chapter 2 - Beans")
public class BeanTypesTest {

    private MessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        messageHandler = new InMemoryMessageHandler();
    }

    @Nested
    @DisplayName("2.2 - Bean Types")
    class BeanTypes {

        @Test
        @DisplayName("2.2 - Bookshop has four types")
        void bookshopHasFourTypes() {
            // Given
            Syringe sut = new Syringe(messageHandler, Bookshop.class);
            // When
            sut.setup();
            // Then
            List<Bean<?>> beans = sut.getKnowledgeBase()
                    .getValidBeans()
                    .stream()
                    .filter(b -> b.getBeanClass().equals(Bookshop.class))
                    .collect(Collectors.toList());
            assertEquals(1, beans.size());
            @SuppressWarnings("unchecked")
            Bean<Bookshop> bookshopBean = (Bean<Bookshop>) beans.get(0);
            assertEquals(4, bookshopBean.getTypes().size());

            List<Type> expected = Arrays.asList(Bookshop.class, Business.class, new TypeToken<Shop<Book>>() {}.getType(), Object.class);

            // The containsInAnyOrder matcher checks for element presence and count, ignoring order.
            assertThat("Collections should contain the same elements", bookshopBean.getTypes(),
                    containsInAnyOrder(expected.toArray()));
        }

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
}
