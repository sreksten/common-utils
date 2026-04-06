package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.definition.interceptorNotListedInBeansXml;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Isolated
class InterceptorNotListedInBeansXmlNotEnabledTest {

    @Test
    void testInterceptorNotListedInBeansXmlNotInvoked() {
        TransactionInterceptor.invoked = false;

        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AccountHolder.class,
                TransactionInterceptor.class,
                Transactional.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);

        try {
            syringe.setup();

            AccountHolder accountHolder = syringe.getBeanManager().createInstance().select(AccountHolder.class).get();
            accountHolder.transfer(0);

            assertFalse(TransactionInterceptor.invoked);
        } finally {
            syringe.shutdown();
        }
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" "
                + "version=\"3.0\" bean-discovery-mode=\"all\">"
                + "<interceptors></interceptors>"
                + "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
