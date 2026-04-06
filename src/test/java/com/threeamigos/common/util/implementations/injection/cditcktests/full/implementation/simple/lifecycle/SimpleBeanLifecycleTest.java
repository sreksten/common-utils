package com.threeamigos.common.util.implementations.injection.cditcktests.full.implementation.simple.lifecycle;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleBeanLifecycleTest {

    private static final Annotation TAME_LITERAL = new Tame.Literal();

    @Test
    void testSpecializedBeanExtendsManagedBean() {
        Syringe syringe = newSyringe();
        try {
            assertNotNull(MountainLion.class.getAnnotation(Specializes.class));

            BeanManager beanManager = syringe.getBeanManager();
            @SuppressWarnings({"unchecked", "rawtypes"})
            Set<Bean<Lion>> lionBeans = (Set) beanManager.getBeans(Lion.class, TAME_LITERAL);

            Bean<Lion> bean = null;
            Bean<Lion> specializedBean = null;
            for (Bean<Lion> lionBean : lionBeans) {
                if (lionBean.getBeanClass().equals(Lion.class)) {
                    bean = lionBean;
                } else if (lionBean.getBeanClass().equals(MountainLion.class)) {
                    specializedBean = lionBean;
                }
            }

            assertNull(bean);
            assertNotNull(specializedBean);
            assertEquals(Lion.class, specializedBean.getBeanClass().getSuperclass());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSerializeRequestScoped() throws Exception {
        Syringe syringe = newSyringe();
        try {
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                Cod codInstance = getContextualReference(syringe.getBeanManager(), Cod.class);
                codInstance = deserialize(serialize(codInstance), Cod.class);
                assertTrue(isProxy(codInstance), codInstance.getClass().getName());
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
    void testSerializeSessionScoped() throws Exception {
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateSession("simple-bean-lifecycle-session");
            try {
                Bream instance = getContextualReference(beanManager, Bream.class);
                instance = deserialize(serialize(instance), Bream.class);
                assertTrue(isProxy(instance), instance.getClass().getName());
            } finally {
                beanManager.getContextManager().invalidateSession("simple-bean-lifecycle-session");
            }
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bream.class,
                Cod.class,
                Lion.class,
                MountainLion.class,
                Tame.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type) {
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beanManager.getBeans(type));
        return (T) beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
    }

    private static byte[] serialize(Object instance) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = new ObjectOutputStream(output);
        objectOutput.writeObject(instance);
        objectOutput.flush();
        return output.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(byte[] bytes, Class<T> type) throws Exception {
        ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object deserialized = objectInput.readObject();
        return (T) type.cast(deserialized);
    }

    private static boolean isProxy(Object instance) {
        if (instance == null) {
            return false;
        }
        String name = instance.getClass().getName();
        return name.contains("$$")
                || name.contains("$Proxy")
                || name.contains("$ByteBuddy$")
                || name.startsWith("com.sun.proxy.$Proxy");
    }
}
