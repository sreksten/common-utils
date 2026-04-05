package com.threeamigos.common.util.implementations.injection.cditcktests.full.implementation.builtin.metadata;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class BuiltinMetadataBeanTest {

    private Syringe syringe;
    private BeanManager beanManager;
    private BeanManagerImpl beanManagerImpl;
    private Yoghurt yoghurt;
    private YoghurtFactory factory;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Frozen.class,
                Fruit.class,
                MilkProduct.class,
                MilkProductDecorator.class,
                Probiotic.class,
                Yoghurt.class,
                YoghurtFactory.class,
                YoghurtInterceptor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);
        syringe.setup();

        beanManager = syringe.getBeanManager();
        beanManagerImpl = (BeanManagerImpl) beanManager;
        beanManagerImpl.getContextManager().activateSession("builtin-metadata-bean-test-session");
        yoghurt = getContextualReference(Yoghurt.class);
        factory = getContextualReference(YoghurtFactory.class);
    }

    @AfterAll
    void tearDown() {
        if (beanManagerImpl != null) {
            beanManagerImpl.getContextManager().deactivateSession();
        }
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanMetadata() {
        Bean<?> resolvedBean = getUniqueBean(Yoghurt.class);
        assertEquals(resolvedBean, yoghurt.getBeanBean());
    }

    @Test
    void testProducerMethodMetadata() {
        Bean<Yoghurt> fruitYoghurtBean = getUniqueBean(Yoghurt.class, new Fruit.Literal());
        CreationalContext<Yoghurt> fruitCtx = beanManager.createCreationalContext(fruitYoghurtBean);
        beanManager.getReference(fruitYoghurtBean, Yoghurt.class, fruitCtx);
        assertEquals(fruitYoghurtBean, factory.getFruitYoghurtBean());

        Bean<Yoghurt> probioticYoghurtBean = getUniqueBean(Yoghurt.class, new Probiotic.Literal());
        CreationalContext<Yoghurt> probioticCtx = beanManager.createCreationalContext(probioticYoghurtBean);
        beanManager.getReference(probioticYoghurtBean, Yoghurt.class, probioticCtx);
        assertEquals(probioticYoghurtBean, factory.getProbioticYoghurtBean());
    }

    @Test
    void testInterceptorMetadata() {
        Bean<?> bean = getUniqueBean(Yoghurt.class);
        Interceptor<?> interceptor = beanManager
                .resolveInterceptors(InterceptionType.AROUND_INVOKE, new Frozen.Literal()).iterator().next();
        YoghurtInterceptor yoghurtInterceptor = yoghurt.getInterceptorInstance();

        assertEquals(interceptor, yoghurtInterceptor.getBean());
        assertEquals(interceptor, yoghurtInterceptor.getInterceptor());
        assertEquals(bean, yoghurtInterceptor.getInterceptedBean());
    }

    @Test
    void testDecoratorMetadata() {
        Bean<?> bean = getUniqueBean(Yoghurt.class);
        Decorator<?> decorator = beanManager.resolveDecorators(Collections.<Type>singleton(MilkProduct.class))
                .iterator().next();
        MilkProductDecorator instance = yoghurt.getDecoratorInstance();

        assertEquals(decorator, instance.getBean());
        assertEquals(decorator, instance.getDecorator());
        assertEquals(bean, instance.getDecoratedBean());
    }

    @SuppressWarnings("unchecked")
    private <T> Bean<T> getUniqueBean(Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        Bean<?> resolved = beanManager.resolve(beans);
        assertNotNull(resolved, "No bean resolved for type " + type.getName());
        return (Bean<T>) resolved;
    }

    private <T> T getContextualReference(Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = getUniqueBean(type, qualifiers);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" "
                + "version=\"3.0\" bean-discovery-mode=\"all\">"
                + "<interceptors><class>" + YoghurtInterceptor.class.getName() + "</class></interceptors>"
                + "<decorators><class>" + MilkProductDecorator.class.getName() + "</class></decorators>"
                + "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
