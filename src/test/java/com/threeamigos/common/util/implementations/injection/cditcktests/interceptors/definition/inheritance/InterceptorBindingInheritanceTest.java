package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.definition.inheritance;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.util.AnnotationLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InterceptorBindingInheritanceTest {

    private static final Annotation EUROPEAN = new AnnotationLiteral<European>() {
        private static final long serialVersionUID = 1L;
    };
    private static final Annotation CULINARY = new AnnotationLiteral<Culinary>() {
        private static final long serialVersionUID = 1L;
    };

    private final String squirrel = SquirrelInterceptor.class.getName();
    private final String woodpecker = WoodpeckerInterceptor.class.getName();

    @Test
    void testInterceptorBindingDirectlyInheritedFromManagedBean() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();
            Larch larch = syringe.getBeanManager().createInstance().select(Larch.class).get();
            Plant.clearInspections();
            larch.pong();
            assertTrue(Plant.inspectedBy(squirrel));
            assertFalse(Plant.inspectedBy(woodpecker));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterceptorBindingIndirectlyInheritedFromManagedBean() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();
            Larch europeanLarch = syringe.getBeanManager().createInstance().select(Larch.class, EUROPEAN).get();
            Plant.clearInspections();
            europeanLarch.pong();
            assertTrue(europeanLarch instanceof EuropeanLarch);
            assertTrue(Plant.inspectedBy(squirrel));
            assertFalse(Plant.inspectedBy(woodpecker));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMethodInterceptorBindingDirectlyInheritedFromManagedBean() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();
            Herb herb = syringe.getBeanManager().createInstance().select(Herb.class).get();
            Plant.clearInspections();
            herb.pong();
            assertTrue(Plant.inspectedBy(squirrel));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMethodInterceptorBindingIndirectlyInheritedFromManagedBean() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();
            Herb thyme = syringe.getBeanManager().createInstance().select(Herb.class, CULINARY).get();
            Plant.clearInspections();
            thyme.pong();
            assertTrue(thyme instanceof Thyme);
            assertTrue(Plant.inspectedBy(squirrel));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMethodInterceptorBindingDirectlyNotInheritedFromManagedBean() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();
            Shrub shrub = syringe.getBeanManager().createInstance().select(Shrub.class).get();
            Plant.clearInspections();
            shrub.pong();
            assertFalse(Plant.inspectedBy(squirrel));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMethodInterceptorBindingIndirectlyNotInheritedFromManagedBean() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();
            Shrub rosehip = syringe.getBeanManager().createInstance().select(Shrub.class, CULINARY).get();
            Plant.clearInspections();
            rosehip.pong();
            assertTrue(rosehip instanceof Rosehip);
            assertFalse(Plant.inspectedBy(squirrel));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();

        syringe.addDiscoveredClass(GuardedBySquirrel.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(GuardedByWoodpecker.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SquirrelInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(WoodpeckerInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tree.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Larch.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(European.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(EuropeanLarch.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PongPlant.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Herb.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Culinary.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Thyme.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Shrub.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Rosehip.class, BeanArchiveMode.EXPLICIT);

        return syringe;
    }
}
