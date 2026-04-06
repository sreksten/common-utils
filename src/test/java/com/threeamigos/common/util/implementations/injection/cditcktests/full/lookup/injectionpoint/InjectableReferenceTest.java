package com.threeamigos.common.util.implementations.injection.cditcktests.full.lookup.injectionpoint;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class InjectableReferenceTest {

    @Test
    void testGetInjectableReferenceOnBeanManager() {
        BeanWithInjectionPointMetadata.reset();

        Syringe syringe = newSyringeWithDecorators(TimestampLogger.class);
        BeanManagerImpl beanManager = null;
        try {
            syringe.setup();
            beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateRequest();

            FieldInjectionPointBean beanWithInjectedBean = getContextualReference(beanManager, FieldInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            InjectionPoint injectionPoint = beanWithInjectionPoint.getInjectedMetadata();
            assertNotNull(injectionPoint);

            @SuppressWarnings("unchecked")
            CreationalContext<BeanWithInjectionPointMetadata> creationalContext =
                    beanManager.createCreationalContext((Bean<BeanWithInjectionPointMetadata>) injectionPoint.getBean());
            Object beanInstance = beanManager.getInjectableReference(injectionPoint, creationalContext);
            assertTrue(beanInstance instanceof BeanWithInjectionPointMetadata);

            Bean<BeanWithInjectionPointMetadata> bean = getBean(beanManager, BeanWithInjectionPointMetadata.class);
            bean.destroy((BeanWithInjectionPointMetadata) beanInstance, creationalContext);
            assertTrue(BeanWithInjectionPointMetadata.isDestroyed());
        } finally {
            if (beanManager != null) {
                beanManager.getContextManager().deactivateRequest();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testGetInjectableReferenceReturnsDelegateForDelegateInjectionPoint() {
        Syringe syringe = newSyringeWithDecorators(TimestampLogger.class);
        try {
            syringe.setup();
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();

            Bean<LoggerConsumer> bean = getBean(beanManager, LoggerConsumer.class);
            InjectionPoint loggerInjectionPoint = null;
            for (InjectionPoint injectionPoint : bean.getInjectionPoints()) {
                if (injectionPoint.getAnnotated().getTypeClosure().contains(Logger.class)
                        && injectionPoint.getQualifiers().size() == 1
                        && injectionPoint.getQualifiers().contains(Default.Literal.INSTANCE)) {
                    loggerInjectionPoint = injectionPoint;
                }
            }
            assertNotNull(loggerInjectionPoint);

            @SuppressWarnings("unchecked")
            CreationalContext<Logger> creationalContext =
                    beanManager.createCreationalContext((Bean<Logger>) loggerInjectionPoint.getBean());
            Object injectedDelegateLogger = beanManager.getInjectableReference(loggerInjectionPoint, creationalContext);
            assertTrue(injectedDelegateLogger instanceof Logger);

            String message = "foo123";
            Logger logger = (Logger) injectedDelegateLogger;
            TimestampLogger.reset();
            logger.log(message);
            assertTrue(message.equals(TimestampLogger.getLoggedMessage()));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringeWithDecorators(Class<?>... decorators) {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Animal.class,
                AnimalDecorator1.class,
                AnimalDecorator2.class,
                AnimalDecorator3.class,
                AnimalStereotype.class,
                BasicLogger.class,
                BeanWithInjectionPointMetadata.class,
                Cat.class,
                Cattery.class,
                ConstructorInjectionPointBean.class,
                FieldInjectionPointBean.class,
                Logger.class,
                LoggerConsumer.class,
                MethodInjectionPointBean.class,
                TimestampLogger.class,
                Toy.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBeansXmlConfiguration(beansXmlWithDecorators(decorators));
        return syringe;
    }

    private BeansXml beansXmlWithDecorators(Class<?>... decorators) {
        StringBuilder xml = new StringBuilder();
        xml.append("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" bean-discovery-mode=\"all\" version=\"4.0\">");
        xml.append("<decorators>");
        for (Class<?> decorator : decorators) {
            xml.append("<class>").append(decorator.getName()).append("</class>");
        }
        xml.append("</decorators>");
        xml.append("</beans>");
        BeansXmlParser parser = new BeansXmlParser();
        return parser.parse(new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private <T> Bean<T> getBean(BeanManagerImpl beanManager, Class<T> type) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Set<Bean<?>> beans = (Set) beanManager.getBeans(type);
        @SuppressWarnings("unchecked")
        Bean<T> bean = (Bean<T>) beanManager.resolve(beans);
        return bean;
    }

    private <T> T getContextualReference(BeanManagerImpl beanManager, Class<T> type) {
        Bean<T> bean = getBean(beanManager, type);
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        return type.cast(beanManager.getReference(bean, type, creationalContext));
    }
}
