package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.inheritance;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.inheritance.stereotypeinheritencetest.test.Horse;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StereotypeInheritenceTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.inheritance.stereotypeinheritencetest.test";

    @Test
    void testInheritence() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            Set<Bean<Horse>> beans = getBeans(syringe.getBeanManager(), Horse.class);
            assertEquals(1, beans.size());
            Bean<Horse> bean = beans.iterator().next();
            assertEquals(RequestScoped.class, bean.getScope());
            assertTrue(bean.isAlternative());
            assertEquals("horse", bean.getName());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        return (Set<Bean<T>>) (Set) beanManager.getBeans(type, qualifiers);
    }
}
