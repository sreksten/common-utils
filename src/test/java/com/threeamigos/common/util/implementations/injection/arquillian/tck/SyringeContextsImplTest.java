package com.threeamigos.common.util.implementations.injection.arquillian.tck;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.scopes.RequestScopedContext;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyringeContextsImplTest {

    @Test
    void shouldDeactivateRequestContextWhenSetInactiveCalled() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), TestRequestBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            syringe.activateRequestContextIfNeeded();
            Context requestContext = beanManager.getContext(RequestScoped.class);
            assertTrue(requestContext.isActive());

            Bean<TestRequestBean> bean = resolveBean(beanManager, TestRequestBean.class);
            SyringeContextsImpl contexts = new SyringeContextsImpl();
            contexts.setInactive(requestContext);

            assertThrows(ContextNotActiveException.class, () -> requestContext.get(bean));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void shouldSuspendAndRestoreSessionContextAcrossInactiveActiveTransitions() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), TestSessionBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateSession("tck-session-contexts-spi");
            Context sessionContext = beanManager.getContext(SessionScoped.class);
            assertTrue(sessionContext.isActive());

            Bean<TestSessionBean> bean = resolveBean(beanManager, TestSessionBean.class);
            CreationalContext<TestSessionBean> creationalContext = beanManager.createCreationalContext(bean);
            TestSessionBean initial = sessionContext.get(bean, creationalContext);
            assertNotNull(initial);
            initial.setId(42);

            SyringeContextsImpl contexts = new SyringeContextsImpl();
            contexts.setInactive(sessionContext);
            assertThrows(ContextNotActiveException.class, () -> sessionContext.get(bean));

            contexts.setActive(sessionContext);
            assertTrue(sessionContext.isActive());
            TestSessionBean restored = sessionContext.get(bean);
            assertNotNull(restored);
            assertEquals(42, restored.getId());

            contexts.destroyContext(sessionContext);
            contexts.setActive(sessionContext);
            assertNull(sessionContext.get(bean));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void shouldDeactivateUnknownScopeContextUsingDirectManagedMethod() {
        DeactivatableManagedContext managedContext = new DeactivatableManagedContext();
        managedContext.activate();
        assertTrue(managedContext.isActive());

        SyringeContextsImpl contexts = new SyringeContextsImpl();
        contexts.setInactive(managedContext);

        assertFalse(managedContext.isActive());
    }

    @Test
    void shouldDeactivateManagedContextUnwrappedFromDelegateField() {
        DeactivatableManagedContext managedContext = new DeactivatableManagedContext();
        managedContext.activate();
        assertTrue(managedContext.isActive());

        Context wrapper = new DelegateWrappingContext(managedContext);
        SyringeContextsImpl contexts = new SyringeContextsImpl();
        contexts.setInactive(wrapper);

        assertFalse(managedContext.isActive());
    }

    @Test
    void shouldReadScopeContextFromSuperclassFieldWhenDeactivatingRequestContext() {
        RequestScopedContext requestScopedContext = new RequestScopedContext();
        requestScopedContext.activateRequest();
        assertTrue(requestScopedContext.isActive());

        Context wrapper = new SuperclassScopeContextWrapper(requestScopedContext);
        SyringeContextsImpl contexts = new SyringeContextsImpl();
        contexts.setInactive(wrapper);

        assertFalse(requestScopedContext.isActive());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    @RequestScoped
    static class TestRequestBean implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    @SessionScoped
    static class TestSessionBean implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;

        int getId() {
            return id;
        }

        void setId(int id) {
            this.id = id;
        }
    }

    private static class DeactivatableManagedContext implements Context {
        private boolean active;

        @Override
        public Class<? extends Annotation> getScope() {
            return Dependent.class;
        }

        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            return null;
        }

        @Override
        public <T> T get(Contextual<T> contextual) {
            return null;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        void activate() {
            active = true;
        }

        public void deactivate() {
            active = false;
        }
    }

    private static class DelegateWrappingContext implements Context {
        @SuppressWarnings("unused")
        private final DeactivatableManagedContext delegate;

        private DelegateWrappingContext(DeactivatableManagedContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return Dependent.class;
        }

        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            return null;
        }

        @Override
        public <T> T get(Contextual<T> contextual) {
            return null;
        }

        @Override
        public boolean isActive() {
            return delegate.isActive();
        }
    }

    private static class ScopeContextHolder {
        @SuppressWarnings("unused")
        private final Object scopeContext;

        private ScopeContextHolder(Object scopeContext) {
            this.scopeContext = scopeContext;
        }
    }

    private static class SuperclassScopeContextWrapper extends ScopeContextHolder implements Context {

        private SuperclassScopeContextWrapper(Object scopeContext) {
            super(scopeContext);
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return RequestScoped.class;
        }

        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            return null;
        }

        @Override
        public <T> T get(Contextual<T> contextual) {
            return null;
        }

        @Override
        public boolean isActive() {
            return true;
        }
    }
}
