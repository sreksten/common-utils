package com.threeamigos.common.util.implementations.injection.decorators;

import com.threeamigos.common.util.implementations.injection.scopes.InjectionPointImpl;
import com.threeamigos.common.util.implementations.injection.knowledgebase.DecoratorInfo;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.hasDelegateAnnotation;
import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.hasInjectAnnotation;

/**
 * Generates decorator proxies that wrap bean instances with decorator chains.
 *
 * <p>This generator creates nested decorator instances where each decorator receives
 * a @Delegate injection point referencing the next decorator in the chain (or the
 * actual bean instance for the innermost decorator).
 *
 * <p><b>Decorator Chain Architecture:</b>
 * <pre>
 * Client Code
 *     ↓ calls method
 * Outermost Decorator Proxy (Priority 100)
 *     ↓ @Delegate field → injects reference to next decorator
 * Middle Decorator Proxy (Priority 200)
 *     ↓ @Delegate field → injects reference to next decorator
 * Innermost Decorator Proxy (Priority 300)
 *     ↓ @Delegate field → injects reference to target
 * Actual Bean Instance (Target)
 * </pre>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Creates decorator instances via CDI (supports dependency injection)</li>
 *   <li>Injects @Delegate references automatically (field, constructor, or method injection)</li>
 *   <li>Handles multiple decorators with proper priority ordering</li>
 *   <li>Caches decorator classes for performance</li>
 *   <li>Supports interface-based and class-based decoration</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Given decorators: TimingDecorator, LoggingDecorator
 * // And target: PaymentProcessorImpl
 *
 * DecoratorAwareProxyGenerator generator = new DecoratorAwareProxyGenerator();
 * DecoratorChain chain = generator.createDecoratorChain(
 *     targetInstance,        // PaymentProcessorImpl
 *     decoratorInfos,        // [TimingDecorator, LoggingDecorator]
 *     beanManager,
 *     creationalContext
 * );
 *
 * Object decorated = chain.getOutermostInstance();
 * // decorated is TimingDecorator wrapping LoggingDecorator wrapping PaymentProcessorImpl
 * }</pre>
 *
 * <p><b>Differences from InterceptorAwareProxyGenerator:</b>
 * <table>
 * <tr><th>Aspect</th><th>InterceptorAwareProxyGenerator</th><th>DecoratorAwareProxyGenerator</th></tr>
 * <tr><td>Wrapping</td><td>Single proxy with method interception</td><td>Nested decorator instances</td></tr>
 * <tr><td>Delegation</td><td>InvocationContext.proceed()</td><td>@Delegate injection</td></tr>
 * <tr><td>Instances</td><td>One proxy wraps target</td><td>Multiple decorators wrap each other</td></tr>
 * <tr><td>Injection</td><td>N/A (interceptors are stateless)</td><td>@Delegate injected at runtime</td></tr>
 * </table>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li>Decorator class cache is thread-safe (ConcurrentHashMap)</li>
 *   <li>Decorator instances may be stateful (managed by CDI scope)</li>
 *   <li>@Delegate references are final once injected</li>
 * </ul>
 *
 * @see DecoratorChain
 * @see DecoratorResolver
 * @see DecoratorInfo
 * @author Stefano Reksten
 */
public class DecoratorAwareProxyGenerator {

    // Cache generated decorator proxy classes to avoid regenerating for the same decorator type
    // Key: Decorator class
    // Value: Generated proxy class (if needed for decoration)
    private final ConcurrentHashMap<Class<?>, Class<?>> decoratorProxyCache = new ConcurrentHashMap<>();

    public void clearCache() {
        decoratorProxyCache.clear();
    }

