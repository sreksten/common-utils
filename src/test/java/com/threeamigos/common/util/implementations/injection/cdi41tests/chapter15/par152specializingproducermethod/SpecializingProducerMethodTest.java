package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter15.par152specializingproducermethod;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("15.2 - Specializing a Producer Method")
@Execution(ExecutionMode.SAME_THREAD)
public class SpecializingProducerMethodTest {

    @Test
    @DisplayName("15.2 - If producer method X is annotated @Specializes, it must be non-static and directly override another producer method Y")
    public void shouldAllowSpecializingProducerMethodThatDirectlyOverridesProducerMethod() {
        Syringe syringe = newSyringe(
                BaseProducer.class,
                DirectSpecializingProducer.class,
                ProducedValue.class);
        assertDoesNotThrow(syringe::setup);
    }

    @Test
    @DisplayName("15.2 - If specializing producer method is static, container treats it as a definition error")
    public void shouldFailWhenSpecializingProducerMethodIsStatic() {
        Syringe syringe = newSyringeIncludingInvalid(
                StaticBaseProducer.class,
                StaticSpecializingProducer.class,
                ProducedValue.class);
        assertThrows(DefinitionException.class, syringe::setup);
    }

    @Test
    @DisplayName("15.2 - If specializing producer method does not directly override another producer method, container treats it as a definition error")
    public void shouldFailWhenSpecializingProducerMethodDoesNotDirectlyOverrideProducerMethod() {
        Syringe syringe = newSyringeIncludingInvalid(
                BaseProducer.class,
                NonOverridingSpecializingProducer.class,
                ProducedValue.class);
        assertThrows(DefinitionException.class, syringe::setup);
    }

    private Syringe newSyringe(Class<?>... beanClasses) {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), beanClasses);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.exclude(StaticSpecializingProducer.class);
        syringe.exclude(NonOverridingSpecializingProducer.class);
        return syringe;
    }

    private Syringe newSyringeIncludingInvalid(Class<?>... beanClasses) {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), beanClasses);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    @Dependent
    public static class BaseProducer {
        @Produces
        public ProducedValue produce() {
            return new ProducedValue("base");
        }
    }

    @Dependent
    public static class DirectSpecializingProducer extends BaseProducer {
        @Override
        @Produces
        @Specializes
        public ProducedValue produce() {
            return new ProducedValue("specialized");
        }
    }

    @Dependent
    public static class StaticBaseProducer {
        @Produces
        public static ProducedValue produce() {
            return new ProducedValue("base-static");
        }
    }

    @Dependent
    public static class StaticSpecializingProducer extends StaticBaseProducer {
        @Produces
        @Specializes
        public static ProducedValue produce() {
            return new ProducedValue("invalid-static");
        }
    }

    @Dependent
    public static class NonOverridingSpecializingProducer extends BaseProducer {
        @Produces
        @Specializes
        public ProducedValue produceOther() {
            return new ProducedValue("invalid-non-override");
        }
    }

    public static class ProducedValue {
        private final String value;

        public ProducedValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
