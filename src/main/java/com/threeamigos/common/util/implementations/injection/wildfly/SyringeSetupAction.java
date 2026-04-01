package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.spi.SyringeCDIProvider;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.as.server.deployment.SetupAction;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

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
    private static final String SYNTHETIC_SESSION_ID_PROPERTY = SyringeSetupAction.class.getName() + ".syntheticSessionId";

    private final Syringe syringe;
    private final ThreadLocal<Deque<ActivationState>> activationStateStack =
            ThreadLocal.withInitial(ArrayDeque::new);

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
        String syntheticSessionId = null;
        try {
            activated = syringe.activateRequestContextIfNeeded();
        } catch (RuntimeException ignored) {
            // Best effort - keep setup resilient if request context control is unavailable.
        }
        try {
            syntheticSessionId = syringe.activateSyntheticSessionContextIfNeeded();
        } catch (RuntimeException ignored) {
            // Best effort - keep setup resilient if session context control is unavailable.
        }

        activationStateStack.get().push(new ActivationState(activated, syntheticSessionId));

        if (properties != null) {
            safePut(properties, REQUEST_ACTIVATED_PROPERTY, activated);
            if (syntheticSessionId != null) {
                safePut(properties, SYNTHETIC_SESSION_ID_PROPERTY, syntheticSessionId);
            }
        }
    }

    @Override
    public void teardown(java.util.Map<String, Object> properties) {
        ActivationState stateFromStack = popActivationState();
        boolean requestActivated = stateFromStack != null && stateFromStack.requestActivated;
        String syntheticSessionId = stateFromStack != null ? stateFromStack.syntheticSessionId : null;

        if (properties != null) {
            Object activated = properties.get(REQUEST_ACTIVATED_PROPERTY);
            if (activated instanceof Boolean) {
                requestActivated = (Boolean) activated;
            }
            Object mappedSessionId = properties.get(SYNTHETIC_SESSION_ID_PROPERTY);
            if (mappedSessionId instanceof String) {
                syntheticSessionId = (String) mappedSessionId;
            }
            safeRemove(properties, REQUEST_ACTIVATED_PROPERTY);
            safeRemove(properties, SYNTHETIC_SESSION_ID_PROPERTY);
        }

        if (requestActivated) {
            try {
                syringe.deactivateRequestContextIfActive();
            } catch (RuntimeException ignored) {
                // Best effort.
            }
        }
        if (syntheticSessionId != null) {
            try {
                syringe.invalidateSessionContext(syntheticSessionId);
            } catch (RuntimeException ignored) {
                // Best effort.
            }
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

    private ActivationState popActivationState() {
        Deque<ActivationState> stack = activationStateStack.get();
        ActivationState state = stack.isEmpty() ? null : stack.pop();
        if (stack.isEmpty()) {
            activationStateStack.remove();
        }
        return state;
    }

    private static void safePut(java.util.Map<String, Object> map, String key, Object value) {
        try {
            map.put(key, value);
        } catch (UnsupportedOperationException ignored) {
            // Undertow may pass immutable setup maps.
        }
    }

    private static void safeRemove(java.util.Map<String, Object> map, String key) {
        try {
            map.remove(key);
        } catch (UnsupportedOperationException ignored) {
            // Undertow may pass immutable setup maps.
        }
    }

    private static final class ActivationState {
        private final boolean requestActivated;
        private final String syntheticSessionId;

        private ActivationState(boolean requestActivated, String syntheticSessionId) {
            this.requestActivated = requestActivated;
            this.syntheticSessionId = syntheticSessionId;
        }
    }

}
