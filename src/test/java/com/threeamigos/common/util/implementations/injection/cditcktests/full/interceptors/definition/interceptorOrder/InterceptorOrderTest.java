package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.definition.interceptorOrder;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class InterceptorOrderTest {

    @Test
    void testInterceptorsCalledInOrderDefinedByBeansXml() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();

            Foo foo = syringe.getBeanManager().createInstance().select(Foo.class).get();
            assertNotNull(foo);

            ActionSequence.reset();
            foo.bar();

            List<String> sequence = ActionSequence.getSequenceData();
            assertEquals(2, sequence.size());
            assertEquals(SecondInterceptor.class.getName(), sequence.get(0));
            assertEquals(FirstInterceptor.class.getName(), sequence.get(1));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterceptorsInvocationOrder() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();

            AccountTransaction transaction = syringe.getBeanManager().createInstance().select(AccountTransaction.class).get();
            assertNotNull(transaction);

            ActionSequence.reset();
            transaction.execute();

            List<String> sequence = ActionSequence.getSequenceData();
            assertEquals(4, sequence.size(), sequence.toString());
            assertEquals(AnotherInterceptor.class.getName(), sequence.get(0));
            assertEquals(TransactionalInterceptor.class.getName(), sequence.get(1));
            assertEquals(Transaction.class.getName(), sequence.get(2));
            assertEquals(AccountTransaction.class.getName(), sequence.get(3));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AccountBinding.class,
                AccountTransaction.class,
                AnotherInterceptor.class,
                FirstInterceptor.class,
                Foo.class,
                Secure.class,
                SecondInterceptor.class,
                Transaction.class,
                Transactional.class,
                TransactionalInterceptor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);
        return syringe;
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" "
                + "version=\"3.0\" bean-discovery-mode=\"all\">"
                + "<interceptors>"
                + "<class>" + SecondInterceptor.class.getName() + "</class>"
                + "<class>" + FirstInterceptor.class.getName() + "</class>"
                + "<class>" + TransactionalInterceptor.class.getName() + "</class>"
                + "</interceptors>"
                + "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
