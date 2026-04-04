package com.threeamigos.common.util.implementations.injection.bce;

import com.threeamigos.common.util.implementations.injection.AnnotationsEnum;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.build.compatible.spi.MethodConfig;
import jakarta.enterprise.inject.build.compatible.spi.ParameterConfig;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

final class BceMetaAnnotations implements MetaAnnotations {

    private final KnowledgeBase knowledgeBase;
    private final MessageHandler messageHandler;

    BceMetaAnnotations(KnowledgeBase knowledgeBase, MessageHandler messageHandler) {
        this.knowledgeBase = knowledgeBase;
        this.messageHandler = messageHandler;
    }

    @Override
    public ClassConfig addQualifier(Class<? extends Annotation> qualifierAnnotation) {
        knowledgeBase.addQualifier(qualifierAnnotation);
        return new DynamicClassConfig(qualifierAnnotation);
    }

    @Override
    public ClassConfig addInterceptorBinding(Class<? extends Annotation> interceptorBindingAnnotation) {
        knowledgeBase.addInterceptorBinding(interceptorBindingAnnotation);
        return new DynamicClassConfig(interceptorBindingAnnotation);
    }

    @Override
    public ClassConfig addStereotype(Class<? extends Annotation> stereotypeAnnotation) {
        knowledgeBase.addStereotype(stereotypeAnnotation);
        return new DynamicClassConfig(stereotypeAnnotation);
    }

    @Override
    public void addContext(Class<? extends Annotation> scopeAnnotation,
                           Class<? extends AlterableContext> contextImplementation) {
        addContext(scopeAnnotation, AnnotationsEnum.hasNormalScopeAnnotation(scopeAnnotation), contextImplementation);
    }

    @Override
    public void addContext(Class<? extends Annotation> scopeAnnotation,
                           boolean isNormal,
                           Class<? extends AlterableContext> contextImplementation) {
        boolean passivating = Boolean.TRUE.equals(AnnotationsEnum.getNormalScopePassivatingValue(scopeAnnotation));
        knowledgeBase.addScope(scopeAnnotation, isNormal, isNormal && passivating);
        knowledgeBase.addContextImplementation(scopeAnnotation, contextImplementation);
        messageHandler.handleInfoMessage("[BCE] Registered context for scope " +
            scopeAnnotation.getName() + " using " + contextImplementation.getName());
    }

    private static final class DynamicClassConfig implements ClassConfig {
        private final Class<? extends Annotation> annotationType;
        private final ClassInfo classInfo;
        private final Collection<MethodConfig> methods;

        private DynamicClassConfig(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
            this.classInfo = BceMetadata.classInfo(annotationType);
            this.methods = buildMethodConfigs(annotationType);
        }

        @Override
        public ClassInfo info() {
            return classInfo;
        }

        @Override
        public ClassConfig addAnnotation(Class<? extends Annotation> annotationType) {
            return this;
        }

        @Override
        public ClassConfig addAnnotation(AnnotationInfo annotation) {
            return this;
        }

        @Override
        public ClassConfig addAnnotation(Annotation annotation) {
            return this;
        }

        @Override
        public ClassConfig removeAnnotation(Predicate<AnnotationInfo> predicate) {
            return this;
        }

        @Override
        public ClassConfig removeAllAnnotations() {
            return this;
        }

        @Override
        public Collection<MethodConfig> constructors() {
            return Collections.emptyList();
        }

        @Override
        public Collection<MethodConfig> methods() {
            return methods;
        }

        @Override
        public Collection<FieldConfig> fields() {
            return Collections.emptyList();
        }

        private Collection<MethodConfig> buildMethodConfigs(Class<? extends Annotation> annotationType) {
            Method[] declaredMethods = annotationType.getDeclaredMethods();
            List<MethodConfig> configs = new ArrayList<>(declaredMethods.length);
            for (Method declaredMethod : declaredMethods) {
                configs.add(new DynamicMethodConfig(this.annotationType, declaredMethod));
            }
            return Collections.unmodifiableList(configs);
        }
    }

    private static final class DynamicMethodConfig implements MethodConfig {
        private final Class<? extends Annotation> annotationType;
        private final String methodName;
        private final MethodInfo methodInfo;

        private DynamicMethodConfig(Class<? extends Annotation> annotationType, Method method) {
            this.annotationType = annotationType;
            this.methodName = method.getName();
            this.methodInfo = BceMetadata.methodInfo(method);
        }

        @Override
        public MethodInfo info() {
            return methodInfo;
        }

        @Override
        public MethodConfig addAnnotation(Class<? extends Annotation> annotationType) {
            if (AnnotationsEnum.hasNonbindingAnnotation(annotationType)) {
                AnnotationsEnum.registerDynamicNonbindingMember(this.annotationType, methodName);
            }
            return this;
        }

        @Override
        public MethodConfig addAnnotation(AnnotationInfo annotation) {
            if (annotation != null &&
                    isNonbindingAnnotationName(annotation.declaration() != null ? annotation.declaration().name() : null)) {
                AnnotationsEnum.registerDynamicNonbindingMember(this.annotationType, methodName);
            }
            return this;
        }

        @Override
        public MethodConfig addAnnotation(Annotation annotation) {
            if (annotation != null && AnnotationsEnum.hasNonbindingAnnotation(annotation.annotationType())) {
                AnnotationsEnum.registerDynamicNonbindingMember(this.annotationType, methodName);
            }
            return this;
        }

        @Override
        public MethodConfig removeAnnotation(Predicate<AnnotationInfo> predicate) {
            return this;
        }

        @Override
        public MethodConfig removeAllAnnotations() {
            return this;
        }

        @Override
        public List<ParameterConfig> parameters() {
            return Collections.emptyList();
        }
    }

    private static boolean isNonbindingAnnotationName(String annotationName) {
        if (annotationName == null) {
            return false;
        }
        for (Class<? extends Annotation> type : AnnotationsEnum.NONBINDING.getAnnotations()) {
            if (type != null && annotationName.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }
}
