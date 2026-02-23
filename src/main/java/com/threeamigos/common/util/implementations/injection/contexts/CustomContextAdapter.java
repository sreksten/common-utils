package com.threeamigos.common.util.implementations.injection.contexts;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

/**
 * Adapter that wraps a Jakarta CDI Context to work with our internal ScopeContext interface.
 * <p>
 * This allows custom contexts registered via {@link jakarta.enterprise.inject.spi.AfterBeanDiscovery#addContext(Context)}
 * to work seamlessly with the container's internal context management system.
 * <p>
 * <h3>Purpose:</h3>
 * The CDI specification defines {@link jakarta.enterprise.context.spi.Context} as the standard interface
 * for custom contexts. However, our container uses {@link ScopeContext} internally for better type safety
 * and additional functionality. This adapter bridges the gap.
 * <p>
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Custom context implementation (user-provided)
 * public class MyCustomContext implements jakarta.enterprise.context.spi.Context {
 *     public Class<? extends Annotation> getScope() { return MyCustomScope.class; }
 *     public <T> T get(Contextual<T> contextual, CreationalContext<T> ctx) { ... }
 *     public <T> T get(Contextual<T> contextual) { ... }
 *     public boolean isActive() { ... }
 * }
 *
 * // Register via extension
 * public void registerContext(@Observes AfterBeanDiscovery event) {
 *     event.addContext(new MyCustomContext());
 * }
 *
 * // Internally wrapped by:
 * ScopeContext adapted = new CustomContextAdapter(new MyCustomContext());
 * contextManager.registerContext(MyCustomScope.class, adapted);
 * }</pre>
 *
 * @author Stefano Reksten
 * @see jakarta.enterprise.context.spi.Context
 * @see ScopeContext
 * @see jakarta.enterprise.inject.spi.AfterBeanDiscovery#addContext(Context)
 */
public class CustomContextAdapter implements ScopeContext {

    /**
     * The wrapped Jakarta CDI Context provided by the extension or application.
     */
    private final Context wrappedContext;

    /**
     * Creates an adapter for a custom context.
     *
     * @param wrappedContext the Jakarta CDI context to wrap
     * @throws IllegalArgumentException if wrappedContext is null
     */
    public CustomContextAdapter(Context wrappedContext) {
        if (wrappedContext == null) {
            throw new IllegalArgumentException("wrappedContext cannot be null");
        }
        this.wrappedContext = wrappedContext;
    }

    /**
     * Gets an existing instance from the wrapped context or creates a new one.
     * <p>
     * This method delegates to the wrapped context's {@link Context#get(Contextual, CreationalContext)} method.
     * The Bean is passed as a Contextual since Bean extends Contextual in the CDI API.
     *
     * @param bean the bean to get/create
     * @param creationalContext the creation context for dependency injection
     * @param <T> the bean type
     * @return the scoped instance
     * @throws IllegalStateException if the wrapped context is not active
     */
    @Override
    public <T> T get(Bean<T> bean, CreationalContext<T> creationalContext) {
        if (!isActive()) {
            throw new IllegalStateException(
                "Context for scope " + wrappedContext.getScope().getName() + " is not active"
            );
        }

        // Bean extends Contextual, so we can pass it directly
        return wrappedContext.get(bean, creationalContext);
    }

    /**
     * Gets an existing instance from the wrapped context without creating a new one.
     * <p>
     * This method delegates to the wrapped context's {@link Context#get(Contextual)} method.
     *
     * @param bean the bean to get
     * @param <T> the bean type
     * @return the existing instance, or null if not present in this context
     */
    @Override
    public <T> T getIfExists(Bean<T> bean) {
        if (!isActive()) {
            return null;
        }

        // Bean extends Contextual, so we can pass it directly
        return wrappedContext.get(bean);
    }

    /**
     * Destroys all instances in the wrapped context.
     * <p>
     * <b>Note:</b> The standard CDI Context interface does not define a destroy() method.
     * This is a limitation of custom contexts - they must manage their own destruction logic.
     * <p>
     * For proper cleanup, custom context implementations should:
     * <ul>
     * <li>Listen to container shutdown events (BeforeShutdown)</li>
     * <li>Call {@link CreationalContext#release()} on all created instances</li>
     * <li>Call Bean.destroy() on all contextual instances</li>
     * </ul>
     * <p>
     * This method is a no-op for custom contexts. The application/extension is responsible
     * for proper cleanup.
     */
    @Override
    public void destroy() {
        // Custom contexts don't have a standard destroy() method in the CDI API.
        // Extensions must handle their own cleanup, typically via @Observes BeforeShutdown.
        // This is intentionally a no-op.
    }

    /**
     * Checks if the wrapped context is currently active.
     * <p>
     * This delegates to the wrapped context's {@link Context#isActive()} method.
     *
     * @return true if the wrapped context is active
     */
    @Override
    public boolean isActive() {
        return wrappedContext.isActive();
    }

    /**
     * Checks if the wrapped context supports passivation.
     * <p>
     * Custom contexts are assumed to be non-passivating unless they implement
     * {@link jakarta.enterprise.context.spi.AlterableContext} or have the
     * {@code @NormalScope(passivating=true)} annotation on their scope type.
     * <p>
     * <b>Current Implementation:</b> Returns false (conservative default).
     * Custom passivating contexts should be explicitly marked.
     *
     * @return false by default (custom contexts are assumed non-passivating)
     */
    @Override
    public boolean isPassivationCapable() {
        // Conservative default: custom contexts are not passivating unless explicitly marked.
        // A more sophisticated implementation could inspect the scope annotation for
        // @NormalScope(passivating=true) or check if the context implements AlterableContext.
        return false;
    }

    /**
     * Returns the wrapped Jakarta CDI Context.
     * <p>
     * This is useful for debugging or accessing context-specific functionality.
     *
     * @return the wrapped context
     */
    public Context getWrappedContext() {
        return wrappedContext;
    }

    @Override
    public String toString() {
        return "CustomContextAdapter{" +
               "scope=" + wrappedContext.getScope().getName() +
               ", active=" + isActive() +
               ", wrapped=" + wrappedContext.getClass().getName() +
               '}';
    }
}
