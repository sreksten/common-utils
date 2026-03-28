package com.threeamigos.common.util.implementations.injection.bce;

import com.threeamigos.common.util.implementations.injection.knowledgebase.InterceptorInfo;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.inject.spi.InterceptionType;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

final class BceInterceptorInfo implements jakarta.enterprise.inject.build.compatible.spi.InterceptorInfo {

    private final BeanInfo beanInfoDelegate;
    private final InterceptorInfo interceptorInfo;

    private BceInterceptorInfo(InterceptorInfo interceptorInfo) {
        this.interceptorInfo = interceptorInfo;
        this.beanInfoDelegate = BceMetadata.beanInfo(interceptorInfo.getInterceptorClass());
    }

    static Collection<jakarta.enterprise.inject.build.compatible.spi.InterceptorInfo> from(
        Collection<InterceptorInfo> interceptorInfos) {
        if (interceptorInfos == null || interceptorInfos.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<jakarta.enterprise.inject.build.compatible.spi.InterceptorInfo> out = new ArrayList<>();
        for (InterceptorInfo interceptorInfo : interceptorInfos) {
            out.add(new BceInterceptorInfo(interceptorInfo));
        }
        return Collections.unmodifiableCollection(out);
    }

    @Override
    public Collection<AnnotationInfo> interceptorBindings() {
        Collection<AnnotationInfo> out = new ArrayList<>();
        for (Annotation annotation : interceptorInfo.getInterceptorBindings()) {
            out.add(BceMetadata.annotationInfo(annotation));
        }
        return Collections.unmodifiableCollection(out);
    }

    @Override
    public boolean intercepts(InterceptionType interceptionType) {
        if (interceptionType == null) {
            return false;
        }
        switch (interceptionType) {
            case AROUND_INVOKE:
                return interceptorInfo.getAroundInvokeMethod() != null;
            case AROUND_CONSTRUCT:
                return interceptorInfo.getAroundConstructMethod() != null;
            case POST_CONSTRUCT:
                return interceptorInfo.getPostConstructMethod() != null;
            case PRE_DESTROY:
                return interceptorInfo.getPreDestroyMethod() != null;
            default:
                return false;
        }
    }

    @Override
    public jakarta.enterprise.lang.model.declarations.ClassInfo declaringClass() {
        return beanInfoDelegate.declaringClass();
    }

    @Override
    public Collection<AnnotationInfo> qualifiers() {
        return beanInfoDelegate.qualifiers();
    }

    @Override
    public jakarta.enterprise.inject.build.compatible.spi.ScopeInfo scope() {
        return beanInfoDelegate.scope();
    }

    @Override
    public String name() {
        return beanInfoDelegate.name();
    }

    @Override
    public Collection<jakarta.enterprise.lang.model.types.Type> types() {
        return beanInfoDelegate.types();
    }

    @Override
    public jakarta.enterprise.inject.build.compatible.spi.DisposerInfo disposer() {
        return beanInfoDelegate.disposer();
    }

    @Override
    public Collection<jakarta.enterprise.inject.build.compatible.spi.StereotypeInfo> stereotypes() {
        return beanInfoDelegate.stereotypes();
    }

    @Override
    public Collection<jakarta.enterprise.inject.build.compatible.spi.InjectionPointInfo> injectionPoints() {
        return beanInfoDelegate.injectionPoints();
    }

    @Override
    public Integer priority() {
        return interceptorInfo.getPriority();
    }

    @Override
    public boolean isClassBean() {
        return beanInfoDelegate.isClassBean();
    }

    @Override
    public boolean isProducerMethod() {
        return beanInfoDelegate.isProducerMethod();
    }

    @Override
    public boolean isProducerField() {
        return beanInfoDelegate.isProducerField();
    }

    @Override
    public boolean isSynthetic() {
        return beanInfoDelegate.isSynthetic();
    }

    @Override
    public jakarta.enterprise.lang.model.declarations.MethodInfo producerMethod() {
        return beanInfoDelegate.producerMethod();
    }

    @Override
    public jakarta.enterprise.lang.model.declarations.FieldInfo producerField() {
        return beanInfoDelegate.producerField();
    }

    @Override
    public boolean isAlternative() {
        return beanInfoDelegate.isAlternative();
    }
}
