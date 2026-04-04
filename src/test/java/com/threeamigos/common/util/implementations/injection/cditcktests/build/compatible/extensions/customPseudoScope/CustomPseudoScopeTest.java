package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customPseudoScope;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customPseudoScope.custompseudoscopetest.test.ApplicationScopedBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customPseudoScope.custompseudoscopetest.test.CustomPseudoScopeExtension;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customPseudoScope.custompseudoscopetest.test.DependentBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customPseudoScope.custompseudoscopetest.test.PrototypeBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customPseudoScope.custompseudoscopetest.test.RequestScopedBean;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CustomPseudoScopeTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customPseudoScope.custompseudoscopetest.test";

    @Test
    void test() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(CustomPseudoScopeExtension.class.getName());
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            PrototypeBean prototypeBean = resolveReference(beanManager, PrototypeBean.class);
            assertNotEquals(resolveReference(beanManager, PrototypeBean.class).getId(), prototypeBean.getId());

            ApplicationScopedBean applicationScopedBean = resolveReference(beanManager, ApplicationScopedBean.class);
            assertEquals(resolveReference(beanManager, ApplicationScopedBean.class).getPrototypeId(),
                    applicationScopedBean.getPrototypeId());
            assertNotEquals(prototypeBean.getId(), applicationScopedBean.getPrototypeId());

            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                RequestScopedBean requestScopedBean = resolveReference(beanManager, RequestScopedBean.class);
                assertEquals(resolveReference(beanManager, RequestScopedBean.class).getPrototypeId(),
                        requestScopedBean.getPrototypeId());
                assertNotEquals(prototypeBean.getId(), requestScopedBean.getPrototypeId());
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }

            DependentBean dependentBean = resolveReference(beanManager, DependentBean.class);
            assertNotEquals(resolveReference(beanManager, DependentBean.class).getPrototypeId(),
                    dependentBean.getPrototypeId());
            assertNotEquals(prototypeBean.getId(), dependentBean.getPrototypeId());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
