package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.equivalence.qualifier;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QualifierEquivalenceTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                ArmorClass.class,
                Level.class,
                Monster.class,
                MonsterQualifier.class,
                Troll.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @SuppressWarnings("serial")
    @Test
    void testAreQualifiersEquivalent() {
        Annotation literal1 = new MonsterQualifier() {
        };
        Annotation literal2 = new MonsterQualifier() {
            @Override
            public String position() {
                return "dungeon";
            }
        };
        Annotation containerProvided = getContainerProvidedQualifier(getUniqueBean(Troll.class, literal1), Monster.class);
        assertTrue(beanManager.areQualifiersEquivalent(literal1, containerProvided));
        assertFalse(beanManager.areQualifiersEquivalent(literal2, containerProvided));
        assertFalse(beanManager.areQualifiersEquivalent(literal1, literal2));
    }

    @SuppressWarnings("serial")
    @Test
    void testGetQualifierHashCode() {
        Annotation literal1 = new MonsterQualifier() {
        };
        Annotation literal2 = new MonsterQualifier() {
            @Override
            public int numberOfVictims() {
                return 7;
            }

            @Override
            public Level level() {
                return Level.B;
            }
        };
        Annotation containerProvided = getContainerProvidedQualifier(getUniqueBean(Troll.class, literal1), Monster.class);
        assertEquals(beanManager.getQualifierHashCode(literal1),
                beanManager.getQualifierHashCode(containerProvided));
        assertNotEquals(beanManager.getQualifierHashCode(literal2),
                beanManager.getQualifierHashCode(containerProvided));
        assertNotEquals(beanManager.getQualifierHashCode(literal1),
                beanManager.getQualifierHashCode(literal2));
    }

    private Annotation getContainerProvidedQualifier(Bean<?> bean, Class<? extends Annotation> qualifierClass) {
        for (Annotation annotation : bean.getQualifiers()) {
            if (annotation.annotationType().equals(qualifierClass)) {
                return annotation;
            }
        }
        fail("Container provided qualifier not found");
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Class<T> beanClass, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }
}
