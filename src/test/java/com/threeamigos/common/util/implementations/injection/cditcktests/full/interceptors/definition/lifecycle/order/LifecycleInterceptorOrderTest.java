package com.threeamigos.common.util.implementations.injection.cditcktests.full.interceptors.definition.lifecycle.order;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Isolated
class LifecycleInterceptorOrderTest {

    @Test
    void testLifecycleCallbackInvocationOrder() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler());
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);

        try {
            syringe.initialize();
            addFixtureClasses(syringe);
            addBeansXmlConfiguration(syringe);
            syringe.start();

            ActionSequence.reset();

            BeanManager beanManager = syringe.getBeanManager();
            Bean<AccountTransaction> bean = getUniqueBean(beanManager, AccountTransaction.class);
            CreationalContext<AccountTransaction> ctx = beanManager.createCreationalContext(bean);
            AccountTransaction transaction = bean.create(ctx);
            transaction.execute();
            bean.destroy(transaction, ctx);

            List<String> postConstruct = ActionSequence.getSequenceData("postConstruct");
            assertEquals(4, postConstruct.size());
            assertEquals(AnotherInterceptor.class.getName(), postConstruct.get(0));
            assertEquals(TransactionalInterceptor.class.getName(), postConstruct.get(1));
            assertEquals(Transaction.class.getName(), postConstruct.get(2));
            assertEquals(AccountTransaction.class.getName(), postConstruct.get(3));

            List<String> preDestroy = ActionSequence.getSequenceData("preDestroy");
            assertEquals(4, preDestroy.size());
            assertEquals(AnotherInterceptor.class.getName(), postConstruct.get(0));
            assertEquals(TransactionalInterceptor.class.getName(), postConstruct.get(1));
            assertEquals(Transaction.class.getName(), postConstruct.get(2));
            assertEquals(AccountTransaction.class.getName(), postConstruct.get(3));
        } finally {
            syringe.shutdown();
        }
    }

    private static void addFixtureClasses(Syringe syringe) {
        syringe.addDiscoveredClass(AccountTransaction.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AnotherInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Transaction.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TransactionalInterceptor.class, BeanArchiveMode.EXPLICIT);
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" "
                + "version=\"3.0\" bean-discovery-mode=\"all\">"
                + "<interceptors>"
                + "<class>" + TransactionalInterceptor.class.getName() + "</class>"
                + "</interceptors>"
                + "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @SuppressWarnings("unchecked")
    private static <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        assertEquals(1, beans.size());
        return (Bean<T>) beans.iterator().next();
    }
}
