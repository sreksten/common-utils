package com.threeamigos.common.util.implementations.injection.decorators;

import com.threeamigos.common.util.implementations.injection.knowledgebase.DecoratorInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a chain of decorators that wrap a target bean instance.
 *
 * <p>A decorator chain is an ordered sequence of decorator instances, where each decorator
 * delegates to the next decorator (via @Delegate injection), with the innermost decorator
 * delegating to the actual bean instance.
 *
 * <p><b>Decorator Chain Structure:</b>
 * <pre>
 * Client Code
 *     ↓ calls method
 * Outermost Decorator (Priority 100)
 *     ↓ @Delegate delegates to
 * Middle Decorator (Priority 200)
 *     ↓ @Delegate delegates to
 * Innermost Decorator (Priority 300)
 *     ↓ @Delegate delegates to
 * Actual Bean Instance (Target)
 * </pre>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Given decorators: TimingDecorator, LoggingDecorator
 * // And target: PaymentProcessorImpl
 *
 * DecoratorChain.Builder builder = DecoratorChain.builder();
 * builder.addDecorator(timingDecoratorInfo, timingInstance);
 * builder.addDecorator(loggingDecoratorInfo, loggingInstance);
 * builder.setTarget(paymentProcessorImpl);
 * DecoratorChain chain = builder.build();
 *
 * // Result:
 * // Client → TimingDecorator → LoggingDecorator → PaymentProcessorImpl
 * }</pre>
 *
 * <p><b>Key Differences from InterceptorChain:</b>
 * <table>
 * <tr><th>Aspect</th><th>InterceptorChain</th><th>DecoratorChain</th></tr>
 * <tr><td>Wrapping</td><td>Method interception</td><td>Object wrapping</td></tr>
 * <tr><td>Delegation</td><td>InvocationContext.proceed()</td><td>@Delegate injection</td></tr>
 * <tr><td>Granularity</td><td>Per-method</td><td>Per-bean (all methods)</td></tr>
 * <tr><td>Instances</td><td>Stateless interceptors</td><td>Stateful decorators</td></tr>
 * </table>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li>DecoratorChain is immutable once built</li>
 *   <li>Decorator instances themselves may be stateful</li>
 *   <li>@Delegate references are final and thread-safe</li>
 * </ul>
 *
 * @see DecoratorInfo
 * @see DecoratorResolver
 * @see DecoratorAwareProxyGenerator
 * @author Stefano Reksten
 */
public class DecoratorChain {

    /**
     * Represents a single decorator in the chain with its metadata and instance.
     */
    public static class DecoratorInstance {
        private final DecoratorInfo decoratorInfo;
        private final Object decoratorInstance;

        /**
         * Creates a decorator instance holder.
         *
         * @param decoratorInfo the decorator metadata
         * @param decoratorInstance the actual decorator instance
         */
        public DecoratorInstance(DecoratorInfo decoratorInfo, Object decoratorInstance) {
            this.decoratorInfo = Objects.requireNonNull(decoratorInfo, "decoratorInfo cannot be null");
            this.decoratorInstance = Objects.requireNonNull(decoratorInstance, "decoratorInstance cannot be null");
        }

        public DecoratorInfo getDecoratorInfo() {
            return decoratorInfo;
        }

        public Object getDecoratorInstance() {
            return decoratorInstance;
        }

        @Override
        public String toString() {
            return "DecoratorInstance{" +
                    "class=" + decoratorInfo.getDecoratorClass().getSimpleName() +
                    ", priority=" + decoratorInfo.getPriority() +
                    '}';
        }
    }

    private final List<DecoratorInstance> decorators;
    private final Object targetInstance;

    /**
     * Creates a decorator chain.
     *
     * @param decorators ordered list of decorator instances (outermost first)
     * @param targetInstance the actual bean instance (innermost)
     */
    private DecoratorChain(List<DecoratorInstance> decorators, Object targetInstance) {
        this.decorators = Collections.unmodifiableList(new ArrayList<>(decorators));
        this.targetInstance = Objects.requireNonNull(targetInstance, "targetInstance cannot be null");
    }

