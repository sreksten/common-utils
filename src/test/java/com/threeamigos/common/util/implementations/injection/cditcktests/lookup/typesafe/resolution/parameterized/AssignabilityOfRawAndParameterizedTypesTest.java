package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.typesafe.resolution.parameterized;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("serial")
class AssignabilityOfRawAndParameterizedTypesTest {

    private static final Class<?>[] RESULT_TYPES = new Class<?>[]{ResultImpl.class, Result.class, Object.class};
    private static final Class<?>[] DAO_TYPES = new Class<?>[]{Dao.class, Object.class};
    private static final Class<?>[] BOX_TYPES = new Class<?>[]{BoxBarBazFooImpl.class, Box.class, Object.class};

    @Test
    void testAssignabilityToRawType() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(4, getBeans(syringe, Dao.class).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAssignabilityOfParameterizedTypeWithActualTypesToParameterizedTypeWithActualTypes() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(2, getBeans(syringe, new TypeLiteral<Map<Integer, Integer>>() {
            }).size());
            assertTrue(getBeans(syringe, new TypeLiteral<HashMap<Integer, Integer>>() {
            }).iterator().next().getTypes().contains(IntegerHashMap.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAssignabilityOfParameterizedTypeWithActualTypesToParameterizedTypeWithWildcards() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, new TypeLiteral<HashMap<? extends Number, ? super Integer>>() {
            }).size());
            assertTrue(getBeans(syringe, new TypeLiteral<HashMap<? extends Number, ? super Integer>>() {
            }).iterator().next().getTypes().contains(IntegerHashMap.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAssignabilityOfParameterizedTypeWithActualTypesToParameterizedTypeWithWildcardsAtInjectionPoint() {
        Syringe syringe = newSyringe();
        try {
            assertTrue(getContextualReference(syringe, InjectedBean.class).getMap() instanceof IntegerHashMap);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAssignabilityOfParameterizedTypeWithTypeVariablesToParameterizedTypeWithWildcards() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Result<? extends Throwable, ? super Exception>>> beans = getBeans(syringe,
                    new TypeLiteral<Result<? extends Throwable, ? super Exception>>() {
                    });
            assertEquals(1, beans.size());
            assertTrue(rawTypeSetMatches(beans.iterator().next().getTypes(), RESULT_TYPES));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAssignabilityOfParameterizedTypeWithTypeVariablesToParameterizedTypeWithWildcards2() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Result<? extends RuntimeException, ? super RuntimeException>>> beans = getBeans(syringe,
                    new TypeLiteral<Result<? extends RuntimeException, ? super RuntimeException>>() {
                    });
            assertEquals(1, beans.size());
            assertTrue(rawTypeSetMatches(beans.iterator().next().getTypes(), RESULT_TYPES));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    <T1 extends SubBar & SubBaz & Foo,
            T2 extends BarBazImpl & Foo,
            T3 extends SubBar & SubBaz & SuperFoo,
            T4 extends SubBar & SubBaz,
            T5 extends BarBazSuperFooImpl,
            T6 extends BarBazSuperFooImpl & SuperBarFooCloneable>
    void testAssignabilityOfParameterizedTypeWithTypeVariablesToParameterizedTypeWithWildcardWithLowerBound() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Result<? extends Exception, ? super Throwable>>> beans = getBeans(syringe,
                    new TypeLiteral<Result<? extends Exception, ? super Throwable>>() {
                    });
            assertEquals(0, beans.size());

            Set<Bean<Box<? super T1>>> beans1 = getBeans(syringe, new TypeLiteral<Box<? super T1>>() {
            });
            assertEquals(1, beans1.size());
            assertTrue(rawTypeSetMatches(beans1.iterator().next().getTypes(), BOX_TYPES));

            Set<Bean<Box<? super T2>>> beans2 = getBeans(syringe, new TypeLiteral<Box<? super T2>>() {
            });
            assertEquals(1, beans2.size());
            assertTrue(rawTypeSetMatches(beans2.iterator().next().getTypes(), BOX_TYPES));

            Set<Bean<Box<? super T3>>> noBeans3 = getBeans(syringe, new TypeLiteral<Box<? super T3>>() {
            });
            assertEquals(0, noBeans3.size());

            Set<Bean<Box<? super T4>>> noBeans4 = getBeans(syringe, new TypeLiteral<Box<? super T4>>() {
            });
            assertEquals(0, noBeans4.size());

            Set<Bean<Box<? super T5>>> noBeans5 = getBeans(syringe, new TypeLiteral<Box<? super T5>>() {
            });
            assertEquals(0, noBeans5.size());

            Set<Bean<Box<? super T6>>> beans6 = getBeans(syringe, new TypeLiteral<Box<? super T6>>() {
            });
            assertEquals(1, beans6.size());
            assertTrue(rawTypeSetMatches(beans6.iterator().next().getTypes(), BOX_TYPES));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAssignabilityOfParameterizedTypeWithTypeVariablesToParameterizedTypeWithActualTypes() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Result<RuntimeException, IllegalStateException>>> beans = getBeans(syringe,
                    new TypeLiteral<Result<RuntimeException, IllegalStateException>>() {
                    });
            assertEquals(1, beans.size());
            assertTrue(rawTypeSetMatches(beans.iterator().next().getTypes(), RESULT_TYPES));

            Set<Bean<Result<RuntimeException, Throwable>>> noBeans = getBeans(syringe,
                    new TypeLiteral<Result<RuntimeException, Throwable>>() {
                    });
            assertEquals(0, noBeans.size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAssignabilityOfParameterizedTypeWithTypeVariablesWithMultipleBoundsToParameterizedTypeWithActualTypes() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Box<BarSubBazFooImpl>>> beans = getBeans(syringe, new TypeLiteral<Box<BarSubBazFooImpl>>() {
            });
            assertEquals(1, beans.size());
            assertTrue(rawTypeSetMatches(beans.iterator().next().getTypes(), BOX_TYPES));

            Set<Bean<Box<BarBazSuperFooImpl>>> noBeans1 = getBeans(syringe, new TypeLiteral<Box<BarBazSuperFooImpl>>() {
            });
            assertEquals(0, noBeans1.size());

            Set<Bean<Box<BarBazImpl>>> noBeans2 = getBeans(syringe, new TypeLiteral<Box<BarBazImpl>>() {
            });
            assertEquals(0, noBeans2.size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    <T1 extends RuntimeException, T2 extends T1, T3> void testAssignabilityOfParameterizedTypeWithTypeVariablesToParameterizedTypeWithTypeVariable() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Result<T1, T2>>> beans = getBeans(syringe, new TypeLiteral<Result<T1, T2>>() {
            });
            assertEquals(1, beans.size());
            assertTrue(rawTypeSetMatches(beans.iterator().next().getTypes(), RESULT_TYPES));

            Set<Bean<Result<T1, T3>>> noBeans = getBeans(syringe, new TypeLiteral<Result<T1, T3>>() {
            });
            assertEquals(0, noBeans.size());

            Set<Bean<Dao<T1, T3>>> daoBeans = getBeans(syringe, new TypeLiteral<Dao<T1, T3>>() {
            });
            assertEquals(1, daoBeans.size());
            assertTrue(rawTypeSetMatches(daoBeans.iterator().next().getTypes(), DAO_TYPES));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    <T1 extends SubBar & SubBaz & Foo,
            T2 extends BarBazImpl & Foo,
            T3 extends SubBar & SubBaz & SuperFoo,
            T4 extends SubBar & SubBaz,
            T5 extends BarBazSuperFooImpl,
            T6 extends BarBazSuperFooImpl & SuperBarFooCloneable>
    void testAssignabilityOfParameterizedTypeWithTypeVariableWithMultipleBoundsToParameterizedTypeWithTypeVariable() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Box<T1>>> beans1 = getBeans(syringe, new TypeLiteral<Box<T1>>() {
            });
            assertEquals(1, beans1.size());
            assertTrue(rawTypeSetMatches(beans1.iterator().next().getTypes(), BOX_TYPES));

            Set<Bean<Box<T2>>> beans2 = getBeans(syringe, new TypeLiteral<Box<T2>>() {
            });
            assertEquals(1, beans2.size());
            assertTrue(rawTypeSetMatches(beans2.iterator().next().getTypes(), BOX_TYPES));

            Set<Bean<Box<T3>>> noBeans3 = getBeans(syringe, new TypeLiteral<Box<T3>>() {
            });
            assertEquals(0, noBeans3.size());

            Set<Bean<Box<T4>>> noBeans4 = getBeans(syringe, new TypeLiteral<Box<T4>>() {
            });
            assertEquals(0, noBeans4.size());

            Set<Bean<Box<T5>>> noBeans5 = getBeans(syringe, new TypeLiteral<Box<T5>>() {
            });
            assertEquals(0, noBeans5.size());

            Set<Bean<Box<T6>>> beans6 = getBeans(syringe, new TypeLiteral<Box<T6>>() {
            });
            assertEquals(1, beans6.size());
            assertTrue(rawTypeSetMatches(beans6.iterator().next().getTypes(), BOX_TYPES));
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Bar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BarBazImpl.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BarBazSuperFooImpl.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BarSubBazFooImpl.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Baz.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Box.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BoxBarBazFooImpl.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Dao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DaoProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(InjectedBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntegerDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntegerHashMap.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(MapProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ObjectDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Result.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ResultImpl.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SubBar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SubBaz.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SuperBar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SuperBarFooCloneable.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SuperFoo.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(Syringe syringe, Class<T> type, Annotation... qualifiers) {
        return (Set) syringe.getBeanManager().getBeans(type, qualifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(Syringe syringe, TypeLiteral<T> type, Annotation... qualifiers) {
        return (Set) syringe.getBeanManager().getBeans(type.getType(), qualifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(Syringe syringe, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(type, qualifiers);
        Bean<T> bean = (Bean<T>) syringe.getBeanManager().resolve((Set) beans);
        return (T) syringe.getBeanManager().getReference(bean, type,
                syringe.getBeanManager().createCreationalContext(bean));
    }

    private static boolean rawTypeSetMatches(Set<Type> actualTypes, Class<?>... expectedTypes) {
        Set<Class<?>> actualRaw = new HashSet<Class<?>>();
        for (Type type : actualTypes) {
            if (type instanceof Class<?>) {
                actualRaw.add((Class<?>) type);
            } else if (type instanceof ParameterizedType) {
                Type raw = ((ParameterizedType) type).getRawType();
                if (raw instanceof Class<?>) {
                    actualRaw.add((Class<?>) raw);
                }
            }
        }
        Set<Class<?>> expected = new HashSet<Class<?>>();
        Collections.addAll(expected, expectedTypes);
        return actualRaw.equals(expected);
    }
}
