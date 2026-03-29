package com.threeamigos.common.util.implementations.injection.bce;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.build.compatible.spi.MethodConfig;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
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
        return new SimpleClassConfig(qualifierAnnotation);
    }

    @Override
    public ClassConfig addInterceptorBinding(Class<? extends Annotation> interceptorBindingAnnotation) {
        knowledgeBase.addInterceptorBinding(interceptorBindingAnnotation);
        return new SimpleClassConfig(interceptorBindingAnnotation);
    }

    @Override
    public ClassConfig addStereotype(Class<? extends Annotation> stereotypeAnnotation) {
        knowledgeBase.addStereotype(stereotypeAnnotation);
        return new SimpleClassConfig(stereotypeAnnotation);
    }

    @Override
    public void addContext(Class<? extends Annotation> scopeAnnotation,
                           Class<? extends AlterableContext> contextImplementation) {
        addContext(scopeAnnotation, false, contextImplementation);
    }

    @Override
    public void addContext(Class<? extends Annotation> scopeAnnotation,
                           boolean isNormal,
                           Class<? extends AlterableContext> contextImplementation) {
        knowledgeBase.addScope(scopeAnnotation, isNormal, false);
        knowledgeBase.addContextImplementation(scopeAnnotation, contextImplementation);
        messageHandler.handleInfoMessage("[BCE] Registered context for scope " +
            scopeAnnotation.getName() + " using " + contextImplementation.getName());
    }

    private static final class SimpleClassConfig implements ClassConfig {
        private final ClassInfo classInfo;

        private SimpleClassConfig(Class<? extends Annotation> annotationType) {
            this.classInfo = BceMetadata.classInfo(annotationType);
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
            return Collections.emptyList();
        }

        @Override
        public Collection<FieldConfig> fields() {
            return Collections.emptyList();
        }
    }
}
