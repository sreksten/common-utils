package com.threeamigos.common.util.implementations.injection.scopes;

import com.threeamigos.common.util.implementations.injection.AnnotationsEnum;
import com.threeamigos.common.util.implementations.injection.util.DefaultLiteral;
import com.threeamigos.common.util.implementations.injection.util.QualifiersHelper;
import com.threeamigos.common.util.implementations.injection.spi.wrappers.AnnotatedFieldWrapper;
import com.threeamigos.common.util.implementations.injection.spi.wrappers.AnnotatedParameterWrapper;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.enterprise.inject.spi.CDI;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.hasDelegateAnnotation;
import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.hasNamedAnnotation;

/**
 * Implementation of CDI InjectionPoint SPI.
 * Represents an injection point in a bean - a field, method parameter, or constructor parameter
 * that will receive an injected value.
 *
 * <p>This class tracks metadata about the injection point, including:
 * <ul>
 *   <li>The type to be injected</li>
 *   <li>Qualifiers that disambiguate which bean to inject</li>
 *   <li>Whether the injection point is transient (not serializable)</li>
 *   <li>Whether it's a decorator delegate injection point</li>
 * </ul>
 */
public class InjectionPointImpl<T> implements InjectionPoint, Serializable {
    private static final long serialVersionUID = 1L;

    private final Member member;
    private final Bean<T> bean;
    private final Type type;
    private final Set<Annotation> qualifiers = new HashSet<>();

    /**
     * True if this injection point is marked as transient (not serializable).
     * Fields marked transient won't be serialized, which affects passivation in CDI.
     */
    private final boolean isTransient;

    /**
     * True if this injection point has a @Delegate annotation (used in decorators).
     * A decorator has exactly one @Delegate injection point that receives the decorated instance.
     */
    private final boolean isDelegate;

    /**
     * The Annotated wrapper for this injection point (field or parameter).
     * Provides type-safe access to annotations for portable extensions.
     */
    private final Annotated annotated;

    /**
     * Creates an injection point for a field.
     * Extracts qualifiers, checks for transient modifier, and checks for @Delegate annotation.
     *
     * @param field the field being injected
     * @param bean the bean that declares this injection point
     */
    public InjectionPointImpl(Field field, Bean<T> bean) {
        this(field, bean, null, null, null);
    }

    public InjectionPointImpl(Field field,
                              Bean<T> bean,
                              Type typeOverride,
                              Annotation[] annotationsOverride,
                              Annotated annotatedOverride) {
        this.member = field;
        this.bean = bean;
        this.type = typeOverride != null ? typeOverride : field.getGenericType();
        this.isTransient = java.lang.reflect.Modifier.isTransient(field.getModifiers());
        Annotation[] annotations = annotationsOverride != null ? annotationsOverride : field.getAnnotations();
        this.isDelegate = checkForDelegateAnnotation(annotations);

        this.annotated = annotatedOverride != null ? annotatedOverride : new AnnotatedFieldWrapper<>(field, null);

        collectQualifiers(annotations);
    }

    /**
     * Creates an injection point for a method or constructor parameter.
     * Extracts qualifiers and checks for @Delegate annotation.
     * Parameters cannot be transient (only fields can).
     *
     * @param parameter the parameter being injected
     * @param bean the bean that declares this injection point
     */
    public InjectionPointImpl(Parameter parameter, Bean<T> bean) {
        this(parameter, bean, null, null, null);
    }

    public InjectionPointImpl(Parameter parameter,
                              Bean<T> bean,
                              Type typeOverride,
                              Annotation[] annotationsOverride,
                              Annotated annotatedOverride) {
        this.member = parameter.getDeclaringExecutable();
        this.bean = bean;
        this.type = typeOverride != null ? typeOverride : parameter.getParameterizedType();
        this.isTransient = false; // Parameters cannot be transient
        Annotation[] annotations = annotationsOverride != null ? annotationsOverride : parameter.getAnnotations();
        this.isDelegate = checkForDelegateAnnotation(annotations);
        this.annotated = annotatedOverride != null ? annotatedOverride : new AnnotatedParameterWrapper<>(parameter, null);

        collectQualifiers(annotations);
    }

