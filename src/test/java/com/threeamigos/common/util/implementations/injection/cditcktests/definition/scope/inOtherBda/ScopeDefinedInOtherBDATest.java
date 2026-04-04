package com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.inOtherBda;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.inOtherBda.scopedefinedinotherbdatest.lib.ThirdPartyScope;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.inOtherBda.scopedefinedinotherbdatest.test.ThirdPartyScopeBean;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScopeDefinedInOtherBDATest {

    @Test
    void testCustomScopeInOtherBDAisBeanDefiningAnnotation() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(ThirdPartyScopeBean.class, BeanArchiveMode.EXPLICIT);
        try {
            syringe.start();
            Bean<ThirdPartyScopeBean> thirdPartyScopeBean = resolveBean(syringe.getBeanManager(), ThirdPartyScopeBean.class);
            assertNotNull(thirdPartyScopeBean);
            assertEquals(ThirdPartyScope.class, thirdPartyScopeBean.getScope());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        return (Bean<T>) beanManager.resolve((Set) beanManager.getBeans(type));
    }
}
