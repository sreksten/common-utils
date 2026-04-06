package com.threeamigos.common.util.implementations.injection.cditcktests.full.lookup.injectionpoint.broken.reference.ambiguous;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AmbiguousInjectableReferenceTest {

    @Test
    void testUnsatisfiedReference() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                SimpleBean.class,
                InjectedBean.class,
                DerivedInjectedBean.class,
                AmbiguousInjectionPoint.class,
                AnnotatedInjectionField.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);

        try {
            syringe.setup();
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            Bean<SimpleBean> bean = getBean(beanManager, SimpleBean.class);
            AmbiguousInjectionPoint injectionPoint = new AmbiguousInjectionPoint(bean);

            @SuppressWarnings("unchecked")
            CreationalContext<SimpleBean> creationalContext =
                    beanManager.createCreationalContext((Bean<SimpleBean>) injectionPoint.getBean());

            assertThrows(AmbiguousResolutionException.class,
                    () -> beanManager.getInjectableReference(injectionPoint, creationalContext));
        } finally {
            syringe.shutdown();
        }
    }

    private <T> Bean<T> getBean(BeanManagerImpl beanManager, Class<T> type) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Set<Bean<?>> beans = (Set) beanManager.getBeans(type);
        @SuppressWarnings("unchecked")
        Bean<T> bean = (Bean<T>) beanManager.resolve(beans);
        return bean;
    }
}