    /**
     * Collects all qualifier annotations from the injection point.
     * Per CDI spec:
     * - If no qualifiers are present, @Default is added
     * - @Delegate is NOT a qualifier, so it's excluded from the qualifiers set
     *
     * @param annotations all annotations present on the injection point
     */
    private void collectQualifiers(Annotation[] annotations) {
        for (Annotation qualifier : QualifiersHelper.extractQualifierAnnotations(annotations)) {
            qualifiers.add(normalizeNamedQualifier(qualifier));
        }

        // CDI defaulting rules for injection points: if no qualifier is declared, add @Default.
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
    }

    private Annotation normalizeNamedQualifier(Annotation qualifier) {
        if (!isNamedQualifierType(qualifier.annotationType())) {
            return qualifier;
        }

        String namedValue = readNamedValue(qualifier).trim();
        if (!namedValue.isEmpty()) {
            return qualifier;
        }

        // CDI 4.1 §3.9: empty @Named on injected fields defaults to the field name.
        if (member instanceof Field) {
            return createNamedLiteral(qualifier.annotationType(), ((Field) member).getName());
        }

        return qualifier;
    }

    private boolean isNamedQualifierType(Class<? extends Annotation> annotationType) {
        return hasNamedAnnotation(annotationType);
    }

    private String readNamedValue(Annotation namedQualifier) {
        try {
            Method valueMethod = namedQualifier.annotationType().getMethod("value");
            Object value = valueMethod.invoke(namedQualifier);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private Annotation createNamedLiteral(Class<? extends Annotation> namedType, String value) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("value".equals(name) && method.getParameterCount() == 0) {
                return value;
            }
            if ("annotationType".equals(name) && method.getParameterCount() == 0) {
                return namedType;
            }
            if ("equals".equals(name) && method.getParameterCount() == 1) {
                Object other = args[0];
                if (!namedType.isInstance(other)) {
                    return false;
                }
                try {
                    Method otherValue = namedType.getMethod("value");
                    Object otherNamedValue = otherValue.invoke(other);
                    return Objects.equals(value, otherNamedValue);
                } catch (ReflectiveOperationException e) {
                    return false;
                }
            }
            if ("hashCode".equals(name) && method.getParameterCount() == 0) {
                return (127 * "value".hashCode()) ^ value.hashCode();
            }
            if ("toString".equals(name) && method.getParameterCount() == 0) {
                return "@" + namedType.getName() + "(value=" + value + ")";
            }
            throw new UnsupportedOperationException("Unsupported @Named literal method: " + name);
        };

        return (Annotation) Proxy.newProxyInstance(
                namedType.getClassLoader(),
                new Class<?>[]{namedType},
                handler);
    }

