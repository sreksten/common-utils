package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.contexts.ContextManager;
import com.threeamigos.common.util.implementations.injection.contexts.ScopeContext;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates client proxies for normal-scoped CDI beans.
 *
 * <h2>Why Proxies Are Needed</h2>
 * In CDI, normal scopes (ApplicationScoped, RequestScoped, SessionScoped, ConversationScoped)
 * require client proxies to ensure correct behavior when beans with different lifecycles interact.
 *
 * <h3>Example Problem Without Proxies:</h3>
 * <pre>
 * {@literal @}ApplicationScoped
 * class GlobalService {
 *     {@literal @}Inject RequestData requestData;  // Different request per HTTP call!
 * }
 *
 * {@literal @}RequestScoped
 * class RequestData {
 *     String userId;
 * }
 * </pre>
 *
 * Without a proxy, the ApplicationScoped bean would get ONE instance of RequestData
 * injected at creation time and use that same instance for ALL requests - WRONG!
 *
 * <h3>Solution With Proxies:</h3>
 * Instead of injecting the actual RequestData instance, CDI injects a PROXY that:
 * 1. Looks identical to RequestData (same class, same methods)
 * 2. On every method call, asks the context "what's the current RequestData instance?"
 * 3. Forwards the method call to the correct contextual instance
 *
 * <h2>How This Generator Works</h2>
 * Uses ByteBuddy to generate a subclass that intercepts all method calls and delegates
 * to the contextual instance from the appropriate scope.
 *
 * <pre>
 * Original Bean:
 * class RequestData {
 *     public String getUserId() { return userId; }
 * }
 *
 * Generated Proxy:
 * class RequestData$$Proxy extends RequestData {
 *     public String getUserId() {
 *         // 1. Get current contextual instance from RequestScoped context
 *         RequestData realInstance = context.get(bean);
 *         // 2. Forward the call to the real instance
 *         return realInstance.getUserId();
 *     }
 * }
 * </pre>
 *
 * @author Stefano Reksten
 */
public class ClientProxyGenerator {

    private final ContextManager contextManager;

    // Cache generated proxy classes to avoid regenerating for the same bean type
    private final ConcurrentHashMap<Class<?>, Class<?>> proxyClassCache = new ConcurrentHashMap<>();

    public ClientProxyGenerator(ContextManager contextManager) {
        this.contextManager = contextManager;
    }

