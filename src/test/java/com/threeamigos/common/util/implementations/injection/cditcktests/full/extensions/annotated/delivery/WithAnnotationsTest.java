package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.annotated.delivery;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithAnnotationsTest {

    private Syringe syringe;
    private ProcessAnnotatedTypeObserver processAnnotatedTypeObserver;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Baby.class,
                BeforeBeanDiscoveryObserver.class,
                Desired.class,
                Egg.class,
                Bird.class,
                Pirate.class,
                Falcon.class,
                BatFalcon.class,
                AplomadoFalcon.class,
                Hen.class,
                Hummingbird.class,
                BeeHummingbird.class,
                Chicken.class,
                RubberChicken.class,
                Phoenix.class,
                ProcessAnnotatedTypeObserver.class,
                Raven.class,
                Sparrow.class,
                Jack.class,
                Turkey.class,
                OcellatedTurkey.class,
                Wanted.class,
                MetaAnnotation.class,
                Griffin.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProcessAnnotatedTypeObserver.class.getName());
        syringe.addExtension(BeforeBeanDiscoveryObserver.class.getName());
        syringe.setup();

        BeanManager beanManager = syringe.getBeanManager();
        processAnnotatedTypeObserver = beanManager.getExtension(ProcessAnnotatedTypeObserver.class);
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testDelivery() {
        assertTypeListMatches(processAnnotatedTypeObserver.getProcessedDesiredAndWantedTypes(),
                Bird.class, Hummingbird.class, BeeHummingbird.class,
                Falcon.class, BatFalcon.class, Griffin.class, Hen.class, RubberChicken.class, Turkey.class,
                OcellatedTurkey.class, Jack.class, Sparrow.class, AplomadoFalcon.class);

        assertTypeListMatches(processAnnotatedTypeObserver.getProcessedDesiredTypes(),
                Bird.class, Hummingbird.class, BeeHummingbird.class,
                Turkey.class, OcellatedTurkey.class, Sparrow.class, Jack.class);

        assertFalse(processAnnotatedTypeObserver.getProcessedRequestScopeTypes().contains(AplomadoFalcon.class));
    }

    @Test
    void testDeliveryMetaAnnotation() {
        assertTypeListMatches(processAnnotatedTypeObserver.getProcessedMetaAnnotationTypes(),
                Chicken.class, Hen.class, RubberChicken.class, Hummingbird.class, BeeHummingbird.class);
    }

    private static void assertTypeListMatches(List<? extends Type> types, Type... requiredTypes) {
        assertTrue(types != null);
        List<Type> requiredTypeList = Arrays.asList(requiredTypes);
        assertTrue(requiredTypes.length == types.size() && types.containsAll(requiredTypeList),
                "List " + types + " does not match array " + requiredTypeList);
    }
}