    /**
     * Checks if the injection point has a @Delegate annotation.
     * @Delegate is used in decorators to mark the injection point that receives the decorated bean.
     * Per CDI spec, a decorator must have exactly one @Delegate injection point.
     *
     * @param annotations all annotations present on the injection point
     * @return true if @Delegate annotation is present
     */
    private boolean checkForDelegateAnnotation(Annotation[] annotations) {
        for (Annotation ann : annotations) {
            if (hasDelegateAnnotation(ann.annotationType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.unmodifiableSet(qualifiers);
    }

    public void addQualifier(Annotation qualifier) {
        qualifiers.add(qualifier);
    }

    @Override
    public Bean<T> getBean() {
        return bean;
    }

    @Override
    public Member getMember() {
        return member;
    }

    @Override
    public Annotated getAnnotated() {
        return annotated;
    }

    /**
     * Returns true if this injection point is marked with @Delegate annotation.
     * @Delegate is used exclusively in decorators to identify the injection point
     * that receives the decorated bean instance.
     *
     * <p><b>CDI Decorator Pattern:</b>
     * <pre>{@code
     * @Decorator
     * public class LoggingDecorator implements MyService {
     *     @Inject @Delegate
     *     private MyService delegate; // This injection point returns true for isDelegate()
     *
     *     public void doWork() {
     *         log("Before");
     *         delegate.doWork(); // Delegate to actual implementation
     *         log("After");
     *     }
     * }
     * }</pre>
     *
     * @return true if this injection point has @Delegate annotation
     */
    @Override
    public boolean isDelegate() {
        return isDelegate;
    }

    /**
     * Returns true if this is a transient field injection point.
     * Transient fields are not serialized, which has implications for passivation in CDI.
     *
     * <p><b>Why this matters:</b>
     * In CDI, passivating scopes (SessionScoped, ConversationScoped) require that beans
     * can be serialized. If a passivating bean has non-transient injection points of
     * non-passivation-capable dependencies, it's a deployment error.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @SessionScoped
     * public class UserSession implements Serializable {
     *     @Inject
     *     private transient Logger logger; // OK - transient, won't be serialized
     *
     *     @Inject
     *     private Database db; // ERROR if Database is not Serializable
     * }
     * }</pre>
     *
     * @return true if this is a transient field (parameters are never transient)
     */
    @Override
    public boolean isTransient() {
        return isTransient;
    }

    private Object writeReplace() {
        return new SerializedInjectionPoint(this);
    }

    private static final class SerializedInjectionPoint implements InjectionPoint, Serializable {
        private static final long serialVersionUID = 1L;

        private final Type type;
        private final Set<Annotation> qualifiers;
        private final MemberRef memberRef;
        private final boolean delegate;
        private final boolean trans;
        private final String beanId;
        private final String beanClassName;
        private final Type annotatedBaseType;

        private transient Bean<?> bean;
        private transient Member member;

        private SerializedInjectionPoint(InjectionPoint injectionPoint) {
            this.type = injectionPoint.getType();
            this.qualifiers = new LinkedHashSet<Annotation>(injectionPoint.getQualifiers());
            this.memberRef = MemberRef.of(injectionPoint.getMember());
            this.delegate = injectionPoint.isDelegate();
            this.trans = injectionPoint.isTransient();
            Bean<?> sourceBean = injectionPoint.getBean();
            this.bean = sourceBean;
            if (sourceBean instanceof PassivationCapable) {
                this.beanId = ((PassivationCapable) sourceBean).getId();
            } else {
                this.beanId = null;
            }
            this.beanClassName = sourceBean != null ? sourceBean.getBeanClass().getName() : null;
            Annotated annotated = injectionPoint.getAnnotated();
            this.annotatedBaseType = annotated != null ? annotated.getBaseType() : injectionPoint.getType();
            this.member = injectionPoint.getMember();
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return Collections.unmodifiableSet(qualifiers);
        }

        @Override
        public Bean<?> getBean() {
            if (bean != null) {
                return bean;
            }
            try {
                jakarta.enterprise.inject.spi.BeanManager beanManager = CDI.current().getBeanManager();
                if (beanId != null) {
                    Bean<?> passivationCapable = beanManager.getPassivationCapableBean(beanId);
                    if (passivationCapable != null) {
                        bean = passivationCapable;
                        return bean;
                    }
                }
                if (beanClassName != null) {
                    Class<?> beanClass = Class.forName(beanClassName);
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    Set<Bean<?>> beans = (Set) beanManager.getBeans(beanClass);
                    Bean<?> resolved = beanManager.resolve(beans);
                    if (resolved != null) {
                        bean = resolved;
                    }
                }
            } catch (Exception ignored) {
                // Bean lookup best effort for serialization support.
            }
            return bean;
        }

        @Override
        public Member getMember() {
            if (member != null) {
                return member;
            }
            member = memberRef.resolve();
            return member;
        }

        @Override
        public Annotated getAnnotated() {
            return new AnnotatedBaseTypeOnly(annotatedBaseType, qualifiers);
        }

        @Override
        public boolean isDelegate() {
            return delegate;
        }

        @Override
        public boolean isTransient() {
            return trans;
        }
    }

    private static final class AnnotatedBaseTypeOnly implements Annotated, Serializable {
        private static final long serialVersionUID = 1L;

        private final Type baseType;
        private final Set<Annotation> annotations;

        private AnnotatedBaseTypeOnly(Type baseType, Set<Annotation> qualifiers) {
            this.baseType = baseType;
            this.annotations = new LinkedHashSet<Annotation>(qualifiers);
        }

        @Override
        public Type getBaseType() {
            return baseType;
        }

        @Override
        public Set<Type> getTypeClosure() {
            return Collections.<Type>singleton(baseType);
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
            for (Annotation annotation : annotations) {
                if (annotationType.equals(annotation.annotationType())) {
                    return annotationType.cast(annotation);
                }
            }
            return null;
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return Collections.unmodifiableSet(annotations);
        }

        @Override
        public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
            if (annotationType == null) {
                return Collections.emptySet();
            }
            Set<T> matches = new LinkedHashSet<T>();
            for (Annotation annotation : annotations) {
                if (annotationType.equals(annotation.annotationType())) {
                    matches.add(annotationType.cast(annotation));
                }
            }
            return matches;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return getAnnotation(annotationType) != null;
        }
    }

    private static final class MemberRef implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String declaringClassName;
        private final String name;
        private final boolean constructor;
        private final String[] parameterTypeNames;

        private MemberRef(String declaringClassName, String name, boolean constructor, String[] parameterTypeNames) {
            this.declaringClassName = declaringClassName;
            this.name = name;
            this.constructor = constructor;
            this.parameterTypeNames = parameterTypeNames;
        }

        private static MemberRef of(Member member) {
            if (member == null) {
                return new MemberRef(null, null, false, new String[0]);
            }
            if (member instanceof Field) {
                Field field = (Field) member;
                return new MemberRef(field.getDeclaringClass().getName(), field.getName(), false, new String[0]);
            }
            if (member instanceof Executable) {
                Executable executable = (Executable) member;
                Class<?>[] parameterTypes = executable.getParameterTypes();
                String[] parameterTypeNames = new String[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    parameterTypeNames[i] = parameterTypes[i].getName();
                }
                return new MemberRef(
                        executable.getDeclaringClass().getName(),
                        executable.getName(),
                        executable instanceof Constructor,
                        parameterTypeNames
                );
            }
            return new MemberRef(member.getDeclaringClass().getName(), member.getName(), false, new String[0]);
        }

        private Member resolve() {
            if (declaringClassName == null || name == null) {
                return null;
            }
            try {
                Class<?> declaringClass = Class.forName(declaringClassName);
                if (constructor) {
                    Class<?>[] parameterTypes = loadParameterTypes();
                    Constructor<?> ctor = declaringClass.getDeclaredConstructor(parameterTypes);
                    ctor.setAccessible(true);
                    return ctor;
                }
                if (parameterTypeNames.length == 0) {
                    Field field = declaringClass.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                }
                Class<?>[] parameterTypes = loadParameterTypes();
                Method method = declaringClass.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (Exception e) {
                return null;
            }
        }

        private Class<?>[] loadParameterTypes() throws ClassNotFoundException {
            Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                parameterTypes[i] = loadClass(parameterTypeNames[i]);
            }
            return parameterTypes;
        }

        private Class<?> loadClass(String name) throws ClassNotFoundException {
            if ("boolean".equals(name)) return boolean.class;
            if ("byte".equals(name)) return byte.class;
            if ("short".equals(name)) return short.class;
            if ("int".equals(name)) return int.class;
            if ("long".equals(name)) return long.class;
            if ("float".equals(name)) return float.class;
            if ("double".equals(name)) return double.class;
            if ("char".equals(name)) return char.class;
            return Class.forName(name);
        }
    }
}
