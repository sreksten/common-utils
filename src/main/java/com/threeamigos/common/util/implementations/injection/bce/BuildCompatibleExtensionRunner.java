package com.threeamigos.common.util.implementations.injection.bce;

import com.threeamigos.common.util.implementations.injection.AnnotationsEnum;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.BuildServices;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.DisposerInfo;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.InterceptorInfo;
import jakarta.enterprise.inject.build.compatible.spi.InjectionPointInfo;
import jakarta.enterprise.inject.build.compatible.spi.MethodConfig;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.inject.build.compatible.spi.ScopeInfo;
import jakarta.enterprise.inject.build.compatible.spi.StereotypeInfo;
import jakarta.enterprise.inject.build.compatible.spi.Types;
import jakarta.enterprise.inject.build.compatible.spi.InvokerFactory;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import com.threeamigos.common.util.implementations.injection.resolution.ProducerBean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal phase executor for Build Compatible Extensions.
 *
 * <p>Step 2 scope: detect methods annotated with supported BCE phase annotations
 * and invoke no-arg methods in deterministic order.
 */
public class BuildCompatibleExtensionRunner {

    private final MessageHandler messageHandler;
    private final KnowledgeBase knowledgeBase;
    private final BeanManagerImpl beanManager;
    private final BceInvokerRegistry invokerRegistry;
    private final BceInvokerFactoryImpl invokerFactory;
    private final BceRegistrationContext registrationContext;
    private final BuildServices buildServices;
    private final Types types;
    private final Messages messages;
    private final MetaAnnotations metaAnnotations;
    private final ScannedClasses scannedClasses;

    public BuildCompatibleExtensionRunner(MessageHandler messageHandler,
                                          KnowledgeBase knowledgeBase,
                                          BeanManagerImpl beanManager,
                                          BceInvokerRegistry invokerRegistry) {
        this.messageHandler = messageHandler;
        this.knowledgeBase = knowledgeBase;
        this.beanManager = beanManager;
        this.invokerRegistry = invokerRegistry;
        this.invokerFactory = new BceInvokerFactoryImpl(knowledgeBase, beanManager, messageHandler, invokerRegistry);
        this.registrationContext = new BceRegistrationContext(knowledgeBase);
        this.buildServices = new BceBuildServices();
        this.types = new BceTypes();
        this.messages = new BceMessages(messageHandler, knowledgeBase);
        this.metaAnnotations = new BceMetaAnnotations(knowledgeBase, messageHandler);
        this.scannedClasses = new BceScannedClasses(knowledgeBase, messageHandler);
    }

    public void runPhase(BuildCompatibleExtensionSupport.SupportedPhase phase,
                         List<BuildCompatibleExtension> extensions) {
        if (phase == null || extensions == null || extensions.isEmpty()) {
            return;
        }

        List<PhaseMethodInvocation> invocations = new ArrayList<>();
        for (BuildCompatibleExtension extension : extensions) {
            collectPhaseMethods(extension, phase, invocations);
        }

        invocations.sort(Comparator
            .comparing((PhaseMethodInvocation i) -> i.extension.getClass().getName())
            .thenComparing(i -> i.method.getName()));

        BceSyntheticComponents syntheticComponents = null;
        EnhancementModelState enhancementModelState = null;
        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.SYNTHESIS) {
            syntheticComponents = new BceSyntheticComponents(knowledgeBase, beanManager, invokerRegistry);
        } else if (phase == BuildCompatibleExtensionSupport.SupportedPhase.ENHANCEMENT) {
            enhancementModelState = new EnhancementModelState();
        }

        try (BceBuildServicesScope ignored = new BceBuildServicesScope(buildServices)) {
            for (PhaseMethodInvocation invocation : invocations) {
                invokePhaseMethod(invocation, phase, syntheticComponents, enhancementModelState);
            }
        }

