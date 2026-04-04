package com.threeamigos.common.util.implementations.injection.cditcktests.full.context;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextDestroysBeansTest {

    @Test
    void testContextDestroysBeansWhenDestroyed() {
        AfterBeanDiscoveryObserver.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), MySessionBean.class, DummyContext.class, DummyScoped.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterBeanDiscoveryObserver.class.getName());

        try {
            syringe.setup();

            MyContextual bean = AfterBeanDiscoveryObserver.getBean();
            assertNotNull(bean);
            bean.setShouldReturnNullInstances(false);

            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            Context sessionContext = beanManager.getContext(SessionScoped.class);
            String sessionId = "context-destroys-beans";

            beanManager.getContextManager().activateSession(sessionId);
            try {
                CreationalContext<MySessionBean> creationalContext = beanManager.createCreationalContext(bean);
                MySessionBean instance = sessionContext.get(bean, creationalContext);
                assertNotNull(instance);
                instance.ping();
                assertTrue(bean.isCreateCalled());
            } finally {
                beanManager.getContextManager().invalidateSession(sessionId);
            }

            assertTrue(bean.isDestroyCalled());
        } finally {
            syringe.shutdown();
            AfterBeanDiscoveryObserver.reset();
        }
    }

    public static class AfterBeanDiscoveryObserver implements Extension {
        private static MyContextual bean;

        public void addCustomBeanImplementation(@Observes AfterBeanDiscovery event, BeanManager manager) {
            bean = new MyContextual(manager);
            event.addBean(bean);
        }

        public void addNewContexts(@Observes AfterBeanDiscovery event) {
            event.addContext(new DummyContext());
            event.addContext(new DummyContext());
        }

        static MyContextual getBean() {
            return bean;
        }

        static void reset() {
            bean = null;
        }
    }

    static class MyContextual implements Bean<MySessionBean>, PassivationCapable {
        private boolean createCalled;
        private boolean destroyCalled;
        private boolean shouldReturnNullInstances;

        MyContextual(BeanManager beanManager) {
            // BeanManager is intentionally accepted to mirror original TCK fixture shape.
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return Collections.emptySet();
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public String getName() {
            return "my-session-bean";
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return SessionScoped.class;
        }

        @Override
        public Set<Type> getTypes() {
            return new HashSet<Type>(Arrays.<Type>asList(Object.class, MySessionBean.class, Serializable.class));
        }

        @Override
        public MySessionBean create(CreationalContext<MySessionBean> creationalContext) {
            createCalled = true;
            if (shouldReturnNullInstances) {
                return null;
            }
            return new MySessionBean();
        }

        @Override
        public void destroy(MySessionBean instance, CreationalContext<MySessionBean> creationalContext) {
            destroyCalled = true;
        }

        boolean isCreateCalled() {
            return createCalled;
        }

        boolean isDestroyCalled() {
            return destroyCalled;
        }

        void setShouldReturnNullInstances(boolean shouldReturnNullInstances) {
            this.shouldReturnNullInstances = shouldReturnNullInstances;
        }

        @Override
        public Class<?> getBeanClass() {
            return MySessionBean.class;
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public String getId() {
            return "org.jboss.cdi.tck.tests.context.myContextual";
        }
    }

    @SessionScoped
    public static class MySessionBean implements Serializable {
        private static final long serialVersionUID = 1L;

        public void ping() {
        }
    }

    public static class DummyContext implements Context {

        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T get(Contextual<T> contextual) {
            return get(contextual, null);
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return DummyScoped.class;
        }

        @Override
        public boolean isActive() {
            return true;
        }
    }

    @Target({TYPE, METHOD, FIELD})
    @Retention(RUNTIME)
    @Documented
    @NormalScope
    public @interface DummyScoped {
    }
}
