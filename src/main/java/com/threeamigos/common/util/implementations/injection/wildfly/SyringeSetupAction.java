package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.spi.SyringeCDIProvider;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.as.server.deployment.SetupAction;

import java.lang.reflect.Method;

/**
 * SyringeSetupAction - Manages the ThreadLocal CDI context for Syringe in WildFly.
 *
 * <p>This implementation ensures that {@code CDI.current()} returns the correct
 * Syringe instance for the current deployment during the execution of a request.
 *
 * @author Stefano Reksten
 */
public class SyringeSetupAction implements SetupAction {

    private static final String REQUEST_ACTIVATED_PROPERTY = SyringeSetupAction.class.getName() + ".requestActivated";

    private final Syringe syringe;

    public SyringeSetupAction(Syringe syringe) {
        this.syringe = syringe;
    }

    @Override
    public void setup(java.util.Map<String, Object> properties) {
        SyringeCDIProvider.ensureProviderConfigured();
        // Request handling can run on a different thread than deployment bootstrap.
        // Refresh global registration to guarantee CDI.current() visibility.
        SyringeCDIProvider.registerGlobalCDI(syringe.getCDI());
        SyringeCDIProvider.registerThreadLocalCDI(syringe.getCDI());
        mirrorProviderStateToThreadContextClassLoader(syringe.getCDI(), true);

        boolean activated = false;
        try {
            activated = syringe.activateRequestContextIfNeeded();
        } catch (RuntimeException ignored) {
            // Best effort - keep setup resilient if request context control is unavailable.
        }
        if (properties != null) {
            properties.put(REQUEST_ACTIVATED_PROPERTY, activated);
        }
    }

    @Override
    public void teardown(java.util.Map<String, Object> properties) {
        if (properties != null) {
            Object activated = properties.get(REQUEST_ACTIVATED_PROPERTY);
            if (Boolean.TRUE.equals(activated)) {
                try {
                    syringe.deactivateRequestContextIfActive();
                } catch (RuntimeException ignored) {
                    // Best effort.
                }
            }
            properties.remove(REQUEST_ACTIVATED_PROPERTY);
        }
        try {
            syringe.deactivateRequestContextIfActive();
        } catch (RuntimeException ignored) {
            // Best effort cleanup for lazy activation fallback.
        }
        SyringeCDIProvider.unregisterThreadLocalCDI();
        mirrorThreadLocalCleanupToThreadContextClassLoader();
    }

    @Override
    public java.util.Set<org.jboss.msc.service.ServiceName> dependencies() {
        return java.util.Collections.emptySet();
    }

    @Override
    public int priority() {
        return 100; // Standard priority
    }

    private void mirrorProviderStateToThreadContextClassLoader(CDI<Object> cdi, boolean registerThreadLocal) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        ClassLoader own = SyringeCDIProvider.class.getClassLoader();
        if (tccl == null || tccl == own) {
            return;
        }
        try {
            Class<?> providerClass = Class.forName(SyringeCDIProvider.class.getName(), true, tccl);
            Method ensure = providerClass.getMethod("ensureProviderConfigured");
            ensure.invoke(null);
            Method registerGlobal = providerClass.getMethod("registerGlobalCDI", CDI.class);
            registerGlobal.invoke(null, cdi);
            if (registerThreadLocal) {
                Method registerThreadLocalMethod = providerClass.getMethod("registerThreadLocalCDI", CDI.class);
                registerThreadLocalMethod.invoke(null, cdi);
            }
        } catch (Throwable ignored) {
            // Best effort for classloader-isolated deployments.
        }
    }

    private void mirrorThreadLocalCleanupToThreadContextClassLoader() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        ClassLoader own = SyringeCDIProvider.class.getClassLoader();
        if (tccl == null || tccl == own) {
            return;
        }
        try {
            Class<?> providerClass = Class.forName(SyringeCDIProvider.class.getName(), true, tccl);
            Method unregisterThreadLocal = providerClass.getMethod("unregisterThreadLocalCDI");
            unregisterThreadLocal.invoke(null);
        } catch (Throwable ignored) {
            // Best effort for classloader-isolated deployments.
        }
    }

}
