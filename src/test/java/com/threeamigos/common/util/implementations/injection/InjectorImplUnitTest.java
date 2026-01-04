package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.annotations.injection.Any;
import com.threeamigos.common.util.annotations.injection.Inject;
import com.threeamigos.common.util.annotations.injection.Singleton;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleannotatedconcreteclasses.MultipleAnnotatedConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleannotatedconcreteclasses.MultipleAnnotatedConcreteClassesAlternative1;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleannotatedconcreteclasses.MultipleAnnotatedConcreteClassesAlternative2;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleannotatedconcreteclasses.MultipleAnnotatedConcreteClassesStandardClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleannotatedconcreteclasses.subpackage.MultipleAnnotatedConcreteClassesAlternative3;
import com.threeamigos.common.util.implementations.injection.abstractclasses.noconcreteclasses.NoConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationConcreteClass;
import com.threeamigos.common.util.interfaces.injection.Injector;
import com.threeamigos.common.util.interfaces.injection.Instance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InjectorImpl unit tests")
class InjectorImplUnitTest {

    private static final String TEST_PACKAGE_NAME = "com.threeamigos";

    @Nested
    @DisplayName("Search Constructor tests")
    class SearchConstructorTests {

        @Nested
        @DisplayName("Tests for valid classes")
        class ValidClasses {

            @Test
            @DisplayName("Interfaces")
            void shouldNotThrowExceptionIfInjectingAnInterface() {
                // Given
                Injector sut = new InjectorImpl();
                // When
                Class<?> clazz = TestInterface.class;
                // Then
                assertDoesNotThrow(() -> sut.inject(clazz));
            }

            @Test
            @DisplayName("Abstract classes")
            void shouldNotThrowExceptionIfInjectingAnAbstractClass() {
                // Given
                Injector sut = new InjectorImpl();
                // When
                Class<?> clazz = AbstractClass.class;
                // Then
                assertDoesNotThrow(() -> sut.inject(clazz));
            }

            @Test
            @DisplayName("Concrete classes")
            void shouldNotThrowExceptionIfInjectingAConcreteClass() {
                // Given
                Injector sut = new InjectorImpl();
                // When
                Class<?> clazz = ConcreteClass.class;
                // Then
                assertDoesNotThrow(() -> sut.inject(clazz));
            }
        }

        @Nested
        @DisplayName("Tests for invalid classes")
        class InvalidClasses {

            @Test
            @DisplayName("Enums")
            void shouldThrowExceptionIfInjectingAnEnum() {
                assertThrows(IllegalArgumentException.class, () -> {
                    new InjectorImpl().inject(TestEnum.class);
                });
            }

            @Test
            @DisplayName("Primitives")
            void shouldThrowExceptionIfInjectingAPrimitive() {
                assertThrows(IllegalArgumentException.class, () -> {
                    new InjectorImpl().inject(int.class);
                });
            }

            @Test
            @DisplayName("Synthetic classes")
            void shouldThrowExceptionIfInjectingASyntheticClass() {
                // Given
                final Class<?> syntheticClass = ((Runnable)() -> {}).getClass();
                // Then
                assertTrue(syntheticClass.isSynthetic());
                assertThrows(IllegalArgumentException.class, () -> {
                    new InjectorImpl().inject(syntheticClass);
                });
            }

            @Test
            @DisplayName("Local classes")
            void shouldThrowExceptionIfInjectingALocalClass() {
                // Given
                class MyLocalClass {};
                // Then
                assertThrows(IllegalArgumentException.class, () -> {
                    new InjectorImpl().inject(MyLocalClass.class);
                });
            }

            @Test
            @DisplayName("Anonymous classes")
            void shouldThrowExceptionIfInjectingAnAnonymousClass() {
                // Given
                Runnable anonymousRunnable = new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Hello from anonymous class!");
                    }
                };
                // Then
                assertThrows(IllegalArgumentException.class, () -> {
                    new InjectorImpl().inject(anonymousRunnable.getClass());
                });
            }

