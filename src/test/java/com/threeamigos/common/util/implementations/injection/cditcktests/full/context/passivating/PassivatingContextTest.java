package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PassivatingContextTest {

    @Test
    void testManagedBeanWithSerializableImplementationClassOK() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Jyvaskyla>> beans = getBeans(syringe.getBeanManager(), Jyvaskyla.class);
            assertFalse(beans.isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testManagedBeanWithSerializableInterceptorClassOK() throws IOException, ClassNotFoundException {
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            Set<Bean<Kokkola>> beans = getBeans(beanManager, Kokkola.class);
            assertFalse(beans.isEmpty());
            Bean<Kokkola> bean = beans.iterator().next();
            beanManager.getContextManager().activateSession("kokkola-session");
            try {
                Kokkola instance = (Kokkola) beanManager.getReference(bean, Kokkola.class,
                        beanManager.createCreationalContext(bean));
                assertEquals(1, instance.ping());
                Kokkola instance2 = deserialize(serialize(instance), Kokkola.class);
                assertEquals(2, instance2.ping());
            } finally {
                beanManager.getContextManager().invalidateSession("kokkola-session");
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testManagedBeanWithSerializableDecoratorOK() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<City>> beans = getBeans(syringe.getBeanManager(), City.class);
            assertFalse(beans.isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPassivationCapableProducerMethodIsOK() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Record>> beans = getBeans(syringe.getBeanManager(), Record.class);
            assertFalse(beans.isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPassivationCapableProducerFieldIsOK() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Wheat>> beans = getBeans(syringe.getBeanManager(), Wheat.class);
            assertFalse(beans.isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionOfDependentPrimitiveProductIntoNormalBean() {
        Syringe syringe = newSyringe();
        try {
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                getContextualReference(syringe.getBeanManager(), NumberConsumer.class).ping();
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionOfDependentSerializableProductIntoNormalBean() {
        Syringe syringe = newSyringe();
        try {
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                getContextualReference(syringe.getBeanManager(), SerializableCityConsumer.class).ping();
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPassivationOccurs() {
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateSession("kajaani-session");
            try {
                Kajaani instance = getContextualReference(beanManager, Kajaani.class);
                instance.setTheNumber(100);
            } finally {
                beanManager.getContextManager().deactivateSession();
            }

            beanManager.getContextManager().activateSession("kajaani-session");
            try {
                Kajaani instance = getContextualReference(beanManager, Kajaani.class);
                assertEquals(100, instance.getTheNumber());
            } finally {
                beanManager.getContextManager().invalidateSession("kajaani-session");
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanWithNonSerializableImplementationInjectedIntoTransientFieldOK() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Joensuu>> beans = getBeans(syringe.getBeanManager(), Joensuu.class);
            assertFalse(beans.isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPassivatingScopeProducerMethodReturnsUnserializableObjectNotOk() {
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateSession("television-session");
            try {
                assertThrows(IllegalProductException.class,
                        () -> getContextualReference(beanManager, Television.class).turnOn());
            } finally {
                beanManager.getContextManager().invalidateSession("television-session");
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonSerializableProducerFieldDeclaredPassivatingThrowsIllegalProductException() {
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateSession("helsinki-session");
            try {
                assertThrows(IllegalProductException.class,
                        () -> getContextualReference(beanManager, HelsinkiNonSerializable.class).ping());
            } finally {
                beanManager.getContextManager().invalidateSession("helsinki-session");
            }
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Big.class,
                City.class,
                CityBinding.class,
                CityProducer.class,
                CityProducer2.class,
                Generator.class,
                HelsinkiNonSerializable.class,
                Hyvinkaa.class,
                Joensuu.class,
                Jyvaskyla.class,
                Kajaani.class,
                Kokkola.class,
                KokkolaInterceptor.class,
                NumberConsumer.class,
                ProducedInteger.class,
                Record.class,
                RecordProducer.class,
                Salo_Broken.class,
                SerializableCity.class,
                SerializableCityConsumer.class,
                Sleeping.class,
                Sysma.class,
                Television.class,
                TelevisionProducer.class,
                Violation.class,
                Wheat.class,
                WheatProducer.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Set<Bean<T>> getBeans(BeanManager beanManager, Class<T> type) {
        return (Set) beanManager.getBeans(type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type) {
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(byte[] bytes, Class<T> type) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            Object value = inputStream.readObject();
            return (T) value;
        } finally {
            inputStream.close();
        }
    }

    private static byte[] serialize(Object value) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        try {
            objectOutputStream.writeObject(value);
            objectOutputStream.flush();
            return outputStream.toByteArray();
        } finally {
            objectOutputStream.close();
        }
    }
}
