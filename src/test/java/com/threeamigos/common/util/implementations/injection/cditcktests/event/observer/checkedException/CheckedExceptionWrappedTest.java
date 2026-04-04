package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.checkedException;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObserverException;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckedExceptionWrappedTest {

    @Test
    void testNonTransactionalObserverThrowsCheckedExceptionIsWrappedAndRethrown() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), TeaCupPomeranian.class, Tame.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            assertThrows(ObserverException.class,
                    () -> syringe.getBeanManager().getEvent().select(Integer.class).fire(1));
        } finally {
            syringe.shutdown();
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Tame {
    }

    @Dependent
    static class TeaCupPomeranian {

        static class TooSmallException extends Exception {
            private static final long serialVersionUID = 1L;
        }

        void observeAnotherSimpleEvent(@Observes Integer someEvent) throws TooSmallException {
            throw new TooSmallException();
        }
    }
}
