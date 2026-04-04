package com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.instance;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.instance.builtininstancedependentobjecttest.testinstancedependentobject.Foo;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.instance.builtininstancedependentobjecttest.testinstancedependentobject.GoodEvent;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinInstanceDependentObjectTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.instance.builtininstancedependentobjecttest.testinstancedependentobject";

    @Test
    void testInstanceDependentObject() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            Foo.reset();
            BeanManager beanManager = syringe.getBeanManager();
            @SuppressWarnings("unchecked")
            Event<GoodEvent> event = (Event<GoodEvent>) beanManager.getEvent().select(GoodEvent.class);
            event.fire(new GoodEvent());
            assertTrue(Foo.created);
            assertTrue(Foo.destroyed);
        } finally {
            syringe.shutdown();
        }
    }
}
