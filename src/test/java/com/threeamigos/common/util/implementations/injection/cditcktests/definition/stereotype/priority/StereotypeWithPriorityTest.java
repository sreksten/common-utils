package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.priority;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StereotypeWithPriorityTest {

    @Test
    void testStereotypeWithPriority() {
        Syringe syringe = newSyringe();
        try {
            Probe probe = syringe.getBeanManager().createInstance().select(Probe.class).get();
            assertEquals(FooAlternative.class.getSimpleName(), probe.foo().ping());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStereotypeWithAlternativeAndPriority() {
        Syringe syringe = newSyringe();
        try {
            Probe probe = syringe.getBeanManager().createInstance().select(Probe.class).get();
            assertEquals(BarExtended.class.getSimpleName(), probe.bar().ping());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanPriorityFromStereotypeOverridesOtherAlternative() {
        Syringe syringe = newSyringe();
        try {
            Probe probe = syringe.getBeanManager().createInstance().select(Probe.class).get();
            assertEquals(BazAlternative2.class.getSimpleName(), probe.baz().ping());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanOverridesPriorityFromStereotype() {
        Syringe syringe = newSyringe();
        try {
            Probe probe = syringe.getBeanManager().createInstance().select(Probe.class).get();
            assertEquals(CharlieAlternative.class.getSimpleName(), probe.charlie().ping());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                Probe.class,
                PriorityStereotype.class,
                AlternativePriorityStereotype.class,
                Foo.class,
                FooAlternative.class,
                Bar.class,
                BarExtended.class,
                Baz.class,
                BazAlternative.class,
                BazAlternative2.class,
                Charlie.class,
                CharlieAlternative.class,
                CharlieAltStereotype.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    @Dependent
    public static class Probe {
        @Inject
        private Foo foo;

        @Inject
        private Bar bar;

        @Inject
        private Baz baz;

        @Inject
        private Charlie charlie;

        public Foo foo() {
            return foo;
        }

        public Bar bar() {
            return bar;
        }

        public Baz baz() {
            return baz;
        }

        public Charlie charlie() {
            return charlie;
        }
    }

    @Stereotype
    @Priority(100)
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface PriorityStereotype {
    }

    @Stereotype
    @Alternative
    @PriorityStereotype
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface AlternativePriorityStereotype {
    }

    @ApplicationScoped
    public static class Foo {
        public String ping() {
            return Foo.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @Alternative
    @PriorityStereotype
    public static class FooAlternative extends Foo {
        @Override
        public String ping() {
            return FooAlternative.class.getSimpleName();
        }
    }

    @ApplicationScoped
    public static class Bar {
        public String ping() {
            return Bar.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @AlternativePriorityStereotype
    public static class BarExtended extends Bar {
        @Override
        public String ping() {
            return BarExtended.class.getSimpleName();
        }
    }

    public static class Baz {
        public String ping() {
            return Baz.class.getSimpleName();
        }
    }

    @Dependent
    @Alternative
    @Priority(1)
    public static class BazAlternative extends Baz {
        @Override
        public String ping() {
            return BazAlternative.class.getSimpleName();
        }
    }

    @Dependent
    @Alternative
    @PriorityStereotype
    public static class BazAlternative2 extends Baz {
        @Override
        public String ping() {
            return BazAlternative2.class.getSimpleName();
        }
    }

    @Dependent
    public static class Charlie {
        public String ping() {
            return Charlie.class.getSimpleName();
        }
    }

    @Dependent
    @Alternative
    @Priority(50)
    public static class CharlieAlternative extends Charlie {
        @Override
        public String ping() {
            return CharlieAlternative.class.getSimpleName();
        }
    }

    @Dependent
    @Alternative
    @Priority(1)
    @PriorityStereotype
    public static class CharlieAltStereotype extends Charlie {
        @Override
        public String ping() {
            return CharlieAltStereotype.class.getSimpleName();
        }
    }
}
