package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.dependency.builtin;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BuiltinBeanPassivationDependencyTest {

    @Test
    void testInstance() throws IOException, ClassNotFoundException {
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            String sessionId = "builtin-worker-" + UUID.randomUUID();
            beanManager.getContextManager().activateSession(sessionId);
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                Worker worker = getContextualReference(beanManager, Worker.class);
                assertNotNull(worker);
                assertNotNull(worker.getInstance());
                Hammer hammer = worker.getInstance().get();
                assertNotNull(hammer);

                String workerId = worker.getId();
                String hammerId = hammer.getId();

                Worker workerCopy = deserialize(serialize(worker), Worker.class);

                assertNotNull(workerCopy);
                assertNotNull(workerCopy.getInstance());
                assertEquals(workerId, workerCopy.getId());
                assertEquals(hammerId, workerCopy.getInstance().get().getId());
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
                beanManager.getContextManager().invalidateSession(sessionId);
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanManager() throws IOException, ClassNotFoundException {
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            String sessionId = "builtin-boss-" + UUID.randomUUID();
            beanManager.getContextManager().activateSession(sessionId);
            try {
                Boss boss = getContextualReference(beanManager, Boss.class);
                assertNotNull(boss);
                assertNotNull(boss.getBeanManager());

                String bossId = boss.getId();
                Boss bossCopy = deserialize(serialize(boss), Boss.class);

                assertNotNull(bossCopy);
                assertNotNull(bossCopy.getBeanManager());
                assertEquals(bossId, bossCopy.getId());
                assertEquals(1, bossCopy.getBeanManager().getBeans(Boss.class).size());
            } finally {
                beanManager.getContextManager().invalidateSession(sessionId);
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionPoint() throws IOException, ClassNotFoundException {
        Syringe syringe = newSyringe();
        try {
            InspectorAssistant inspectorAssistant = getContextualReference(syringe.getBeanManager(), InspectorAssistant.class);
            Inspector inspector = inspectorAssistant.getInspector();
            assertNotNull(inspector);
            assertNotNull(inspector.getInjectionPoint());
            String inspectorId = inspector.getId();

            Inspector inspectorCopy = deserialize(serialize(inspector), Inspector.class);

            assertNotNull(inspectorCopy);
            assertNotNull(inspectorCopy.getInjectionPoint());
            assertEquals(inspectorId, inspectorCopy.getId());
            assertInjectionPointEquals(inspector.getInjectionPoint(), inspectorCopy.getInjectionPoint());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Boss.class,
                Hammer.class,
                Inspector.class,
                InspectorAssistant.class,
                Worker.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private void assertInjectionPointEquals(InjectionPoint expected, InjectionPoint actual) {
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getQualifiers(), actual.getQualifiers());
        assertEquals(expected.getBean(), actual.getBean());
        assertEquals(expected.getMember(), actual.getMember());
        assertEquals(expected.isDelegate(), actual.isDelegate());
        assertEquals(expected.isTransient(), actual.isTransient());
        assertAnnotatedEquals(expected.getAnnotated(), actual.getAnnotated());
    }

    private void assertAnnotatedEquals(Annotated expected, Annotated actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(expected.getBaseType(), actual.getBaseType());
        assertEquals(expected.getAnnotations(), actual.getAnnotations());

        if (expected instanceof AnnotatedMember && actual instanceof AnnotatedMember) {
            AnnotatedMember<?> expectedMember = (AnnotatedMember<?>) expected;
            AnnotatedMember<?> actualMember = (AnnotatedMember<?>) actual;
            assertEquals(expectedMember.getJavaMember(), actualMember.getJavaMember());
            assertEquals(expectedMember.isStatic(), actualMember.isStatic());
        }
        if (expected instanceof AnnotatedParameter && actual instanceof AnnotatedParameter) {
            AnnotatedParameter<?> expectedParameter = (AnnotatedParameter<?>) expected;
            AnnotatedParameter<?> actualParameter = (AnnotatedParameter<?>) actual;
            assertEquals(expectedParameter.getPosition(), actualParameter.getPosition());
            assertAnnotatedEquals(expectedParameter.getDeclaringCallable(), actualParameter.getDeclaringCallable());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> beanType) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return (T) beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean));
    }

    private byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        try {
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            return outputStream.toByteArray();
        } finally {
            objectOutputStream.close();
        }
    }

    private <T> T deserialize(byte[] serialized, Class<T> type) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(serialized));
        try {
            return type.cast(objectInputStream.readObject());
        } finally {
            objectInputStream.close();
        }
    }
}
