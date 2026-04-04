package com.threeamigos.common.util.implementations.injection.cditcktests.full.context;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.SessionScoped;
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
import jakarta.inject.Inject;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NormalContextTest {

    @Test
    void testGetReturnsExistingInstance() {
        AfterBeanDiscoveryObserver.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            Bean<MySessionBean> mySessionBean = getAnyBean(beanManager, MySessionBean.class);
            CreationalContext<MySessionBean> creationalContext = beanManager.createCreationalContext(mySessionBean);
            Context sessionContext = beanManager.getContext(SessionScoped.class);
            String sessionId = "full-normal-context-existing-instance";

            beanManager.getContextManager().activateSession(sessionId);
            try {
                MySessionBean first = sessionContext.get(mySessionBean, creationalContext);
                first.setId(10);
                MySessionBean second = sessionContext.get(mySessionBean, creationalContext);
                assertEquals(10, second.getId());
                MySessionBean third = sessionContext.get(mySessionBean);
                assertEquals(10, third.getId());
                MySessionBean fourth = sessionContext.get(mySessionBean,
                        beanManager.createCreationalContext(mySessionBean));
                assertEquals(10, fourth.getId());
            } finally {
                beanManager.getContextManager().invalidateSession(sessionId);
            }
        } finally {
            syringe.shutdown();
            AfterBeanDiscoveryObserver.reset();
        }
    }

    @Test
    void testGetWithCreationalContextReturnsNewInstance() {
        AfterBeanDiscoveryObserver.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            MyContextual bean = AfterBeanDiscoveryObserver.getBean();
            bean.setShouldReturnNullInstances(false);
            Context sessionContext = beanManager.getContext(SessionScoped.class);
            String sessionId = "full-normal-context-new-instance";

            beanManager.getContextManager().activateSession(sessionId);
            try {
                CreationalContext<MySessionBean> creationalContext = beanManager.createCreationalContext(bean);
                MySessionBean newBean = sessionContext.get(bean, creationalContext);
                assertNotNull(newBean);
                assertTrue(bean.isCreateCalled());
            } finally {
                beanManager.getContextManager().invalidateSession(sessionId);
            }
        } finally {
            syringe.shutdown();
            AfterBeanDiscoveryObserver.reset();
        }
    }

    @Test
    void testGetMayNotReturnNullUnlessContextualCreateReturnsNull() {
        AfterBeanDiscoveryObserver.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            MyContextual bean = AfterBeanDiscoveryObserver.getBean();
            bean.setShouldReturnNullInstances(true);
            Context sessionContext = beanManager.getContext(SessionScoped.class);
            String sessionId = "full-normal-context-allow-null";

            beanManager.getContextManager().activateSession(sessionId);
            try {
                CreationalContext<MySessionBean> creationalContext = beanManager.createCreationalContext(bean);
                assertNull(sessionContext.get(bean, creationalContext));
                assertTrue(bean.isCreateCalled());
            } finally {
                beanManager.getContextManager().invalidateSession(sessionId);
            }
        } finally {
            syringe.shutdown();
            AfterBeanDiscoveryObserver.reset();
        }
    }

    @Test
    void testSameNormalScopeBeanInjectedEverywhere() {
        AfterBeanDiscoveryObserver.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            String sessionId = "full-normal-context-same-bean";

            beanManager.getContextManager().activateSession(sessionId);
            try {
                SimpleBeanA instanceOfA = getContextualReference(beanManager, SimpleBeanA.class);
                SimpleBeanB instanceOfB = getContextualReference(beanManager, SimpleBeanB.class);
                instanceOfA.getZ().setName("Ben");
                assertEquals("Ben", instanceOfA.getZ().getName());
                assertEquals("Ben", instanceOfB.getZ().getName());
            } finally {
                beanManager.getContextManager().invalidateSession(sessionId);
            }
        } finally {
            syringe.shutdown();
            AfterBeanDiscoveryObserver.reset();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                MySessionBean.class,
                SimpleBeanA.class,
                SimpleBeanB.class,
                SimpleBeanZ.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterBeanDiscoveryObserver.class.getName());
        return syringe;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Bean<T> getAnyBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beans.iterator().next();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type) {
        Bean<T> bean = resolveBean(beanManager, type);
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, type, creationalContext);
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
        }

        boolean isCreateCalled() {
            return createCalled;
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

        private int id;

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    @SessionScoped
    public static class SimpleBeanA implements Serializable {
        private static final long serialVersionUID = 1L;

        @Inject
        private SimpleBeanZ z;

        public SimpleBeanZ getZ() {
            return z;
        }
    }

    @SessionScoped
    public static class SimpleBeanB implements Serializable {
        private static final long serialVersionUID = 1L;

        @Inject
        private SimpleBeanZ z;

        public SimpleBeanZ getZ() {
            return z;
        }
    }

    @SessionScoped
    public static class SimpleBeanZ implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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
