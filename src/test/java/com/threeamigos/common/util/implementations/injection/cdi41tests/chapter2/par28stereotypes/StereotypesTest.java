package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par28stereotypes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par28stereotypes.bullet1.StereotypedRequestBean;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par28stereotypes.bullet2.InvalidStereotypedBean;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par28stereotypes.bullet3.DefaultActionBean;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par28stereotypes.bullet3.ExplicitDependentActionBean;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par28stereotypes.bullet4.ActionBoundBean;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par28stereotypes.bullet4.SecureInterceptor;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par28stereotypes.bullet4.TransactionalInterceptor;
import com.threeamigos.common.util.implementations.injection.interceptors.InterceptorResolver;
import com.threeamigos.common.util.implementations.injection.knowledgebase.InterceptorInfo;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Paragraph 2.8 - Stereotypes")
public class StereotypesTest {

    @Test
    @DisplayName("2.8 - Stereotype default scope is inherited by the bean")
    void stereotypeDefaultScopeIsInheritedByBean() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), StereotypedRequestBean.class);
        syringe.setup();

        Bean<?> bean = findBean(syringe, StereotypedRequestBean.class);
        assertEquals(RequestScoped.class, bean.getScope());
    }

    @Test
    @DisplayName("2.8.1.1 - Stereotype declaring more than one scope is a definition error")
    void stereotypeDeclaringMoreThanOneScopeIsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), InvalidStereotypedBean.class);

        assertThrows(DefinitionException.class, syringe::setup);
    }

    @Test
    @DisplayName("2.8.1.1 - Action stereotype defaults bean scope to @RequestScoped")
    void actionStereotypeDefaultsBeanScopeToRequestScoped() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), DefaultActionBean.class);
        syringe.setup();

        Bean<?> bean = findBean(syringe, DefaultActionBean.class);
        assertEquals(RequestScoped.class, bean.getScope());
    }

    @Test
    @DisplayName("2.8.1.1 - Explicit bean scope overrides Action stereotype default scope")
    void explicitBeanScopeOverridesActionStereotypeDefaultScope() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ExplicitDependentActionBean.class);
        syringe.setup();

        Bean<?> bean = findBean(syringe, ExplicitDependentActionBean.class);
        assertEquals(Dependent.class, bean.getScope());
    }

    @Test
    @DisplayName("2.8.1.2 - Interceptor bindings declared by stereotype apply to the bean")
    void interceptorBindingsDeclaredByStereotypeApplyToBean() throws NoSuchMethodException {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ActionBoundBean.class);
        syringe.setup();

        Method executeMethod = ActionBoundBean.class.getMethod("execute");
        InterceptorResolver resolver = new InterceptorResolver(syringe.getKnowledgeBase());
        List<InterceptorInfo> interceptors = resolver.resolve(
                ActionBoundBean.class,
                executeMethod,
                InterceptionType.AROUND_INVOKE
        );

        assertIterableEquals(
                List.of(SecureInterceptor.class, TransactionalInterceptor.class),
                interceptors.stream().map(InterceptorInfo::getInterceptorClass).toList()
        );
    }

    private Bean<?> findBean(Syringe syringe, Class<?> beanClass) {
        return syringe.getKnowledgeBase().getBeans().stream()
                .filter(bean -> bean.getBeanClass().equals(beanClass))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Bean not found: " + beanClass.getName()));
    }

}
