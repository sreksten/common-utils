package com.threeamigos.common.util.implementations.injection.arquillian.tck;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.cdi.tck.spi.Contexts;

/**
 * Basic TCK Contexts SPI implementation for Syringe.
 */
public class SyringeContextsImpl implements Contexts<Context> {

    @Override
    public void setActive(Context context) {
        if (context != null && RequestScoped.class.equals(context.getScope())) {
            RequestContextController controller = getRequestContextController();
            if (controller != null) {
                controller.activate();
            }
        }
    }

    @Override
    public void setInactive(Context context) {
        if (context != null && RequestScoped.class.equals(context.getScope())) {
            RequestContextController controller = getRequestContextController();
            if (controller != null) {
                controller.deactivate();
            }
        }
    }

    @Override
    public Context getRequestContext() {
        return beanManager().getContext(RequestScoped.class);
    }

    @Override
    public Context getDependentContext() {
        return beanManager().getContext(Dependent.class);
    }

    @Override
    public void destroyContext(Context context) {
        if (context != null && RequestScoped.class.equals(context.getScope())) {
            RequestContextController controller = getRequestContextController();
            if (controller != null) {
                controller.deactivate();
            }
        }
    }

    private static BeanManager beanManager() {
        return CDI.current().getBeanManager();
    }

    private static RequestContextController getRequestContextController() {
        try {
            return CDI.current().select(RequestContextController.class).get();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
