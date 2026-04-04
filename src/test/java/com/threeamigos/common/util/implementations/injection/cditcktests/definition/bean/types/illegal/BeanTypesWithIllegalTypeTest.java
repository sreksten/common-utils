package com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.illegal;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.illegal.beantypeswithillegaltypetest.test.AnimalHolder;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.illegal.beantypeswithillegaltypetest.test.Eagle;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.illegal.beantypeswithillegaltypetest.test.ProducedWithField;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.illegal.beantypeswithillegaltypetest.test.ProducedWithMethod;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BeanTypesWithIllegalTypeTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.illegal.beantypeswithillegaltypetest.test";

    @Test
    void beanSetOfBeanTypesContainsOnlyLegalTypes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                getUniqueEagleAndCheckItsTypes(beanManager);
            }
        });
    }

    @Test
    void producerFieldsetOfBeanTypesContainsOnlyLegalTypes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                getUniqueEagleAndCheckItsTypes(beanManager, ProducedWithField.ProducedWithFieldLiteral.INSTANCE);
            }
        });
    }

    @Test
    void producerMethodsetOfBeanTypesContainsOnlyLegalTypes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                getUniqueEagleAndCheckItsTypes(beanManager, ProducedWithMethod.ProducedWithMethoddLiteral.INSTANCE);
            }
        });
    }

    private static void getUniqueEagleAndCheckItsTypes(BeanManager beanManager, Annotation... qualifiers) {
        Bean<Eagle> eagleBean = getUniqueBean(beanManager, Eagle.class, qualifiers);
        for (Type type : eagleBean.getTypes()) {
            if (type instanceof ParameterizedType) {
                assertNotEquals(AnimalHolder.class, ((ParameterizedType) type).getRawType());
            }
        }
        assertEquals(3, eagleBean.getTypes().size());
    }

    private static void runInContainer(BeanManagerConsumer assertions) {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            assertions.accept(syringe.getBeanManager());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        assertEquals(1, beans.size(), "Expected a unique bean for type " + type.getName());
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private interface BeanManagerConsumer {
        void accept(BeanManager beanManager);
    }
}