            @Test
            @DisplayName("Non-static inner classes")
            void shouldThrowExceptionIfInjectingNonStaticInnerClass() {
                assertThrows(IllegalArgumentException.class, () -> {
                    new InjectorImpl().inject(NonStaticInnerClass.class);
                });
            }
        }

        @Test
        @DisplayName("Should return constructor annotated with @Inject")
        void shouldReturnConstructorAnnotatedWithInject() throws NoSuchMethodException {
            // Given
            class TestClass {
                @Inject
                public TestClass() { }
            }
            InjectorImpl sut = new InjectorImpl();
            // When
            Constructor<TestClass> constructor = sut.getConstructor(TestClass.class);
            // Then
            assertNotNull(constructor);
        }

        @Test
        @DisplayName("Should throw NoSuchMethodException if no constructor annotated with @Inject is found and no-arguments constructor is not available")
        void shouldThrow() {
            // Given
            class TestClass {
                TestClass(int i) { }
            }
            InjectorImpl sut = new InjectorImpl();
            // When, Then
            assertThrows(NoSuchMethodException.class, () -> sut.getConstructor(TestClass.class));
        }

        @Test
        @DisplayName("Should return constructor marked with @Inject when available, preferring it to the no-arguments constructor")
        void shouldReturnConstructorAnnotatedWithInjectPreferringItToTheNoArgsConstructor() throws NoSuchMethodException {
            // Given
            class TestClass {
                @Inject
                public TestClass(java.util.Date date) { }
                public TestClass() { }
            }
            InjectorImpl sut = new InjectorImpl();
            Constructor<TestClass> constructor = sut.getConstructor(TestClass.class);
            // When
            int numberOfArguments = constructor.getParameterCount();
            // Then
            // Being TestClass a non-static inner class, the constructor has the class itself as the first argument
            assertEquals(2, numberOfArguments);
        }

        @Test
        @DisplayName("Should throw exception if more than one constructor is annotated with @Inject")
        void shouldThrowExceptionWhenMoreThanOneConstructorIsAnnotatedWithInject() {
            // Given
            class TestClass {
                @Inject
                public TestClass(java.util.Date date) { }
                @Inject
                public TestClass(String s) { }
            }
            InjectorImpl sut = new InjectorImpl();
            // When, Then
            assertThrows(IllegalStateException.class, () -> sut.getConstructor(TestClass.class));
        }

        @Test
        @DisplayName("Should throw exception if any parameter is not an interface, an abstract or a concrete class")
        void shouldThrowExceptionIfInvalidParameter() {
            // Given
            class TestClass {
                @Inject
                public TestClass(int i) { }
            }
            InjectorImpl sut = new InjectorImpl();
            // When, Then
            assertThrows(IllegalArgumentException.class, () -> sut.inject(TestClass.class));
        }
    }

    @Nested
    @DisplayName("Instantiation tests")
    class InstantiationTests {

        @Test
        @DisplayName("Should instantiate an object with annotated no-args constructor")
        void shouldInstantiateAnObjectWithAnnotatedNoArgsConstructor() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl();
            // When
            TestClassWithAnnotatedNoArgsConstructor instance = sut.inject(TestClassWithAnnotatedNoArgsConstructor.class);
            // Then
            assertNotNull(instance);
        }

        @Test
        @DisplayName("Should instantiate an object with a no-args constructor if no other annotated constructors are present")
        void shouldInstantiateAnObjectWithNoArgsConstructorIfNoOtherAnnotatedConstructorsArePResent() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl();
            // When
            TestClassWithAnnotatedNoArgsConstructor instance = sut.inject(TestClassWithAnnotatedNoArgsConstructor.class);
            // Then
            assertNotNull(instance);
        }

        @Test
        @DisplayName("Should instantiate an object with an annotated constructor")
        void shouldInstantiateAnObjectWithAnAnnotatedConstructor() throws Exception {
            // Given
            InjectorImpl sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            TestClassWithAnnotatedConstructor instance = sut.inject(TestClassWithAnnotatedConstructor.class);
            // Then
            assertNotNull(instance);
        }

        @Test
        @DisplayName("Should instantiate a singleton class only once")
        void shouldInstantiateASingletonClass() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            Class<?> singletonClass = SingletonClass.class;
            SingletonClass instance1 = sut.inject(SingletonClass.class);
            // When
            SingletonClass instance2 = sut.inject(SingletonClass.class);
            assertSame(instance1, instance2);
        }

        @Test
        @DisplayName("Should instantiate a non-singleton class as much as needed")
        void shouldInstantiateANonSingletonClassAsMuchAsNeeded() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            TestClassWithAnnotatedNoArgsConstructor instance1 = sut.inject(TestClassWithAnnotatedNoArgsConstructor.class);
            TestClassWithAnnotatedNoArgsConstructor instance2 = sut.inject(TestClassWithAnnotatedNoArgsConstructor.class);
            // Then
            assertNotSame(instance1, instance2);
        }

        @Test
        @DisplayName("Should instantiate an object with private constructor")
        void shouldInstantiateObjectWithPrivateConstructor() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            // When
            ClassWithPrivateConstructor instance = sut.inject(ClassWithPrivateConstructor.class);
            // Then
            assertNotNull(instance);

        }
    }

    @Nested
    @DisplayName("Instance Wrapper Tests")
    class InstanceWrapperTests {

        @Test
        @DisplayName("Instance with single implementation")
        void instanceWithSingleImplementation() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithInstance classWithInstance = sut.inject(ClassWithInstance.class);
            // When
            Instance<TestInterface> instance = classWithInstance.getTestInterfaceInstance();
            TestInterface resolvedInstance = instance.get();
            // Then
            assertNotNull(resolvedInstance);
            assertInstanceOf(TestInterface.class, resolvedInstance);
        }

        @Test
        @DisplayName("@Any Instance with single implementation")
        void anyInstanceWithSingleImplementation() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithAnyInstanceAndSingleImplementation classWithAnyInstance = sut.inject(ClassWithAnyInstanceAndSingleImplementation.class);
            // When
            Instance<SingleImplementationAbstractClass> instance = classWithAnyInstance.getInstance();
            // Then
            assertNotNull(instance);
            List<Class<? extends SingleImplementationAbstractClass>> implementations = new ArrayList<>();
            for (SingleImplementationAbstractClass multipleAnnotatedConcreteClassesAbstractClass : instance) {
                implementations.add(multipleAnnotatedConcreteClassesAbstractClass.getClass());
            }
            assertEquals(1, implementations.size());
            assertEquals(SingleImplementationConcreteClass.class, implementations.get(0));
        }

        @Test
        @DisplayName("@Any Instance and multiple implementations")
        void anyInstanceAndMultipleImplementations() throws Exception {
            // Given
            Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
            ClassWithAnyInstanceAndMultipleImplementations classWithAnyInstanceAndMultipleImplementations = sut.inject(ClassWithAnyInstanceAndMultipleImplementations.class);
            List<Class<? extends MultipleAnnotatedConcreteClassesAbstractClass>> expectedImplementations = new ArrayList<>();
            expectedImplementations.add(MultipleAnnotatedConcreteClassesStandardClass.class);
            expectedImplementations.add(MultipleAnnotatedConcreteClassesAlternative1.class);
            expectedImplementations.add(MultipleAnnotatedConcreteClassesAlternative2.class);
            expectedImplementations.add(MultipleAnnotatedConcreteClassesAlternative3.class);
            // When
            Instance<MultipleAnnotatedConcreteClassesAbstractClass> instance = classWithAnyInstanceAndMultipleImplementations.getInstance();
            // Then
            assertNotNull(instance);
            List<Class<? extends MultipleAnnotatedConcreteClassesAbstractClass>> implementations = new ArrayList<>();
            for (MultipleAnnotatedConcreteClassesAbstractClass multipleAnnotatedConcreteClassesAbstractClass : instance) {
                implementations.add(multipleAnnotatedConcreteClassesAbstractClass.getClass());
            }
            expectedImplementations.sort(Comparator.comparing(Class::getName));
            implementations.sort(Comparator.comparing(Class::getName));
            assertEquals(expectedImplementations, implementations);
        }

        @Nested
        @DisplayName("Instance with no concrete implementations")
        class InstancesWithNoConcreteImplementations {

            @Nested
            @DisplayName("@Any Instance")
            class AnyInstance {

                @Test
                @DisplayName("Instance.get() should fail")
                void instanceGetShouldFail() throws Exception {
                    // Given
                    Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                    ClassWithAnyInstanceButNoImplementation instance = sut.inject(ClassWithAnyInstanceButNoImplementation.class);
                    // Then
                    assertThrows(RuntimeException.class, () -> instance.getInstance().get());
                }

                @Test
                @DisplayName("Instance.iterator().hasNext() should return false")
                void instanceIteratorHasNextShouldReturnFalse() throws Exception {
                    // Given
                    Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                    ClassWithAnyInstanceButNoImplementation instance = sut.inject(ClassWithAnyInstanceButNoImplementation.class);
                    // Then
                    assertFalse(instance.getInstance().iterator().hasNext());
                }
            }

            @Nested
            @DisplayName("Instance (not @Any)")
            class Instance {

                @Test
                @DisplayName("Instance.get() should fail")
                void instanceGetShouldFail() throws Exception {
                    // Given
                    Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                    ClassWithInstanceButNoImplementation instance = sut.inject(ClassWithInstanceButNoImplementation.class);
                    // Then
                    assertThrows(RuntimeException.class, () -> instance.getInstance().get());
                }

                @Test
                @DisplayName("Instance.iterator().hasNext() should throw exception")
                void instanceIteratorNextShouldThrowException() throws Exception {
                    // Given
                    Injector sut = new InjectorImpl(TEST_PACKAGE_NAME);
                    ClassWithInstanceButNoImplementation instance = sut.inject(ClassWithInstanceButNoImplementation.class);
                    // Then
                    assertThrows(RuntimeException.class, () -> instance.getInstance().iterator());
                }
            }
        }
    }

    interface TestInterface{}

    enum TestEnum{}

    static class TestClassWithAnnotatedNoArgsConstructor {
        @Inject
        public TestClassWithAnnotatedNoArgsConstructor() { }
    }

    static class TestClassWithNoArgsConstructor {
        @Inject
        public TestClassWithNoArgsConstructor() { }
    }

    static class TestClassWithAnnotatedConstructor {
        private final TestClass testClass;
        @Inject
        public TestClassWithAnnotatedConstructor(TestClass testClass) {
            this.testClass = testClass;
        }
        public TestClass getTestClass() {
            return testClass;
        }
    }
    static class TestClass implements TestInterface {
        public TestClass() { }
    }

    // MUST NOT be static for its test!
    class NonStaticInnerClass { }

    static abstract class AbstractClass { }
    static class ConcreteClass extends AbstractClass { }

    @Singleton static class SingletonClass { }

    static class ClassWithInstance {
        private final Instance<TestInterface> testInterfaceInstance;
        @Inject
        public ClassWithInstance(Instance<TestInterface> testInterfaceInstance) {
            this.testInterfaceInstance = testInterfaceInstance;
        }
        public Instance<TestInterface> getTestInterfaceInstance() {
            return testInterfaceInstance;
        }
    }

    static class ClassWithAnyInstanceAndSingleImplementation {
        private final Instance<SingleImplementationAbstractClass> instance;
        @Inject
        public ClassWithAnyInstanceAndSingleImplementation(Instance<SingleImplementationAbstractClass> instance) {
            this.instance = instance;
        }
        public Instance<SingleImplementationAbstractClass> getInstance() {
            return instance;
        }
    }

    static class ClassWithAnyInstanceAndMultipleImplementations {
        private final Instance<MultipleAnnotatedConcreteClassesAbstractClass> instance;
        @Inject
        public ClassWithAnyInstanceAndMultipleImplementations(@Any Instance<MultipleAnnotatedConcreteClassesAbstractClass> instance) {
            this.instance = instance;
        }
        public Instance<MultipleAnnotatedConcreteClassesAbstractClass> getInstance() {
            return instance;
        }
    }

    static class ClassWithAnyInstanceButNoImplementation {
        private final Instance<NoConcreteClassesAbstractClass> instance;
        @Inject
        public ClassWithAnyInstanceButNoImplementation(@Any Instance<NoConcreteClassesAbstractClass> instance) {
            this.instance = instance;
        }
        public Instance<NoConcreteClassesAbstractClass> getInstance() {
            return instance;
        }
    }

    static class ClassWithInstanceButNoImplementation {
        private final Instance<NoConcreteClassesAbstractClass> instance;
        @Inject
        public ClassWithInstanceButNoImplementation(Instance<NoConcreteClassesAbstractClass> instance) {
            this.instance = instance;
        }
        public Instance<NoConcreteClassesAbstractClass> getInstance() {
            return instance;
        }
    }

    static class ClassWithPrivateConstructor {
        @Inject
        private ClassWithPrivateConstructor() { }
    }

    static class ClassWithInvalidConstructorParameters {
        @Inject
        public ClassWithInvalidConstructorParameters(int i) { }
    }

}
