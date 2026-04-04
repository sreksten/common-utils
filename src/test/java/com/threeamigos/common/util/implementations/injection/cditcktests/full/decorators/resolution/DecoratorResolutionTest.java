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
package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.resolution;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class DecoratorResolutionTest<C extends Cow, F extends FresianCow> {

    private final TypeLiteral<Qux<String>> quxStringLiteral = new TypeLiteral<Qux<String>>() {
    };
    private final TypeLiteral<Qux<List<String>>> quxStringListLiteral = new TypeLiteral<Qux<List<String>>>() {
    };
    private final TypeLiteral<Grault<Integer>> graultIntegerLiteral = new TypeLiteral<Grault<Integer>>() {
    };
    private final TypeLiteral<Corge<C, C>> corgeTypeVariableExtendsCowLiteral = new TypeLiteral<Corge<C, C>>() {
    };
    private final TypeLiteral<Garply<F>> garplyExtendsFresianCowLiteral = new TypeLiteral<Garply<F>>() {
    };
    private final TypeLiteral<Garply<Cow>> garplyCowLiteral = new TypeLiteral<Garply<Cow>>() {
    };

    @Test
    void testUnboundedTypeVariables() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(Collections.<Type>singleton(Bar.class));
            assertTrue(decoratorCollectionMatches(decorators, BarDecorator.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObject() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(Collections.<Type>singleton(Baz.class));
            assertTrue(decoratorCollectionMatches(decorators, BazDecorator.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testUnboundedTypeVariablesAndObject() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(Collections.<Type>singleton(Foo.class));
            assertTrue(decoratorCollectionMatches(decorators, FooDecorator.class, FooObjectDecorator.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testIdenticalTypeParamerters() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(
                    Collections.<Type>singleton(quxStringLiteral.getType()));
            assertTrue(decoratorCollectionMatches(decorators, QuxDecorator.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNestedIdenticalTypeParamerters() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(
                    Collections.<Type>singleton(quxStringListLiteral.getType()));
            assertTrue(decoratorCollectionMatches(decorators, QuxListDecorator.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDelegateWildcardBeanActualType() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(
                    Collections.<Type>singleton(graultIntegerLiteral.getType()));
            assertTrue(decoratorCollectionMatches(decorators, GraultExtendsDecorator.class, GraultSuperDecorator.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDelegateWildcardBeanTypeVariable() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(
                    Collections.<Type>singleton(corgeTypeVariableExtendsCowLiteral.getType()));
            assertTrue(decoratorCollectionMatches(decorators, CorgeDecorator.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDelegateTypeVariableBeanTypeVariable() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(
                    Collections.<Type>singleton(garplyExtendsFresianCowLiteral.getType()));
            assertTrue(decoratorCollectionMatches(decorators, GarplyDecorator.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDelegateTypeVariableBeanActualType() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            List<Decorator<?>> decorators = syringe.getBeanManager().resolveDecorators(
                    Collections.<Type>singleton(garplyCowLiteral.getType()));
            assertTrue(decoratorCollectionMatches(decorators, GarplyDecorator.class));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                Animal.class,
                Bar.class,
                BarDecorator.class,
                BarImpl.class,
                Baz.class,
                BazDecorator.class,
                BazImpl.class,
                Corge.class,
                CorgeDecorator.class,
                CorgeDecorator2.class,
                CorgeImpl.class,
                CorgeImpl2.class,
                Cow.class,
                DecoratedType.class,
                FemaleFresianCow.class,
                Foo.class,
                FooDecorator.class,
                FooImpl.class,
                FooObjectDecorator.class,
                FresianCow.class,
                Garply.class,
                GarplyDecorator.class,
                GarplyImpl.class,
                Grault.class,
                GraultExtendsDecorator.class,
                GraultIntegerImpl.class,
                GraultSuperDecorator.class,
                Qux.class,
                QuxDecorator.class,
                QuxImpl.class,
                QuxListDecorator.class,
                QuxListImpl.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addDecoratorBeansXml(syringe,
                BarDecorator.class,
                FooDecorator.class,
                FooObjectDecorator.class,
                QuxDecorator.class,
                QuxListDecorator.class,
                BazDecorator.class,
                GraultExtendsDecorator.class,
                GraultSuperDecorator.class,
                CorgeDecorator2.class,
                CorgeDecorator.class,
                GarplyDecorator.class);
        return syringe;
    }

    private static boolean decoratorCollectionMatches(Collection<Decorator<?>> decorators, Class<?>... types) {
        Set<Class<?>> typeSet = new HashSet<Class<?>>(Arrays.asList(types));
        for (Decorator<?> decorator : decorators) {
            typeSet.remove(decorator.getBeanClass());
        }
        return typeSet.isEmpty();
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
}
