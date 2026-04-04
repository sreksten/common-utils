package com.threeamigos.common.util.implementations.injection.cditcktests.definition.name;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.name.namedefinitiontest.test.$Dollar;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.name.namedefinitiontest.test.JSFBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.name.namedefinitiontest.test.Haddock;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.name.namedefinitiontest.test.Minnow;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.name.namedefinitiontest.test.Moose;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.name.namedefinitiontest.test.RedSnapper;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.name.namedefinitiontest.test.SeaBass;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.name.namedefinitiontest.test._Underscore;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.name.namedefinitiontest.test.lowerCase;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameDefinitionTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.name.namedefinitiontest.test";

    @Test
    void testNonDefaultNamed() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Moose> moose = getUniqueBean(beanManager, Moose.class);
                assertEquals("aMoose", moose.getName());
            }
        });
    }

    @Test
    void testDefaultNamed() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                String name = "haddock";
                Bean<Haddock> haddock = getUniqueBean(beanManager, Haddock.class);
                assertEquals(name, haddock.getName());
                assertAnnotationSetMatches(haddock.getQualifiers(), Any.Literal.INSTANCE, Default.Literal.INSTANCE,
                        NamedLiteral.of(name));
            }
        });
    }

    @Test
    void testStereotypeDefaultsName() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<RedSnapper> bean = getUniqueBean(beanManager, RedSnapper.class);
                assertEquals("redSnapper", bean.getName());
            }
        });
    }

    @Test
    void testNamedNotDeclaredByBean() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<SeaBass> bean = getUniqueBean(beanManager, SeaBass.class);
                assertNull(bean.getName());
            }
        });
    }

    @Test
    void testNamedNotDeclaredByStereotype() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Minnow> bean = getUniqueBean(beanManager, Minnow.class);
                assertNull(bean.getName());
            }
        });
    }

    @Test
    void testNameStartingWithMultipleUpperCaseCharacters() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<JSFBean> bean = getUniqueBean(beanManager, JSFBean.class);
                assertEquals("jSFBean", bean.getName());
            }
        });
    }

    @Test
    void testNameStartingWithUnderScoreCharacter() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<_Underscore> bean = getUniqueBean(beanManager, _Underscore.class);
                assertEquals("_Underscore", bean.getName());
            }
        });
    }

    @Test
    void testNameStartingWithDollarCharacter() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<$Dollar> bean = getUniqueBean(beanManager, $Dollar.class);
                assertEquals("$Dollar", bean.getName());
            }
        });
    }

    @Test
    void testNameStartingWithLowerCaseCharacter() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<lowerCase> bean = getUniqueBean(beanManager, lowerCase.class);
                assertEquals("lowerCase", bean.getName());
            }
        });
    }

    private static void assertAnnotationSetMatches(Set<Annotation> qualifiers, Annotation... expected) {
        assertEquals(expected.length, qualifiers.size());
        for (Annotation annotation : expected) {
            assertTrue(qualifiers.contains(annotation), "Expected qualifier missing: " + annotation);
        }
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
