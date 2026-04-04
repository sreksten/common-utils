/*
 * Copyright 2010, Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.definition;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("serial")
class DecoratorDefinitionTest {

    @Test
    void testDecoratorIsManagedBean() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(MockLogger.TYPES);
            assertEquals(1, decorators.size());
            boolean implementsInterface = false;
            for (Class<?> interfaze : decorators.get(0).getClass().getInterfaces()) {
                if (Decorator.class.isAssignableFrom(interfaze)) {
                    implementsInterface = true;
                    break;
                }
            }
            assertTrue(implementsInterface);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDecoratedTypes() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(FooBar.TYPES);
            assertEquals(1, decorators.size());
            assertEquals(4, decorators.get(0).getDecoratedTypes().size());
            assertTrue(decorators.get(0).getDecoratedTypes().contains(Foo.class));
            assertTrue(decorators.get(0).getDecoratedTypes().contains(Bar.class));
            assertTrue(decorators.get(0).getDecoratedTypes().contains(Baz.class));
            assertTrue(decorators.get(0).getDecoratedTypes().contains(Boo.class));
            assertFalse(decorators.get(0).getDecoratedTypes().contains(Serializable.class));
            assertFalse(decorators.get(0).getDecoratedTypes().contains(FooDecorator.class));
            assertFalse(decorators.get(0).getDecoratedTypes().contains(AbstractFooDecorator.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDelegateInjectionPoint() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(Logger.TYPES);
            assertEquals(1, decorators.size());

            Decorator<?> decorator = decorators.get(0);
            assertEquals(1, decorator.getInjectionPoints().size());
            assertEquals(Logger.class, decorator.getInjectionPoints().iterator().next().getType());
            assertTrue(decorator.getInjectionPoints().iterator().next().getAnnotated().isAnnotationPresent(Delegate.class));
            assertEquals(Logger.class, decorator.getDelegateType());

            Set<Annotation> delegateQualifiers = decorator.getDelegateQualifiers();
            assertEquals(1, delegateQualifiers.size());
            assertEquals(Default.class, delegateQualifiers.iterator().next().annotationType());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDecoratorDoesNotImplementDelegateType() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(Bazt.TYPES);
            assertEquals(2, decorators.size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDecoratorOrdering() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(Bazt.TYPES);
            assertEquals(2, decorators.size());
            assertTrue(decorators.get(0).getTypes().contains(BazDecorator1.class));
            assertTrue(decorators.get(1).getTypes().contains(BazDecorator2.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonEnabledDecoratorNotResolved() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(Field.TYPES);
            assertEquals(0, decorators.size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInstanceOfDecoratorForEachEnabled() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertFalse(beanManager.resolveDecorators(MockLogger.TYPES).isEmpty());
            assertFalse(beanManager.resolveDecorators(FooBar.TYPES).isEmpty());
            assertFalse(beanManager.resolveDecorators(Logger.TYPES).isEmpty());
            assertEquals(2, beanManager.resolveDecorators(Bazt.TYPES).size());
            assertTrue(beanManager.resolveDecorators(Field.TYPES).isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDuplicateBindingsOnResolveDecoratorsFails() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            final Annotation binding = new Meta.Literal();
            assertThrows(IllegalArgumentException.class,
                    () -> syringe.getBeanManager().resolveDecorators(FooBar.TYPES, binding, binding));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonBindingsOnResolveDecoratorsFails() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            final Annotation binding = new NonMeta.Literal();
            assertThrows(IllegalArgumentException.class,
                    () -> syringe.getBeanManager().resolveDecorators(FooBar.TYPES, binding));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEmptyTypeSetOnResolveDecoratorsFails() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            final Annotation binding = new NonMeta.Literal();
            assertThrows(IllegalArgumentException.class,
                    () -> syringe.getBeanManager().resolveDecorators(new HashSet<Type>(), binding));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAbstractDecoratorNotImplementingMethodOfDecoratedType() {
        Syringe syringe = createSyringe();
        syringe.setup();
        try {
            BankAccount account = getContextualReference(syringe.getBeanManager(), BankAccount.class);
            ChargeDecorator.reset();
            account.deposit(100);
            assertEquals(0, ChargeDecorator.charged);
            account.withdraw(50);
            assertEquals(5, ChargeDecorator.charged);
            assertEquals(45, account.getBalance());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe createSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AbstractFooDecorator.class,
                Account.class,
                BankAccount.class,
                Bar.class,
                Baz.class,
                BazDecorator1.class,
                BazDecorator2.class,
                Bazt.class,
                BaztImpl.class,
                Boo.class,
                ChargeDecorator.class,
                CowShed.class,
                Field.class,
                FieldDecorator.class,
                FieldImpl.class,
                Foo.class,
                FooBar.class,
                FooBarImpl.class,
                FooDecorator.class,
                Logger.class,
                Meta.class,
                MockLogger.class,
                NonMeta.class,
                TimestampLogger.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addDecoratorBeansXml(
                syringe,
                BazDecorator1.class,
                BazDecorator2.class,
                FooDecorator.class,
                TimestampLogger.class,
                ChargeDecorator.class
        );
        return syringe;
    }

    private void addDecoratorBeansXml(Syringe syringe, Class<?>... decoratorClasses) {
        StringBuilder xmlBuilder = new StringBuilder()
                .append("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" ")
                .append("version=\"3.0\" bean-discovery-mode=\"all\">")
                .append("<decorators>");
        for (Class<?> decoratorClass : decoratorClasses) {
            xmlBuilder.append("<class>").append(decoratorClass.getName()).append("</class>");
        }
        xmlBuilder.append("</decorators></beans>");
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xmlBuilder.toString().getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> beanType) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return (T) beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean));
    }
}
