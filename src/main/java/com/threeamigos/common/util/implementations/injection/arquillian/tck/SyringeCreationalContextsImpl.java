package com.threeamigos.common.util.implementations.injection.arquillian.tck;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.cdi.tck.spi.CreationalContexts;

/**
 * Basic TCK CreationalContexts SPI implementation for Syringe.
 */
public class SyringeCreationalContextsImpl implements CreationalContexts {

    @Override
    public <T> Inspectable<T> create(Contextual<T> contextual) {
        CreationalContext<T> delegate = CDI.current().getBeanManager().createCreationalContext(contextual);
        return new InspectableCreationalContext<T>(delegate);
    }

    private static final class InspectableCreationalContext<T> implements Inspectable<T> {

        private final CreationalContext<T> delegate;
        private boolean pushCalled;
        private Object lastBeanPushed;
        private boolean releaseCalled;

        private InspectableCreationalContext(CreationalContext<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void push(T incompleteInstance) {
            pushCalled = true;
            lastBeanPushed = incompleteInstance;
            delegate.push(incompleteInstance);
        }

        @Override
        public void release() {
            releaseCalled = true;
            delegate.release();
        }

        @Override
        public boolean isPushCalled() {
            return pushCalled;
        }

        @Override
        public Object getLastBeanPushed() {
            return lastBeanPushed;
        }

        @Override
        public boolean isReleaseCalled() {
            return releaseCalled;
        }
    }
}
