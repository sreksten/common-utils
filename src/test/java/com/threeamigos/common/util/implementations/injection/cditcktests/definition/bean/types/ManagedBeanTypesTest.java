package com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.managedbeantypestest.test.Animal;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.managedbeantypestest.test.Bird;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.managedbeantypestest.test.Flock;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.managedbeantypestest.test.Gathering;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.managedbeantypestest.test.GriffonVulture;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.managedbeantypestest.test.GroupingOfCertainType;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.managedbeantypestest.test.Mammal;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.managedbeantypestest.test.Tiger;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.managedbeantypestest.test.Vulture;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedBeanTypesTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.types.managedbeantypestest.test";

    @SuppressWarnings("serial")
    @Test
    void testGenericHierarchyBeanTypes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<GriffonVulture> vultureBean = getUniqueBean(beanManager, GriffonVulture.class);
                assertNotNull(vultureBean);
                assertEquals(5, vultureBean.getTypes().size());
                assertTypeSetMatches(vultureBean.getTypes(),
                        Object.class,
                        GriffonVulture.class,
                        new TypeLiteral<Animal<Integer>>() {}.getType(),
                        new TypeLiteral<Bird<String, Integer>>() {}.getType(),
                        new TypeLiteral<Vulture<Integer>>() {}.getType());

                Bean<Tiger> tigerBean = getUniqueBean(beanManager, Tiger.class);
                assertNotNull(tigerBean);
                assertEquals(4, tigerBean.getTypes().size());
                assertTypeSetMatches(tigerBean.getTypes(),
                        Object.class,
                        Tiger.class,
                        new TypeLiteral<Animal<String>>() {}.getType(),
                        new TypeLiteral<Mammal<String>>() {}.getType());

                Bean<Flock> flockBean = getUniqueBean(beanManager, Flock.class);
                assertNotNull(flockBean);
                assertTypeSetMatches(flockBean.getTypes(),
                        Object.class,
                        Flock.class,
                        new TypeLiteral<Gathering<Vulture<Integer>>>() {}.getType(),
                        new TypeLiteral<GroupingOfCertainType<Vulture<Integer>>>() {}.getType());
            }
        });
    }

    private static void assertTypeSetMatches(Set<Type> actualTypes, Type... expectedTypes) {
        assertEquals(expectedTypes.length, actualTypes.size());
        for (Type expectedType : expectedTypes) {
            assertTrue(actualTypes.contains(expectedType), "Expected bean type not found: " + expectedType);
        }
    }

    private static void runInContainer(BeanManagerConsumer assertions) {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(GriffonVulture.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Vulture.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tiger.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Flock.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
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
