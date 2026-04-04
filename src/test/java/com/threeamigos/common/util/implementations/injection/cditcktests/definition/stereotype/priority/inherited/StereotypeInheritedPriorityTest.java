package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.priority.inherited;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Stereotype;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StereotypeInheritedPriorityTest {

    @Test
    void testPriorityWasInherited() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                StereotypeWithPriority.class,
                DumbStereotype.class,
                FooAncestor.class,
                Foo.class,
                FooAlternative.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            Instance<FooAlternative> fooInstance = syringe.getBeanManager().createInstance().select(FooAlternative.class);
            assertTrue(fooInstance.isResolvable());
        } finally {
            syringe.shutdown();
        }
    }

    @Stereotype
    @Priority(100)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface StereotypeWithPriority {
    }

    @Stereotype
    @StereotypeWithPriority
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface DumbStereotype {
    }

    @DumbStereotype
    public static class FooAncestor {
        public String ping() {
            return FooAncestor.class.getSimpleName();
        }
    }

    public static class Foo extends FooAncestor {
        @Override
        public String ping() {
            return Foo.class.getSimpleName();
        }
    }

    @Dependent
    @Alternative
    public static class FooAlternative extends Foo {
        @Override
        public String ping() {
            return FooAlternative.class.getSimpleName();
        }
    }
}
