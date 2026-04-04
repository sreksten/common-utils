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
package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.interceptor;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
class DecoratorAndInterceptorTest {

    @Test
    void testMethodCallbacks() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            ActionSequence.reset();
            useFoo(syringe.getBeanManager());

            List<String> sequence = ActionSequence.getSequenceData();
            assertEquals(4, sequence.size());
            assertEquals(FooInterceptor1.NAME, sequence.get(0));
            assertEquals(FooInterceptor2.NAME, sequence.get(1));
            assertEquals(FooDecorator1.NAME, sequence.get(2));
            assertEquals(FooDecorator2.NAME, sequence.get(3));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testLifecycleCallbacks() {
        Syringe syringe = newSyringe();
        syringe.setup();
        try {
            ActionSequence.reset();
            useFoo(syringe.getBeanManager());

            List<String> postConstruct = ActionSequence.getSequenceData("postConstruct");
            assertEquals(2, postConstruct.size());
            assertEquals(FooInterceptor1.NAME, postConstruct.get(0));
            assertEquals(FooInterceptor2.NAME, postConstruct.get(1));

            List<String> preDestroy = ActionSequence.getSequenceData("preDestroy");
            assertEquals(2, preDestroy.size());
            assertEquals(FooInterceptor1.NAME, preDestroy.get(0));
            assertEquals(FooInterceptor2.NAME, preDestroy.get(1));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                ActionSequence.class,
                Foo.class,
                FooBinding1.class,
                FooBinding2.class,
                FooDecorator1.class,
                FooDecorator2.class,
                FooInterceptor1.class,
                FooInterceptor2.class,
                FooStuff.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addInterceptorsAndDecoratorsBeansXml(syringe,
                new Class<?>[]{FooInterceptor1.class, FooInterceptor2.class},
                new Class<?>[]{FooDecorator1.class, FooDecorator2.class});
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String useFoo(BeanManager beanManager) {
        Set<Bean<?>> beans = beanManager.getBeans(Foo.class);
        Bean<Foo> bean = (Bean<Foo>) beanManager.resolve((Set) beans);
        CreationalContext<Foo> ctx = beanManager.createCreationalContext(bean);
        Foo foo = bean.create(ctx);
        String fooClass = foo.getClass().getName();
        foo.doSomething();
        bean.destroy(foo, ctx);
        return fooClass;
    }

    private void addInterceptorsAndDecoratorsBeansXml(Syringe syringe, Class<?>[] interceptorClasses, Class<?>[] decoratorClasses) {
        StringBuilder xmlBuilder = new StringBuilder()
                .append("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" ")
                .append("version=\"3.0\" bean-discovery-mode=\"all\">")
                .append("<interceptors>");
        for (Class<?> interceptorClass : interceptorClasses) {
            xmlBuilder.append("<class>").append(interceptorClass.getName()).append("</class>");
        }
        xmlBuilder.append("</interceptors><decorators>");
        for (Class<?> decoratorClass : decoratorClasses) {
            xmlBuilder.append("<class>").append(decoratorClass.getName()).append("</class>");
        }
        xmlBuilder.append("</decorators></beans>");
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xmlBuilder.toString().getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
