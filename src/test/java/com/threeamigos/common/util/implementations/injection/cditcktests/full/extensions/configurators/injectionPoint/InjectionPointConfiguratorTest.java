package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.injectionPoint;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InjectionPointConfiguratorTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                AirPlane.class,
                Car.class,
                CarDecorator.class,
                Driving.class,
                Engine.class,
                EngineProducer.class,
                Flying.class,
                Helicopter.class,
                ProcessInjectionPointObserver.class,
                Ship.class,
                Tank.class,
                Vehicle.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProcessInjectionPointObserver.class.getName());
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void changeTypeAndAddQualifier() {
        Bean<AirPlane> airPlaneBean = getUniqueBean(AirPlane.class);
        assertEquals(1, airPlaneBean.getInjectionPoints().size());
        InjectionPoint engineIP = airPlaneBean.getInjectionPoints().iterator().next();
        assertNotNull(engineIP);
        assertEquals(Engine.class, engineIP.getType());
        assertEquals(Collections.singleton(Flying.FlyingLiteral.INSTANCE), engineIP.getQualifiers());
    }

    @Test
    @SuppressWarnings("unchecked")
    void replaceQualifiersAndDelegate() {
        List<Decorator<?>> vehicleDecorators = beanManager.resolveDecorators(
                Collections.<Type>singleton(Car.class),
                Driving.DrivingLiteral.INSTANCE
        );
        assertEquals(1, vehicleDecorators.size());
        Decorator<Car> vehicleDecorator = (Decorator<Car>) vehicleDecorators.get(0);
        assertEquals(1, vehicleDecorator.getInjectionPoints().size());
        InjectionPoint vehicleIp = vehicleDecorator.getInjectionPoints().iterator().next();
        assertTrue(vehicleIp.isDelegate());
        assertEquals(Collections.singleton(Driving.DrivingLiteral.INSTANCE), vehicleIp.getQualifiers());
    }

    @Test
    void readFromFieldAndCheckTransientField() {
        Bean<Ship> shipBean = getUniqueBean(Ship.class);
        assertEquals(1, shipBean.getInjectionPoints().size());
        InjectionPoint engineIP = shipBean.getInjectionPoints().iterator().next();
        assertTrue(engineIP.isTransient());
    }

    @Test
    void configuratorInitializedWithOriginalInjectionPoint() {
        InjectionPoint configuredOne = getUniqueBean(Helicopter.class).getInjectionPoints().iterator().next();
        InjectionPoint originalOne = beanManager.getExtension(ProcessInjectionPointObserver.class).getEngineIP();
        assertEquals(configuredOne.getType(), originalOne.getType());
        assertEquals(configuredOne.getQualifiers(), originalOne.getQualifiers());
        assertEquals(configuredOne.getAnnotated(), originalOne.getAnnotated());
        assertEquals(configuredOne.getBean(), originalOne.getBean());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Class<T> beanClass, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }
}
