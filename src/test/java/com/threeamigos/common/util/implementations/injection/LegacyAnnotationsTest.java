package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.legacyfixtures.CdiApplicationScopedFixtureBean;
import com.threeamigos.common.util.implementations.injection.legacyfixtures.LegacySingletonFixtureBean;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("Legacy annotations test")
public class LegacyAnnotationsTest {

    @Test
    @DisplayName("@Singleton bean is semantically equivalent to @ApplicationScoped")
    void shouldTreatSingletonLikeApplicationScoped() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                LegacySingletonFixtureBean.class,
                CdiApplicationScopedFixtureBean.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();

        BeanManager beanManager = syringe.getBeanManager();

        Bean<LegacySingletonFixtureBean> singletonBeanDef = resolveBean(beanManager, LegacySingletonFixtureBean.class);
        Bean<CdiApplicationScopedFixtureBean> appScopedBeanDef = resolveBean(beanManager, CdiApplicationScopedFixtureBean.class);

        LegacySingletonFixtureBean singleton1 = getBeanInstance(beanManager, singletonBeanDef);
        LegacySingletonFixtureBean singleton2 = getBeanInstance(beanManager, singletonBeanDef);
        CdiApplicationScopedFixtureBean appScoped1 = getBeanInstance(beanManager, appScopedBeanDef);
        CdiApplicationScopedFixtureBean appScoped2 = getBeanInstance(beanManager, appScopedBeanDef);

        assertSame(singleton1, singleton2, "@Singleton should resolve to a single contextual instance");
        assertSame(appScoped1, appScoped2, "@ApplicationScoped should resolve to a single contextual instance");
        assertEquals(
                ApplicationScoped.class.getName(),
                singletonBeanDef.getScope().getName(),
                "@Singleton should be normalized to @ApplicationScoped scope"
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> beanClass) {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass);
        assertFalse(beans.isEmpty(), "No bean discovered for " + beanClass.getName());
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    @SuppressWarnings("unchecked")
    private <T> T getBeanInstance(BeanManager beanManager, Bean<T> bean) {
        assertNotNull(bean, "Resolved bean must not be null");
        CreationalContext<T> ctx = beanManager.createCreationalContext(bean);
        return (T) beanManager.getContext(bean.getScope()).get(bean, ctx);
    }
}
