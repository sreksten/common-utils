package com.threeamigos.common.util.implementations.injection.arquillian.tck;

import com.threeamigos.common.util.implementations.injection.scopes.ContextManager;
import com.threeamigos.common.util.implementations.injection.scopes.RequestScopedContext;
import com.threeamigos.common.util.implementations.injection.scopes.SessionScopedContext;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.cdi.tck.spi.Contexts;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Basic TCK Contexts SPI implementation for Syringe.
 */
public class SyringeContextsImpl implements Contexts<Context> {

    private final ThreadLocal<String> suspendedSessionId = new ThreadLocal<>();
    private final ThreadLocal<RequestContextController> requestContextController = new ThreadLocal<>();

    @Override
    public void setActive(Context context) {
        if (context == null) {
            return;
        }

        RequestScopedContext requestScopedContext = unwrapRequestScopedContext(context);
        if (requestScopedContext != null) {
            if (!requestScopedContext.isActive()) {
                requestScopedContext.activateRequest();
            }
            return;
        }

        SessionScopedContext sessionScopedContext = unwrapSessionScopedContext(context);
        if (sessionScopedContext != null) {
            if (sessionScopedContext.getCurrentSessionId() == null) {
                String sessionId = suspendedSessionId.get();
                if (sessionId == null || sessionId.trim().isEmpty()) {
                    sessionId = "cdi-tck-session-" + UUID.randomUUID();
                }
                sessionScopedContext.activateSession(sessionId);
            }
            return;
        }

        ContextManager contextManager = contextManagerOrNull();
        if (contextManager != null) {
            if (RequestScoped.class.equals(context.getScope())) {
                if (!contextManager.getContext(RequestScoped.class).isActive()) {
                    contextManager.activateRequest();
                }
            } else if (SessionScoped.class.equals(context.getScope())) {
                if (contextManager.getCurrentSessionId() == null) {
                    String sessionId = suspendedSessionId.get();
                    if (sessionId == null || sessionId.trim().isEmpty()) {
                        sessionId = "cdi-tck-session-" + UUID.randomUUID();
                    }
                    contextManager.activateSession(sessionId);
                }
            }
            return;
        }

        if (RequestScoped.class.equals(context.getScope())) {
            RequestContextController controller = getOrCreateRequestContextController();
            if (controller != null) {
                controller.activate();
            }
        }
    }

    @Override
    public void setInactive(Context context) {
        if (context == null) {
            return;
        }

        RequestScopedContext requestScopedContext = unwrapRequestScopedContext(context);
        if (requestScopedContext != null) {
            if (requestScopedContext.isActive()) {
                requestScopedContext.deactivateRequest();
            }
            return;
        }

        SessionScopedContext sessionScopedContext = unwrapSessionScopedContext(context);
        if (sessionScopedContext != null) {
            String sessionId = sessionScopedContext.getCurrentSessionId();
            if (sessionId != null) {
                suspendedSessionId.set(sessionId);
                sessionScopedContext.deactivateSession();
            }
            return;
        }

        ContextManager contextManager = contextManagerOrNull();
        if (contextManager != null) {
            if (RequestScoped.class.equals(context.getScope())) {
                if (contextManager.getContext(RequestScoped.class).isActive()) {
                    contextManager.deactivateRequest();
                }
            } else if (SessionScoped.class.equals(context.getScope())) {
                String sessionId = contextManager.getCurrentSessionId();
                if (sessionId != null) {
                    suspendedSessionId.set(sessionId);
                    contextManager.deactivateSession();
                }
            }
            return;
        }

        if (RequestScoped.class.equals(context.getScope())) {
            RequestContextController controller = getOrCreateRequestContextController();
            if (controller != null) {
                try {
                    controller.deactivate();
                } catch (RuntimeException ignored) {
                    // Not active or already deactivated; no-op for compatibility.
                }
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
        if (context == null) {
            return;
        }

        RequestScopedContext requestScopedContext = unwrapRequestScopedContext(context);
        if (requestScopedContext != null) {
            if (requestScopedContext.isActive()) {
                requestScopedContext.deactivateRequest();
            }
            return;
        }

        SessionScopedContext sessionScopedContext = unwrapSessionScopedContext(context);
        if (sessionScopedContext != null) {
            String sessionId = sessionScopedContext.getCurrentSessionId();
            if (sessionId == null) {
                sessionId = suspendedSessionId.get();
            }
            if (sessionId != null && !sessionId.trim().isEmpty()) {
                suspendedSessionId.set(sessionId);
                sessionScopedContext.invalidateSession(sessionId);
            }
            return;
        }

        ContextManager contextManager = contextManagerOrNull();
        if (contextManager != null) {
            if (RequestScoped.class.equals(context.getScope())) {
                if (contextManager.getContext(RequestScoped.class).isActive()) {
                    contextManager.deactivateRequest();
                }
            } else if (SessionScoped.class.equals(context.getScope())) {
                String sessionId = contextManager.getCurrentSessionId();
                if (sessionId == null) {
                    sessionId = suspendedSessionId.get();
                }
                if (sessionId != null && !sessionId.trim().isEmpty()) {
                    suspendedSessionId.set(sessionId);
                    contextManager.invalidateSession(sessionId);
                }
            }
            return;
        }

        if (RequestScoped.class.equals(context.getScope())) {
            RequestContextController controller = getOrCreateRequestContextController();
            if (controller != null) {
                try {
                    controller.deactivate();
                } catch (RuntimeException ignored) {
                    // No active context, nothing to destroy.
                }
            }
        }
    }

    private static BeanManager beanManager() {
        return CDI.current().getBeanManager();
    }

    private ContextManager contextManagerOrNull() {
        try {
            BeanManager beanManager = beanManager();
            if (beanManager instanceof BeanManagerImpl) {
                return ((BeanManagerImpl) beanManager).getContextManager();
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private RequestContextController getOrCreateRequestContextController() {
        RequestContextController controller = requestContextController.get();
        if (controller != null) {
            return controller;
        }
        try {
            controller = CDI.current().select(RequestContextController.class).get();
            requestContextController.set(controller);
            return controller;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private RequestScopedContext unwrapRequestScopedContext(Context context) {
        Object scopeContext = unwrapScopeContext(context);
        if (scopeContext instanceof RequestScopedContext) {
            return (RequestScopedContext) scopeContext;
        }
        return null;
    }

    private SessionScopedContext unwrapSessionScopedContext(Context context) {
        Object scopeContext = unwrapScopeContext(context);
        if (scopeContext instanceof SessionScopedContext) {
            return (SessionScopedContext) scopeContext;
        }
        return null;
    }

    private Object unwrapScopeContext(Context context) {
        try {
            Field scopeContextField = context.getClass().getDeclaredField("scopeContext");
            scopeContextField.setAccessible(true);
            return scopeContextField.get(context);
        } catch (Exception ignored) {
            return null;
        }
    }
}
