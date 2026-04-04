package com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.AbstractAntelope;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.Animal;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.ComplicatedTuna;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.DeadlyAnimal;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.DeadlySpider;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.DependentFinalTuna;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.FriendlyAntelope;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.Horse;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.MyBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.MyGenericBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.MyInterface;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.MyRawBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.MySuperInterface;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.RedSnapper;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.Spider;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test.Tarantula;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanDefinitionTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.bean.beandefinitiontest.test";

    @Test
    void testBeanTypesNonEmpty() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, beanManager.getBeans(RedSnapper.class).size());
                assertFalse(resolveBean(beanManager, RedSnapper.class).getTypes().isEmpty());
            }
        });
    }

    @Test
    void testQualifiersNonEmpty() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, beanManager.getBeans(RedSnapper.class).size());
                assertFalse(resolveBean(beanManager, RedSnapper.class).getQualifiers().isEmpty());
            }
        });
    }

    @Test
    void testHasScopeType() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, beanManager.getBeans(RedSnapper.class).size());
                assertEquals(RequestScoped.class, resolveBean(beanManager, RedSnapper.class).getScope());
            }
        });
    }

    @Test
    void testBeanTypes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Tarantula> bean = resolveBean(beanManager, Tarantula.class);
                assertEquals(6, bean.getTypes().size());
                assertTrue(bean.getTypes().contains(Tarantula.class));
                assertTrue(bean.getTypes().contains(Spider.class));
                assertTrue(bean.getTypes().contains(Animal.class));
                assertTrue(bean.getTypes().contains(Object.class));
                assertTrue(bean.getTypes().contains(DeadlySpider.class));
                assertTrue(bean.getTypes().contains(DeadlyAnimal.class));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGenericBeanTypes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            @SuppressWarnings("unchecked")
            public void accept(BeanManager beanManager) {
                assertEquals(1, beanManager.getBeans(MyRawBean.class).size());
                Bean<MyGenericBean<?>> bean = (Bean<MyGenericBean<?>>) beanManager.resolve((Set) beanManager.getBeans(MyGenericBean.class));
                assertEquals(5, bean.getTypes().size());

                assertTrue(containsClass(bean.getTypes(), MyGenericBean.class));
                assertFalse(bean.getTypes().contains(MyGenericBean.class));

                assertTrue(containsClass(bean.getTypes(), MyBean.class));
                assertFalse(bean.getTypes().contains(MyBean.class));

                assertTrue(bean.getTypes().contains(MyInterface.class));

                assertTrue(containsClass(bean.getTypes(), MySuperInterface.class));
                assertFalse(bean.getTypes().contains(MySuperInterface.class));

                assertTrue(bean.getTypes().contains(Object.class));
            }
        });
    }

    @Test
    void testRawBeanTypes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertEquals(1, beanManager.getBeans(MyRawBean.class).size());
                Bean<MyRawBean> bean = resolveBean(beanManager, MyRawBean.class);
                assertEquals(5, bean.getTypes().size());
                assertTrue(bean.getTypes().contains(MyRawBean.class));
                assertTrue(bean.getTypes().contains(MyBean.class));
                assertTrue(bean.getTypes().contains(MyInterface.class));
                assertTrue(bean.getTypes().contains(MySuperInterface.class)
                        || bean.getTypes().contains(new TypeLiteral<MySuperInterface<Number>>() {
                        }.getType()));
                assertTrue(bean.getTypes().contains(Object.class));
            }
        });
    }

    @Test
    void testBeanClientCanCastBeanInstanceToAnyBeanType() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Tarantula> bean = resolveBean(beanManager, Tarantula.class);
                CreationalContext<Tarantula> creationalContext = beanManager.createCreationalContext(bean);
                Tarantula tarantula = bean.create(creationalContext);

                Animal animal = tarantula;
                Object obj = tarantula;
                DeadlySpider deadlySpider = tarantula;
                DeadlyAnimal deadlyAnimal = tarantula;
            }
        });
    }

    @Test
    void testAbstractApiType() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<FriendlyAntelope> bean = resolveBean(beanManager, FriendlyAntelope.class);
                assertEquals(4, bean.getTypes().size());
                assertTrue(bean.getTypes().contains(FriendlyAntelope.class));
                assertTrue(bean.getTypes().contains(AbstractAntelope.class));
                assertTrue(bean.getTypes().contains(Animal.class));
                assertTrue(bean.getTypes().contains(Object.class));
            }
        });
    }

    @Test
    void testFinalApiType() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertFalse(beanManager.getBeans(DependentFinalTuna.class).isEmpty());
            }
        });
    }

    @Test
    void testMultipleStereotypes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<ComplicatedTuna> tunaBean = resolveBean(beanManager, ComplicatedTuna.class);
                assertEquals(RequestScoped.class, tunaBean.getScope());
                assertEquals("complicatedTuna", tunaBean.getName());
            }
        });
    }

    @Test
    void testBeanExtendsAnotherBean() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertFalse(beanManager.getBeans(Spider.class).isEmpty());
                assertFalse(beanManager.getBeans(Tarantula.class).isEmpty());
            }
        });
    }

    @Test
    void testBeanClassOnSimpleBean() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Set<Bean<?>> beans = beanManager.getBeans(Horse.class);
                assertEquals(1, beans.size());
                assertEquals(Horse.class, beanManager.resolve(beans).getBeanClass());
            }
        });
    }

    private static boolean containsClass(Set<Type> types, Class<?> clazz) {
        for (Type type : types) {
            if (type instanceof Class<?>) {
                if (((Class<?>) type).equals(clazz)) {
                    return true;
                }
            }
            if (type instanceof ParameterizedType) {
                if (((ParameterizedType) type).getRawType().equals(clazz)) {
                    return true;
                }
            }
        }
        return false;
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
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        return (Bean<T>) beanManager.resolve((Set) beanManager.getBeans(type));
    }

    private interface BeanManagerConsumer {
        void accept(BeanManager beanManager);
    }
}