        if (syntheticComponents != null) {
            syntheticComponents.complete();
        }
    }

    private void collectPhaseMethods(BuildCompatibleExtension extension,
                                     BuildCompatibleExtensionSupport.SupportedPhase phase,
                                     List<PhaseMethodInvocation> sink) {
        for (Method method : extension.getClass().getDeclaredMethods()) {
            if (matchesPhaseAnnotation(method, phase)) {
                sink.add(new PhaseMethodInvocation(extension, method));
            }
        }
    }

    private boolean matchesPhaseAnnotation(Method method,
                                           BuildCompatibleExtensionSupport.SupportedPhase phase) {
        switch (phase) {
            case DISCOVERY:
                return AnnotationsEnum.hasDiscoveryAnnotation(method);
            case ENHANCEMENT:
                return AnnotationsEnum.hasEnhancementAnnotation(method);
            case REGISTRATION:
                return AnnotationsEnum.hasRegistrationAnnotation(method);
            case SYNTHESIS:
                return AnnotationsEnum.hasSynthesisAnnotation(method);
            case VALIDATION:
                return AnnotationsEnum.hasValidationAnnotation(method);
            default:
                return false;
        }
    }

    private void invokePhaseMethod(PhaseMethodInvocation invocation,
                                   BuildCompatibleExtensionSupport.SupportedPhase phase,
                                   BceSyntheticComponents syntheticComponents,
                                   EnhancementModelState enhancementModelState) {
        Method method = invocation.method;
        validatePhaseMethodSignature(method, phase);

        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.ENHANCEMENT &&
            hasEnhancementModelParameter(method)) {
            invokeEnhancementModelMethods(invocation, enhancementModelState);
            return;
        }
        if ((phase == BuildCompatibleExtensionSupport.SupportedPhase.REGISTRATION ||
            phase == BuildCompatibleExtensionSupport.SupportedPhase.VALIDATION) &&
            hasRegistrationOrValidationModelParameter(method)) {
            invokeRegistrationOrValidationModelMethods(invocation, phase);
            return;
        }

        try {
            method.setAccessible(true);
            Object[] args = resolvePhaseMethodArguments(method, phase, syntheticComponents);
            method.invoke(invocation.extension, args);
            messageHandler.handleInfoMessage("[Syringe] Invoked BCE phase method: " +
                invocation.extension.getClass().getSimpleName() + "." + method.getName() +
                " (" + phase + ")");
        } catch (IllegalAccessException e) {
            throw new DefinitionException("Cannot access BCE phase method " +
                invocation.extension.getClass().getName() + "." + method.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof DefinitionException) {
                throw (DefinitionException) target;
            }
            if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            }
            throw new DefinitionException("Error invoking BCE phase method " +
                invocation.extension.getClass().getName() + "." + method.getName(), target);
        }
    }

    private void validatePhaseMethodSignature(Method method,
                                              BuildCompatibleExtensionSupport.SupportedPhase phase) {
        int phaseAnnotationCount = 0;
        if (AnnotationsEnum.hasDiscoveryAnnotation(method)) {
            phaseAnnotationCount++;
        }
        if (AnnotationsEnum.hasEnhancementAnnotation(method)) {
            phaseAnnotationCount++;
        }
        if (AnnotationsEnum.hasRegistrationAnnotation(method)) {
            phaseAnnotationCount++;
        }
        if (AnnotationsEnum.hasSynthesisAnnotation(method)) {
            phaseAnnotationCount++;
        }
        if (AnnotationsEnum.hasValidationAnnotation(method)) {
            phaseAnnotationCount++;
        }
        if (phaseAnnotationCount != 1) {
            throw new DefinitionException("Invalid BCE method " + method.getDeclaringClass().getName() +
                "." + method.getName() + ": method must declare exactly one BCE phase annotation.");
        }

        if (!void.class.equals(method.getReturnType())) {
            throw new DefinitionException("Invalid BCE " + phase + " method " +
                method.getDeclaringClass().getName() + "." + method.getName() +
                ": return type must be void.");
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        switch (phase) {
            case DISCOVERY:
                if (!areSupportedParameterTypes(parameterTypes,
                    BuildServices.class, Types.class, Messages.class, MetaAnnotations.class, ScannedClasses.class)) {
                    throw new DefinitionException("Invalid BCE " + phase + " method " +
                        method.getDeclaringClass().getName() + "." + method.getName() +
                        ": supported parameters are from {BuildServices, Types, Messages, MetaAnnotations, ScannedClasses}.");
                }
                break;
            case ENHANCEMENT:
                validateEnhancementSignature(method);
                break;
            case VALIDATION:
                validateRegistrationOrValidationSignature(method, phase);
                break;
            case SYNTHESIS:
                if (parameterTypes.length == 0 || parameterTypes.length > 4) {
                    throw new DefinitionException("Invalid BCE SYNTHESIS method " +
                        method.getDeclaringClass().getName() + "." + method.getName() +
                        ": expected SyntheticComponents and optional parameters from {BuildServices, Types, Messages}.");
                }
                boolean seenSyntheticComponents = false;
                boolean seenBuildServices = false;
                boolean seenTypes = false;
                boolean seenMessages = false;
                for (Class<?> parameterType : parameterTypes) {
                    if (SyntheticComponents.class.isAssignableFrom(parameterType)) {
                        if (seenSyntheticComponents) {
                            throw new DefinitionException("Invalid BCE SYNTHESIS method " +
                                method.getDeclaringClass().getName() + "." + method.getName() +
                                ": duplicate SyntheticComponents parameter.");
                        }
                        seenSyntheticComponents = true;
                        continue;
                    }
                    if (BuildServices.class.isAssignableFrom(parameterType)) {
                        if (seenBuildServices) {
                            throw new DefinitionException("Invalid BCE SYNTHESIS method " +
                                method.getDeclaringClass().getName() + "." + method.getName() +
                                ": duplicate BuildServices parameter.");
                        }
                        seenBuildServices = true;
                        continue;
                    }
                    if (Types.class.isAssignableFrom(parameterType)) {
                        if (seenTypes) {
                            throw new DefinitionException("Invalid BCE SYNTHESIS method " +
                                method.getDeclaringClass().getName() + "." + method.getName() +
                                ": duplicate Types parameter.");
                        }
                        seenTypes = true;
                        continue;
                    }
                    if (Messages.class.isAssignableFrom(parameterType)) {
                        if (seenMessages) {
                            throw new DefinitionException("Invalid BCE SYNTHESIS method " +
                                method.getDeclaringClass().getName() + "." + method.getName() +
                                ": duplicate Messages parameter.");
                        }
                        seenMessages = true;
                        continue;
                    }
                    throw new DefinitionException("Invalid BCE SYNTHESIS method " +
                        method.getDeclaringClass().getName() + "." + method.getName() +
                        ": unsupported parameter type " + parameterType.getName());
                }
                if (!seenSyntheticComponents) {
                    throw new DefinitionException("Invalid BCE SYNTHESIS method " +
                        method.getDeclaringClass().getName() + "." + method.getName() +
                        ": missing SyntheticComponents parameter.");
                }
                break;
            case REGISTRATION:
                validateRegistrationOrValidationSignature(method, phase);
                break;
            default:
                break;
        }
    }

    private void validateEnhancementSignature(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        int modelParamCount = 0;
        boolean seenBuildServices = false;
        boolean seenTypes = false;
        boolean seenMessages = false;
        for (Class<?> parameterType : parameterTypes) {
            if (BuildServices.class.isAssignableFrom(parameterType)) {
                if (seenBuildServices) {
                    throw new DefinitionException("Invalid BCE ENHANCEMENT method " +
                        method.getDeclaringClass().getName() + "." + method.getName() +
                        ": duplicate BuildServices parameter.");
                }
                seenBuildServices = true;
                continue;
            }
            if (Types.class.isAssignableFrom(parameterType)) {
                if (seenTypes) {
                    throw new DefinitionException("Invalid BCE ENHANCEMENT method " +
                        method.getDeclaringClass().getName() + "." + method.getName() +
                        ": duplicate Types parameter.");
                }
                seenTypes = true;
                continue;
            }
            if (Messages.class.isAssignableFrom(parameterType)) {
                if (seenMessages) {
                    throw new DefinitionException("Invalid BCE ENHANCEMENT method " +
                        method.getDeclaringClass().getName() + "." + method.getName() +
                        ": duplicate Messages parameter.");
                }
                seenMessages = true;
                continue;
            }
            if (isEnhancementModelType(parameterType)) {
                modelParamCount++;
                continue;
            }
            throw new DefinitionException("Invalid BCE ENHANCEMENT method " +
                method.getDeclaringClass().getName() + "." + method.getName() +
                ": unsupported parameter type " + parameterType.getName());
        }
        if (modelParamCount > 1) {
            throw new DefinitionException("Invalid BCE ENHANCEMENT method " +
                method.getDeclaringClass().getName() + "." + method.getName() +
                ": at most one model/config parameter is allowed from {ClassInfo, ClassConfig, MethodInfo, MethodConfig, FieldInfo, FieldConfig}.");
        }
    }

    private Object[] resolvePhaseMethodArguments(Method method,
                                                 BuildCompatibleExtensionSupport.SupportedPhase phase,
                                                 BceSyntheticComponents syntheticComponents) {
        if (method.getParameterCount() == 0) {
            return new Object[0];
        }

        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.SYNTHESIS) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                if (SyntheticComponents.class.isAssignableFrom(parameterType)) {
                    args[i] = syntheticComponents;
                } else {
                    Object commonService = resolveCommonServiceArgument(parameterType);
                    if (commonService != null) {
                        args[i] = commonService;
                    } else {
                        throw new DefinitionException("Unsupported synthesis parameter type " +
                            parameterType.getName() + " for method " +
                            method.getDeclaringClass().getName() + "." + method.getName());
                    }
                }
            }
            return args;
        }
        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.REGISTRATION) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                if (InvokerFactory.class.isAssignableFrom(parameterType)) {
                    args[i] = invokerFactory;
                } else if (BceRegistrationContext.class.isAssignableFrom(parameterType)) {
                    args[i] = registrationContext;
                } else {
                    Object commonService = resolveCommonServiceArgument(parameterType);
                    if (commonService != null) {
                        args[i] = commonService;
                    } else {
                        throw new DefinitionException("Unsupported registration parameter type " +
                            parameterType.getName() + " for method " +
                            method.getDeclaringClass().getName() + "." + method.getName());
                    }
                }
            }
            return args;
        }
        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.DISCOVERY ||
            phase == BuildCompatibleExtensionSupport.SupportedPhase.ENHANCEMENT ||
            phase == BuildCompatibleExtensionSupport.SupportedPhase.VALIDATION) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Object commonService = resolveCommonServiceArgument(parameterTypes[i]);
                if (commonService == null) {
                    throw new DefinitionException("Unsupported " + phase + " parameter type " +
                        parameterTypes[i].getName() + " for method " +
                        method.getDeclaringClass().getName() + "." + method.getName());
                }
                args[i] = commonService;
            }
            return args;
        }

        throw new DefinitionException("Unsupported BCE " + phase + " method signature: " +
            method.getDeclaringClass().getName() + "." + method.getName() +
            " - currently supported signatures are no-arg or BuildServices for Discovery/Enhancement/Validation, " +
            "@Registration methods with parameters from {BeanInfo, ObserverInfo, InvokerFactory, BceRegistrationContext, BuildServices, Types, Messages}, " +
            "@Validation methods with parameters from {BeanInfo, ObserverInfo, BuildServices, Types, Messages}, and " +
            "@Synthesis methods with SyntheticComponents and optional parameters from {BuildServices, Types, Messages}.");
    }

    private void validateRegistrationOrValidationSignature(Method method,
                                                           BuildCompatibleExtensionSupport.SupportedPhase phase) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.REGISTRATION && parameterTypes.length > 7) {
            throw new DefinitionException("Invalid BCE REGISTRATION method " +
                method.getDeclaringClass().getName() + "." + method.getName() +
                ": too many parameters.");
        }
        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.VALIDATION && parameterTypes.length > 5) {
            throw new DefinitionException("Invalid BCE VALIDATION method " +
                method.getDeclaringClass().getName() + "." + method.getName() +
                ": too many parameters.");
        }
        boolean seenInvokerFactory = false;
        boolean seenContext = false;
        boolean seenBuildServices = false;
        boolean seenTypes = false;
        boolean seenMessages = false;
        int modelCount = 0;
        for (Class<?> parameterType : parameterTypes) {
            if (InvokerFactory.class.isAssignableFrom(parameterType)) {
                if (phase != BuildCompatibleExtensionSupport.SupportedPhase.REGISTRATION || seenInvokerFactory) {
                    throw invalidRegistrationOrValidationParameter(method, phase, parameterType, "duplicate/illegal InvokerFactory");
                }
                seenInvokerFactory = true;
                continue;
            }
            if (BceRegistrationContext.class.isAssignableFrom(parameterType)) {
                if (phase != BuildCompatibleExtensionSupport.SupportedPhase.REGISTRATION || seenContext) {
                    throw invalidRegistrationOrValidationParameter(method, phase, parameterType, "duplicate/illegal BceRegistrationContext");
                }
                seenContext = true;
                continue;
            }
            if (BuildServices.class.isAssignableFrom(parameterType)) {
                if (seenBuildServices) {
                    throw invalidRegistrationOrValidationParameter(method, phase, parameterType, "duplicate BuildServices");
                }
                seenBuildServices = true;
                continue;
            }
            if (Types.class.isAssignableFrom(parameterType)) {
                if (seenTypes) {
                    throw invalidRegistrationOrValidationParameter(method, phase, parameterType, "duplicate Types");
                }
                seenTypes = true;
                continue;
            }
            if (Messages.class.isAssignableFrom(parameterType)) {
                if (seenMessages) {
                    throw invalidRegistrationOrValidationParameter(method, phase, parameterType, "duplicate Messages");
                }
                seenMessages = true;
                continue;
            }
            if (isRegistrationOrValidationModelType(parameterType)) {
                modelCount++;
                continue;
            }
            throw invalidRegistrationOrValidationParameter(method, phase, parameterType, "unsupported parameter");
        }
        if (modelCount > 1) {
            throw new DefinitionException("Invalid BCE " + phase + " method " +
                method.getDeclaringClass().getName() + "." + method.getName() +
                ": at most one model parameter is allowed from {BeanInfo, ObserverInfo, InterceptorInfo, InjectionPointInfo, DisposerInfo, ScopeInfo, StereotypeInfo}.");
        }
    }

    private DefinitionException invalidRegistrationOrValidationParameter(Method method,
                                                                         BuildCompatibleExtensionSupport.SupportedPhase phase,
                                                                         Class<?> parameterType,
                                                                         String reason) {
        return new DefinitionException("Invalid BCE " + phase + " method " +
            method.getDeclaringClass().getName() + "." + method.getName() +
            ": " + reason + " (" + parameterType.getName() + ").");
    }

    private boolean hasRegistrationOrValidationModelParameter(Method method) {
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (isRegistrationOrValidationModelType(parameterType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRegistrationOrValidationModelType(Class<?> parameterType) {
        return BeanInfo.class.isAssignableFrom(parameterType) ||
            ObserverInfo.class.isAssignableFrom(parameterType) ||
            InterceptorInfo.class.isAssignableFrom(parameterType) ||
            InjectionPointInfo.class.isAssignableFrom(parameterType) ||
            DisposerInfo.class.isAssignableFrom(parameterType) ||
            ScopeInfo.class.isAssignableFrom(parameterType) ||
            StereotypeInfo.class.isAssignableFrom(parameterType);
    }

    private void invokeRegistrationOrValidationModelMethods(PhaseMethodInvocation invocation,
                                                            BuildCompatibleExtensionSupport.SupportedPhase phase) {
        Method phaseMethod = invocation.method;
        Class<?> modelType = null;
        for (Class<?> parameterType : phaseMethod.getParameterTypes()) {
            if (isRegistrationOrValidationModelType(parameterType)) {
                modelType = parameterType;
                break;
            }
        }
        if (modelType == null) {
            return;
        }

        if (BeanInfo.class.isAssignableFrom(modelType)) {
            List<BeanInfo> beans = collectBeanInfosForPhase(phaseMethod, phase);
            for (BeanInfo beanInfo : beans) {
                invokeRegistrationOrValidationForModel(invocation, phase, beanInfo, null, null, null, null, null, null);
            }
            return;
        }
        if (ObserverInfo.class.isAssignableFrom(modelType)) {
            List<ObserverInfo> observers = collectObserverInfosForPhase(phaseMethod, phase);
            for (ObserverInfo observerInfo : observers) {
                invokeRegistrationOrValidationForModel(invocation, phase, null, observerInfo, null, null, null, null, null);
            }
            return;
        }
        if (InterceptorInfo.class.isAssignableFrom(modelType)) {
            List<InterceptorInfo> interceptors = collectInterceptorInfosForPhase(phaseMethod, phase);
            for (InterceptorInfo interceptorInfo : interceptors) {
                invokeRegistrationOrValidationForModel(invocation, phase, null, null, interceptorInfo, null, null, null, null);
            }
            return;
        }
        if (InjectionPointInfo.class.isAssignableFrom(modelType)) {
            List<InjectionPointInfo> injectionPoints = collectInjectionPointInfosForPhase(phaseMethod, phase);
            for (InjectionPointInfo injectionPointInfo : injectionPoints) {
                invokeRegistrationOrValidationForModel(
                    invocation, phase, null, null, null, injectionPointInfo, null, null, null);
            }
            return;
        }
        if (DisposerInfo.class.isAssignableFrom(modelType)) {
            List<DisposerInfo> disposers = collectDisposerInfosForPhase(phaseMethod, phase);
            for (DisposerInfo disposerInfo : disposers) {
                invokeRegistrationOrValidationForModel(
                    invocation, phase, null, null, null, null, disposerInfo, null, null);
            }
            return;
        }
        if (ScopeInfo.class.isAssignableFrom(modelType)) {
            List<ScopeInfo> scopes = collectScopeInfosForPhase(phaseMethod, phase);
            for (ScopeInfo scopeInfo : scopes) {
                invokeRegistrationOrValidationForModel(
                    invocation, phase, null, null, null, null, null, scopeInfo, null);
            }
            return;
        }
        if (StereotypeInfo.class.isAssignableFrom(modelType)) {
            List<StereotypeInfo> stereotypes = collectStereotypeInfosForPhase(phaseMethod, phase);
            for (StereotypeInfo stereotypeInfo : stereotypes) {
                invokeRegistrationOrValidationForModel(
                    invocation, phase, null, null, null, null, null, null, stereotypeInfo);
            }
        }
    }

    private List<BeanInfo> collectBeanInfosForPhase(Method phaseMethod,
                                                    BuildCompatibleExtensionSupport.SupportedPhase phase) {
        List<BeanInfo> beans = new ArrayList<BeanInfo>();
        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.REGISTRATION) {
            Registration registration = phaseMethod.getAnnotation(Registration.class);
            Class<?>[] acceptedTypes = registration != null ? registration.types() : new Class<?>[0];
            for (Class<?> clazz : knowledgeBase.getClasses()) {
                if (acceptedTypes.length == 0 || isClassAcceptedByRegistrationTypes(clazz, acceptedTypes)) {
                    beans.add(BceMetadata.beanInfo(clazz));
                }
            }
        } else {
            for (Class<?> clazz : knowledgeBase.getClasses()) {
                beans.add(BceMetadata.beanInfo(clazz));
            }
        }
        beans.sort(Comparator.comparing(bean -> bean.declaringClass().name()));
        return beans;
    }

    private List<ObserverInfo> collectObserverInfosForPhase(Method phaseMethod,
                                                            BuildCompatibleExtensionSupport.SupportedPhase phase) {
        List<ObserverInfo> out = new ArrayList<ObserverInfo>(BceObserverInfo.from(knowledgeBase.getObserverMethodInfos()));
        out.addAll(BceObserverInfo.fromSynthetic(knowledgeBase.getSyntheticObserverMethods()));
        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.REGISTRATION) {
            Registration registration = phaseMethod.getAnnotation(Registration.class);
            Class<?>[] acceptedTypes = registration != null ? registration.types() : new Class<?>[0];
            if (acceptedTypes.length > 0) {
                List<ObserverInfo> filtered = new ArrayList<ObserverInfo>();
                for (ObserverInfo observerInfo : out) {
                    Class<?> declaringClass = BceMetadata.unwrapClassInfo(observerInfo.declaringClass());
                    if (isClassAcceptedByRegistrationTypes(declaringClass, acceptedTypes)) {
                        filtered.add(observerInfo);
                    }
                }
                out = filtered;
            }
        }
        out.sort(Comparator.comparing(observer -> observer.declaringClass().name()));
        return out;
    }

    private List<InterceptorInfo> collectInterceptorInfosForPhase(Method phaseMethod,
                                                                  BuildCompatibleExtensionSupport.SupportedPhase phase) {
        List<InterceptorInfo> out = new ArrayList<InterceptorInfo>(
            BceInterceptorInfo.from(knowledgeBase.getInterceptorInfos()));
        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.REGISTRATION) {
            Registration registration = phaseMethod.getAnnotation(Registration.class);
            Class<?>[] acceptedTypes = registration != null ? registration.types() : new Class<?>[0];
            if (acceptedTypes.length > 0) {
                List<InterceptorInfo> filtered = new ArrayList<InterceptorInfo>();
                for (InterceptorInfo interceptorInfo : out) {
                    Class<?> declaringClass = BceMetadata.unwrapClassInfo(interceptorInfo.declaringClass());
                    if (isClassAcceptedByRegistrationTypes(declaringClass, acceptedTypes)) {
                        filtered.add(interceptorInfo);
                    }
                }
                out = filtered;
            }
        }
        out.sort(Comparator.comparing(interceptor -> interceptor.declaringClass().name()));
        return out;
    }

    private List<InjectionPointInfo> collectInjectionPointInfosForPhase(Method phaseMethod,
                                                                        BuildCompatibleExtensionSupport.SupportedPhase phase) {
        List<InjectionPointInfo> out = new ArrayList<InjectionPointInfo>();
        List<BeanInfo> beans = collectBeanInfosForPhase(phaseMethod, phase);
        for (BeanInfo beanInfo : beans) {
            Collection<InjectionPointInfo> points = beanInfo.injectionPoints();
            if (points != null) {
                out.addAll(points);
            }
        }
        out.sort(Comparator.comparing(this::injectionPointSortKey));
        return out;
    }

    private List<DisposerInfo> collectDisposerInfosForPhase(Method phaseMethod,
                                                            BuildCompatibleExtensionSupport.SupportedPhase phase) {
        List<DisposerInfo> out = new ArrayList<DisposerInfo>();
        Collection<ProducerBean<?>> producerBeans = knowledgeBase.getProducerBeans();
        if (producerBeans == null || producerBeans.isEmpty()) {
            return out;
        }
        Class<?>[] acceptedTypes = new Class<?>[0];
        if (phase == BuildCompatibleExtensionSupport.SupportedPhase.REGISTRATION) {
            Registration registration = phaseMethod.getAnnotation(Registration.class);
            acceptedTypes = registration != null ? registration.types() : new Class<?>[0];
        }
        for (ProducerBean<?> producerBean : producerBeans) {
            Class<?> declaringClass = producerBean.getDeclaringClass();
            if (declaringClass == null) {
                continue;
            }
            if (acceptedTypes.length > 0 && !isClassAcceptedByRegistrationTypes(declaringClass, acceptedTypes)) {
                continue;
            }
            DisposerInfo disposerInfo = BceDisposerInfo.from(producerBean);
            if (disposerInfo != null) {
                out.add(disposerInfo);
            }
        }
        out.sort(Comparator.comparing(this::disposerSortKey));
        return out;
    }

    private List<ScopeInfo> collectScopeInfosForPhase(Method phaseMethod,
                                                      BuildCompatibleExtensionSupport.SupportedPhase phase) {
        List<BeanInfo> beans = collectBeanInfosForPhase(phaseMethod, phase);
        List<ScopeInfo> out = new ArrayList<ScopeInfo>();
        List<String> seen = new ArrayList<String>();
        for (BeanInfo beanInfo : beans) {
            ScopeInfo scopeInfo = beanInfo.scope();
            if (scopeInfo == null || scopeInfo.annotation() == null) {
                continue;
            }
            String key = scopeInfo.annotation().name();
            if (key == null || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            out.add(scopeInfo);
        }
        out.sort(Comparator.comparing(scopeInfo -> scopeInfo.annotation().name()));
        return out;
    }

    private List<StereotypeInfo> collectStereotypeInfosForPhase(Method phaseMethod,
                                                                BuildCompatibleExtensionSupport.SupportedPhase phase) {
        List<BeanInfo> beans = collectBeanInfosForPhase(phaseMethod, phase);
        List<StereotypeInfo> out = new ArrayList<StereotypeInfo>();
        List<String> seen = new ArrayList<String>();
        for (BeanInfo beanInfo : beans) {
            Collection<StereotypeInfo> stereotypes = beanInfo.stereotypes();
            if (stereotypes == null) {
                continue;
            }
            for (StereotypeInfo stereotypeInfo : stereotypes) {
                if (stereotypeInfo == null) {
                    continue;
                }
                String key = stereotypeSortKey(stereotypeInfo);
                if (seen.contains(key)) {
                    continue;
                }
                seen.add(key);
                out.add(stereotypeInfo);
            }
        }
        out.sort(Comparator.comparing(this::stereotypeSortKey));
        return out;
    }

    private String stereotypeSortKey(StereotypeInfo stereotypeInfo) {
        if (stereotypeInfo == null) {
            return "";
        }
        ScopeInfo defaultScope = stereotypeInfo.defaultScope();
        String scopeKey = defaultScope != null && defaultScope.annotation() != null
            ? defaultScope.annotation().name() : "";
        return scopeKey + "|" + stereotypeInfo.isAlternative() + "|" + stereotypeInfo.isNamed();
    }

    private String disposerSortKey(DisposerInfo disposerInfo) {
        if (disposerInfo == null || disposerInfo.disposerMethod() == null) {
            return "";
        }
        return disposerInfo.disposerMethod().declaringClass().name() + "#" + disposerInfo.disposerMethod().name();
    }

    private String injectionPointSortKey(InjectionPointInfo injectionPointInfo) {
        if (injectionPointInfo == null || injectionPointInfo.declaration() == null) {
            return "";
        }
        if (injectionPointInfo.declaration() instanceof FieldInfo) {
            FieldInfo field = (FieldInfo) injectionPointInfo.declaration();
            return "F:" + field.declaringClass().name() + "#" + field.name();
        }
        if (injectionPointInfo.declaration() instanceof jakarta.enterprise.lang.model.declarations.ParameterInfo) {
            jakarta.enterprise.lang.model.declarations.ParameterInfo parameter =
                (jakarta.enterprise.lang.model.declarations.ParameterInfo) injectionPointInfo.declaration();
            return "P:" + parameter.declaringMethod().declaringClass().name() + "#" +
                parameter.declaringMethod().name() + ":" + parameter.name();
        }
        return "Z:" + injectionPointInfo.declaration().kind().name();
    }

    private boolean isClassAcceptedByRegistrationTypes(Class<?> candidate, Class<?>[] acceptedTypes) {
        for (Class<?> acceptedType : acceptedTypes) {
            if (acceptedType.isAssignableFrom(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void invokeRegistrationOrValidationForModel(PhaseMethodInvocation invocation,
                                                        BuildCompatibleExtensionSupport.SupportedPhase phase,
                                                        BeanInfo beanInfo,
                                                        ObserverInfo observerInfo,
                                                        InterceptorInfo interceptorInfo,
                                                        InjectionPointInfo injectionPointInfo,
                                                        DisposerInfo disposerInfo,
                                                        ScopeInfo scopeInfo,
                                                        StereotypeInfo stereotypeInfo) {
        try {
            Method phaseMethod = invocation.method;
            phaseMethod.setAccessible(true);
            Class<?>[] parameterTypes = phaseMethod.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                if (InterceptorInfo.class.isAssignableFrom(parameterType)) {
                    args[i] = interceptorInfo;
                    continue;
                }
                if (BeanInfo.class.isAssignableFrom(parameterType)) {
                    args[i] = beanInfo;
                    continue;
                }
                if (ObserverInfo.class.isAssignableFrom(parameterType)) {
                    args[i] = observerInfo;
                    continue;
                }
                if (InvokerFactory.class.isAssignableFrom(parameterType)) {
                    args[i] = invokerFactory;
                    continue;
                }
                if (BceRegistrationContext.class.isAssignableFrom(parameterType)) {
                    args[i] = registrationContext;
                    continue;
                }
                if (InjectionPointInfo.class.isAssignableFrom(parameterType)) {
                    args[i] = injectionPointInfo;
                    continue;
                }
                if (DisposerInfo.class.isAssignableFrom(parameterType)) {
                    args[i] = disposerInfo;
                    continue;
                }
                if (ScopeInfo.class.isAssignableFrom(parameterType)) {
                    args[i] = scopeInfo;
                    continue;
                }
                if (StereotypeInfo.class.isAssignableFrom(parameterType)) {
                    args[i] = stereotypeInfo;
                    continue;
                }
                Object common = resolveCommonServiceArgument(parameterType);
                if (common == null) {
                    throw new DefinitionException("Unsupported BCE " + phase + " parameter type " +
                        parameterType.getName() + " for method " +
                        phaseMethod.getDeclaringClass().getName() + "." + phaseMethod.getName());
                }
                args[i] = common;
            }
            phaseMethod.invoke(invocation.extension, args);
            messageHandler.handleInfoMessage("[Syringe] Invoked BCE " + phase + " method: " +
                invocation.extension.getClass().getSimpleName() + "." + phaseMethod.getName());
        } catch (IllegalAccessException e) {
            throw new DefinitionException("Cannot access BCE " + phase + " method " +
                invocation.extension.getClass().getName() + "." + invocation.method.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof DefinitionException) {
                throw (DefinitionException) target;
            }
            if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            }
            throw new DefinitionException("Error invoking BCE " + phase + " method " +
                invocation.extension.getClass().getName() + "." + invocation.method.getName(), target);
        }
    }

    private void invokeEnhancementModelMethods(PhaseMethodInvocation invocation) {
        invokeEnhancementModelMethods(invocation, new EnhancementModelState());
    }

    private void invokeEnhancementModelMethods(PhaseMethodInvocation invocation,
                                               EnhancementModelState enhancementModelState) {
        Method phaseMethod = invocation.method;
        Class<?> modelType = findEnhancementModelParameterType(phaseMethod);
        if (modelType == null) {
            invokePhaseMethod(invocation, BuildCompatibleExtensionSupport.SupportedPhase.ENHANCEMENT, null,
                enhancementModelState);
            return;
        }

        if (ClassInfo.class.isAssignableFrom(modelType) || ClassConfig.class.isAssignableFrom(modelType)) {
            for (Class<?> clazz : getEnhancedClasses(phaseMethod)) {
                invokeEnhancementForTarget(invocation, clazz, null, null, enhancementModelState);
            }
            return;
        }
        if (MethodInfo.class.isAssignableFrom(modelType) || MethodConfig.class.isAssignableFrom(modelType)) {
            for (Class<?> clazz : getEnhancedClasses(phaseMethod)) {
                List<Method> methods = new ArrayList<Method>(Arrays.asList(clazz.getDeclaredMethods()));
                methods.sort(Comparator.comparing(Method::getName).thenComparingInt(Method::getParameterCount));
                for (Method method : methods) {
                    if (matchesEnhancementAnnotationFilter(method, phaseMethod)) {
                        invokeEnhancementForTarget(invocation, clazz, method, null, enhancementModelState);
                    }
                }
            }
            return;
        }
        if (FieldInfo.class.isAssignableFrom(modelType) || FieldConfig.class.isAssignableFrom(modelType)) {
            for (Class<?> clazz : getEnhancedClasses(phaseMethod)) {
                List<Field> fields = new ArrayList<Field>(Arrays.asList(clazz.getDeclaredFields()));
                fields.sort(Comparator.comparing(Field::getName));
                for (Field field : fields) {
                    if (matchesEnhancementAnnotationFilter(field, phaseMethod)) {
                        invokeEnhancementForTarget(invocation, clazz, null, field, enhancementModelState);
                    }
                }
            }
        }
    }

    private void invokeEnhancementForTarget(PhaseMethodInvocation invocation,
                                            Class<?> clazz,
                                            Method method,
                                            Field field) {
        invokeEnhancementForTarget(invocation, clazz, method, field, new EnhancementModelState());
    }

    private void invokeEnhancementForTarget(PhaseMethodInvocation invocation,
                                            Class<?> clazz,
                                            Method method,
                                            Field field,
                                            EnhancementModelState enhancementModelState) {
        try {
            Method phaseMethod = invocation.method;
            phaseMethod.setAccessible(true);
            Class<?>[] parameterTypes = phaseMethod.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                Object mapped = mapEnhancementParameter(parameterType, clazz, method, field, enhancementModelState);
                if (mapped == null) {
                    throw new DefinitionException("Unsupported ENHANCEMENT parameter type " +
                        parameterType.getName() + " for method " +
                        phaseMethod.getDeclaringClass().getName() + "." + phaseMethod.getName());
                }
                args[i] = mapped;
            }
            phaseMethod.invoke(invocation.extension, args);
            messageHandler.handleInfoMessage("[Syringe] Invoked BCE ENHANCEMENT method: " +
                invocation.extension.getClass().getSimpleName() + "." + phaseMethod.getName());
        } catch (IllegalAccessException e) {
            throw new DefinitionException("Cannot access BCE ENHANCEMENT method " +
                invocation.extension.getClass().getName() + "." + invocation.method.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof DefinitionException) {
                throw (DefinitionException) target;
            }
            if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            }
            throw new DefinitionException("Error invoking BCE ENHANCEMENT method " +
                invocation.extension.getClass().getName() + "." + invocation.method.getName(), target);
        }
    }

    private Object mapEnhancementParameter(Class<?> parameterType,
                                           Class<?> clazz,
                                           Method method,
                                           Field field,
                                           EnhancementModelState enhancementModelState) {
        if (ClassInfo.class.isAssignableFrom(parameterType)) {
            if (enhancementModelState != null && enhancementModelState.classConfigs.containsKey(clazz)) {
                return enhancementModelState.classConfigs.get(clazz).info();
            }
            return BceMetadata.classInfo(clazz);
        }
        if (ClassConfig.class.isAssignableFrom(parameterType)) {
            if (enhancementModelState == null) {
                return BceEnhancementModels.classConfig(clazz);
            }
            return enhancementModelState.classConfig(clazz);
        }
        if (MethodInfo.class.isAssignableFrom(parameterType)) {
            if (method == null) {
                return null;
            }
            if (enhancementModelState != null && enhancementModelState.methodConfigs.containsKey(method)) {
                return enhancementModelState.methodConfigs.get(method).info();
            }
            return BceMetadata.methodInfo(method);
        }
        if (MethodConfig.class.isAssignableFrom(parameterType)) {
            if (method == null) {
                return null;
            }
            if (enhancementModelState == null) {
                return BceEnhancementModels.methodConfig(method);
            }
            return enhancementModelState.methodConfig(method);
        }
        if (FieldInfo.class.isAssignableFrom(parameterType)) {
            if (field == null) {
                return null;
            }
            if (enhancementModelState != null && enhancementModelState.fieldConfigs.containsKey(field)) {
                return enhancementModelState.fieldConfigs.get(field).info();
            }
            return BceMetadata.fieldInfo(field);
        }
        if (FieldConfig.class.isAssignableFrom(parameterType)) {
            if (field == null) {
                return null;
            }
            if (enhancementModelState == null) {
                return BceEnhancementModels.fieldConfig(field);
            }
            return enhancementModelState.fieldConfig(field);
        }
        return resolveCommonServiceArgument(parameterType);
    }

    private static final class EnhancementModelState {
        private final Map<Class<?>, ClassConfig> classConfigs = new HashMap<Class<?>, ClassConfig>();
        private final Map<Method, MethodConfig> methodConfigs = new HashMap<Method, MethodConfig>();
        private final Map<Field, FieldConfig> fieldConfigs = new HashMap<Field, FieldConfig>();

        private ClassConfig classConfig(Class<?> clazz) {
            if (!classConfigs.containsKey(clazz)) {
                classConfigs.put(clazz, BceEnhancementModels.classConfig(clazz, methodConfigs, fieldConfigs));
            }
            return classConfigs.get(clazz);
        }

        private MethodConfig methodConfig(Method method) {
            if (!methodConfigs.containsKey(method)) {
                methodConfigs.put(method, BceEnhancementModels.methodConfig(method));
            }
            return methodConfigs.get(method);
        }

        private FieldConfig fieldConfig(Field field) {
            if (!fieldConfigs.containsKey(field)) {
                fieldConfigs.put(field, BceEnhancementModels.fieldConfig(field));
            }
            return fieldConfigs.get(field);
        }
    }

    private List<Class<?>> getEnhancedClasses(Method phaseMethod) {
        Enhancement enhancement = phaseMethod.getAnnotation(Enhancement.class);
        if (enhancement == null) {
            return Collections.emptyList();
        }
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (Class<?> clazz : knowledgeBase.getClasses()) {
            if (matchesEnhancementTypeFilter(clazz, enhancement) &&
                matchesEnhancementAnnotationFilter(clazz, phaseMethod)) {
                classes.add(clazz);
            }
        }
        classes.sort(Comparator.comparing(Class::getName));
        return classes;
    }

    private boolean matchesEnhancementTypeFilter(Class<?> clazz, Enhancement enhancement) {
        Class<?>[] acceptedTypes = enhancement.types();
        if (acceptedTypes == null || acceptedTypes.length == 0) {
            return true;
        }
        for (Class<?> accepted : acceptedTypes) {
            if (enhancement.withSubtypes()) {
                if (accepted.isAssignableFrom(clazz)) {
                    return true;
                }
            } else if (accepted.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesEnhancementAnnotationFilter(java.lang.reflect.AnnotatedElement element,
                                                       Method phaseMethod) {
        Enhancement enhancement = phaseMethod.getAnnotation(Enhancement.class);
        if (enhancement == null) {
            return false;
        }
        Class<? extends Annotation>[] requiredAnnotations = enhancement.withAnnotations();
        if (requiredAnnotations == null || requiredAnnotations.length == 0) {
            return true;
        }
        for (Class<? extends Annotation> annotation : requiredAnnotations) {
            if (element.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEnhancementModelParameter(Method method) {
        return findEnhancementModelParameterType(method) != null;
    }

    private Class<?> findEnhancementModelParameterType(Method method) {
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (isEnhancementModelType(parameterType)) {
                return parameterType;
            }
        }
        return null;
    }

    private boolean isEnhancementModelType(Class<?> parameterType) {
        return ClassInfo.class.isAssignableFrom(parameterType) ||
            ClassConfig.class.isAssignableFrom(parameterType) ||
            MethodInfo.class.isAssignableFrom(parameterType) ||
            MethodConfig.class.isAssignableFrom(parameterType) ||
            FieldInfo.class.isAssignableFrom(parameterType) ||
            FieldConfig.class.isAssignableFrom(parameterType);
    }

    private boolean areSupportedParameterTypes(Class<?>[] parameterTypes, Class<?>... supportedTypes) {
        List<Class<?>> allowed = java.util.Arrays.asList(supportedTypes);
        List<Class<?>> seen = new ArrayList<Class<?>>();
        for (Class<?> parameterType : parameterTypes) {
            Class<?> matched = null;
            for (Class<?> allowedType : allowed) {
                if (allowedType.isAssignableFrom(parameterType)) {
                    matched = allowedType;
                    break;
                }
            }
            if (matched == null) {
                return false;
            }
            if (seen.contains(matched)) {
                return false;
            }
            seen.add(matched);
        }
        return true;
    }

    private Object resolveCommonServiceArgument(Class<?> parameterType) {
        if (BuildServices.class.isAssignableFrom(parameterType)) {
            return buildServices;
        }
        if (Types.class.isAssignableFrom(parameterType)) {
            return types;
        }
        if (Messages.class.isAssignableFrom(parameterType)) {
            return messages;
        }
        if (MetaAnnotations.class.isAssignableFrom(parameterType)) {
            return metaAnnotations;
        }
        if (ScannedClasses.class.isAssignableFrom(parameterType)) {
            return scannedClasses;
        }
        return null;
    }

    private static class PhaseMethodInvocation {
        private final BuildCompatibleExtension extension;
        private final Method method;

        private PhaseMethodInvocation(BuildCompatibleExtension extension, Method method) {
            this.extension = extension;
            this.method = method;
        }
    }
}