    /**
     * Creates a decorator chain wrapping the target instance.
     *
     * <p>This method:
     * <ol>
     *   <li>Creates decorator instances via BeanManager.getReference()</li>
     *   <li>Injects @Delegate references (each decorator receives the next decorator/target)</li>
     *   <li>Builds a DecoratorChain with all decorator instances</li>
     *   <li>Returns the chain (caller uses chain.getOutermostInstance())</li>
     * </ol>
     *
     * <p><b>Decorator Instance Creation:</b>
     * Decorators are CDI beans, so they're created using BeanManager.getReference().
     * This ensures they receive full dependency injection (constructor, field, method, @PostConstruct).
     *
     * <p><b>@Delegate Injection:</b>
     * After creating each decorator instance, this method finds the @Delegate injection point
     * (field, constructor parameter, or method parameter) and injects the appropriate delegate
     * (next decorator or target instance).
     *
     * @param targetInstance the actual bean instance to be decorated
     * @param decoratorInfos ordered list of decorators (by priority, outermost first)
     * @param beanManager the BeanManager for creating decorator instances
     * @param creationalContext the CreationalContext for managing decorator lifecycle
     * @return a DecoratorChain containing all decorator instances
     * @throws IllegalStateException if decorator creation or injection fails
     */
    public DecoratorChain createDecoratorChain(
            @Nonnull Object targetInstance,
            @Nonnull List<DecoratorInfo> decoratorInfos,
            @Nonnull BeanManager beanManager,
            @Nonnull CreationalContext<?> creationalContext) {

        // If no decorators, return a chain with just the target
        if (decoratorInfos.isEmpty()) {
            return new DecoratorChainBuilder()
                    .setTarget(targetInstance)
                    .build();
        }

        // Build a decorator chain from innermost to outermost
        // (We need to create decorators in reverse order for constructor injection)
        DecoratorChainBuilder chainBuilder = new DecoratorChainBuilder();
        List<Object> decoratorInstances = new ArrayList<>();

        // Create decorators in reverse order (innermost first)
        // This way, when we create a decorator, its delegate already exists
        Object currentDelegate = targetInstance;

        for (int i = decoratorInfos.size() - 1; i >= 0; i--) {
            DecoratorInfo decoratorInfo = decoratorInfos.get(i);

            // Check if this decorator uses constructor @Delegate injection
            InjectionPoint delegateInjectionPoint = decoratorInfo.getDelegateInjectionPoint();
            boolean isConstructorInjection = delegateInjectionPoint.getMember() instanceof Constructor;

            Object decoratorInstance;
            if (isConstructorInjection) {
                // Constructor injection: Pass delegate during instantiation
                decoratorInstance = createDecoratorInstanceWithConstructorDelegate(
                        decoratorInfo,
                        currentDelegate,
                        beanManager,
                        creationalContext
                );
            } else {
                // Field/method injection: Create instance first, inject delegate later
                decoratorInstance = createDecoratorInstance(
                        decoratorInfo,
                        beanManager,
                        creationalContext
                );

                // Inject the delegate
                injectDelegate(decoratorInstance, decoratorInfo, currentDelegate);
            }

            // Add to the list (will be reversed later)
            decoratorInstances.add(0, decoratorInstance);

            // Update delegate for next decorator
            currentDelegate = decoratorInstance;
        }

        // Add decorators to the chain (now in correct order: outermost first)
        for (int i = 0; i < decoratorInstances.size(); i++) {
            chainBuilder.addDecorator(decoratorInfos.get(i), decoratorInstances.get(i));
        }

        // Set target and build
        chainBuilder.setTarget(targetInstance);
        return chainBuilder.build();
    }