    /**
     * Returns the ordered list of decorators (outermost first).
     *
     * @return immutable list of decorator instances
     */
    public List<DecoratorInstance> getDecorators() {
        return decorators;
    }

    /**
     * Returns the actual bean instance (the target being decorated).
     *
     * @return the target bean instance
     */
    public Object getTargetInstance() {
        return targetInstance;
    }

    /**
     * Returns the outermost decorator (the one client code interacts with).
     *
     * <p>If there are no decorators, returns the target instance.
     *
     * @return the outermost decorator instance or target if no decorators
     */
    public Object getOutermostInstance() {
        if (decorators.isEmpty()) {
            return targetInstance;
        }
        return decorators.get(0).getDecoratorInstance();
    }

    /**
     * Returns the number of decorators in the chain.
     *
     * @return decorator count
     */
    public int size() {
        return decorators.size();
    }

    /**
     * Checks if the chain has any decorators.
     *
     * @return true if there are no decorators
     */
    public boolean isEmpty() {
        return decorators.isEmpty();
    }

    /**
     * Gets the delegate instance for a decorator at the given index.
     *
     * <p>The delegate is the next instance in the chain:
     * <ul>
     *   <li>For decorator at index i, delegate is decorator at index i+1</li>
     *   <li>For the last decorator, delegate is the target instance</li>
     * </ul>
     *
     * @param decoratorIndex the index of the decorator (0-based)
     * @return the delegate instance for that decorator
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Object getDelegateFor(int decoratorIndex) {
        if (decoratorIndex < 0 || decoratorIndex >= decorators.size()) {
            throw new IndexOutOfBoundsException("Invalid decorator index: " + decoratorIndex);
        }

        // If this is the last decorator, delegate to target
        if (decoratorIndex == decorators.size() - 1) {
            return targetInstance;
        }

        // Otherwise, delegate to next decorator in chain
        return decorators.get(decoratorIndex + 1).getDecoratorInstance();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DecoratorChain{");
        sb.append("size=").append(decorators.size());
        sb.append(", chain=[");

        for (int i = 0; i < decorators.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(decorators.get(i).getDecoratorInfo().getDecoratorClass().getSimpleName());
        }

        if (!decorators.isEmpty()) {
            sb.append(" → ");
        }
        sb.append(targetInstance.getClass().getSimpleName());
        sb.append("]}");

        return sb.toString();
    }

    /**
     * Creates a new builder for constructing decorator chains.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing decorator chains in a fluent manner.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * DecoratorChain chain = DecoratorChain.builder()
     *     .addDecorator(timingDecoratorInfo, timingInstance)
     *     .addDecorator(loggingDecoratorInfo, loggingInstance)
     *     .setTarget(paymentProcessor)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private final List<DecoratorInstance> decorators = new ArrayList<>();
        private Object targetInstance;

        /**
         * Adds a decorator to the chain.
         *
         * <p>Decorators should be added in priority order (outermost first).
         * The builder does NOT automatically sort by priority - caller must
         * add decorators in the correct order (typically using DecoratorResolver).
         *
         * @param decoratorInfo the decorator metadata
         * @param decoratorInstance the decorator instance
         * @return this builder for chaining
         * @throws NullPointerException if any parameter is null
         */
        public Builder addDecorator(DecoratorInfo decoratorInfo, Object decoratorInstance) {
            decorators.add(new DecoratorInstance(decoratorInfo, decoratorInstance));
            return this;
        }

        /**
         * Sets the target bean instance.
         *
         * <p>This is the actual bean instance that will be decorated (innermost).
         *
         * @param targetInstance the target bean
         * @return this builder for chaining
         * @throws NullPointerException if targetInstance is null
         */
        public Builder setTarget(Object targetInstance) {
            this.targetInstance = targetInstance;
            return this;
        }

        /**
         * Builds the decorator chain.
         *
         * @return the completed decorator chain
         * @throws IllegalStateException if target instance is not set
         */
        public DecoratorChain build() {
            if (targetInstance == null) {
                throw new IllegalStateException("Target instance must be set");
            }
            return new DecoratorChain(decorators, targetInstance);
        }
    }
}
