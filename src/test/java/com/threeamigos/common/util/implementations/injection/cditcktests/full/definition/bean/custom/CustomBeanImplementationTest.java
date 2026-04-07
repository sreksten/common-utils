package com.threeamigos.common.util.implementations.injection.cditcktests.full.definition.bean.custom;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomBeanImplementationTest {

    private Syringe syringe;
    private BeanManagerImpl beanManager;

    @BeforeAll
    void setUp() {
        CustomInjectionPoint.getMembersClasses().clear();

        syringe = new Syringe(
                new InMemoryMessageHandler(),
                AfterBeanDiscoveryObserver.class,
                House.class,
                CustomInjectionPoint.class,
                Bar.class,
                PassivationCapableBean.class,
                SomeBean.class,
                AlternativeSomeBean.class,
                Foo.class,
                FooBean.class,
                IntegerBean.class,
                Passivable.class,
                PassivableLiteral.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterBeanDiscoveryObserver.class.getName());
        syringe.setup();

        beanManager = (BeanManagerImpl) syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
        CustomInjectionPoint.getMembersClasses().clear();
    }

    @Test
    void testGetBeanClassCalled() {
        assertTrue(AfterBeanDiscoveryObserver.integerBean.isGetBeanClassCalled());
    }

    @Test
    void testGetStereotypesCalled() {
        assertTrue(AfterBeanDiscoveryObserver.integerBean.isGetStereotypesCalled());
    }

    @Test
    void testIsPolicyCalled() {
        assertTrue(AfterBeanDiscoveryObserver.integerBean.isAlternativeCalled());
    }

    @Test
    void testCustomBeanNotAutomaticallySelected() {
        Instance<SomeBean> instance = beanManager.createInstance().select(SomeBean.class);
        assertTrue(instance.isResolvable());
        assertEquals(SomeBean.class.getSimpleName(), instance.get().whoAmI());
    }

    @Test
    void testGetTypesCalled() {
        assertTrue(AfterBeanDiscoveryObserver.integerBean.isGetTypesCalled());
    }

    @Test
    void testGetBindingsCalled() {
        assertTrue(AfterBeanDiscoveryObserver.integerBean.isGetQualifiersCalled());
    }

    @Test
    void testGetInjectionPointsCalled() {
        Bar bar = getContextualReference(beanManager, Bar.class);

        assertTrue(AfterBeanDiscoveryObserver.integerBean.isGetInjectionPointsCalled());
        assertTrue(FooBean.barInjectionPoint.isTransientCalled());
        assertEquals(1, bar.getOne());
    }

    @Test
    void testGetNameCalled() {
        assertTrue(AfterBeanDiscoveryObserver.integerBean.isGetNameCalled());
    }

    @Test
    void testGetScopeTypeCalled() {
        assertTrue(AfterBeanDiscoveryObserver.integerBean.isGetScopeCalled());
    }

    @Test
    void testCustomBeanIsPassivationCapable() throws IOException, ClassNotFoundException {
        beanManager.getContextManager().activateSession("custom-bean-passivation");
        try {
            Foo customFoo = getContextualReference(beanManager, Foo.class, new PassivableLiteral());
            Foo customFooDeserialized = deserialize(serialize(customFoo), Foo.class);
            assertEquals(customFoo.getId(), customFooDeserialized.getId());
        } finally {
            beanManager.getContextManager().invalidateSession("custom-bean-passivation");
        }
    }

    @Test
    void testCustomBeanIsPassivationCapableDependency() throws IOException, ClassNotFoundException {
        beanManager.getContextManager().activateSession("custom-bean-passivation-dependency");
        try {
            PassivationCapableBean passivationCapableBean = getContextualReference(beanManager, PassivationCapableBean.class);
            PassivationCapableBean deserialized = deserialize(serialize(passivationCapableBean), PassivationCapableBean.class);
            assertEquals(passivationCapableBean.getFoo().getId(), deserialized.getFoo().getId());
        } finally {
            beanManager.getContextManager().invalidateSession("custom-bean-passivation-dependency");
        }
    }

    @Test
    void testInjectionPointGetMemberIsUsedToDetermineTheClassThatDeclaresAnInjectionPoint() {
        CustomInjectionPoint.getMembersClasses().clear();

        beanManager.getContextManager().activateSession("custom-bean-ip-member");
        try {
            Foo foo = getContextualReference(beanManager, Foo.class, new PassivableLiteral());
            foo.getId();

            assertEquals(2, CustomInjectionPoint.getMembersClasses().size());
            assertTrue(CustomInjectionPoint.getMembersClasses().contains(Bar.class));
            assertTrue(CustomInjectionPoint.getMembersClasses().contains(Integer.class));
        } finally {
            beanManager.getContextManager().invalidateSession("custom-bean-ip-member");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beanManager.getBeans(type, qualifiers));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
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

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(byte[] bytes, Class<T> type) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            return (T) inputStream.readObject();
        } finally {
            inputStream.close();
        }
    }
}