    /**
     * Creates a decorator instance using the BeanManager.
     *
     * <p>This method resolves the decorator bean and creates an instance using
     * BeanManager.getReference(). This ensures the decorator receives full CDI
     * dependency injection.
     *
     * @param decoratorInfo the decorator metadata
     * @param beanManager the BeanManager
     * @param creationalContext the CreationalContext
     * @return the decorator instance
     * @throws IllegalStateException if the decorator bean cannot be resolved
     */
    private Object createDecoratorInstance(
            DecoratorInfo decoratorInfo,
            BeanManager beanManager,
            CreationalContext<?> creationalContext) {

        Class<?> decoratorClass = decoratorInfo.getDecoratorClass();
        Bean<?> decoratorBean = createSyntheticDecoratorBean(decoratorClass);

        try {
            Constructor<?> constructor = findInjectionConstructor(decoratorClass);
            constructor.setAccessible(true);
            Parameter[] parameters = constructor.getParameters();
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                if (hasDelegateAnnotation(parameter)) {
                    throw new IllegalStateException("Decorator " + decoratorClass.getName() +
                            " requires @Delegate constructor injection, but createDecoratorInstance was used");
                }
                args[i] = beanManager.getInjectableReference(
                        new InjectionPointImpl<>(parameter, decoratorBean), creationalContext);
            }

            Object instance = constructor.newInstance(args);
            injectNonDelegateMembers(instance, decoratorClass, decoratorBean, beanManager, creationalContext);
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create decorator instance " + decoratorClass.getName(), e);
        }
    }

    /**
     * Creates a decorator instance with constructor @Delegate injection.
     *
     * <p>This method manually instantiates the decorator by calling its constructor
     * with the delegate parameter, then performs field/method injection.
     *
     * @param decoratorInfo the decorator metadata
     * @param delegate the delegate to inject via constructor
     * @param beanManager the BeanManager
     * @param creationalContext the CreationalContext
     * @return the decorator instance
     * @throws IllegalStateException if decorator creation fails
     */
    private Object createDecoratorInstanceWithConstructorDelegate(
            DecoratorInfo decoratorInfo,
            Object delegate,
            BeanManager beanManager,
            CreationalContext<?> creationalContext) {

        Class<?> decoratorClass = decoratorInfo.getDecoratorClass();
        InjectionPoint delegateInjectionPoint = decoratorInfo.getDelegateInjectionPoint();

        try {
            Constructor<?> constructor = (Constructor<?>) delegateInjectionPoint.getMember();
            constructor.setAccessible(true);
            Parameter[] parameters = constructor.getParameters();
            Object[] args = new Object[parameters.length];
            Bean<?> decoratorBean = createSyntheticDecoratorBean(decoratorClass);

            for (int i = 0; i < parameters.length; i++) {
                if (hasDelegateAnnotation(parameters[i])) {
                    args[i] = delegate;
                } else {
                    args[i] = beanManager.getInjectableReference(
                            new InjectionPointImpl<>(parameters[i], decoratorBean),
                            creationalContext
                    );
                }
            }

            Object decoratorInstance = constructor.newInstance(args);
            injectNonDelegateMembers(decoratorInstance, decoratorClass, decoratorBean, beanManager, creationalContext);
            return decoratorInstance;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create decorator instance with constructor @Delegate: " +
                    decoratorClass.getName() + ": " + e.getMessage(), e
            );
        }
    }

    /**
     * Resolves the decorator bean from the BeanManager.
     *
     * @param decoratorClass the decorator class
     * @param beanManager the BeanManager
     * @return the decorator bean, or null if not found
     */
    private Bean<?> resolveDecoratorBean(Class<?> decoratorClass, BeanManager beanManager) {
        Set<Bean<?>> beans = beanManager.getBeans(decoratorClass);
        if (beans.isEmpty()) {
            return createSyntheticDecoratorBean(decoratorClass);
        }
        return beanManager.resolve(beans);
    }

    private Constructor<?> findInjectionConstructor(Class<?> decoratorClass) throws NoSuchMethodException {
        Constructor<?> injectConstructor = null;
        for (Constructor<?> constructor : decoratorClass.getDeclaredConstructors()) {
            if (hasInjectAnnotation(constructor)) {
                injectConstructor = constructor;
                break;
            }
        }
        if (injectConstructor != null) {
            return injectConstructor;
        }
        return decoratorClass.getDeclaredConstructor();
    }

    private void injectNonDelegateMembers(Object instance,
                                          Class<?> decoratorClass,
                                          Bean<?> decoratorBean,
                                          BeanManager beanManager,
                                          CreationalContext<?> creationalContext) throws Exception {
        for (Field field : decoratorClass.getDeclaredFields()) {
            if (!hasInjectAnnotation(field) || hasDelegateAnnotation(field)) {
                continue;
            }
            field.setAccessible(true);
            Object value = beanManager.getInjectableReference(new InjectionPointImpl<>(field, decoratorBean), creationalContext);
            field.set(instance, value);
        }

        for (Method method : decoratorClass.getDeclaredMethods()) {
            if (!hasInjectAnnotation(method)) {
                continue;
            }
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                if (hasDelegateAnnotation(parameters[i])) {
                    continue;
                }
                args[i] = beanManager.getInjectableReference(
                        new InjectionPointImpl<>(parameters[i], decoratorBean), creationalContext);
            }
            method.setAccessible(true);
            method.invoke(instance, args);
        }
    }

    private Bean<?> createSyntheticDecoratorBean(Class<?> decoratorClass) {
        Set<Type> types = new HashSet<>();
        types.add(decoratorClass);
        Collections.addAll(types, decoratorClass.getGenericInterfaces());

        return new Bean<Object>() {
            @Override
            public Class<?> getBeanClass() {
                return decoratorClass;
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return Collections.emptySet();
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public Set<Annotation> getQualifiers() {
                Set<Annotation> qualifiers = new HashSet<>();
                qualifiers.add(Default.Literal.INSTANCE);
                qualifiers.add(Any.Literal.INSTANCE);
                return qualifiers;
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return Dependent.class;
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return Collections.emptySet();
            }

            @Override
            public Set<Type> getTypes() {
                return types;
            }

            @Override
            public boolean isAlternative() {
                return false;
            }

            @Override
            public Object create(CreationalContext<Object> creationalContext) {
                throw new UnsupportedOperationException("Synthetic decorator bean does not support create()");
            }

            @Override
            public void destroy(Object instance, CreationalContext<Object> creationalContext) {
                // no-op
            }
        };
    }

    /**
     * Injects the @Delegate reference into a decorator instance.
     *
     * <p>This method finds the @Delegate injection point (field, constructor param, or method param)
     * and injects the delegate instance using reflection.
     *
     * <p><b>Supported @Delegate Injection Points:</b>
     * <ul>
     *   <li><b>Field injection</b>: {@code @Inject @Delegate PaymentProcessor delegate;}</li>
     *   <li><b>Constructor injection</b>: {@code @Inject TimingDecorator(@Delegate PaymentProcessor delegate)}</li>
     *   <li><b>Method injection</b>: {@code @Inject setDelegate(@Delegate PaymentProcessor delegate)}</li>
     * </ul>
     *
     * @param decoratorInstance the decorator instance
     * @param decoratorInfo the decorator metadata (contains @Delegate injection point info)
     * @param delegate the delegate to inject (next decorator or target)
     * @throws IllegalStateException if injection fails
     */
    private void injectDelegate(
            Object decoratorInstance,
            DecoratorInfo decoratorInfo,
            Object delegate) {

        InjectionPoint delegateInjectionPoint = decoratorInfo.getDelegateInjectionPoint();

        try {
            // Field injection
            if (delegateInjectionPoint.getMember() instanceof Field) {
                Field field = (Field) delegateInjectionPoint.getMember();
                field.setAccessible(true);
                field.set(decoratorInstance, delegate);
            }
            // Constructor injection - already handled during instance creation
            else if (delegateInjectionPoint.getMember() instanceof Constructor) {
                // Constructor injection is handled by createDecoratorInstanceWithConstructorDelegate()
                // Nothing to do here - delegate was passed during constructor call
            }
            // Method injection
            else if (delegateInjectionPoint.getMember() instanceof Method) {
                Method method = (Method) delegateInjectionPoint.getMember();
                method.setAccessible(true);

                // Find the @Delegate parameter index
                Parameter[] parameters = method.getParameters();
                int delegateParamIndex = -1;
                for (int i = 0; i < parameters.length; i++) {
                    if (hasDelegateAnnotation(parameters[i])) {
                        delegateParamIndex = i;
                        break;
                    }
                }

                if (delegateParamIndex >= 0) {
                    // Create an args array with delegate at the correct position
                    Object[] args = new Object[parameters.length];
                    args[delegateParamIndex] = delegate;
                    method.invoke(decoratorInstance, args);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to inject @Delegate into " + decoratorInfo.getDecoratorClass().getName() +
                    ": " + e.getMessage(), e
            );
        }
    }

    /**
     * Creates a proxy class for a decorator (if needed).
     *
     * <p>This method is currently a placeholder. In most cases, decorators don't need
     * proxy classes because they're concrete classes that implement the decorated interface.
     *
     * <p>Future enhancement: If a decorator needs to be proxied (e.g., for lazy loading
     * or additional interception), this method can be implemented using ByteBuddy.
     *
     * @param decoratorClass the decorator class
     * @return the decorator class (or proxy class if needed)
     */
    @SuppressWarnings("unused")
    private Class<?> getOrGenerateDecoratorProxyClass(Class<?> decoratorClass) {
        return decoratorProxyCache.computeIfAbsent(decoratorClass, this::generateDecoratorProxyClass);
    }

    /**
     * Generates a proxy class for a decorator using ByteBuddy.
     *
     * <p>This is a placeholder for future enhancement. Currently, decorators are used
     * directly without additional proxying.
     *
     * @param decoratorClass the decorator class
     * @return the generated proxy class
     */
    private Class<?> generateDecoratorProxyClass(Class<?> decoratorClass) {
        // For now, just return the original class
        // Future: Generate ByteBuddy proxy if needed
        return decoratorClass;
    }
}
