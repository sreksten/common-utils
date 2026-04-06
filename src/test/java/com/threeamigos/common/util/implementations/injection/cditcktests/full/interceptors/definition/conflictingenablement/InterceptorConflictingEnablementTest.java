package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.definition.conflictingenablement;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Isolated
class InterceptorConflictingEnablementTest {

    @Test
    void testInterception() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler());
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);

        try {
            syringe.initialize();
            addFixtureClasses(syringe);
            addBeansXmlConfiguration(syringe);
            syringe.start();

            ActionSequence.reset();
            TestBean testBean = syringe.getBeanManager().createInstance().select(TestBean.class).get();
            testBean.ping();

            List<String> data = ActionSequence.getSequenceData();
            assertEquals(4, data.size());
            assertEquals(LoggingInterceptor.class.getName(), data.get(0));
            assertEquals(TransactionalInterceptor.class.getName(), data.get(1));
            assertEquals(AnotherTestDecorator.class.getName(), data.get(2));
            assertEquals(TestDecorator.class.getName(), data.get(3));
        } finally {
            syringe.shutdown();
        }
    }

    private static void addFixtureClasses(Syringe syringe) {
        syringe.addDiscoveredClass(AnotherTestDecorator.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Logged.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(LoggingInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Test.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TestBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TestDecorator.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Transactional.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TransactionalInterceptor.class, BeanArchiveMode.EXPLICIT);
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" "
                + "version=\"3.0\" bean-discovery-mode=\"all\">"
                + "<interceptors>"
                + "<class>" + TransactionalInterceptor.class.getName() + "</class>"
                + "<class>" + LoggingInterceptor.class.getName() + "</class>"
                + "</interceptors>"
                + "<decorators>"
                + "<class>" + TestDecorator.class.getName() + "</class>"
                + "<class>" + AnotherTestDecorator.class.getName() + "</class>"
                + "</decorators>"
                + "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
