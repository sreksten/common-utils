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
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
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

        if (tryInvokeManagedContextMethod(context, "activate")) {
            return;
        }

        ContextManager contextManager = contextManagerOrNull();
        if (contextManager != null) {
            boolean handled = false;
            if (RequestScoped.class.equals(context.getScope())) {
                if (!contextManager.getContext(RequestScoped.class).isActive()) {
                    contextManager.activateRequest();
                    handled = true;
                }
            } else if (SessionScoped.class.equals(context.getScope())) {
                if (contextManager.getCurrentSessionId() == null) {
                    String sessionId = suspendedSessionId.get();
                    if (sessionId == null || sessionId.trim().isEmpty()) {
                        sessionId = "cdi-tck-session-" + UUID.randomUUID();
                    }
                    contextManager.activateSession(sessionId);
                    handled = true;
                }
            }
            if (handled) {
                return;
            }
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

        if (tryInvokeManagedContextMethod(context, "deactivate")) {
            return;
        }

        ContextManager contextManager = contextManagerOrNull();
        if (contextManager != null) {
            boolean handled = false;
            if (RequestScoped.class.equals(context.getScope())) {
                if (contextManager.getContext(RequestScoped.class).isActive()) {
                    contextManager.deactivateRequest();
                    handled = true;
                }
            } else if (SessionScoped.class.equals(context.getScope())) {
                String sessionId = contextManager.getCurrentSessionId();
                if (sessionId != null) {
                    suspendedSessionId.set(sessionId);
                    contextManager.deactivateSession();
                    handled = true;
                }
            }
            if (handled) {
                return;
            }
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

        if (tryInvokeManagedContextDestroy(context)) {
            return;
        }

        ContextManager contextManager = contextManagerOrNull();
        if (contextManager != null) {
            boolean handled = false;
            if (RequestScoped.class.equals(context.getScope())) {
                if (contextManager.getContext(RequestScoped.class).isActive()) {
                    contextManager.deactivateRequest();
                    handled = true;
                }
            } else if (SessionScoped.class.equals(context.getScope())) {
                String sessionId = contextManager.getCurrentSessionId();
                if (sessionId == null) {
                    sessionId = suspendedSessionId.get();
                }
                if (sessionId != null && !sessionId.trim().isEmpty()) {
                    suspendedSessionId.set(sessionId);
                    contextManager.invalidateSession(sessionId);
                    handled = true;
                }
            }
            if (handled) {
                return;
            }
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
        Context unwrapped = unwrapForwardingContext(context);
        Object scopeContext = tryReadField(unwrapped, "scopeContext");
        if (scopeContext != null) {
            return scopeContext;
        }
        Object recursivelyUnwrapped = unwrapByDelegateFields(unwrapped);
        if (recursivelyUnwrapped != null && recursivelyUnwrapped != unwrapped) {
            return tryReadField(recursivelyUnwrapped, "scopeContext");
        }
        return null;
    }

    private boolean tryInvokeManagedContextDestroy(Context context) {
        Context unwrapped = unwrapForwardingContext(context);
        boolean invalidated = tryInvokeNoArg(unwrapped, "invalidate");
        if (!invalidated) {
            return false;
        }
        // Follow Weld TCK porting package behavior:
        // invalidate -> deactivate -> activate
        tryInvokeNoArg(unwrapped, "deactivate");
        tryInvokeNoArg(unwrapped, "activate");
        return true;
    }

    private boolean tryInvokeManagedContextMethod(Context context, String methodName) {
        if (tryInvokeNoArg(context, methodName)) {
            return true;
        }
        Context unwrapped = unwrapForwardingContext(context);
        if (unwrapped != context && tryInvokeNoArg(unwrapped, methodName)) {
            return true;
        }
        Object recursivelyUnwrapped = unwrapByDelegateFields(context);
        return recursivelyUnwrapped != context && tryInvokeNoArg(recursivelyUnwrapped, methodName);
    }

    private Context unwrapForwardingContext(Context context) {
        Context unwrapped = tryUnwrapStatic(context, "org.jboss.weld.util.ForwardingContext");
        if (unwrapped != null) {
            return unwrapped;
        }
        unwrapped = tryUnwrapStatic(context, "org.jboss.weld.contexts.PassivatingContextWrapper");
        if (unwrapped != null) {
            return unwrapped;
        }
        Object recursivelyUnwrapped = unwrapByDelegateFields(context);
        return recursivelyUnwrapped instanceof Context ? (Context) recursivelyUnwrapped : context;
    }

    private Context tryUnwrapStatic(Context context, String className) {
        ClassLoader[] candidateLoaders = new ClassLoader[] {
                context != null ? context.getClass().getClassLoader() : null,
                Thread.currentThread().getContextClassLoader(),
                SyringeContextsImpl.class.getClassLoader()
        };
        for (ClassLoader loader : candidateLoaders) {
            if (loader == null) {
                continue;
            }
            try {
                Class<?> wrapperClass = Class.forName(className, false, loader);
                Method unwrapMethod = wrapperClass.getMethod("unwrap", Context.class);
                Object unwrapped = unwrapMethod.invoke(null, context);
                if (unwrapped instanceof Context) {
                    return (Context) unwrapped;
                }
            } catch (Exception ignored) {
                // Try next loader
            }
        }
        return null;
    }

    private boolean tryInvokeNoArg(Object target, String methodName) {
        if (target == null) {
            return false;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.invoke(target);
            return true;
        } catch (NoSuchMethodException ignored) {
            Class<?> current = target.getClass();
            while (current != null) {
                try {
                    Method declaredMethod = current.getDeclaredMethod(methodName);
                    declaredMethod.setAccessible(true);
                    declaredMethod.invoke(target);
                    return true;
                } catch (NoSuchMethodException ignoredDeclared) {
                    current = current.getSuperclass();
                } catch (Exception ignoredDeclaredInvocation) {
                    return false;
                }
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Object unwrapByDelegateFields(Object context) {
        Object current = context;
        Set<Object> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            Object next = tryReadField(current, "delegate");
            if (next == null) {
                next = tryReadField(current, "context");
            }
            if (next == null) {
                next = tryInvokeAccessor(current, "delegate");
            }
            if (next == null) {
                next = tryInvokeAccessor(current, "getDelegate");
            }
            if (next == null || next == current) {
                break;
            }
            current = next;
        }
        return current;
    }

    private Object tryReadField(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private Object tryInvokeAccessor(Object target, String accessorName) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(accessorName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