    /**
     * Creates a client proxy for a normal-scoped bean.
     * <p>
     * The proxy lifecycle:
     * 1. Proxy is created once and injected into dependent beans
     * 2. Proxy lives as long as the dependent bean (could be the entire application)
     * 3. Each method call on the proxy:
     *    a. Looks up the current contextual instance from the scope's context
     *    b. Delegates the call to that instance
     *    c. Returns the result
     * <p>
     * This ensures that even if the proxy is held by a long-lived bean,
     * it always accesses the correct contextual instance for the current scope.
     * <p>
     * IMPORTANT - Constructor Handling:
     * The bean class may have an @Inject constructor with parameters, like:
     *
     * <pre>
     * {@literal @}RequestScoped
     * class MyBean {
     *     {@literal @}Inject
     *     public MyBean(SomeService service) { ... }
     * }
     * </pre>
     *
     * The generated proxy will have a no-arg constructor that calls super() with
     * default/null values. This is SAFE because:
     * - The proxy itself never executes any business logic
     * - All method calls are intercepted and delegated to real contextual instances
     * - The real instances are created properly via BeanImpl.create() with full DI
     * - The proxy is just an empty shell for delegation
     *
     * @param bean the bean to create a proxy for
     * @param <T> the bean type
     * @return a proxy instance that delegates to contextual instances
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Bean<T> bean) {
        Class<?> beanClass = bean.getBeanClass();

        // Get or generate the proxy class
        Class<?> proxyClass = proxyClassCache.computeIfAbsent(beanClass, this::generateProxyClass);

        try {
            // Create an instance of the proxy
            T proxy = (T) proxyClass.getDeclaredConstructor().newInstance();

            // Initialize the proxy with the bean and context manager
            // This is done via the ProxyState interface that the proxy implements
            if (proxy instanceof ProxyState) {
                ((ProxyState) proxy).$$_setProxyState(bean, contextManager);
            }

            return proxy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create proxy for bean: " + beanClass.getName(), e);
        }
    }

    /**
     * Generates a proxy class using ByteBuddy.
     * <p>
     * The generated class:
     * 1. Extends the target bean class (so it's type-compatible)
     * 2. Adds a DEFAULT (no-arg) constructor
     *    - This is CRITICAL: The bean class might have @Inject constructors with parameters
     *    - The proxy doesn't need to initialize the bean properly because it NEVER calls
     *      business methods on itself - it always delegates to contextual instances
     *    - We use DEFAULT constructor strategy which calls super() on any existing constructor,
     *      using default values (null for objects, 0 for primitives, false for booleans)
     * 3. Implements ProxyState (to hold bean and contextManager references)
     * 4. Implements Serializable (required by CDI spec for passivation)
     * 5. Adds fields for storing the bean and contextManager
     * 6. Overrides all public methods to delegate to contextual instance
     * <p>
     * Note: The proxy shell is never actually "used" as a bean - it's just a delegation wrapper.
     * All business logic runs on the real contextual instances retrieved from the scope.
     *
     * @param beanClass the class to proxy
     * @return the generated proxy class
     */
    private Class<?> generateProxyClass(Class<?> beanClass) {
        try {
            return new ByteBuddy()
                // Create a subclass of the target bean class
                // IMPORTANT: Use DEFAULT constructor strategy to handle parent constructors.
                // This will create a constructor that calls the super constructor with default values:
                // - null for object parameters
                // - 0 for numeric primitives
                // - false for boolean
                // This is safe because the proxy never uses the parent's state - it only delegates.
                .subclass(beanClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)

                // Add fields to store the bean and contextManager
                .defineField("$$_bean", Bean.class, net.bytebuddy.description.modifier.Visibility.PRIVATE)
                .defineField("$$_contextManager", ContextManager.class, net.bytebuddy.description.modifier.Visibility.PRIVATE)

                // Implement ProxyState to store bean and contextManager
                .implement(ProxyState.class)

                // Implement ProxyState methods using field accessors
                .method(ElementMatchers.named("$$_setProxyState"))
                .intercept(FieldAccessor.ofField("$$_bean").setsArgumentAt(0)
                    .andThen(FieldAccessor.ofField("$$_contextManager").setsArgumentAt(1)))

                .method(ElementMatchers.named("$$_getBean"))
                .intercept(FieldAccessor.ofField("$$_bean"))

                .method(ElementMatchers.named("$$_getContextManager"))
                .intercept(FieldAccessor.ofField("$$_contextManager"))

                // Implement Serializable (CDI requirement for passivating scopes)
                .implement(Serializable.class)

                // Add serialization support via writeReplace method
                // This ensures that when the proxy is serialized, we serialize a minimal
                // SerializedProxy marker that can recreate the proxy on deserialization
                .defineMethod("writeReplace", Object.class, net.bytebuddy.description.modifier.Visibility.PRIVATE)
                .intercept(MethodDelegation.to(SerializationInterceptor.class))

                // Intercept ALL business methods (not Object methods, not ProxyState methods)
                // and delegate them to our ContextualInstanceInterceptor
                .method(ElementMatchers.any()
                    .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)))
                    .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(ProxyState.class))))
                .intercept(MethodDelegation.to(ContextualInstanceInterceptor.class))

                // Load the class into the same classloader as the target class
                .make()
                .load(beanClass.getClassLoader())
                .getLoaded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate proxy class for: " + beanClass.getName(), e);
        }
    }

    /**
     * Interface implemented by all generated proxies to store proxy state.
     * This allows us to pass the bean and contextManager to the proxy instance.
     */
    public interface ProxyState {
        void $$_setProxyState(Bean<?> bean, ContextManager contextManager);
        Bean<?> $$_getBean();
        ContextManager $$_getContextManager();
    }

    /**
     * ByteBuddy interceptor that handles method calls on proxy instances.
     *
     * <h3>Call Flow:</h3>
     * <pre>
     * 1. User calls: proxy.getUserId()
     * 2. ByteBuddy intercepts and calls: ContextualInstanceInterceptor.intercept(...)
     * 3. Interceptor does:
     *    a. Extract bean and contextManager from proxy (via ProxyState)
     *    b. Get bean's scope annotation (e.g., @RequestScoped)
     *    c. Get the context for that scope from contextManager
     *    d. Get the current contextual instance from the context
     *    e. Invoke the original method on the contextual instance
     *    f. Return the result
     * 4. Result flows back to user
     * </pre>
     *
     * This interceptor is what makes the proxy "transparent" - from the caller's
     * perspective, they're just calling a normal method, but behind the scenes
     * we're ensuring they get the right contextual instance for the current scope.
     */
    public static class ContextualInstanceInterceptor {

        /**
         * Intercepts method calls on the proxy and delegates to the contextual instance.
         *
         * @param bean the bean stored in the proxy
         * @param contextManager the contextManager stored in the proxy
         * @param method the method being called
         * @param args the method arguments
         * @return the result from the contextual instance
         * @throws Throwable if the method invocation fails
         */
        @RuntimeType  // Tells ByteBuddy to adapt return types dynamically
        public static Object intercept(
                @FieldValue("$$_bean") Bean<?> bean,                           // The bean from proxy field
                @FieldValue("$$_contextManager") ContextManager contextManager, // The contextManager from proxy field
                @Origin Method method,                                          // The method being called
                @AllArguments Object[] args                                     // The method arguments
        ) throws Throwable {

            // Step 1: Validate proxy state
            if (bean == null || contextManager == null) {
                throw new IllegalStateException("Proxy has not been initialized. Call $$_setProxyState first.");
            }

            // Step 2: Get the bean's scope annotation
            // Every normal-scoped bean has exactly one scope annotation
            Class<? extends Annotation> scopeType = bean.getScope();

            // Step 3: Get the context for this scope
            // E.g., for @RequestScoped beans, this returns the RequestScopedContext
            // The context maintains the actual bean instances for the current scope
            // (current request, current session, etc.)
            ScopeContext context = contextManager.getContext(scopeType);

            // Step 4: Get the current contextual instance from the context
            // This is THE KEY STEP that makes proxies work:
            // - For RequestScoped: returns the instance for the CURRENT HTTP request
            // - For SessionScoped: returns the instance for the CURRENT user session
            // - For ApplicationScoped: returns the singleton instance
            // - For ConversationScoped: returns the instance for the CURRENT conversation
            //
            // If no instance exists yet, the context will create one.
            Object contextualInstance = context.get(bean, null);

            if (contextualInstance == null) {
                throw new IllegalStateException(
                    "No contextual instance available for bean: " + bean.getBeanClass().getName() +
                    " in scope: " + scopeType.getName() + ". Is the scope active?"
                );
            }

            // Step 5: Invoke the method on the actual contextual instance
            // This is the real bean instance that will process the call
            return method.invoke(contextualInstance, args);
        }
    }

    /**
     * Interceptor for proxy serialization.
     * <p>
     * When a proxy is serialized (e.g., when an HTTP session is passivated), we don't want to
     * serialize the proxy itself - instead, we serialize a minimal marker object that knows
     * how to recreate the proxy on deserialization.
     * <p>
     * This is the standard CDI proxy serialization pattern.
     */
    public static class SerializationInterceptor {

        /**
         * Called automatically during serialization via the writeReplace() method.
         *
         * @param proxyState the proxy being serialized
         * @return a SerializedProxy marker object
         */
        @RuntimeType
        public static Object writeReplace(@This ProxyState proxyState) {
            // Extract bean class from the proxy
            Bean<?> bean = proxyState.$$_getBean();
            Class<?> beanClass = bean.getBeanClass();

            // Return a serializable marker that can recreate the proxy
            return new SerializedProxy(beanClass);
        }
    }

    /**
     * Serialization marker for CDI client proxies.
     * <p>
     * This small object is what actually gets serialized when a proxy is passivated.
     * On deserialization (via readResolve), it recreates the proxy.
     * <p>
     * Why this pattern?
     * - Proxies contain references to Bean and ContextManager (not serializable)
     * - We only need the bean class to recreate the proxy
     * - The proxy will get re-initialized with proper Bean/ContextManager references
     *   when retrieved from the container after deserialization
     */
    private static class SerializedProxy implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Class<?> beanClass;

        SerializedProxy(Class<?> beanClass) {
            this.beanClass = beanClass;
        }

        /**
         * Called automatically during deserialization.
         *
         * Note: This implementation returns a placeholder. In a full CDI implementation,
         * this would look up the bean from the container and return a properly initialized
         * proxy. For now, this prevents serialization failures and documents the pattern.
         *
         * @return a proxy instance (or placeholder in current implementation)
         */
        private Object readResolve() {
            // TODO: In a full implementation, this would:
            // 1. Look up the bean from the BeanManager/Container
            // 2. Create a new proxy via ClientProxyGenerator
            // 3. Initialize it with the bean and contextManager
            //
            // For now, we throw an exception with helpful message
            throw new UnsupportedOperationException(
                "Proxy deserialization not yet fully implemented. " +
                "Bean class: " + beanClass.getName() + ". " +
                "To complete this, integrate with container to retrieve Bean and ContextManager."
            );
        }
    }
}
