package com.threeamigos.common.util.implementations.injection.arquillian.tck;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import org.jboss.cdi.tck.spi.Contextuals;

/**
 * Basic TCK Contextuals SPI implementation for Syringe.
 */
public class SyringeContextualsImpl implements Contextuals {

    @Override
    public <T> Inspectable<T> create(T instance, Context context) {
        return new InspectableContextual<T>(instance);
    }

    private static final class InspectableContextual<T> implements Inspectable<T> {

        private final T instanceToCreate;
        private CreationalContext<T> creationalContextPassedToCreate;
        private T instancePassedToDestroy;
        private CreationalContext<T> creationalContextPassedToDestroy;

        private InspectableContextual(T instanceToCreate) {
            this.instanceToCreate = instanceToCreate;
        }

        @Override
        public T create(CreationalContext<T> creationalContext) {
            this.creationalContextPassedToCreate = creationalContext;
            return instanceToCreate;
        }

        @Override
        public void destroy(T instance, CreationalContext<T> creationalContext) {
            this.instancePassedToDestroy = instance;
            this.creationalContextPassedToDestroy = creationalContext;
        }

        @Override
        public CreationalContext<T> getCreationalContextPassedToCreate() {
            return creationalContextPassedToCreate;
        }

        @Override
        public T getInstancePassedToDestroy() {
            return instancePassedToDestroy;
        }

        @Override
        public CreationalContext<T> getCreationalContextPassedToDestroy() {
            return creationalContextPassedToDestroy;
        }
    }
}
