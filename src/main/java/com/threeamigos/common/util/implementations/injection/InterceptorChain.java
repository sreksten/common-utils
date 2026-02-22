package com.threeamigos.common.util.implementations.injection;

import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a chain of interceptor invocations using the Chain of Responsibility pattern.
 *
 * <p>This class manages the ordered sequence of interceptor method invocations that wrap
 * a target bean method, constructor, or lifecycle callback. Interceptors are invoked in
 * priority order (lower priority value = earlier execution).
 *
 * <p><b>Execution Flow:</b>
 * <pre>
 * InterceptorChain:
 *   1. LoggingInterceptor (priority 100)
 *   2. SecurityInterceptor (priority 200)
 *   3. TransactionalInterceptor (priority 300)
 *   4. → Target Bean Method
 *
 * Invocation:
 *   Client
 *     → LoggingInterceptor.intercept(ctx)
 *         → SecurityInterceptor.intercept(ctx)
 *             → TransactionalInterceptor.intercept(ctx)
 *                 → Target.method()
 *                 ← return result
 *             ← return result
 *         ← return result
 *     ← return result
 * </pre>
 *
 * <p><b>Thread Safety:</b> InterceptorChain instances are immutable and thread-safe once built.
 * They can be cached and reused for multiple invocations of the same method.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Build chain
 * InterceptorChain chain = InterceptorChain.builder()
 *     .addInterceptor(loggingInterceptor, loggingMethod)
 *     .addInterceptor(securityInterceptor, securityMethod)
 *     .build();
 *
 * // Invoke chain
 * Object result = chain.invoke(targetInstance, targetMethod, args);
 * }</pre>
 *
 * @see InvocationContextImpl
 * @see InterceptorInvocation
 */
public class InterceptorChain {

    /**
     * Immutable list of interceptor invocations in execution order.
     */
    private final List<InterceptorInvocation> invocations;

    /**
     * Private constructor - use Builder to create instances.
     *
     * @param invocations the list of interceptor invocations
     */
    private InterceptorChain(List<InterceptorInvocation> invocations) {
        this.invocations = Collections.unmodifiableList(new ArrayList<>(invocations));
    }

    /**
     * Returns the immutable list of interceptor invocations.
     *
     * @return the interceptor invocations in execution order
     */
    public List<InterceptorInvocation> getInvocations() {
        return invocations;
    }

    /**
     * Invokes the interceptor chain for a method interception.
     *
     * <p>This creates an InvocationContext and starts the chain execution.
     * Each interceptor calls {@link InvocationContext#proceed()} to continue the chain.
     *
     * @param target the target bean instance
     * @param method the method being intercepted
     * @param args the method arguments
     * @return the result of the method invocation
     * @throws Exception any exception thrown by interceptors or target method
     */
    public Object invoke(Object target, Method method, Object[] args) throws Exception {
        // Create target invocation (final step in the chain)
        InvocationContextImpl.TargetInvocation targetInvocation = ctx -> {
            method.setAccessible(true);
            return method.invoke(target, ctx.getParameters());
        };

        // Create invocation context
        InvocationContextImpl context = new InvocationContextImpl(
                target, method, args, this, targetInvocation
        );

        // Start chain execution
        return context.proceed();
    }

    /**
     * Invokes the interceptor chain for a lifecycle callback interception.
     *
     * <p>This is used for @PostConstruct and @PreDestroy callbacks, which have no parameters
     * or return value.
     *
     * @param target the target bean instance
     * @param lifecycleCallback the lifecycle callback method (can be null if no target callback)
     * @throws Exception any exception thrown by interceptors or callback
     */
    public void invokeLifecycle(Object target, Method lifecycleCallback) throws Exception {
        // Create target invocation
        InvocationContextImpl.TargetInvocation targetInvocation = ctx -> {
            if (lifecycleCallback != null) {
                lifecycleCallback.setAccessible(true);
                lifecycleCallback.invoke(target);
            }
            return null; // Lifecycle callbacks return void
        };

        // Create invocation context
        InvocationContextImpl context = new InvocationContextImpl(
                target, this, targetInvocation
        );

        // Start chain execution
        context.proceed();
    }

    /**
     * Checks if the chain is empty (no interceptors).
     *
     * @return true if there are no interceptors in the chain
     */
    public boolean isEmpty() {
        return invocations.isEmpty();
    }

    /**
     * Returns the number of interceptors in the chain.
     *
     * @return the interceptor count
     */
    public int size() {
        return invocations.size();
    }

    /**
     * Creates a new builder for constructing an InterceptorChain.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing InterceptorChain instances.
     *
     * <p>Interceptors should be added in priority order (lowest priority first).
     * The builder does not automatically sort - callers must add interceptors in the correct order.
     */
    public static class Builder {
        private final List<InterceptorInvocation> invocations = new ArrayList<>();

        /**
         * Adds an interceptor to the chain.
         *
         * <p>The interceptor will be invoked in the order it was added to the builder.
         * Callers should add interceptors in priority order (lower priority value = earlier).
         *
         * @param interceptorInstance the interceptor instance
         * @param interceptorMethod the interceptor method (@AroundInvoke, @AroundConstruct, etc.)
         * @return this builder for method chaining
         */
        public Builder addInterceptor(Object interceptorInstance, Method interceptorMethod) {
            Objects.requireNonNull(interceptorInstance, "interceptorInstance cannot be null");
            Objects.requireNonNull(interceptorMethod, "interceptorMethod cannot be null");

            InterceptorInvocation invocation = ctx -> {
                interceptorMethod.setAccessible(true);
                return interceptorMethod.invoke(interceptorInstance, ctx);
            };

            invocations.add(invocation);
            return this;
        }

        /**
         * Adds a custom interceptor invocation to the chain.
         *
         * <p>This allows for programmatic interceptor logic without a separate interceptor class.
         *
         * @param invocation the interceptor invocation
         * @return this builder for method chaining
         */
        public Builder addInvocation(InterceptorInvocation invocation) {
            Objects.requireNonNull(invocation, "invocation cannot be null");
            invocations.add(invocation);
            return this;
        }

        /**
         * Builds an immutable InterceptorChain from the added interceptors.
         *
         * @return the constructed chain
         */
        public InterceptorChain build() {
            return new InterceptorChain(invocations);
        }
    }

    /**
     * Functional interface representing a single interceptor invocation in the chain.
     *
     * <p>This wraps the interceptor method invocation, allowing the chain to invoke
     * interceptors generically without knowing their specific types or methods.
     */
    @FunctionalInterface
    public interface InterceptorInvocation {
        /**
         * Invokes the interceptor with the given context.
         *
         * <p>The interceptor should call {@link InvocationContext#proceed()} to continue the chain,
         * and can inspect/modify parameters before and after the proceed() call.
         *
         * @param context the invocation context
         * @return the result of the invocation (or modified result)
         * @throws Exception any exception thrown by the interceptor
         */
        Object invoke(InvocationContext context) throws Exception;
    }

    @Override
    public String toString() {
        return "InterceptorChain{" +
                "size=" + invocations.size() +
                '}';
    }
}
