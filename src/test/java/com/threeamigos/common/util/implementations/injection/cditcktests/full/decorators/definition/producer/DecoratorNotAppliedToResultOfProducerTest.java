/*
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package com.threeamigos.common.util.implementations.injection.cditcktests.full.decorators.definition.producer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.AnnotationLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Execution(ExecutionMode.SAME_THREAD)
class DecoratorNotAppliedToResultOfProducerTest {

    @Test
    void testDecoratorNotAppliedToResultOfProducerMethod() {
        Syringe syringe = newSyringe();
        syringe.setup();
        boolean requestActivatedByTest = syringe.activateRequestContextIfNeeded();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            ShortTermAccount account = getContextualReference(beanManager, ShortTermAccount.class);
            ShortTermAccount producedAccount = getContextualReference(beanManager, ShortTermAccount.class, SyntheticLiteral.INSTANCE);

            assertNotNull(account);
            assertNotNull(producedAccount);

            account.deposit(10);
            account.withdraw(5);
            assertEquals(0, account.getBalance());

            producedAccount.deposit(10);
            producedAccount.withdraw(5);
            assertEquals(5, producedAccount.getBalance());
        } finally {
            if (requestActivatedByTest) {
                syringe.deactivateRequestContextIfActive();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testDecoratorNotAppliedToResultOfProducerField() {
        Syringe syringe = newSyringe();
        syringe.setup();
        boolean requestActivatedByTest = syringe.activateRequestContextIfNeeded();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            DurableAccount account = getContextualReference(beanManager, DurableAccount.class);
            DurableAccount producedAccount = getContextualReference(beanManager, DurableAccount.class, SyntheticLiteral.INSTANCE);

            assertNotNull(account);
            assertNotNull(producedAccount);

            account.deposit(20);
            account.withdraw(25);
            assertEquals(-10, account.getBalance());

            producedAccount.deposit(20);
            producedAccount.withdraw(25);
            assertEquals(-5, producedAccount.getBalance());
        } finally {
            if (requestActivatedByTest) {
                syringe.deactivateRequestContextIfActive();
            }
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                Bank.class,
                BankAccount.class,
                ChargeDecorator.class,
                DurableAccount.class,
                ShortTermAccount.class,
                Synthetic.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addDecoratorBeansXml(syringe, ChargeDecorator.class);
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
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> beanType, java.lang.annotation.Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType, qualifiers);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return (T) beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean));
    }

    private static final class SyntheticLiteral extends AnnotationLiteral<Synthetic> implements Synthetic {
        private static final SyntheticLiteral INSTANCE = new SyntheticLiteral();
    }
}
